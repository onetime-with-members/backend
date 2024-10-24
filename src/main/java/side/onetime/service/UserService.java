package side.onetime.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import side.onetime.domain.RefreshToken;
import side.onetime.domain.User;
import side.onetime.dto.user.request.OnboardUserRequest;
import side.onetime.dto.user.request.UpdateUserProfileRequest;
import side.onetime.dto.user.response.GetUserProfileResponse;
import side.onetime.dto.user.response.OnboardUserResponse;
import side.onetime.exception.CustomException;
import side.onetime.exception.status.UserErrorStatus;
import side.onetime.repository.RefreshTokenRepository;
import side.onetime.repository.UserRepository;
import side.onetime.util.JwtUtil;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final int NICKNAME_LENGTH_LIMIT = 10;

    @Value("${jwt.access-token.expiration-time}")
    private long ACCESS_TOKEN_EXPIRATION_TIME; // 액세스 토큰 유효기간

    @Value("${jwt.refresh-token.expiration-time}")
    private long REFRESH_TOKEN_EXPIRATION_TIME; // 리프레쉬 토큰 유효기간

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    // 유저 온보딩 메서드
    @Transactional
    public OnboardUserResponse onboardUser(OnboardUserRequest onboardUserRequest) {
        // 레지스터 토큰을 이용하여 사용자 정보 추출
        String registerToken = onboardUserRequest.registerToken();
        jwtUtil.validateTokenExpiration(registerToken);
        String provider = jwtUtil.getProviderFromToken(registerToken);
        String providerId = jwtUtil.getProviderIdFromToken(registerToken);
        String name = jwtUtil.getNameFromToken(registerToken);
        String email = jwtUtil.getEmailFromToken(registerToken);

        if (onboardUserRequest.nickname().length() > NICKNAME_LENGTH_LIMIT) {
            throw new CustomException(UserErrorStatus._NICKNAME_TOO_LONG);
        }

        User user = User.builder()
                .name(name)
                .email(email)
                .nickname(onboardUserRequest.nickname())
                .provider(provider)
                .providerId(providerId)
                .build();
        userRepository.save(user);
        Long userId = user.getId();

        // 액세스 & 리프레쉬 토큰 발급
        String accessToken = jwtUtil.generateAccessToken(userId, ACCESS_TOKEN_EXPIRATION_TIME);
        String refreshToken = jwtUtil.generateRefreshToken(userId, REFRESH_TOKEN_EXPIRATION_TIME);

        // 새로운 리프레쉬 토큰 Redis 저장
        RefreshToken newRefreshToken = new RefreshToken(userId, refreshToken);
        refreshTokenRepository.save(newRefreshToken);

        // 액세스 토큰 반환
        return OnboardUserResponse.of(accessToken, refreshToken);
    }

    // 유저 정보 조회 메서드
    @Transactional(readOnly = true)
    public GetUserProfileResponse getUserProfile(String authorizationHeader) {
        User user = jwtUtil.getUserFromHeader(authorizationHeader);

        return GetUserProfileResponse.of(user);
    }

    // 유저 정보 수정 메서드
    @Transactional
    public void updateUserProfile(String authorizationHeader, UpdateUserProfileRequest updateUserProfileRequest) {
        User user = jwtUtil.getUserFromHeader(authorizationHeader);
        String nickname = updateUserProfileRequest.nickname();

        if (nickname.length() > NICKNAME_LENGTH_LIMIT) {
            throw new CustomException(UserErrorStatus._NICKNAME_TOO_LONG);
        }
        user.updateNickName(nickname);
        userRepository.save(user);
    }

    // 유저 서비스 탈퇴 메서드
    @Transactional
    public void withdrawService(String authorizationHeader) {
        User user = jwtUtil.getUserFromHeader(authorizationHeader);
        userRepository.delete(user);
    }
}
