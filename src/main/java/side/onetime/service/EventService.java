package side.onetime.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import side.onetime.domain.*;
import side.onetime.domain.enums.Category;
import side.onetime.domain.enums.EventStatus;
import side.onetime.dto.event.request.CreateEventRequest;
import side.onetime.dto.event.request.ModifyEventRequest;
import side.onetime.dto.event.response.*;
import side.onetime.dto.schedule.request.GetFilteredSchedulesRequest;
import side.onetime.exception.CustomException;
import side.onetime.exception.status.EventErrorStatus;
import side.onetime.exception.status.EventParticipationErrorStatus;
import side.onetime.exception.status.ScheduleErrorStatus;
import side.onetime.exception.status.UserErrorStatus;
import side.onetime.repository.*;
import side.onetime.util.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Service
@RequiredArgsConstructor
public class EventService {
    private static final int MAX_MOST_POSSIBLE_TIMES_SIZE = 10;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final EventParticipationRepository eventParticipationRepository;
    private final ScheduleRepository scheduleRepository;
    private final SelectionRepository selectionRepository;
    private final ScheduleBatchRepository scheduleBatchRepository;
    private final JwtUtil jwtUtil;
    private final S3Util s3Util;
    private final QrUtil qrUtil;

    /**
     * 비로그인 사용자를 위한 이벤트 생성 메서드.
     *
     * @param createEventRequest 이벤트 생성 요청 데이터
     * @return 생성된 이벤트 응답
     */
    @Transactional
    public CreateEventResponse createEventForAnonymousUser(CreateEventRequest createEventRequest) {
        Event savedEvent = eventRepository.save(createEventRequest.toEntity());
        createAndAddQrCode(savedEvent);
        validateAndSaveSchedules(savedEvent, createEventRequest);
        return CreateEventResponse.of(savedEvent);
    }

    /**
     * 인증된 사용자를 위한 이벤트 생성 메서드.
     *
     * @param createEventRequest 이벤트 생성 요청 데이터
     * @param authorizationHeader 인증된 사용자의 토큰
     * @return 생성된 이벤트 응답
     */
    @Transactional
    public CreateEventResponse createEventForAuthenticatedUser(CreateEventRequest createEventRequest, String authorizationHeader) {
        User user = jwtUtil.getUserFromHeader(authorizationHeader);
        Event savedEvent = eventRepository.save(createEventRequest.toEntity());
        createAndAddQrCode(savedEvent);

        // 이벤트 참여 정보 저장
        EventParticipation eventParticipation = EventParticipation.builder()
                .user(user)
                .event(savedEvent)
                .eventStatus(EventStatus.CREATOR)
                .build();
        eventParticipationRepository.save(eventParticipation);

        validateAndSaveSchedules(savedEvent, createEventRequest);
        return CreateEventResponse.of(savedEvent);
    }

    /**
     * 이벤트 저장 및 QR 코드 생성 후 저장하는 메서드.
     *
     * @param event 생성된 이벤트
     */
    @Async
    public void createAndAddQrCode(Event event) {
        // QR 코드 생성 및 업로드
        String qrFileName = generateAndUploadQrCode(event.getEventId());
        event.addQrFileName(qrFileName);
    }

    /**
     * 날짜/요일 기반 스케줄을 검증 후 저장하는 메서드.
     *
     * @param event 이벤트 객체
     * @param createEventRequest 이벤트 요청 데이터
     */
    private void validateAndSaveSchedules(Event event, CreateEventRequest createEventRequest) {
        if (createEventRequest.category().equals(Category.DATE)) {
            if (!isDateFormat(createEventRequest.ranges().get(0))) {
                throw new CustomException(EventErrorStatus._IS_NOT_DATE_FORMAT);
            }
            createAndSaveDateSchedules(event, createEventRequest.ranges(), createEventRequest.startTime(), createEventRequest.endTime());
        } else {
            if (isDateFormat(createEventRequest.ranges().get(0))) {
                throw new CustomException(EventErrorStatus._IS_NOT_DAY_FORMAT);
            }
            createAndSaveDaySchedules(event, createEventRequest.ranges(), createEventRequest.startTime(), createEventRequest.endTime());
        }
    }

