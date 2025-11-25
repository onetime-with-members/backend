package side.onetime.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import side.onetime.domain.FixedSchedule;
import side.onetime.domain.FixedSelection;
import side.onetime.domain.User;
import side.onetime.dto.fixed.request.UpdateFixedScheduleRequest;
import side.onetime.dto.fixed.response.FixedScheduleResponse;
import side.onetime.dto.fixed.response.GetFixedScheduleResponse;
import side.onetime.exception.CustomException;
import side.onetime.exception.status.FixedErrorStatus;
import side.onetime.exception.status.UserErrorStatus;
import side.onetime.infra.everytime.client.EverytimeApiClient;
import side.onetime.repository.FixedScheduleRepository;
import side.onetime.repository.FixedSelectionRepository;
import side.onetime.repository.UserRepository;
import side.onetime.util.UserAuthorizationUtil;

@Service
@RequiredArgsConstructor
public class FixedScheduleService {

	private static final int EVERYTIME_PRIVATE_STATUS = -2;
	private static final int EVERYTIME_PUBLIC_STATUS = 1;

    private final UserRepository userRepository;
    private final FixedScheduleRepository fixedScheduleRepository;
    private final FixedSelectionRepository fixedSelectionRepository;
	private final EverytimeApiClient everytimeApiClient;

    /**
     * 유저의 고정 스케줄 조회 메서드.
     *
     * 유저의 모든 고정 스케줄을 조회하여 요일별 그룹화하여 반환합니다.
     *
     * @return 유저의 고정 스케줄 목록
     */
    @Transactional(readOnly = true)
    public GetFixedScheduleResponse getUserFixedSchedule() {
        User user = userRepository.findById(UserAuthorizationUtil.getLoginUserId())
                .orElseThrow(() -> new CustomException(UserErrorStatus._NOT_FOUND_USER));
        List<FixedSelection> fixedSelections = fixedSelectionRepository.findAllByUser(user);

        Map<String, List<String>> groupedSchedules = fixedSelections.stream()
                .collect(Collectors.groupingBy(
                        selection -> selection.getFixedSchedule().getDay(),
                        Collectors.mapping(selection -> selection.getFixedSchedule().getTime(), Collectors.toList())
                ));

        List<FixedScheduleResponse> scheduleResponses = groupedSchedules.entrySet().stream()
                .map(entry -> FixedScheduleResponse.of(entry.getKey(), entry.getValue()))
                .toList();

        return new GetFixedScheduleResponse(scheduleResponses);
    }

    /**
     * 유저의 고정 스케줄 수정 메서드.
     *
     * 기존에 저장된 유저의 고정 스케줄을 모두 삭제한 후, 새로운 스케줄을 등록합니다.
     *
     * @param request 유저가 입력한 새로운 스케줄 목록
     */
    @Transactional
    public void updateUserFixedSchedules(UpdateFixedScheduleRequest request) {
        User user = userRepository.findById(UserAuthorizationUtil.getLoginUserId())
                .orElseThrow(() -> new CustomException(UserErrorStatus._NOT_FOUND_USER));
        // 기존 고정 스케줄 삭제
        fixedSelectionRepository.deleteFixedSelectionsByUser(user);

        List<FixedSelection> newFixedSelections = new ArrayList<>();

        for (FixedScheduleResponse fixedScheduleResponse : request.schedules()) {
            String day = fixedScheduleResponse.timePoint();
            List<String> times = fixedScheduleResponse.times();

            List<FixedSchedule> fixedSchedules = fixedScheduleRepository.findAllByDay(day)
                    .orElseThrow(() -> new CustomException(FixedErrorStatus._NOT_FOUND_FIXED_SCHEDULES));

            for (FixedSchedule fixedSchedule : fixedSchedules) {
                if (times.contains(fixedSchedule.getTime())) {
                    newFixedSelections.add(FixedSelection.builder()
                            .user(user)
                            .fixedSchedule(fixedSchedule)
                            .build());
                }
            }
        }

        fixedSelectionRepository.saveAll(newFixedSelections);
    }

	/**
	 * 에브리타임 시간표 조회 메서드
	 *
	 * 1. Feign으로 XML 호출
	 * 2. Jsoup으로 XML 파싱
	 */
	public GetFixedScheduleResponse getUserEverytimeTimetable(String identifier) {
		// 1. Feign Client로 XML 데이터 요청
		String xmlResponse = fetchTimetableXml(identifier);

		// 2. Jsoup으로 XML 파싱
		List<FixedScheduleResponse> scheduleResponses = parseXmlToSchedules(xmlResponse);

		return new GetFixedScheduleResponse(scheduleResponses);
	}

