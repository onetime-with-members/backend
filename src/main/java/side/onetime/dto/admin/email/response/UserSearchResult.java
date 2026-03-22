package side.onetime.dto.admin.email.response;

public record UserSearchResult(
        Long userId,
        String name,
        String email,
        String nickname,
        String provider
) {
    public static UserSearchResult from(Object[] row) {
        return new UserSearchResult(
                row[0] != null ? ((Number) row[0]).longValue() : null,
                row[1] != null ? (String) row[1] : null,
                row[2] != null ? (String) row[2] : null,
                row[3] != null ? (String) row[3] : null,
                row[4] != null ? (String) row[4] : null
        );
    }
}
