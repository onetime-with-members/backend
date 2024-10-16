package side.onetime.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import side.onetime.dto.ScheduleDto;
import side.onetime.global.common.ApiResponse;
import side.onetime.global.common.status.SuccessStatus;
import side.onetime.service.ScheduleService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/schedules")
@RequiredArgsConstructor
public class ScheduleController {
    private final ScheduleService scheduleService;

    // 요일 스케줄 등록 API
    @PostMapping("/day")
    public ResponseEntity<ApiResponse<SuccessStatus>> createDaySchedules(
            @RequestBody ScheduleDto.CreateDayScheduleRequest createDayScheduleRequest,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {

        if (authorizationHeader != null) {
            scheduleService.createDaySchedulesForAuthenticatedUser(createDayScheduleRequest, authorizationHeader);
        } else {
            scheduleService.createDaySchedulesForAnonymousUser(createDayScheduleRequest);
        }
        return ApiResponse.onSuccess(SuccessStatus._CREATED_DAY_SCHEDULES);
    }

    // 날짜 스케줄 등록 API
    @PostMapping("/date")
    public ResponseEntity<ApiResponse<SuccessStatus>> createDateSchedules(
            @RequestBody ScheduleDto.CreateDateScheduleRequest createDateScheduleRequest,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {

        if (authorizationHeader != null) {
            scheduleService.createDateSchedulesForAuthenticatedUser(createDateScheduleRequest, authorizationHeader);
        } else {
            scheduleService.createDateSchedulesForAnonymousUser(createDateScheduleRequest);
        }
        return ApiResponse.onSuccess(SuccessStatus._CREATED_DATE_SCHEDULES);
    }

    // 전체 요일 스케줄 조회 API
    @GetMapping("/day/{event_id}")
    public ResponseEntity<ApiResponse<List<ScheduleDto.PerDaySchedulesResponse>>> getAllDaySchedules(
            @PathVariable("event_id") String eventId) {

        List<ScheduleDto.PerDaySchedulesResponse> perDaySchedulesResponses = scheduleService.getAllDaySchedules(eventId);
        return ApiResponse.onSuccess(SuccessStatus._GET_ALL_DAY_SCHEDULES, perDaySchedulesResponses);
    }

    // 개인 요일 스케줄 조회 API (비로그인)
    @GetMapping("/day/{event_id}/{member_id}")
    public ResponseEntity<ApiResponse<ScheduleDto.PerDaySchedulesResponse>> getMemberDaySchedules(
            @PathVariable("event_id") String eventId,
            @PathVariable("member_id") String memberId) {

        ScheduleDto.PerDaySchedulesResponse perDaySchedulesResponse = scheduleService.getMemberDaySchedules(eventId, memberId);
        return ApiResponse.onSuccess(SuccessStatus._GET_MEMBER_DAY_SCHEDULES, perDaySchedulesResponse);
    }

    // 개인 요일 스케줄 조회 API (로그인)
    @GetMapping("/day/{event_id}/user")
    public ResponseEntity<ApiResponse<ScheduleDto.PerDaySchedulesResponse>> getUserDaySchedules(
            @PathVariable("event_id") String eventId,
            @RequestHeader(value = "Authorization") String authorizationHeader) {

        ScheduleDto.PerDaySchedulesResponse perDaySchedulesResponse = scheduleService.getUserDaySchedules(eventId, authorizationHeader);
        return ApiResponse.onSuccess(SuccessStatus._GET_USER_DAY_SCHEDULES, perDaySchedulesResponse);
    }

    // 멤버 필터링 요일 스케줄 조회 API
    @GetMapping("/day/action-filtering")
    public ResponseEntity<ApiResponse<List<ScheduleDto.PerDaySchedulesResponse>>> getFilteredDaySchedules(
            @RequestBody ScheduleDto.GetFilteredSchedulesRequest getFilteredSchedulesRequest) {

        List<ScheduleDto.PerDaySchedulesResponse> perDaySchedulesResponses = scheduleService.getFilteredDaySchedules(getFilteredSchedulesRequest);
        return ApiResponse.onSuccess(SuccessStatus._GET_FILTERED_DAY_SCHEDULES, perDaySchedulesResponses);
    }

    // 전체 날짜 스케줄 조회 API
    @GetMapping("/date/{event_id}")
    public ResponseEntity<ApiResponse<List<ScheduleDto.PerDateSchedulesResponse>>> getAllDateSchedules(
            @PathVariable("event_id") String eventId) {

        List<ScheduleDto.PerDateSchedulesResponse> perDateSchedulesResponses = scheduleService.getAllDateSchedules(eventId);
        return ApiResponse.onSuccess(SuccessStatus._GET_ALL_DATE_SCHEDULES, perDateSchedulesResponses);
    }

    // 개인 날짜 스케줄 조회 API (비로그인)
    @GetMapping("/date/{event_id}/{member_id}")
    public ResponseEntity<ApiResponse<ScheduleDto.PerDateSchedulesResponse>> getMemberDateSchedules(
            @PathVariable("event_id") String eventId,
            @PathVariable("member_id") String memberId) {

        ScheduleDto.PerDateSchedulesResponse perDateSchedulesResponse = scheduleService.getMemberDateSchedules(eventId, memberId);
        return ApiResponse.onSuccess(SuccessStatus._GET_MEMBER_DATE_SCHEDULES, perDateSchedulesResponse);
    }

    // 개인 날짜 스케줄 조회 API (로그인)
    @GetMapping("/date/{event_id}/user")
    public ResponseEntity<ApiResponse<ScheduleDto.PerDateSchedulesResponse>> getUserDateSchedules(
            @PathVariable("event_id") String eventId,
            @RequestHeader(value = "Authorization") String authorizationHeader) {

        ScheduleDto.PerDateSchedulesResponse perDateSchedulesResponse = scheduleService.getUserDateSchedules(eventId, authorizationHeader);
        return ApiResponse.onSuccess(SuccessStatus._GET_USER_DATE_SCHEDULES, perDateSchedulesResponse);
    }

    // 멤버 필터링 날짜 스케줄 조회 API
    @GetMapping("/date/action-filtering")
    public ResponseEntity<ApiResponse<List<ScheduleDto.PerDateSchedulesResponse>>> getFilteredDateSchedules(
            @RequestBody ScheduleDto.GetFilteredSchedulesRequest getFilteredSchedulesRequest) {

        List<ScheduleDto.PerDateSchedulesResponse> perDateSchedulesResponses = scheduleService.getFilteredDateSchedules(getFilteredSchedulesRequest);
        return ApiResponse.onSuccess(SuccessStatus._GET_FILTERED_DATE_SCHEDULES, perDateSchedulesResponses);
    }
}
