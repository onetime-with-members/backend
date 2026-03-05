package side.onetime.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import side.onetime.auth.annotation.IsUser;
import side.onetime.auth.annotation.PublicApi;
import side.onetime.dto.schedule.request.CreateDateScheduleRequest;
import side.onetime.dto.schedule.request.CreateDayScheduleRequest;
import side.onetime.dto.schedule.request.GetFilteredSchedulesRequest;
import side.onetime.dto.schedule.response.PerDateSchedulesResponse;
import side.onetime.dto.schedule.response.PerDaySchedulesResponse;
import side.onetime.global.common.ApiResponse;
import side.onetime.global.common.status.SuccessStatus;
import side.onetime.service.ScheduleService;

@RestController
@RequestMapping("/api/v1/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    /**
     * 요일 스케줄 등록 API.
     *
     * 요일별 반복되는 스케줄을 등록하는 API입니다.
     * 인증된 사용자와 비인증 사용자에 따라 스케줄 생성 방식이 다릅니다.
     *
     * @param createDayScheduleRequest 요일 스케줄 생성 요청 객체 (이벤트 ID, 멤버 ID, 요일 스케줄 목록)
     * @param authorizationHeader 인증된 유저의 토큰 (선택사항)
     * @return 스케줄 등록 성공 상태
     */
    @PublicApi
    @PostMapping("/day")
    public ResponseEntity<ApiResponse<SuccessStatus>> createDaySchedules(
            @Valid @RequestBody CreateDayScheduleRequest createDayScheduleRequest,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {

        if (authorizationHeader != null) {
            scheduleService.createDaySchedulesForAuthenticatedUser(createDayScheduleRequest, authorizationHeader);
        } else {
            scheduleService.createDaySchedulesForAnonymousUser(createDayScheduleRequest);
        }
        return ApiResponse.onSuccess(SuccessStatus._CREATED_DAY_SCHEDULES);
    }

    /**
     * 날짜 스케줄 등록 API.
     *
     * 특정 날짜에 대한 스케줄을 등록하는 API입니다.
     * 인증된 사용자와 비인증 사용자에 따라 스케줄 생성 방식이 다릅니다.
     *
     * @param createDateScheduleRequest 날짜 스케줄 생성 요청 객체 (이벤트 ID, 멤버 ID, 날짜 스케줄 목록)
     * @param authorizationHeader 인증된 유저의 토큰 (선택사항)
     * @return 스케줄 등록 성공 상태
     */
    @PublicApi
    @PostMapping("/date")
    public ResponseEntity<ApiResponse<SuccessStatus>> createDateSchedules(
            @Valid @RequestBody CreateDateScheduleRequest createDateScheduleRequest,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {

        if (authorizationHeader != null) {
            scheduleService.createDateSchedulesForAuthenticatedUser(createDateScheduleRequest, authorizationHeader);
        } else {
            scheduleService.createDateSchedulesForAnonymousUser(createDateScheduleRequest);
        }
        return ApiResponse.onSuccess(SuccessStatus._CREATED_DATE_SCHEDULES);
    }

    /**
     * 전체 요일 스케줄 조회 API.
     *
     * 특정 이벤트에 등록된 모든 요일 스케줄을 조회합니다.
     *
     * @param eventId 조회할 이벤트의 ID
     * @return 이벤트에 등록된 요일 스케줄 목록
     */
    @PublicApi
    @GetMapping("/day/{event_id}")
    public ResponseEntity<ApiResponse<List<PerDaySchedulesResponse>>> getAllDaySchedules(
            @PathVariable("event_id") String eventId) {

        List<PerDaySchedulesResponse> perDaySchedulesResponses = scheduleService.getAllDaySchedules(eventId);
        return ApiResponse.onSuccess(SuccessStatus._GET_ALL_DAY_SCHEDULES, perDaySchedulesResponses);
    }

    /**
     * 개인 요일 스케줄 조회 API (비로그인).
     *
     * 비로그인 사용자의 특정 이벤트에 대한 개인 요일 스케줄을 조회합니다.
     *
     * @param eventId 조회할 이벤트의 ID
     * @param memberId 조회할 멤버의 ID
     * @return 멤버의 요일 스케줄
     */
    @PublicApi
    @GetMapping("/day/{event_id}/{member_id}")
    public ResponseEntity<ApiResponse<PerDaySchedulesResponse>> getMemberDaySchedules(
            @PathVariable("event_id") String eventId,
            @PathVariable("member_id") String memberId) {

        PerDaySchedulesResponse perDaySchedulesResponse = scheduleService.getMemberDaySchedules(eventId, memberId);
        return ApiResponse.onSuccess(SuccessStatus._GET_MEMBER_DAY_SCHEDULES, perDaySchedulesResponse);
    }

    /**
     * 개인 요일 스케줄 조회 API (로그인).
     *
     * 인증된 사용자의 특정 이벤트에 대한 개인 요일 스케줄을 조회합니다.
     *
     * @param eventId 조회할 이벤트의 ID
     * @return 사용자의 요일 스케줄
     */
    @IsUser
    @GetMapping("/day/{event_id}/user")
    public ResponseEntity<ApiResponse<PerDaySchedulesResponse>> getUserDaySchedules(
            @PathVariable("event_id") String eventId) {

        PerDaySchedulesResponse perDaySchedulesResponse = scheduleService.getUserDaySchedules(eventId);
        return ApiResponse.onSuccess(SuccessStatus._GET_USER_DAY_SCHEDULES, perDaySchedulesResponse);
    }

    /**
     * 참여자 필터링 요일 스케줄 조회 API.
     *
     * 참여자 ID로 필터링하여 특정 이벤트의 요일 스케줄을 조회합니다.
     *
     * @param eventId 조회할 이벤트의 ID
     * @param getFilteredSchedulesRequest 필터링할 스케줄 요청 객체 (유저 ID 목록, 멤버 ID 목록)
     * @return 필터링된 요일 스케줄 목록
     */
    @PublicApi
    @PostMapping("/day/{event_id}/filtering")
    public ResponseEntity<ApiResponse<List<PerDaySchedulesResponse>>> getFilteredDaySchedules(
            @PathVariable("event_id") String eventId,
            @RequestBody GetFilteredSchedulesRequest getFilteredSchedulesRequest) {

        List<PerDaySchedulesResponse> perDaySchedulesResponses = scheduleService.getFilteredDaySchedules(eventId, getFilteredSchedulesRequest);
        return ApiResponse.onSuccess(SuccessStatus._GET_FILTERED_DAY_SCHEDULES, perDaySchedulesResponses);
    }

    /**
     * 전체 날짜 스케줄 조회 API.
     *
     * 특정 이벤트에 등록된 모든 날짜 스케줄을 조회합니다.
     *
     * @param eventId 조회할 이벤트의 ID
     * @return 이벤트에 등록된 날짜 스케줄 목록
     */
    @PublicApi
    @GetMapping("/date/{event_id}")
    public ResponseEntity<ApiResponse<List<PerDateSchedulesResponse>>> getAllDateSchedules(
            @PathVariable("event_id") String eventId) {

        List<PerDateSchedulesResponse> perDateSchedulesResponses = scheduleService.getAllDateSchedules(eventId);
        return ApiResponse.onSuccess(SuccessStatus._GET_ALL_DATE_SCHEDULES, perDateSchedulesResponses);
    }

    /**
     * 개인 날짜 스케줄 조회 API (비로그인).
     *
     * 비로그인 사용자의 특정 이벤트에 대한 개인 날짜 스케줄을 조회합니다.
     *
     * @param eventId 조회할 이벤트의 ID
     * @param memberId 조회할 멤버의 ID
     * @return 멤버의 날짜 스케줄
     */
    @PublicApi
    @GetMapping("/date/{event_id}/{member_id}")
    public ResponseEntity<ApiResponse<PerDateSchedulesResponse>> getMemberDateSchedules(
            @PathVariable("event_id") String eventId,
            @PathVariable("member_id") String memberId) {

        PerDateSchedulesResponse perDateSchedulesResponse = scheduleService.getMemberDateSchedules(eventId, memberId);
        return ApiResponse.onSuccess(SuccessStatus._GET_MEMBER_DATE_SCHEDULES, perDateSchedulesResponse);
    }

    /**
     * 개인 날짜 스케줄 조회 API (로그인).
     *
     * 인증된 사용자의 특정 이벤트에 대한 개인 날짜 스케줄을 조회합니다.
     *
     * @param eventId 조회할 이벤트의 ID
     * @return 사용자의 날짜 스케줄
     */
    @IsUser
    @GetMapping("/date/{event_id}/user")
    public ResponseEntity<ApiResponse<PerDateSchedulesResponse>> getUserDateSchedules(
            @PathVariable("event_id") String eventId) {

        PerDateSchedulesResponse perDateSchedulesResponse = scheduleService.getUserDateSchedules(eventId);
        return ApiResponse.onSuccess(SuccessStatus._GET_USER_DATE_SCHEDULES, perDateSchedulesResponse);
    }

    /**
     * 참여자 필터링 날짜 스케줄 조회 API.
     *
     * 참여자 ID로 필터링하여 특정 이벤트의 날짜 스케줄을 조회합니다.
     *
     * @param eventId 조회할 이벤트의 ID
     * @param getFilteredSchedulesRequest 필터링할 스케줄 요청 객체 (유저 ID 목록, 멤버 ID 목록)
     * @return 필터링된 날짜 스케줄 목록
     */
    @PublicApi
    @PostMapping("/date/{event_id}/filtering")
    public ResponseEntity<ApiResponse<List<PerDateSchedulesResponse>>> getFilteredDateSchedules(
            @PathVariable("event_id") String eventId,
            @RequestBody GetFilteredSchedulesRequest getFilteredSchedulesRequest) {

        List<PerDateSchedulesResponse> perDateSchedulesResponses = scheduleService.getFilteredDateSchedules(eventId, getFilteredSchedulesRequest);
        return ApiResponse.onSuccess(SuccessStatus._GET_FILTERED_DATE_SCHEDULES, perDateSchedulesResponses);
    }
}
