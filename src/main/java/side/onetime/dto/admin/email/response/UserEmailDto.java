package side.onetime.dto.admin.email.response;

/**
 * 이메일 발송용 유저 정보 DTO
 * email과 userId를 함께 관리하여 데이터 정합성 보장
 */
public record UserEmailDto(
        String email,
        Long userId
) {
    public static UserEmailDto from(Object[] row) {
        return new UserEmailDto(
                (String) row[0],
                ((Number) row[1]).longValue()
        );
    }
}