    /**
     * QR 코드를 생성하고 S3에 업로드하는 메서드.
     * 주어진 이벤트 ID를 기반으로 QR 코드를 생성한 후, S3에 업로드합니다.
     *
     * @param eventId QR 코드를 생성할 이벤트의 ID
     * @return S3에 업로드된 QR 코드 파일 이름
     * @throws CustomException QR 코드 생성 또는 S3 업로드 실패 시 발생
     */
    private String generateAndUploadQrCode(UUID eventId) {
        try {
            MultipartFile qrCodeFile = qrUtil.getQrCodeFile(eventId);
            return s3Util.uploadImage("qr", qrCodeFile);
        } catch (Exception e) {
            throw new CustomException(EventErrorStatus._FAILED_GENERATE_QR_CODE);
        }
    }

    /**
     * 날짜 기반 스케줄을 생성하고 저장하는 메서드.
     * 이벤트의 날짜 범위와 시작/종료 시간을 기반으로 모든 가능한 스케줄을 생성합니다.
     *
     * @param event 이벤트 객체
     * @param ranges 날짜 범위 리스트 (예: ["2024.12.10", "2024.12.11"])
     * @param startTime 시작 시간 (HH:mm 포맷)
     * @param endTime 종료 시간 (HH:mm 포맷)
     */
    private void createAndSaveDateSchedules(Event event, List<String> ranges, String startTime, String endTime) {
        List<String> timeSets = DateUtil.createTimeSets(startTime, endTime);
        List<Schedule> schedules = ranges.stream()
                .flatMap(range -> timeSets.stream()
                        .map(time -> Schedule.builder()
                                .event(event)
                                .date(range)
                                .time(time)
                                .build()))
                .collect(Collectors.toList());
        scheduleBatchRepository.insertAll(schedules);
    }

    /**
     * 요일 기반 스케줄을 생성하고 저장하는 메서드.
     * 이벤트의 요일 범위와 시작/종료 시간을 기반으로 모든 가능한 스케줄을 생성합니다.
     *
     * @param event 이벤트 객체
     * @param ranges 요일 범위 리스트 (예: ["MONDAY", "TUESDAY"])
     * @param startTime 시작 시간 (HH:mm 포맷)
     * @param endTime 종료 시간 (HH:mm 포맷)
     */
    private void createAndSaveDaySchedules(Event event, List<String> ranges, String startTime, String endTime) {
        List<String> timeSets = DateUtil.createTimeSets(startTime, endTime);
        List<Schedule> schedules = ranges.stream()
                .flatMap(range -> timeSets.stream()
                        .map(time -> Schedule.builder()
                                .event(event)
                                .day(range)
                                .time(time)
                                .build()))
                .collect(Collectors.toList());
        scheduleBatchRepository.insertAll(schedules);
    }

    /**
     * 이벤트 조회 메서드.
     * 특정 이벤트의 세부 정보를 조회하며, 인증된 유저의 경우 추가 정보를 반환합니다.
     *
     * @param eventId 조회할 이벤트의 ID
     * @param authorizationHeader 인증된 유저의 토큰 (선택 사항)
     * @return 조회된 이벤트의 세부 정보
     * @throws CustomException 이벤트 또는 관련 스케줄을 찾을 수 없는 경우
     */
    @Transactional(readOnly = true)
    public GetEventResponse getEvent(String eventId, String authorizationHeader) {
        Event event = eventRepository.findByEventId(UUID.fromString(eventId))
                .orElseThrow(() -> new CustomException(EventErrorStatus._NOT_FOUND_EVENT));

        List<Schedule> schedules = scheduleRepository.findAllByEvent(event)
                .orElseThrow(() -> new CustomException(ScheduleErrorStatus._NOT_FOUND_ALL_SCHEDULES));

        List<String> ranges = event.getCategory() == Category.DATE
                ? DateUtil.getSortedDateRanges(schedules.stream().map(Schedule::getDate).toList(), "yyyy.MM.dd")
                : DateUtil.getSortedDayRanges(schedules.stream().map(Schedule::getDay).toList());

        EventStatus eventStatus = null;
        if (authorizationHeader != null) {
            User user = jwtUtil.getUserFromHeader(authorizationHeader);
            EventParticipation eventParticipation = eventParticipationRepository.findByUserAndEvent(user, event);
            if (eventParticipation != null) {
                eventStatus = eventParticipation.getEventStatus();
            }
        }

        return GetEventResponse.of(event, ranges, eventStatus);
    }

