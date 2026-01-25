package side.onetime.token;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import jakarta.persistence.EntityManager;
import side.onetime.configuration.DatabaseTestConfig;
import side.onetime.domain.RefreshToken;
import side.onetime.domain.enums.TokenStatus;
import side.onetime.global.config.QueryDslConfig;
import side.onetime.repository.RefreshTokenRepository;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(QueryDslConfig.class)
@DisplayName("RefreshTokenRepository 테스트")
class RefreshTokenRepositoryTest extends DatabaseTestConfig {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private EntityManager entityManager;

    private static final Long TEST_USER_ID = 1L;
    private static final String TEST_USER_TYPE = "USER";
    private static final String TEST_BROWSER_ID = "browser-hash-123";
    private static final String TEST_USER_IP = "127.0.0.1";
    private static final String TEST_USER_AGENT = "Mozilla/5.0";

    private RefreshToken createAndSaveToken(String jti) {
        RefreshToken token = RefreshToken.create(
                TEST_USER_ID, TEST_USER_TYPE, jti, TEST_BROWSER_ID, "token-value-" + jti,
                LocalDateTime.now(), LocalDateTime.now().plusDays(14),
                TEST_USER_IP, TEST_USER_AGENT
        );
        return refreshTokenRepository.saveAndFlush(token);
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    @Nested
    @DisplayName("findByJti 메서드")
    class FindByJti {

        @Test
        @DisplayName("jti로 토큰 조회 성공")
        void findByJti_Success() {
            // given
            String jti = "test-jti-1";
            createAndSaveToken(jti);
            flushAndClear();

            // when
            Optional<RefreshToken> found = refreshTokenRepository.findByJti(jti);

            // then
            assertThat(found).isPresent();
            assertThat(found.get().getJti()).isEqualTo(jti);
            assertThat(found.get().getUserId()).isEqualTo(TEST_USER_ID);
        }

        @Test
        @DisplayName("존재하지 않는 jti 조회 시 빈 Optional 반환")
        void findByJti_NotFound() {
            // when
            Optional<RefreshToken> found = refreshTokenRepository.findByJti("non-existent-jti");

            // then
            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("markAsRotatedIfActive 메서드 (원자적 업데이트)")
    class MarkAsRotatedIfActive {

        @Test
        @DisplayName("ACTIVE 토큰 ROTATED로 변경 성공")
        void markAsRotatedIfActive_Success() {
            // given
            String jti = "active-token-jti";
            RefreshToken activeToken = createAndSaveToken(jti);
            Long tokenId = activeToken.getId();
            LocalDateTime lastUsedAt = LocalDateTime.now();
            String lastUsedIp = "192.168.1.1";
            flushAndClear();

            // when
            int updated = refreshTokenRepository.markAsRotatedIfActive(tokenId, lastUsedAt, lastUsedIp);
            flushAndClear();

            // then
            assertThat(updated).isEqualTo(1);

            RefreshToken updatedToken = refreshTokenRepository.findById(tokenId).orElseThrow();
            assertThat(updatedToken.getStatus()).isEqualTo(TokenStatus.ROTATED);
            assertThat(updatedToken.getLastUsedIp()).isEqualTo(lastUsedIp);
        }

        @Test
        @DisplayName("이미 ROTATED된 토큰은 업데이트되지 않음")
        void markAsRotatedIfActive_AlreadyRotated() {
            // given
            String jti = "rotated-token-jti";
            RefreshToken token = createAndSaveToken(jti);
            Long tokenId = token.getId();
            flushAndClear();

            // First rotation
            refreshTokenRepository.markAsRotatedIfActive(tokenId, LocalDateTime.now(), "first-ip");
            flushAndClear();

            // when - try to rotate again
            int updated = refreshTokenRepository.markAsRotatedIfActive(tokenId, LocalDateTime.now(), "second-ip");
            flushAndClear();

            // then
            assertThat(updated).isEqualTo(0);

            RefreshToken found = refreshTokenRepository.findById(tokenId).orElseThrow();
            assertThat(found.getLastUsedIp()).isEqualTo("first-ip");
        }
    }

    @Nested
    @DisplayName("revokeByUserIdAndBrowserId 메서드")
    class RevokeByUserIdAndBrowserId {

        @Test
        @DisplayName("특정 유저+브라우저의 ACTIVE 토큰 revoke")
        void revokeByUserIdAndBrowserId_Success() {
            // given
            RefreshToken token = createAndSaveToken("revoke-test-jti");
            Long tokenId = token.getId();
            flushAndClear();

            // when
            refreshTokenRepository.revokeByUserIdAndBrowserId(TEST_USER_ID, TEST_BROWSER_ID);
            flushAndClear();

            // then
            RefreshToken found = refreshTokenRepository.findById(tokenId).orElseThrow();
            assertThat(found.getStatus()).isEqualTo(TokenStatus.REVOKED);
        }
    }

    @Nested
    @DisplayName("revokeAllByUserId 메서드")
    class RevokeAllByUserId {

        @Test
        @DisplayName("특정 유저의 모든 ACTIVE 토큰 revoke")
        void revokeAllByUserId_Success() {
            // given
            RefreshToken token1 = createAndSaveToken("user-token-1");
            RefreshToken token2 = createAndSaveToken("user-token-2");
            Long token1Id = token1.getId();
            Long token2Id = token2.getId();
            flushAndClear();

            // when
            refreshTokenRepository.revokeAllByUserId(TEST_USER_ID);
            flushAndClear();

            // then
            RefreshToken found1 = refreshTokenRepository.findById(token1Id).orElseThrow();
            RefreshToken found2 = refreshTokenRepository.findById(token2Id).orElseThrow();
            assertThat(found1.getStatus()).isEqualTo(TokenStatus.REVOKED);
            assertThat(found2.getStatus()).isEqualTo(TokenStatus.REVOKED);
        }
    }

    @Nested
    @DisplayName("revokeAllByFamilyId 메서드")
    class RevokeAllByFamilyId {

        @Test
        @DisplayName("특정 family의 모든 ACTIVE/ROTATED 토큰 revoke")
        void revokeAllByFamilyId_Success() {
            // given
            RefreshToken parentToken = createAndSaveToken("family-parent");
            String familyId = parentToken.getFamilyId();
            Long parentId = parentToken.getId();

            // Create a child token in the same family (simulating rotation)
            RefreshToken childToken = parentToken.rotate(
                    "family-child", "child-token-value",
                    LocalDateTime.now(), LocalDateTime.now().plusDays(14),
                    TEST_USER_IP, TEST_USER_AGENT
            );
            childToken = refreshTokenRepository.saveAndFlush(childToken);
            Long childId = childToken.getId();

            // Mark parent as rotated
            refreshTokenRepository.markAsRotatedIfActive(parentId, LocalDateTime.now(), TEST_USER_IP);
            flushAndClear();

            // when
            refreshTokenRepository.revokeAllByFamilyId(familyId);
            flushAndClear();

            // then
            RefreshToken foundParent = refreshTokenRepository.findById(parentId).orElseThrow();
            RefreshToken foundChild = refreshTokenRepository.findById(childId).orElseThrow();
            assertThat(foundParent.getStatus()).isEqualTo(TokenStatus.REVOKED);
            assertThat(foundChild.getStatus()).isEqualTo(TokenStatus.REVOKED);
        }
    }

    @Nested
    @DisplayName("updateExpiredTokens 메서드")
    class UpdateExpiredTokens {

        @Test
        @DisplayName("만료된 ACTIVE 토큰을 EXPIRED로 변경")
        void updateExpiredTokens_Success() {
            // given
            RefreshToken expiredToken = RefreshToken.create(
                    TEST_USER_ID, TEST_USER_TYPE, "expired-jti", TEST_BROWSER_ID, "expired-token-value",
                    LocalDateTime.now().minusDays(15), LocalDateTime.now().minusDays(1),
                    TEST_USER_IP, TEST_USER_AGENT
            );
            expiredToken = refreshTokenRepository.saveAndFlush(expiredToken);
            Long expiredId = expiredToken.getId();

            RefreshToken validToken = RefreshToken.create(
                    TEST_USER_ID, TEST_USER_TYPE, "valid-jti", TEST_BROWSER_ID, "valid-token-value",
                    LocalDateTime.now(), LocalDateTime.now().plusDays(14),
                    TEST_USER_IP, TEST_USER_AGENT
            );
            validToken = refreshTokenRepository.saveAndFlush(validToken);
            Long validId = validToken.getId();
            flushAndClear();

            // when
            int updated = refreshTokenRepository.updateExpiredTokens(LocalDateTime.now());
            flushAndClear();

            // then
            assertThat(updated).isEqualTo(1);
            RefreshToken foundExpired = refreshTokenRepository.findById(expiredId).orElseThrow();
            RefreshToken foundValid = refreshTokenRepository.findById(validId).orElseThrow();
            assertThat(foundExpired.getStatus()).isEqualTo(TokenStatus.EXPIRED);
            assertThat(foundValid.getStatus()).isEqualTo(TokenStatus.ACTIVE);
        }
    }

    @Nested
    @DisplayName("hardDeleteOldInactiveTokens 메서드")
    class HardDeleteOldInactiveTokens {

        @Test
        @DisplayName("오래된 비활성 토큰 물리적 삭제")
        void hardDeleteOldInactiveTokens_Success() {
            // given
            RefreshToken token = createAndSaveToken("to-delete-jti");
            Long tokenId = token.getId();
            flushAndClear();

            // Mark as rotated first
            refreshTokenRepository.markAsRotatedIfActive(tokenId, LocalDateTime.now(), TEST_USER_IP);
            flushAndClear();

            // when - threshold in future to match all ROTATED tokens
            LocalDateTime threshold = LocalDateTime.now().plusDays(1);
            int deleted = refreshTokenRepository.hardDeleteOldInactiveTokens(threshold);
            flushAndClear();

            // then
            assertThat(deleted).isEqualTo(1);
            assertThat(refreshTokenRepository.findById(tokenId)).isEmpty();
        }
    }
}
