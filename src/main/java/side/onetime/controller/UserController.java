package side.onetime.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import side.onetime.domain.enums.GuideType;
import side.onetime.dto.user.request.*;
import side.onetime.dto.user.response.*;
import side.onetime.global.common.ApiResponse;
import side.onetime.global.common.status.SuccessStatus;
import side.onetime.service.UserService;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 유저 온보딩 API.
     *
     * 제공된 레지스터 토큰을 검증한 후, 해당 정보를 기반으로 유저 데이터를 저장하고, 액세스 및 리프레쉬 토큰을 발급합니다.
     *
     * @param onboardUserRequest 유저의 레지스터 토큰, 닉네임, 약관 동의 여부, 수면 시간 정보를 포함하는 요청 객체
     * @return 발급된 액세스 토큰과 리프레쉬 토큰을 포함하는 응답 객체
     */
    @PostMapping("/onboarding")
    public ResponseEntity<ApiResponse<OnboardUserResponse>> onboardUser(
            @Valid @RequestBody OnboardUserRequest onboardUserRequest) {

        OnboardUserResponse onboardUserResponse = userService.onboardUser(onboardUserRequest);
        return ApiResponse.onSuccess(SuccessStatus._ONBOARD_USER, onboardUserResponse);
    }

    /**
     * 유저 정보 조회 API.
     *
     * 로그인한 유저의 정보를 조회합니다.
     *
     * @return 유저의 정보를 포함한 응답 객체
     */
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<GetUserProfileResponse>> getUserProfile() {

        GetUserProfileResponse getUserProfileResponse = userService.getUserProfile();
        return ApiResponse.onSuccess(SuccessStatus._GET_USER_PROFILE, getUserProfileResponse);
    }

    /**
     * 유저 정보 수정 API.
     *
     * 유저의 정보를 수정하는 API입니다.
     *
     * @param updateUserProfileRequest 수정할 닉네임 or 언어를 포함하는 요청 객체
     * @return 성공 상태 응답 객체
     */
    @PatchMapping("/profile/action-update")
    public ResponseEntity<ApiResponse<SuccessStatus>> updateUserProfile(
            @Valid @RequestBody UpdateUserProfileRequest updateUserProfileRequest) {

        userService.updateUserProfile(updateUserProfileRequest);
        return ApiResponse.onSuccess(SuccessStatus._UPDATE_USER_PROFILE);
    }

    /**
     * 유저 서비스 탈퇴 API.
     *
     * 유저의 계정을 삭제하여 서비스에서 탈퇴하는 API입니다.
     *
     * @return 성공 상태 응답 객체
     */
    @PostMapping("/action-withdraw")
    public ResponseEntity<ApiResponse<SuccessStatus>> withdrawUser() {

        userService.withdrawUser();
        return ApiResponse.onSuccess(SuccessStatus._WITHDRAW_USER);
    }

    /**
     * 유저 약관 동의 여부 조회 API.
     *
     * 인증된 사용자의 필수 및 선택 약관 동의 상태를 조회합니다.
     *
     * @return 약관 동의 여부 응답 객체
     */
    @GetMapping("/policy")
    public ResponseEntity<ApiResponse<GetUserPolicyAgreementResponse>> getUserPolicyAgreement() {

        GetUserPolicyAgreementResponse response = userService.getUserPolicyAgreement();
        return ApiResponse.onSuccess(SuccessStatus._GET_USER_POLICY_AGREEMENT, response);
    }

    /**
     * 유저 약관 동의 여부 수정 API.
     *
     * 사용자의 서비스 이용약관, 개인정보 수집 및 이용 동의, 마케팅 정보 수신 동의 상태를 업데이트합니다.
     * 모든 필드는 필수 값이며, 기존 동의 여부를 새로운 값으로 변경합니다.
     *
     * @param request 약관 동의 여부 수정 요청 데이터 (필수 값)
     * @return 성공 상태 응답 객체
     */
    @PutMapping("/policy")
    public ResponseEntity<ApiResponse<SuccessStatus>> updateUserPolicyAgreement(
            @Valid @RequestBody UpdateUserPolicyAgreementRequest request) {

        userService.updateUserPolicyAgreement(request);
        return ApiResponse.onSuccess(SuccessStatus._UPDATE_USER_POLICY_AGREEMENT);
    }

    /**
     * 유저 수면 시간 조회 API.
     *
     * 로그인한 사용자의 수면 시작 시간과 종료 시간을 조회합니다.
     *
     * @return 유저의 수면 시작 시간 및 종료 시간을 포함한 응답 객체
     */
    @GetMapping("/sleep-time")
    public ResponseEntity<ApiResponse<GetUserSleepTimeResponse>> getUserSleepTime() {

        GetUserSleepTimeResponse response = userService.getUserSleepTime();
        return ApiResponse.onSuccess(SuccessStatus._GET_USER_SLEEP_TIME, response);
    }

    /**
     * 유저 수면 시간 수정 API.
     *
     * 사용자의 수면 시작 시간과 종료 시간을 업데이트합니다.
     *
     * @param request 수면 시간 수정 요청 데이터 (필수 값)
     * @return 성공 상태 응답 객체
     */
    @PutMapping("/sleep-time")
    public ResponseEntity<ApiResponse<SuccessStatus>> updateUserSleepTime(
            @Valid @RequestBody UpdateUserSleepTimeRequest request) {

        userService.updateUserSleepTime(request);
        return ApiResponse.onSuccess(SuccessStatus._UPDATE_USER_SLEEP_TIME);
    }

    /**
     * 유저 로그아웃 API.
     *
     * 로그아웃 시, 유저의 리프레쉬 토큰을 제거합니다.
     *
     * @return 성공 상태 응답 객체
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<SuccessStatus>> logoutUser(
            @Valid @RequestBody LogoutUserRequest logoutUserRequest
    ) {

        userService.logoutUser(logoutUserRequest);
        return ApiResponse.onSuccess(SuccessStatus._LOGOUT_USER);
    }

    /**
     * 가이드 확인 여부 저장 API.
     *
     * GuideType에 정의된 가이드에 대해 사용자의 확인 여부를 저장합니다.
     * 이미 확인한 상태일 경우, Conflict 에러를 반환합니다.
     *
     * @param request 확인 여부를 저장할 가이드 타입 객체
     * @return 성공 상태 응답 객체
     */
    @PostMapping("/guides/view-status")
    public ResponseEntity<ApiResponse<SuccessStatus>> createGuideViewStatus(
            @Valid @RequestBody CreateGuideViewStatusRequest request
    ) {

        userService.createGuideViewStatus(request);
        return ApiResponse.onSuccess(SuccessStatus._CREATE_GUIDE_VIEW_STATUS);
    }

    /**
     * 가이드 확인 여부 조회 API.
     *
     * GuideType에 정의된 가이드에 대해 사용자의 확인 여부를 조회합니다.
     *
     * @param guideType 조회할 가이드 타입
     * @return 가이드 확인 여부 응답 객체
     */
    @GetMapping("/guides/view-status")
    public ResponseEntity<ApiResponse<GetGuideViewStatusResponse>> getGuideViewStatus(
            @RequestParam("guide_type") GuideType guideType
    ){

        GetGuideViewStatusResponse response = userService.getGuideViewStatus(guideType);
        return ApiResponse.onSuccess(SuccessStatus._GET_GUIDE_VIEW_STATUS, response);
    }
}
