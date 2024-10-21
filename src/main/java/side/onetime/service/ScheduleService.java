package side.onetime.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import side.onetime.domain.*;
import side.onetime.domain.enums.EventStatus;
import side.onetime.dto.event.response.GetParticipantsResponse;
import side.onetime.dto.schedule.request.CreateDateScheduleRequest;
import side.onetime.dto.schedule.request.CreateDayScheduleRequest;
import side.onetime.dto.schedule.request.GetFilteredSchedulesRequest;
import side.onetime.dto.schedule.response.DateSchedule;
import side.onetime.dto.schedule.response.DaySchedule;
import side.onetime.dto.schedule.response.PerDateSchedulesResponse;
import side.onetime.dto.schedule.response.PerDaySchedulesResponse;
import side.onetime.exception.CustomException;
import side.onetime.exception.status.EventErrorStatus;
import side.onetime.exception.status.MemberErrorStatus;
import side.onetime.exception.status.ScheduleErrorStatus;
import side.onetime.repository.*;
import side.onetime.util.JwtUtil;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScheduleService {
    private final EventRepository eventRepository;
    private final EventParticipationRepository eventParticipationRepository;
    private final MemberRepository memberRepository;
    private final ScheduleRepository scheduleRepository;
    private final SelectionRepository selectionRepository;
    private final JwtUtil jwtUtil;
    private final EventService eventService;

    // 요일 스케줄 등록 메서드 (비로그인)
    @Transactional
    public void createDaySchedulesForAnonymousUser(CreateDayScheduleRequest createDayScheduleRequest) {
        Event event = eventRepository.findByEventId(UUID.fromString(createDayScheduleRequest.eventId()))
                .orElseThrow(() -> new CustomException(EventErrorStatus._NOT_FOUND_EVENT));
        Member member = memberRepository.findByMemberId(UUID.fromString(createDayScheduleRequest.memberId()))
                .orElseThrow(() -> new CustomException(MemberErrorStatus._NOT_FOUND_MEMBER));

        List<DaySchedule> daySchedules = createDayScheduleRequest.daySchedules();
        List<Selection> selections = new ArrayList<>();
        for (DaySchedule daySchedule : daySchedules) {
            String day = daySchedule.day();
            List<String> times = daySchedule.times();
            List<Schedule> schedules = scheduleRepository.findAllByEventAndDay(event, day)
                    .orElseThrow(() -> new CustomException(ScheduleErrorStatus._NOT_FOUND_DAY_SCHEDULES));

            for (Schedule schedule : schedules) {
                if (times.contains(schedule.getTime())) {
                    selections.add(Selection.builder()
                            .member(member)
                            .schedule(schedule)
                            .build());
                }
            }
        }
        selectionRepository.deleteAllByMember(member);
        selectionRepository.flush();
        selectionRepository.saveAll(selections);
    }

    // 요일 스케줄 등록 메서드 (로그인)
    @Transactional
    public void createDaySchedulesForAuthenticatedUser(CreateDayScheduleRequest createDayScheduleRequest, String authorizationHeader) {
        Event event = eventRepository.findByEventId(UUID.fromString(createDayScheduleRequest.eventId()))
                .orElseThrow(() -> new CustomException(EventErrorStatus._NOT_FOUND_EVENT));
        User user = jwtUtil.getUserFromHeader(authorizationHeader);
        // 참여 정보가 없는 경우 참여자로 저장
        eventParticipationRepository.findByUserAndEvent(user, event)
                .orElseGet(() -> eventParticipationRepository.save(
                        EventParticipation.builder()
                                .user(user)
                                .event(event)
                                .eventStatus(EventStatus.PARTICIPANT)
                                .build()
                ));

        List<DaySchedule> daySchedules = createDayScheduleRequest.daySchedules();
        List<Selection> newSelections = new ArrayList<>();
        List<Schedule> allSchedules = new ArrayList<>();

        for (DaySchedule daySchedule : daySchedules) {
            String day = daySchedule.day();
            List<String> times = daySchedule.times();
            List<Schedule> schedules = scheduleRepository.findAllByEventAndDay(event, day)
                    .orElseThrow(() -> new CustomException(ScheduleErrorStatus._NOT_FOUND_DAY_SCHEDULES));

            for (Schedule schedule : schedules) {
                if (times.contains(schedule.getTime())) {
                    newSelections.add(Selection.builder()
                            .user(user)
                            .schedule(schedule)
                            .build());
                }
            }
            allSchedules.addAll(schedules);
        }
        selectionRepository.deleteAllByUserAndScheduleIn(user, allSchedules);
        selectionRepository.saveAll(newSelections);
    }

    // 날짜 스케줄 등록 메서드 (비로그인)
    @Transactional
    public void createDateSchedulesForAnonymousUser(CreateDateScheduleRequest createDateScheduleRequest) {
        Event event = eventRepository.findByEventId(UUID.fromString(createDateScheduleRequest.eventId()))
                .orElseThrow(() -> new CustomException(EventErrorStatus._NOT_FOUND_EVENT));
        Member member = memberRepository.findByMemberId(UUID.fromString(createDateScheduleRequest.memberId()))
                .orElseThrow(() -> new CustomException(MemberErrorStatus._NOT_FOUND_MEMBER));

        List<DateSchedule> dateSchedules = createDateScheduleRequest.dateSchedules();
        List<Selection> selections = new ArrayList<>();
        for (DateSchedule dateSchedule : dateSchedules) {
            String date = dateSchedule.date();
            List<String> times = dateSchedule.times();
            List<Schedule> schedules = scheduleRepository.findAllByEventAndDate(event, date)
                    .orElseThrow(() -> new CustomException(ScheduleErrorStatus._NOT_FOUND_DATE_SCHEDULES));

            for (Schedule schedule : schedules) {
                if (times.contains(schedule.getTime())) {
                    selections.add(Selection.builder()
                            .member(member)
                            .schedule(schedule)
                            .build());
                }
            }
        }
        selectionRepository.deleteAllByMember(member);
        selectionRepository.flush();
        selectionRepository.saveAll(selections);
    }

    // 날짜 스케줄 등록 메서드 (로그인)
    @Transactional
    public void createDateSchedulesForAuthenticatedUser(CreateDateScheduleRequest createDateScheduleRequest, String authorizationHeader) {
        Event event = eventRepository.findByEventId(UUID.fromString(createDateScheduleRequest.eventId()))
                .orElseThrow(() -> new CustomException(EventErrorStatus._NOT_FOUND_EVENT));
        User user = jwtUtil.getUserFromHeader(authorizationHeader);
        // 참여 정보가 없는 경우 참여자로 저장
        eventParticipationRepository.findByUserAndEvent(user, event)
                .orElseGet(() -> eventParticipationRepository.save(
                        EventParticipation.builder()
                                .user(user)
                                .event(event)
                                .eventStatus(EventStatus.PARTICIPANT)
                                .build()
                ));

        List<DateSchedule> dateSchedules = createDateScheduleRequest.dateSchedules();
        List<Selection> newSelections = new ArrayList<>();
        List<Schedule> allSchedules = new ArrayList<>();

        for (DateSchedule dateSchedule : dateSchedules) {
            String date = dateSchedule.date();
            List<String> times = dateSchedule.times();

            List<Schedule> schedules = scheduleRepository.findAllByEventAndDate(event, date)
                    .orElseThrow(() -> new CustomException(ScheduleErrorStatus._NOT_FOUND_DATE_SCHEDULES));

            for (Schedule schedule : schedules) {
                if (times.contains(schedule.getTime())) {
                    newSelections.add(Selection.builder()
                            .user(user)
                            .schedule(schedule)
                            .build());
                }
            }

            allSchedules.addAll(schedules);
        }

        selectionRepository.deleteAllByUserAndScheduleIn(user, allSchedules);
        selectionRepository.saveAll(newSelections);
    }

    // 전체 요일 스케줄 반환 메서드
    @Transactional(readOnly = true)
    public List<PerDaySchedulesResponse> getAllDaySchedules(String eventId) {
        Event event = eventRepository.findByEventId(UUID.fromString(eventId))
                .orElseThrow(() -> new CustomException(EventErrorStatus._NOT_FOUND_EVENT));

        // 이벤트에 참여하는 모든 멤버
        List<Member> members = memberRepository.findAllWithSelectionsAndSchedulesByEvent(event);

        // 이벤트에 참여하는 모든 참여자 목록
        GetParticipantsResponse getParticipantsResponse = eventService.getParticipants(eventId);
        List<String> participantNames = getParticipantsResponse.names();

        // 이벤트에 참여하는 모든 유저
        List<EventParticipation> eventParticipations = eventParticipationRepository.findAllByEvent(event);
        List<User> users = eventParticipations.stream()
                .map(EventParticipation::getUser)
                .filter(user -> participantNames.contains(user.getNickname())) // 유저가 참여자 목록에 있는지 확인
                .toList();

        List<PerDaySchedulesResponse> perDaySchedulesResponses = new ArrayList<>();

        // 멤버 스케줄 추가
        for (Member member : members) {
            Map<String, List<Selection>> groupedSelectionsByDay = member.getSelections().stream()
                    .collect(Collectors.groupingBy(
                            selection -> selection.getSchedule().getDay(),
                            LinkedHashMap::new,
                            Collectors.toList()
                    ));

            List<DaySchedule> daySchedules = groupedSelectionsByDay.entrySet().stream()
                    .map(entry -> DaySchedule.from(entry.getValue()))
                    .collect(Collectors.toList());
            perDaySchedulesResponses.add(PerDaySchedulesResponse.of(member.getName(), daySchedules));
        }

        // 유저 스케줄 추가
        for (User user : users) {
            Map<String, List<Selection>> groupedSelectionsByDay = user.getSelections().stream()
                    .filter(selection -> selection.getSchedule().getEvent().equals(event))
                    .collect(Collectors.groupingBy(
                            selection -> selection.getSchedule().getDay(),
                            LinkedHashMap::new,
                            Collectors.toList()
                    ));

            List<DaySchedule> daySchedules = groupedSelectionsByDay.entrySet().stream()
                    .map(entry -> DaySchedule.from(entry.getValue()))
                    .collect(Collectors.toList());
            perDaySchedulesResponses.add(PerDaySchedulesResponse.of(user.getNickname(), daySchedules));
        }

        return perDaySchedulesResponses;
    }

    // 개인 요일 스케줄 반환 메서드 (비로그인)
    @Transactional(readOnly = true)
    public PerDaySchedulesResponse getMemberDaySchedules(String eventId, String memberId) {
        Event event = eventRepository.findByEventId(UUID.fromString(eventId))
                .orElseThrow(() -> new CustomException(EventErrorStatus._NOT_FOUND_EVENT));

        Member member = memberRepository.findByMemberIdWithSelections(UUID.fromString(memberId))
                .orElseThrow(() -> new CustomException(MemberErrorStatus._NOT_FOUND_MEMBER));

        Map<String, List<Selection>> groupedSelectionsByDay = member.getSelections().stream()
                .collect(Collectors.groupingBy(
                        selection -> selection.getSchedule().getDay(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<DaySchedule> daySchedules = groupedSelectionsByDay.entrySet().stream()
                .map(entry -> DaySchedule.from(entry.getValue()))
                .collect(Collectors.toList());

        return PerDaySchedulesResponse.of(member.getName(), daySchedules);
    }

    // 개인 요일 스케줄 반환 메서드 (로그인)
    @Transactional(readOnly = true)
    public PerDaySchedulesResponse getUserDaySchedules(String eventId, String authorizationHeader) {
        Event event = eventRepository.findByEventId(UUID.fromString(eventId))
                .orElseThrow(() -> new CustomException(EventErrorStatus._NOT_FOUND_EVENT));

        User user = jwtUtil.getUserFromHeader(authorizationHeader);

        Map<String, List<Selection>> groupedSelectionsByDay = user.getSelections().stream()
                .filter(selection -> selection.getSchedule().getEvent().equals(event))
                .collect(Collectors.groupingBy(
                        selection -> selection.getSchedule().getDay(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<DaySchedule> daySchedules = groupedSelectionsByDay.entrySet().stream()
                .map(entry -> DaySchedule.from(entry.getValue()))
                .collect(Collectors.toList());

        return PerDaySchedulesResponse.of(user.getNickname(), daySchedules);
    }

    // 전체 날짜 스케줄 반환 메서드
    @Transactional(readOnly = true)
    public List<PerDateSchedulesResponse> getAllDateSchedules(String eventId) {
        Event event = eventRepository.findByEventId(UUID.fromString(eventId))
                .orElseThrow(() -> new CustomException(EventErrorStatus._NOT_FOUND_EVENT));

        // 이벤트에 참여하는 모든 멤버
        List<Member> members = memberRepository.findAllWithSelectionsAndSchedulesByEvent(event);

        // 이벤트에 참여하는 모든 참여자 목록
        GetParticipantsResponse getParticipantsResponse = eventService.getParticipants(eventId);
        List<String> participantNames = getParticipantsResponse.names();

        // 이벤트에 참여하는 모든 유저
        List<EventParticipation> eventParticipations = eventParticipationRepository.findAllByEvent(event);
        List<User> users = eventParticipations.stream()
                .map(EventParticipation::getUser)
                .filter(user -> participantNames.contains(user.getNickname())) // 유저가 참여자 목록에 있는지 확인
                .toList();

        List<PerDateSchedulesResponse> perDateSchedulesResponses = new ArrayList<>();

        // 멤버 스케줄 추가
        for (Member member : members) {
            Map<String, List<Selection>> groupedSelectionsByDate = member.getSelections().stream()
                    .collect(Collectors.groupingBy(
                            selection -> selection.getSchedule().getDate(),
                            LinkedHashMap::new,
                            Collectors.toList()
                    ));

            List<DateSchedule> dateSchedules = groupedSelectionsByDate.entrySet().stream()
                    .map(entry -> DateSchedule.from(entry.getValue()))
                    .collect(Collectors.toList());
            perDateSchedulesResponses.add(PerDateSchedulesResponse.of(member.getName(), dateSchedules));
        }

        // 유저 스케줄 추가
        for (User user : users) {
            Map<String, List<Selection>> groupedSelectionsByDate = user.getSelections().stream()
                    .filter(selection -> selection.getSchedule().getEvent().equals(event))
                    .collect(Collectors.groupingBy(
                            selection -> selection.getSchedule().getDate(),
                            LinkedHashMap::new,
                            Collectors.toList()
                    ));

            List<DateSchedule> dateSchedules = groupedSelectionsByDate.entrySet().stream()
                    .map(entry -> DateSchedule.from(entry.getValue()))
                    .collect(Collectors.toList());
            perDateSchedulesResponses.add(PerDateSchedulesResponse.of(user.getNickname(), dateSchedules));
        }

        return perDateSchedulesResponses;
    }

    // 개인 날짜 스케줄 반환 메서드 (비로그인)
    @Transactional(readOnly = true)
    public PerDateSchedulesResponse getMemberDateSchedules(String eventId, String memberId) {
        Event event = eventRepository.findByEventId(UUID.fromString(eventId))
                .orElseThrow(() -> new CustomException(EventErrorStatus._NOT_FOUND_EVENT));

        Member member = memberRepository.findByMemberIdWithSelections(UUID.fromString(memberId))
                .orElseThrow(() -> new CustomException(MemberErrorStatus._NOT_FOUND_MEMBER));

        Map<String, List<Selection>> groupedSelectionsByDate = member.getSelections().stream()
                .collect(Collectors.groupingBy(
                        selection -> selection.getSchedule().getDate(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<DateSchedule> dateSchedules = groupedSelectionsByDate.entrySet().stream()
                .map(entry -> DateSchedule.from(entry.getValue()))
                .collect(Collectors.toList());

        return PerDateSchedulesResponse.of(member.getName(), dateSchedules);
    }

    // 개인 날짜 스케줄 반환 메서드 (로그인)
    @Transactional(readOnly = true)
    public PerDateSchedulesResponse getUserDateSchedules(String eventId, String authorizationHeader) {
        Event event = eventRepository.findByEventId(UUID.fromString(eventId))
                .orElseThrow(() -> new CustomException(EventErrorStatus._NOT_FOUND_EVENT));

        User user = jwtUtil.getUserFromHeader(authorizationHeader);

        Map<String, List<Selection>> groupedSelectionsByDate = user.getSelections().stream()
                .filter(selection -> selection.getSchedule().getEvent().equals(event))
                .collect(Collectors.groupingBy(
                        selection -> selection.getSchedule().getDate(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<DateSchedule> dateSchedules = groupedSelectionsByDate.entrySet().stream()
                .map(entry -> DateSchedule.from(entry.getValue()))
                .collect(Collectors.toList());

        return PerDateSchedulesResponse.of(user.getNickname(), dateSchedules);
    }

    // 멤버 필터링 요일 스케줄 반환 메서드
    @Transactional(readOnly = true)
    public List<PerDaySchedulesResponse> getFilteredDaySchedules(GetFilteredSchedulesRequest getFilteredSchedulesRequest) {
        Event event = eventRepository.findByEventId(UUID.fromString(getFilteredSchedulesRequest.eventId()))
                .orElseThrow(() -> new CustomException(EventErrorStatus._NOT_FOUND_EVENT));

        List<Member> members = memberRepository.findAllWithSelectionsAndSchedulesByEventAndNames(event, getFilteredSchedulesRequest.names());

        List<PerDaySchedulesResponse> perDaySchedulesResponses = new ArrayList<>();

        for (Member member : members) {
            Map<String, List<Selection>> groupedSelectionsByDay = member.getSelections().stream()
                    .collect(Collectors.groupingBy(
                            selection -> selection.getSchedule().getDay(),
                            LinkedHashMap::new,
                            Collectors.toList()
                    ));

            List<DaySchedule> daySchedules = groupedSelectionsByDay.entrySet().stream()
                    .map(entry -> DaySchedule.from(entry.getValue()))
                    .collect(Collectors.toList());
            perDaySchedulesResponses.add(PerDaySchedulesResponse.of(member.getName(), daySchedules));
        }
        return perDaySchedulesResponses;
    }

    // 멤버 필터링 날짜 스케줄 반환 메서드
    @Transactional(readOnly = true)
    public List<PerDateSchedulesResponse> getFilteredDateSchedules(GetFilteredSchedulesRequest getFilteredSchedulesRequest) {
        Event event = eventRepository.findByEventId(UUID.fromString(getFilteredSchedulesRequest.eventId()))
                .orElseThrow(() -> new CustomException(EventErrorStatus._NOT_FOUND_EVENT));

        List<Member> members = memberRepository.findAllWithSelectionsAndSchedulesByEventAndNames(event, getFilteredSchedulesRequest.names());

        List<PerDateSchedulesResponse> perDateSchedulesResponses = new ArrayList<>();

        for (Member member : members) {
            Map<String, List<Selection>> groupedSelectionsByDate = member.getSelections().stream()
                    .collect(Collectors.groupingBy(
                            selection -> selection.getSchedule().getDate(),
                            LinkedHashMap::new,
                            Collectors.toList()
                    ));

            List<DateSchedule> dateSchedules = groupedSelectionsByDate.entrySet().stream()
                    .map(entry -> DateSchedule.from(entry.getValue()))
                    .collect(Collectors.toList());
            perDateSchedulesResponses.add(PerDateSchedulesResponse.of(member.getName(), dateSchedules));
        }
        return perDateSchedulesResponses;
    }
}