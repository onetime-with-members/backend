package side.onetime.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import side.onetime.dto.fixed.request.UpdateFixedScheduleRequest;
import side.onetime.dto.fixed.response.GetFixedScheduleResponse;
import side.onetime.global.common.ApiResponse;
import side.onetime.global.common.status.SuccessStatus;
import side.onetime.service.FixedScheduleService;

@RestController
@RequestMapping("/api/v1/fixed-schedules")
@RequiredArgsConstructor
public class FixedController {

    private final FixedScheduleService fixedScheduleService;

    /**
     * 유저의 고정 스케줄 조회 API.
     *
     * 인증된 유저의 현재 등록된 고정 스케줄을 조회합니다.
     *
     * @return 유저의 고정 스케줄 목록
     */
    @GetMapping
    public ResponseEntity<ApiResponse<GetFixedScheduleResponse>> getUserFixedSchedule() {

        GetFixedScheduleResponse response = fixedScheduleService.getUserFixedSchedule();
        return ApiResponse.onSuccess(SuccessStatus._GET_USER_FIXED_SCHEDULE, response);
    }

    /**
     * 유저의 고정 스케줄 수정 API.
     *
     * 기존에 등록된 고정 스케줄을 삭제하고, 새로운 스케줄을 저장합니다.
     *
     * @param request 새로운 고정 스케줄 목록
     * @return 성공 상태 응답 객체
     */
    @PutMapping
    public ResponseEntity<ApiResponse<SuccessStatus>> updateUserFixedSchedules(
            @Valid @RequestBody UpdateFixedScheduleRequest request) {

        fixedScheduleService.updateUserFixedSchedules(request);
        return ApiResponse.onSuccess(SuccessStatus._UPDATE_USER_FIXED_SCHEDULE);
    }

	/**
	 * 에브리타임 시간표 조회 API.
	 *
	 * 유저의 에브리타임 시간표를 조회한 후 파싱하여, 고정 스케줄 형태로 반환합니다.
	 *
	 * @param identifier 파싱할 에브리타임 시간표 URL 식별자
	 * @return 성공 상태 응답 객체
	 */
	@GetMapping("/everytime/{identifier}")
	public ResponseEntity<ApiResponse<GetFixedScheduleResponse>> getUserEverytimeTimetable(
		@PathVariable String identifier) {

		GetFixedScheduleResponse response = fixedScheduleService.getUserEverytimeTimetable(identifier);
		return ApiResponse.onSuccess(SuccessStatus._GET_USER_EVERYTIME_TIMETABLE, response);
	}
}