    /**
     * 이벤트 참여자 조회 메서드.
     * 특정 이벤트에 참여한 모든 참여자의 이름 목록(멤버 및 유저)을 반환합니다.
     *
     * @param eventId 참여자를 조회할 이벤트의 ID
     * @return 참여자의 이름 목록
     * @throws CustomException 이벤트를 찾을 수 없는 경우
     */
    @Transactional(readOnly = true)
    public GetParticipantsResponse getParticipants(String eventId) {
        Event event = eventRepository.findByEventIdWithMembers(UUID.fromString(eventId))
                .orElseThrow(() -> new CustomException(EventErrorStatus._NOT_FOUND_EVENT));

        // 이벤트 참여 상태가 CREATOR가 아닌 유저만 필터링하여 가져오기
        List<User> users = eventParticipationRepository.findAllByEvent(event).stream()
                .filter(eventParticipation -> eventParticipation.getEventStatus() != EventStatus.CREATOR)
                .map(EventParticipation::getUser)
                .toList();

        return GetParticipantsResponse.of(event.getMembers(), users);
    }

    /**
     * 이벤트 참여자 조회 메서드.
     * 특정 이벤트에 참여한 모든 참여자의 이름 목록(멤버 및 유저)을 반환합니다.
     *
     * @param event 참여자를 조회할 이벤트
     * @param eventParticipations 이벤트에 속한 참여자 목록
     * @return 참여자의 이름 목록
     */
    private GetParticipantsResponse getParticipants(Event event, List<EventParticipation> eventParticipations) {
        List<User> users = eventParticipations.stream()
                .filter(eventParticipation -> eventParticipation.getEventStatus() != EventStatus.CREATOR)
                .map(EventParticipation::getUser)
                .toList();

        return GetParticipantsResponse.of(event.getMembers(), users);
    }

    /**
     * 가장 많이 되는 시간 조회 메서드.
     * 특정 이벤트에서 참여자 수가 가장 많은 시간대를 계산하여 반환합니다.
     *
     * @param eventId 조회할 이벤트의 ID
     * @return 가능 인원이 많은 시간대 목록
     * @throws CustomException 이벤트를 찾을 수 없는 경우
     */
    @Transactional(readOnly = true)
    public List<GetMostPossibleTime> getMostPossibleTime(String eventId) {
        // 1. 이벤트 + 멤버 fetch join으로 조회
        Event event = eventRepository.findByEventIdWithMembers(UUID.fromString(eventId))
                .orElseThrow(() -> new CustomException(EventErrorStatus._NOT_FOUND_EVENT));

        // 2. 멤버 이름 리스트
        List<String> memberNames = event.getMembers().stream()
                .map(Member::getName)
                .toList();

        // 3. 참여자(user) 조회 (CREATOR 제외)
        List<String> userNicknames = eventParticipationRepository.findAllByEvent(event).stream()
                .filter(ep -> ep.getEventStatus() != EventStatus.CREATOR)
                .map(ep -> ep.getUser().getNickname())
                .toList();

        // 4. 전체 참여자 이름 통합
        List<String> allParticipants = new ArrayList<>(memberNames);
        allParticipants.addAll(userNicknames);

        // 5. 선택 정보 가져오기
        List<Selection> selections = selectionRepository.findAllSelectionsByEvent(event);

        // 6. 스케줄 → 참여자 이름 리스트 매핑
        Map<Schedule, List<String>> scheduleToNamesMap = buildScheduleToNamesMap(selections, event.getCategory());

        // 7. 최적 시간대 리스트 생성
        List<GetMostPossibleTime> mostPossibleTimes = buildMostPossibleTimes(
                scheduleToNamesMap, allParticipants, event.getCategory());

        return DateUtil.sortMostPossibleTimes(mostPossibleTimes, event.getCategory());
    }

    /**
     * 가장 많이 되는 시간 조회 메서드.
     * 특정 이벤트에서 참여자 수가 가장 많은 시간대를 계산하여 반환합니다.
     *
     * @param event 참여자를 조회할 이벤트
     * @param eventParticipations 이벤트에 속한 참여자 목록
     * @return 가능 인원이 많은 시간대 목록
     */
    private List<GetMostPossibleTime> getMostPossibleTimes(Event event, List<EventParticipation> eventParticipations) {
        List<String> memberNames = event.getMembers().stream()
                .map(Member::getName)
                .toList();

        List<String> userNicknames = eventParticipations.stream()
                .filter(ep -> ep.getEventStatus() != EventStatus.CREATOR)
                .map(ep -> ep.getUser().getNickname())
                .toList();

        List<String> allParticipants = new ArrayList<>(memberNames);
        allParticipants.addAll(userNicknames);

        List<Selection> selections = selectionRepository.findAllSelectionsByEvent(event);

        Map<Schedule, List<String>> scheduleToNamesMap = buildScheduleToNamesMap(selections, event.getCategory());

        List<GetMostPossibleTime> mostPossibleTimes = buildMostPossibleTimes(
                scheduleToNamesMap, allParticipants, event.getCategory());

        return DateUtil.sortMostPossibleTimes(mostPossibleTimes, event.getCategory());
    }

