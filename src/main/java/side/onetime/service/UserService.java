package side.onetime.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import side.onetime.domain.GuideViewStatus;
import side.onetime.domain.RefreshToken;
import side.onetime.domain.User;
import side.onetime.domain.enums.GuideType;
import side.onetime.dto.user.request.*;
import side.onetime.dto.user.response.GetUserPolicyAgreementResponse;
import side.onetime.dto.user.response.GetUserProfileResponse;
import side.onetime.dto.user.response.GetUserSleepTimeResponse;
import side.onetime.dto.user.response.OnboardUserResponse;
import side.onetime.exception.CustomException;
import side.onetime.exception.status.UserErrorStatus;
import side.onetime.repository.GuideViewStatusRepository;
import side.onetime.repository.RefreshTokenRepository;
import side.onetime.repository.UserRepository;
import side.onetime.util.JwtUtil;
import side.onetime.util.UserAuthorizationUtil;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final GuideViewStatusRepository guideViewStatusRepository;

    /**
     * 유저 온보딩 처리 메서드.
     *
     * 회원가입 이후 필수 정보를 설정하고 유저를 저장한 뒤, 액세스 토큰과 리프레쉬 토큰을 발급합니다.
     * 리프레쉬 토큰은 브라우저 식별자(browserId)와 함께 Redis에 저장됩니다.
     *
     * @param request 유저의 레지스터 토큰, 닉네임, 약관 동의, 수면 시간 등 온보딩 정보가 포함된 요청 객체
     * @return 발급된 액세스 토큰과 리프레쉬 토큰을 포함한 응답 객체
     */
    @Transactional
    public OnboardUserResponse onboardUser(OnboardUserRequest request) {
        String registerToken = request.registerToken();
        jwtUtil.validateToken(registerToken);
        User newUser = createUserFromRegisterToken(request, registerToken);
        userRepository.save(newUser);

        Long userId = newUser.getId();
        String browserId = jwtUtil.getClaimFromToken(registerToken, "browserId", String.class);
        String accessToken = jwtUtil.generateAccessToken(userId, "USER");
        String refreshToken = jwtUtil.generateRefreshToken(userId, browserId);
        refreshTokenRepository.save(new RefreshToken(userId, browserId, refreshToken));

        return OnboardUserResponse.of(accessToken, refreshToken);
    }

    /**
     * 레지스터 토큰을 기반으로 User 엔티티를 생성하는 메서드.
     *
     * JWT 레지스터 토큰의 유효성을 검증하고, 토큰에서 제공자 정보 및 유저 정보를 추출하여
     * 닉네임, 약관 동의, 수면 정보 등을 포함한 새로운 User 객체를 빌드합니다.
     *
     * @param request 레지스터 토큰 및 기타 온보딩 정보를 포함한 요청 객체
     * @return 생성된 User 엔티티 객체
     */
    private User createUserFromRegisterToken(OnboardUserRequest request, String registerToken) {

        return User.builder()
                .name(jwtUtil.getClaimFromToken(registerToken, "name", String.class))
                .email(jwtUtil.getClaimFromToken(registerToken, "email", String.class))
                .nickname(request.nickname())
                .provider(jwtUtil.getClaimFromToken(registerToken, "provider", String.class))
                .providerId(jwtUtil.getClaimFromToken(registerToken, "providerId", String.class))
                .servicePolicyAgreement(request.servicePolicyAgreement())
                .privacyPolicyAgreement(request.privacyPolicyAgreement())
                .marketingPolicyAgreement(request.marketingPolicyAgreement())
                .sleepStartTime(request.sleepStartTime())
                .sleepEndTime(request.sleepEndTime())
                .language(request.language())
                .build();
    }

    /**
     * 유저 정보 조회 메서드.
     *
     * 인증된 유저의 프로필 정보를 반환합니다.
     *
     * @return 유저 프로필 응답 데이터
     */
    @Transactional(readOnly = true)
    public GetUserProfileResponse getUserProfile() {
        User user = userRepository.findById(UserAuthorizationUtil.getLoginUserId())
                .orElseThrow(() -> new CustomException(UserErrorStatus._NOT_FOUND_USER));
        return GetUserProfileResponse.of(user);
    }

    /**
     * 유저 정보 수정 메서드.
     *
     * 인증된 유저의 닉네임 or 언어를 수정합니다.
     * 수정된 닉네임은 길이 제한을 검증합니다.
     *
     * @param updateUserProfileRequest 유저 정보 수정 요청 데이터
     */
    @Transactional
    public void updateUserProfile(UpdateUserProfileRequest updateUserProfileRequest) {
        User user = userRepository.findById(UserAuthorizationUtil.getLoginUserId())
                .orElseThrow(() -> new CustomException(UserErrorStatus._NOT_FOUND_USER));
        Optional.ofNullable(updateUserProfileRequest.nickname()).ifPresent(user::updateNickName);
        Optional.ofNullable(updateUserProfileRequest.language()).ifPresent(user::updateLanguage);
        userRepository.save(user);
    }

    /**
     * 유저 서비스 탈퇴 메서드.
     *
     * 인증된 유저의 계정을 삭제합니다.
     *
     */
    @Transactional
    public void withdrawUser() {
        User user = userRepository.findById(UserAuthorizationUtil.getLoginUserId())
                .orElseThrow(() -> new CustomException(UserErrorStatus._NOT_FOUND_USER));
        userRepository.withdraw(user);
        refreshTokenRepository.deleteAllByUserId(user.getId());
    }

    /**
     * 유저 약관 동의 여부 조회 메서드.
     *
     * 인증된 사용자의 필수 및 선택 약관 동의 상태를 반환합니다.
     * 값이 null일 경우 기본값(false)을 반환합니다.
     *
     * @return 유저 약관 동의 여부 응답 객체
     */
    @Transactional(readOnly = true)
    public GetUserPolicyAgreementResponse getUserPolicyAgreement() {
        User user = userRepository.findById(UserAuthorizationUtil.getLoginUserId())
                .orElseThrow(() -> new CustomException(UserErrorStatus._NOT_FOUND_USER));
        return GetUserPolicyAgreementResponse.from(user);
    }

    /**
     * 유저 약관 동의 여부 수정 메서드.
     *
     * 인증된 유저의 서비스 이용약관, 개인정보 수집 및 이용 동의, 마케팅 정보 수신 동의 상태를 업데이트합니다.
     * 모든 필드는 필수 값이며, `@NotNull` 검증을 거칩니다.
     *
     * @param request 약관 동의 여부 수정 요청 데이터
     */
    @Transactional
    public void updateUserPolicyAgreement(UpdateUserPolicyAgreementRequest request) {
        User user = userRepository.findById(UserAuthorizationUtil.getLoginUserId())
                .orElseThrow(() -> new CustomException(UserErrorStatus._NOT_FOUND_USER));
        user.updateServicePolicyAgreement(request.servicePolicyAgreement());
        user.updatePrivacyPolicyAgreement(request.privacyPolicyAgreement());
        user.updateMarketingPolicyAgreement(request.marketingPolicyAgreement());
        userRepository.save(user);
    }

    /**
     * 유저 수면 시간 조회 메서드.
     *
     * 인증된 사용자의 수면 시작 시간과 종료 시간을 조회합니다.
     *
     * @return 유저 수면 시간 응답 데이터 (시작 시간 및 종료 시간 포함)
     */
    @Transactional(readOnly = true)
    public GetUserSleepTimeResponse getUserSleepTime() {
        User user = userRepository.findById(UserAuthorizationUtil.getLoginUserId())
                .orElseThrow(() -> new CustomException(UserErrorStatus._NOT_FOUND_USER));
        return GetUserSleepTimeResponse.from(user);
    }

    /**
     * 유저 수면 시간 수정 메서드.
     *
     * 인증된 사용자의 수면 시작 시간과 종료 시간을 업데이트합니다.
     *
     * @param request 수면 시간 수정 요청 데이터 (필수 값)
     */
    @Transactional
    public void updateUserSleepTime(UpdateUserSleepTimeRequest request) {
        User user = userRepository.findById(UserAuthorizationUtil.getLoginUserId())
                .orElseThrow(() -> new CustomException(UserErrorStatus._NOT_FOUND_USER));
        user.updateSleepStartTime(request.sleepStartTime());
        user.updateSleepEndTime(request.sleepEndTime());
        userRepository.save(user);
    }

    /**
     * 유저 로그아웃 메서드.
     *
     * 로그아웃 시, 리프레쉬 토큰을 제거합니다.
     *
     * @param request 리프레쉬 토큰 요청 데이터
     */
    @Transactional
    public void logoutUser(LogoutUserRequest request) {
        String refreshToken = request.refreshToken();
        jwtUtil.validateToken(refreshToken);
        Long userId = jwtUtil.getClaimFromToken(refreshToken, "userId", Long.class);
        String browserId = jwtUtil.getClaimFromToken(refreshToken, "browserId", String.class);
        refreshTokenRepository.deleteRefreshToken(userId, browserId);
    }

    /**
     * 가이드 확인 여부 저장 메서드.
     *
     * GuideType에 정의된 가이드에 대해 사용자의 확인 여부를 저장합니다.
     * 이미 확인한 상태일 경우, Conflict 에러를 반환합니다.
     *
     * @param request 확인 여부를 저장할 가이드 타입 객체
     */
    @Transactional
    public void createGuideViewStatus(CreateGuideViewStatusRequest request) {
        User user = userRepository.findById(UserAuthorizationUtil.getLoginUserId())
                .orElseThrow(() -> new CustomException(UserErrorStatus._NOT_FOUND_USER));
        GuideType guideType = request.guideType();

        boolean isViewed = guideViewStatusRepository.existsByUserAndGuideType(user, guideType);
        if (isViewed) {
            throw new CustomException(UserErrorStatus._IS_ALREADY_VIEWED_GUIDE);
        }

        GuideViewStatus guideViewStatus = GuideViewStatus.builder()
                .user(user)
                .guideType(guideType)
                .build();

        guideViewStatusRepository.save(guideViewStatus);
    }
}
