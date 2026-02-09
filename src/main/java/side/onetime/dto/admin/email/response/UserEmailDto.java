package side.onetime.dto.admin.email.response;

/**
 * 이메일 발송용 유저 정보 DTO
 * email, userId, name, nickname을 함께 관리하여 데이터 정합성 및 개인화 지원
 */
public record UserEmailDto(
        String email,
        Long userId,
        String name,
        String nickname
) {
    public static UserEmailDto from(Object[] row) {
        return new UserEmailDto(
                (String) row[0],
                ((Number) row[1]).longValue(),
                row.length > 2 ? (String) row[2] : null,
                row.length > 3 ? (String) row[3] : null
        );
    }
}