    /**
     * 필터링한 참여자의 가장 많이 되는 시간을 조회하는 메서드.
     * 특정 이벤트에서 전달받은 멤버 및 유저 ID를 기준으로 선택 정보를 필터링하고, 가능한 시간대를 정리하여 반환합니다.
     *
     * @param eventId 조회할 이벤트의 ID
     * @param getFilteredSchedulesRequest 필터링할 스케줄 요청 객체 (유저 ID 목록, 멤버 ID 목록)
     * @return 필터링된 참여자의 가능한 시간대 정보 리스트
     * @throws CustomException 이벤트를 찾을 수 없는 경우
     */
    @Transactional(readOnly = true)
    public List<GetMostPossibleTime> getFilteredMostPossibleTimes(String eventId, GetFilteredSchedulesRequest getFilteredSchedulesRequest) {
        // 1. 이벤트 + 멤버 fetch join으로 조회
        Event event = eventRepository.findByEventIdWithMembers(UUID.fromString(eventId))
                .orElseThrow(() -> new CustomException(EventErrorStatus._NOT_FOUND_EVENT));

        List<Long> memberIds = getFilteredSchedulesRequest.members();
        List<Long> userIds = getFilteredSchedulesRequest.users();

        // 요청 값이 없을 경우, 빈 리스트 반환
        if (memberIds.isEmpty() && userIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 멤버 이름 리스트 (요청된 멤버 ID에 해당하는 이름만 추출)
        List<String> memberNames = event.getMembers().stream()
                .filter(member -> memberIds.contains(member.getId()))
                .map(Member::getName)
                .toList();

        // 3. 참여자(user) 조회 (CREATOR 제외, 요청된 유저 ID에 해당하는 이름만 추출)
        List<String> userNicknames = eventParticipationRepository.findAllByEvent(event).stream()
                .filter(ep -> userIds.contains(ep.getUser().getId()))
                .filter(ep -> ep.getEventStatus() != EventStatus.CREATOR)
                .map(ep -> ep.getUser().getNickname())
                .toList();

        // 4. 전체 참여자 이름 목록 생성 (멤버 + 유저)
        List<String> allParticipants = Stream.concat(memberNames.stream(), userNicknames.stream())
                .collect(Collectors.toList());

        // 5. 선택 정보 조회 (멤버 ID 및 유저 ID 기준)
        List<Selection> allSelections = selectionRepository.findAllByUserIdsOrMemberIdsWithScheduleAndEvent(event, userIds, memberIds);

        // 6. 스케줄 기준으로 참여자 이름 매핑 생성
        Map<Schedule, List<String>> scheduleToNamesMap = buildScheduleToNamesMap(allSelections, event.getCategory());

        // 7. 최적 시간대 리스트 구성
        List<GetMostPossibleTime> mostPossibleTimes = buildMostPossibleTimes(
                scheduleToNamesMap, allParticipants, event.getCategory());

        return DateUtil.sortMostPossibleTimes(mostPossibleTimes, event.getCategory());
    }

    /**
     * 스케줄과 선택된 참여자 이름 매핑 메서드.
     * 이벤트 카테고리에 따라 요일 또는 날짜를 유지하며, 같은 날짜/요일 내에서는 time 기준으로 정렬합니다.
     *
     * @param selections 선택 정보 리스트
     * @param category 이벤트의 카테고리 (DATE 또는 DAY)
     * @return 스케줄과 참여자 이름의 매핑 데이터
     */
    private Map<Schedule, List<String>> buildScheduleToNamesMap(List<Selection> selections, Category category) {
        Map<String, Integer> dayOrderIndex = Map.of(
                "일", 0, "월", 1, "화", 2, "수", 3, "목", 4, "금", 5, "토", 6
        );

        // Comparator 정의
        Comparator<Schedule> scheduleComparator = (s1, s2) -> {
            if (category == Category.DAY) {
                int cmp = Integer.compare(
                        dayOrderIndex.getOrDefault(s1.getDay(), 7),
                        dayOrderIndex.getOrDefault(s2.getDay(), 7)
                );
                if (cmp != 0) return cmp;
            } else {
                int cmp = Comparator.nullsLast(String::compareTo)
                        .compare(s1.getDate(), s2.getDate());
                if (cmp != 0) return cmp;
            }
            return s1.getTime().compareTo(s2.getTime());
        };

        // 정렬된 TreeMap 사용
        Map<Schedule, List<String>> scheduleToNamesMap = new TreeMap<>(scheduleComparator);

        for (Selection selection : selections) {
            Schedule schedule = selection.getSchedule();
            if (schedule == null) continue;

            String name = selection.getMember() != null
                    ? selection.getMember().getName()
                    : (selection.getUser() != null ? selection.getUser().getNickname() : null);

            if (name == null) continue;

            scheduleToNamesMap.computeIfAbsent(schedule, k -> new ArrayList<>()).add(name);
        }

        return scheduleToNamesMap;
    }

    /**
     * 참여 인원이 많은 순서대로 정렬된 시간대 리스트 생성 메서드.
     * 참여 가능한 인원이 많은 시간대 순으로 시간대 리스트를 생성합니다.
     *
     * @param scheduleToNamesMap 스케줄과 참여자 이름 매핑 데이터
     * @param allMembersName 이벤트 참여자의 전체 이름 목록
     * @param category 이벤트의 카테고리 (날짜 또는 요일)
     * @return 참여 인원이 많은 순서대로 정렬된 시간대 리스트
     */
    private List<GetMostPossibleTime> buildMostPossibleTimes(Map<Schedule, List<String>> scheduleToNamesMap, List<String> allMembersName, Category category) {
        List<GetMostPossibleTime> mostPossibleTimes = new ArrayList<>();
        GetMostPossibleTime previousTime = null;

        // 참여자 많은 순으로 정렬
        List<Map.Entry<Schedule, List<String>>> sortedEntries = new ArrayList<>(scheduleToNamesMap.entrySet());
        sortedEntries.sort((a, b) -> Integer.compare(b.getValue().size(), a.getValue().size()));

        for (Map.Entry<Schedule, List<String>> entry : sortedEntries) {
            Schedule schedule = entry.getKey();
            List<String> curNames = entry.getValue();

            if (canMergeWithPrevious(previousTime, schedule, curNames, category)) {
                // 이전 시간대와 병합 가능한 경우
                previousTime = previousTime.updateEndTime(schedule.getTime());
                mostPossibleTimes.set(mostPossibleTimes.size() - 1, previousTime); // 종료 시간을 더해 업데이트
            } else {
                // 새로운 시간대를 추가하는 경우
                if (mostPossibleTimes.size() == MAX_MOST_POSSIBLE_TIMES_SIZE) {
                    // 10개를 찾았을 시 종료
                    break;
                }
                List<String> impossibleNames = new ArrayList<>();
                // 유저, 멤버 동명이인을 고려하기 위함
                List<String> curNamesCopy = new ArrayList<>(curNames);
                for (String name : allMembersName) {
                    if (curNamesCopy.contains(name)) {
                        curNamesCopy.remove(name);
                    } else {
                        impossibleNames.add(name);
                    }
                }

                // 새로운 시간대를 추가
                GetMostPossibleTime newTime = createMostPossibleTime(schedule, curNames, impossibleNames, category);
                mostPossibleTimes.add(newTime);
                previousTime = newTime;
            }
        }
        return mostPossibleTimes;
    }

    /**
     * 이전 시간대와 병합 가능 여부 확인 메서드.
     * 현재 시간대가 이전 시간대와 연속적이며 참여 가능한 인원이 동일한지 확인합니다.
     *
     * @param previousTime 이전 시간대 정보
     * @param schedule 현재 스케줄
     * @param curNames 현재 시간대의 참여자 이름 목록
     * @param category 이벤트의 카테고리 (날짜 또는 요일)
     * @return 병합 가능 여부
     */
    private boolean canMergeWithPrevious(GetMostPossibleTime previousTime, Schedule schedule, List<String> curNames, Category category) {
        if (previousTime == null) return false;

        boolean isSameTimePoint = category.equals(Category.DAY)
                ? previousTime.timePoint().equals(schedule.getDay())
                : previousTime.timePoint().equals(schedule.getDate());

        return isSameTimePoint
                && previousTime.endTime().equals(schedule.getTime())
                && new HashSet<>(previousTime.possibleNames()).containsAll(curNames);
    }

    /**
     * 새로운 시간대 객체 생성 메서드.
     * 주어진 스케줄 정보와 참여자 데이터를 기반으로 새로운 시간대 객체를 생성합니다.
     *
     * @param schedule 스케줄 정보
     * @param curNames 참여 가능한 이름 목록
     * @param impossibleNames 참여 불가능한 이름 목록
     * @param category 이벤트의 카테고리 (날짜 또는 요일)
     * @return 생성된 시간대 객체
     */
    private GetMostPossibleTime createMostPossibleTime(Schedule schedule, List<String> curNames, List<String> impossibleNames, Category category) {
        return category.equals(Category.DAY)
                ? GetMostPossibleTime.dayOf(schedule, curNames, impossibleNames)
                : GetMostPossibleTime.dateOf(schedule, curNames, impossibleNames);
    }

    /**
     * 날짜 포맷 여부 검증 메서드.
     * 주어진 문자열이 날짜 형식인지 확인합니다.
     *
     * @param range 검증할 문자열
     * @return 날짜 형식 여부
     */
    private boolean isDateFormat(String range) {
        return Character.isDigit(range.charAt(0));
    }

    /**
     * 유저 참여 이벤트 반환 메서드.
     * 인증된 유저가 참여한 모든 이벤트 목록을 조회하며, 각 이벤트에 대한 세부 정보를 반환합니다.
     *
     * @return 유저가 참여한 이벤트 목록
     */
    @Transactional(readOnly = true)
    public List<GetUserParticipatedEventsResponse> getUserParticipatedEvents() {
        User user = userRepository.findById(UserAuthorizationUtil.getLoginUserId())
                .orElseThrow(() -> new CustomException(UserErrorStatus._NOT_FOUND_USER));

        List<EventParticipation> participations = eventParticipationRepository.findAllByUserWithEvent(user);

        // 캐시 맵 선언
        Map<String, GetParticipantsResponse> participantsCache = new HashMap<>();
        Map<String, List<GetMostPossibleTime>> mostPossibleCache = new HashMap<>();

        return participations.stream()
                .sorted(Comparator.comparing((EventParticipation ep) -> ep.getEvent().getCreatedDate()).reversed())
                .map(ep -> {
                    Event event = ep.getEvent();
                    String eventId = event.getEventId().toString();

                    // 캐시 또는 메서드 실행
                    GetParticipantsResponse participants = participantsCache.computeIfAbsent(
                            eventId, this::getParticipants);
                    List<GetMostPossibleTime> mostPossibleTimes = mostPossibleCache.computeIfAbsent(
                            eventId, this::getMostPossibleTime);

                    return GetUserParticipatedEventsResponse.of(
                            event,
                            ep,
                            participants.users().size() + participants.members().size(),
                            mostPossibleTimes
                    );
                })
                .collect(Collectors.toList());
    }

    /**
     * 유저 참여 이벤트 목록 조회 메서드.
     *
     * 인증된 유저가 참여한 이벤트 목록을 페이지 단위로 조회하며, 각 이벤트에 대한 세부 정보를 반환합니다.
     * 각 이벤트에 대해 참여자 및 가장 많이 되는 시간을 조회한 뒤 페이지(커서) 정보를 감싸 반환합니다.
     * 이벤트는 항상 최신 순으로 정렬됩니다.
     *
     * @param size 한 번에 가져올 이벤트 개수
     * @param createdDate 마지막으로 조회한 이벤트 생성일
     * @return 유저가 참여한 이벤트 목록
     */
    @Transactional(readOnly = true)
    public GetAllUserParticipatedEventsResponse getAllUserParticipatedEvents(int size, LocalDateTime createdDate) {
        User user = userRepository.findById(UserAuthorizationUtil.getLoginUserId())
                .orElseThrow(() -> new CustomException(UserErrorStatus._NOT_FOUND_USER));

        List<EventParticipation> participations = eventParticipationRepository.pageAllByUserWithCursor(user, createdDate, size);
        List<GetUserParticipatedEventResponse> userParticipatedEvents = participations.stream()
                .map(ep -> {
                    Event event = ep.getEvent();
                    List<EventParticipation> eventParticipations = eventParticipationRepository.findAllByEventWithEventAndMemberAndUser(event);

                    GetParticipantsResponse participants = this.getParticipants(event, eventParticipations);
                    List<GetMostPossibleTime> mostPossibleTimes = this.getMostPossibleTimes(event, eventParticipations);

                    return GetUserParticipatedEventResponse.of(
                            event,
                            ep,
                            participants.users().size() + participants.members().size(),
                            mostPossibleTimes
                    );
                })
                .toList();

        String nextCursor = participations.isEmpty() ? null : participations.get(participations.size() - 1).getEvent().getCreatedDate().toString();
        boolean hasNext = participations.size() == size;
        PageCursorInfo<String> pageCursorInfo = PageCursorInfo.of(nextCursor, hasNext);

        return GetAllUserParticipatedEventsResponse.of(userParticipatedEvents, pageCursorInfo);
    }


    /**
     * 유저가 생성한 이벤트 삭제 메서드.
     * 인증된 유저가 생성한 특정 이벤트를 삭제합니다.
     *
     * @param eventId 삭제할 이벤트의 ID
     */
    @Transactional
    public void removeUserCreatedEvent(String eventId) {
        User user = userRepository.findById(UserAuthorizationUtil.getLoginUserId())
                .orElseThrow(() -> new CustomException(UserErrorStatus._NOT_FOUND_USER));
        EventParticipation eventParticipation = verifyUserHasEventAccess(user, eventId);
        eventRepository.deleteEvent(eventParticipation.getEvent());
        s3Util.deleteFile(eventParticipation.getEvent().getQrFileName()); // QR 이미지 삭제
    }

    /**
     * 이벤트 수정 메서드.
     * 특정 이벤트를 수정합니다.
     *
     * @param eventId 수정할 이벤트의 ID
     * @param modifyEventRequest 새로운 이벤트 데이터
     */
    @Transactional
    public void modifyEvent(String eventId, ModifyEventRequest modifyEventRequest) {
        Event event = eventRepository.findByEventId(UUID.fromString(eventId))
                .orElseThrow(() -> new CustomException(EventErrorStatus._NOT_FOUND_EVENT));

        event.updateTitle(modifyEventRequest.title());
        updateEventRanges(event, event.getSchedules(), modifyEventRequest.ranges(), modifyEventRequest.startTime(), modifyEventRequest.endTime());

        // 변경된 범위에 따른 새로운 스케줄 목록
        List<Schedule> newSchedules = scheduleRepository.findAllByEvent(event)
                .orElseThrow(() -> new CustomException(ScheduleErrorStatus._NOT_FOUND_ALL_SCHEDULES));
        updateEventTimes(event, newSchedules, modifyEventRequest.startTime(), modifyEventRequest.endTime());
    }

    /**
     * 이벤트 범위 업데이트 메서드.
     * 기존의 범위를 새로운 범위로 업데이트하며, 삭제 및 생성 대상 범위를 처리합니다.
     *
     * @param event 이벤트 객체
     * @param schedules 기존 스케줄 목록
     * @param newRanges 새로운 범위 리스트
     * @param newStartTime 새로 설정할 시작 시간
     * @param newEndTime 새로 설정할 종료 시간
     */
    private void updateEventRanges(Event event, List<Schedule> schedules, List<String> newRanges, String newStartTime, String newEndTime) {
        Set<String> existRanges = event.getCategory() == Category.DATE
                ? schedules.stream().map(Schedule::getDate).filter(Objects::nonNull).collect(Collectors.toSet())
                : schedules.stream().map(Schedule::getDay).filter(Objects::nonNull).collect(Collectors.toSet());

        // 삭제 대상 처리
        List<String> rangesToDelete = existRanges.stream()
                .filter(range -> !newRanges.contains(range))
                .toList();
        eventRepository.deleteSchedulesByRanges(event, rangesToDelete);

        // 생성 대상 처리
        List<String> rangesToCreate = newRanges.stream()
                .filter(range -> !existRanges.contains(range))
                .collect(Collectors.toList());

        if (!rangesToCreate.isEmpty()) {
            if (event.getCategory() == Category.DATE) {
                createAndSaveDateSchedules(event, rangesToCreate, newStartTime, newEndTime);
            } else if (event.getCategory() == Category.DAY) {
                createAndSaveDaySchedules(event, rangesToCreate, newStartTime, newEndTime);
            }
        }
        scheduleRepository.flush();
    }

    /**
     * 이벤트 시간 업데이트 메서드.
     * 기존의 시간대를 새로운 시간대로 업데이트하며, 삭제 및 생성 대상 시간대를 처리합니다.
     *
     * @param event 이벤트 객체
     * @param schedules 기존 스케줄 목록
     * @param newStartTime 새로 설정할 시작 시간
     * @param newEndTime 새로 설정할 종료 시간
     */
    private void updateEventTimes(Event event, List<Schedule> schedules, String newStartTime, String newEndTime) {
        if (!event.getStartTime().equals(newStartTime) || !event.getEndTime().equals(newEndTime)) {
            List<String> newTimeSets = DateUtil.createTimeSets(newStartTime, newEndTime);

            Set<String> existTimes = schedules.stream()
                    .map(Schedule::getTime)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            // 삭제 대상 시간 처리
            List<String> timesToDelete = existTimes.stream()
                    .filter(time -> !newTimeSets.contains(time))
                    .toList();
            eventRepository.deleteSchedulesByTimes(event, timesToDelete);

            // 생성 대상 시간 처리
            List<String> timesToCreate = newTimeSets.stream()
                    .filter(newTime -> !existTimes.contains(newTime))
                    .toList();

            if (!timesToCreate.isEmpty()) {
                if (event.getCategory() == Category.DATE) {
                    createSchedulesForTime(event, extractExistingRanges(schedules, event.getCategory()), timesToCreate, true);
                } else if (event.getCategory() == Category.DAY) {
                    createSchedulesForTime(event, extractExistingRanges(schedules, event.getCategory()), timesToCreate, false);
                }
            }

            event.updateStartTime(newStartTime);
            event.updateEndTime(newEndTime);
        }
    }

    /**
     * 기존 범위 추출 메서드.
     * 스케줄 목록에서 현재 존재하는 날짜 또는 요일 범위를 추출합니다.
     *
     * @param schedules 스케줄 목록
     * @param category 이벤트의 카테고리 (날짜 또는 요일)
     * @return 현재 존재하는 범위 집합
     */
    private Set<String> extractExistingRanges(List<Schedule> schedules, Category category) {
        return category == Category.DATE
                ? schedules.stream().map(Schedule::getDate).filter(Objects::nonNull).collect(Collectors.toSet())
                : schedules.stream().map(Schedule::getDay).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    /**
     * 시간 기반 스케줄 생성 메서드.
     * 시간대를 기반으로 새로운 스케줄을 생성하고 저장합니다.
     *
     * @param event 이벤트 객체
     * @param ranges 범위 리스트 (날짜 또는 요일)
     * @param timesToCreate 새로 생성할 시간 리스트
     * @param isDateBased 날짜 기반 여부
     */
    private void createSchedulesForTime(Event event, Set<String> ranges, List<String> timesToCreate, boolean isDateBased) {
        List<Schedule> newSchedules = ranges.stream()
                .flatMap(range -> timesToCreate.stream()
                        .map(time -> Schedule.builder()
                                .event(event)
                                .date(isDateBased ? range : null)
                                .day(!isDateBased ? range : null)
                                .time(time)
                                .build()))
                .collect(Collectors.toList());
        scheduleBatchRepository.insertAll(newSchedules);
    }

    /**
     * 유저가 이벤트의 생성자인지 검증하는 메서드.
     * 인증된 유저가 특정 이벤트의 생성자이거나 특정 권한을 가진 상태인지 확인하고, 관련 정보를 반환합니다.
     *
     * @param user 인증된 사용자 정보
     * @param eventId 확인할 이벤트의 ID
     * @return 이벤트 참여 정보
     * @throws CustomException 유저가 참여자로만 등록된 경우 또는 참여 정보를 찾을 수 없는 경우
     */
    private EventParticipation verifyUserHasEventAccess(User user, String eventId) {
        Event event = eventRepository.findByEventId(UUID.fromString(eventId))
                .orElseThrow(() -> new CustomException(EventErrorStatus._NOT_FOUND_EVENT));

        EventParticipation eventParticipation = eventParticipationRepository.findByUserAndEvent(user, event);
        if (eventParticipation == null) {
            throw new CustomException(EventParticipationErrorStatus._NOT_FOUND_EVENT_PARTICIPATION);
        }
        if (EventStatus.PARTICIPANT.equals(eventParticipation.getEventStatus())) {
            throw new CustomException(EventParticipationErrorStatus._IS_NOT_AUTHORIZED_EVENT_PARTICIPATION);
        }

        return eventParticipation;
    }

    /**
     * 이벤트 QR Code 조회 메서드.
     * 특정 이벤트의 QR 코드 이미지를 S3에서 가져와 URL을 반환합니다.
     *
     * @param eventId QR 코드를 조회할 이벤트의 ID
     * @return QR 코드 이미지 URL
     * @throws CustomException 이벤트 또는 QR 코드를 찾을 수 없는 경우
     */
    @Transactional(readOnly = true)
    public GetEventQrCodeResponse getEventQrCode(String eventId) {
        Event event = eventRepository.findByEventId(UUID.fromString(eventId))
                .orElseThrow(() -> new CustomException(EventErrorStatus._NOT_FOUND_EVENT));
        if (event.getQrFileName() == null) {
            throw new CustomException(EventErrorStatus._NOT_FOUND_EVENT_QR_CODE);
        }

        String qrCodeImgUrl = s3Util.getPublicUrl(event.getQrFileName());
        return GetEventQrCodeResponse.from(qrCodeImgUrl);
    }
}