	/**
	 * Feign Client를 통해 에브리타임 XML을 요청합니다.
	 */
	private String fetchTimetableXml(String identifier) {
		String xmlResponse;

		try {
			// Feign Client 호출
			xmlResponse = everytimeApiClient.getUserTimetable(identifier);
		} catch (Exception e) {
			throw new CustomException(FixedErrorStatus._EVERYTIME_API_FAILED);
		}

		if (!xmlResponse.contains("subject")) {
			// 200 OK 응답이 왔지만, 테이블이 비어있는 경우
			int status = extractStatusFromXml(xmlResponse);
			if (EVERYTIME_PRIVATE_STATUS == status) {
				// 1. 공개 범위가 '전체 공개'가 아닌 경우
				throw new CustomException(FixedErrorStatus._EVERYTIME_TIMETABLE_NOT_PUBLIC);
			} else if (EVERYTIME_PUBLIC_STATUS == status) {
				// 2. '전체 공개'이지만, 등록된 수업이 없는 경우
				throw new CustomException(FixedErrorStatus._NOT_FOUND_EVERYTIME_TIMETABLE);
			} else {
				// 3. 예상치 못한 status 값
				throw new CustomException(FixedErrorStatus._EVERYTIME_TIMETABLE_PARSE_ERROR);
			}
		}

		return xmlResponse;
	}

	/**
	 * XML 문자열에서 status 속성값을 추출합니다.
	 * 예: <table ... status="1" ... /> -> 1 반환
	 */
	private int extractStatusFromXml(String xml) {
		// status="숫자" 패턴을 찾음
		Pattern pattern = Pattern.compile("status=\"(-?\\d+)\"");
		Matcher matcher = pattern.matcher(xml);

		if (matcher.find()) {
			try {
				return Integer.parseInt(matcher.group(1));
			} catch (NumberFormatException e) {
				// 숫자가 아닌 경우 파싱 에러 처리
				throw new CustomException(FixedErrorStatus._EVERYTIME_TIMETABLE_PARSE_ERROR);
			}
		}

		// status 속성을 찾지 못한 경우 파싱 에러 처리
		throw new CustomException(FixedErrorStatus._EVERYTIME_TIMETABLE_PARSE_ERROR);
	}

	/**
	 * Jsoup을 사용하여 XML을 파싱하고 DTO 리스트로 변환합니다.
	 */
	private List<FixedScheduleResponse> parseXmlToSchedules(String xmlResponse) {
		Map<String, Set<String>> schedulesByDay = new TreeMap<>();

		try {
			Document doc = Jsoup.parse(xmlResponse, "", Parser.xmlParser());
			Elements subjects = doc.select("subject");

			for (Element subject : subjects) {
				for (Element data : subject.select("time > data")) {
					String dayName = convertDayCodeToName(data.attr("day"));
					if (dayName.equals("알 수 없음")) {
						continue;
					}

					// 속성 검증 및 파싱 안정성 확보
					String startTimeStr = data.attr("starttime");
					String endTimeStr = data.attr("endtime");

					// 1. 속성 누락/빈 값 검증
					if (startTimeStr.isEmpty() || endTimeStr.isEmpty()) {
						continue;
					}

					int startMinutes;
					int endMinutes;

					try {
						startMinutes = Integer.parseInt(startTimeStr) * 5; // (분 / 5) -> 분
						endMinutes = Integer.parseInt(endTimeStr) * 5;   // (분 / 5) -> 분
					} catch (NumberFormatException e) {
						// 2. 숫자가 아닌 값이 들어왔을 경우 스킵
						continue;
					}

					// 3. 시간 범위 및 논리적 오류 검증 (0분 미만, 24시간 초과, 시작 >= 종료)
					final int MINUTES_IN_DAY = 1440; // 24 * 60
					if (startMinutes >= endMinutes || startMinutes < 0 || endMinutes > MINUTES_IN_DAY) {
						continue;
					}

					// 해당 요일의 Set을 가져오거나 새로 생성 (시간 정렬)
					Set<String> timeSlots = schedulesByDay.computeIfAbsent(dayName, k -> new TreeSet<>());

					// 30분 단위로 쪼개서 Set에 추가
					generateTimeSlots(timeSlots, startMinutes, endMinutes);
				}
			}
		} catch (Exception e) {
			throw new CustomException(FixedErrorStatus._EVERYTIME_TIMETABLE_PARSE_ERROR);
		}

		return schedulesByDay.entrySet().stream()
			.map(entry -> FixedScheduleResponse.of(entry.getKey(), new ArrayList<>(entry.getValue())))
			.collect(Collectors.toList());
	}

	/**
	 * XML의 day 코드를 요일 이름으로 변환합니다. ("0" -> "월")
	 */
	private String convertDayCodeToName(String dayCode) {
		return switch (dayCode) {
			case "0" -> "월";
			case "1" -> "화";
			case "2" -> "수";
			case "3" -> "목";
			case "4" -> "금";
			case "5" -> "토";
			case "6" -> "일";
			default -> "알 수 없음";
		};
	}

	/**
	 * 시작/종료 분을 기준으로 30분 단위 시간 문자열을 생성하여 Set에 추가합니다.
	 */
	private void generateTimeSlots(Set<String> timeSlots, int startMinutes, int endMinutes) {
		for (int min = startMinutes; min < endMinutes; min += 30) {
			int hour = min / 60;
			int minute = min % 60;
			timeSlots.add(String.format("%02d:%02d", hour, minute));
		}
	}
}
