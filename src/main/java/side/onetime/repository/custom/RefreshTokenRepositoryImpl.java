package side.onetime.repository.custom;

import static side.onetime.domain.QRefreshToken.*;

import java.time.LocalDateTime;

import org.springframework.transaction.annotation.Transactional;

import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;
import side.onetime.domain.enums.TokenStatus;

@RequiredArgsConstructor
public class RefreshTokenRepositoryImpl implements RefreshTokenRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    @Transactional
    public void revokeByUserIdAndBrowserId(Long userId, String userType, String browserId) {
        queryFactory.update(refreshToken)
                .set(refreshToken.status, TokenStatus.REVOKED)
                .set(refreshToken.updatedDate, LocalDateTime.now())
                .where(refreshToken.userId.eq(userId)
                        .and(refreshToken.userType.eq(userType))
                        .and(refreshToken.browserId.eq(browserId))
                        .and(refreshToken.status.eq(TokenStatus.ACTIVE)))
                .execute();
    }

    @Override
    @Transactional
    public void revokeAllByUserId(Long userId, String userType) {
        queryFactory.update(refreshToken)
                .set(refreshToken.status, TokenStatus.REVOKED)
                .set(refreshToken.updatedDate, LocalDateTime.now())
                .where(refreshToken.userId.eq(userId)
                        .and(refreshToken.userType.eq(userType))
                        .and(refreshToken.status.eq(TokenStatus.ACTIVE)))
                .execute();
    }

    @Override
    @Transactional
    public void revokeAllByFamilyId(String familyId) {
        queryFactory.update(refreshToken)
                .set(refreshToken.status, TokenStatus.REVOKED)
                .set(refreshToken.updatedDate, LocalDateTime.now())
                .where(refreshToken.familyId.eq(familyId)
                        .and(refreshToken.status.in(TokenStatus.ACTIVE, TokenStatus.ROTATED)))
                .execute();
    }

    @Override
    @Transactional
    public int updateExpiredTokens(LocalDateTime now) {
        return (int) queryFactory.update(refreshToken)
                .set(refreshToken.status, TokenStatus.EXPIRED)
                .set(refreshToken.updatedDate, now)
                .where(refreshToken.status.eq(TokenStatus.ACTIVE)
                        .and(refreshToken.expiryAt.lt(now)))
                .execute();
    }

    @Override
    @Transactional
    public int hardDeleteOldInactiveTokens(LocalDateTime threshold) {
        return (int) queryFactory.delete(refreshToken)
                .where(refreshToken.status.in(TokenStatus.REVOKED, TokenStatus.EXPIRED, TokenStatus.ROTATED)
                        .and(refreshToken.updatedDate.lt(threshold)))
                .execute();
    }
}
