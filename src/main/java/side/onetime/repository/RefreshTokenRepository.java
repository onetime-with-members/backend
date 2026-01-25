package side.onetime.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import side.onetime.domain.RefreshToken;
import side.onetime.repository.custom.RefreshTokenRepositoryCustom;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long>, RefreshTokenRepositoryCustom {

    /**
     * jti(JWT ID)로 RefreshToken 조회
     *
     * @param jti JWT 고유 식별자
     * @return RefreshToken
     */
    Optional<RefreshToken> findByJti(String jti);

    /**
     * 원자적 업데이트: ACTIVE 상태인 경우에만 ROTATED로 변경
     * Race condition 방지를 위해 WHERE 절에서 상태 체크
     * updatedDate도 함께 업데이트 (JPA auditing이 bulk 쿼리에서 동작하지 않음)
     *
     * @param tokenId    토큰 ID
     * @param lastUsedAt 마지막 사용 시각
     * @param lastUsedIp 마지막 사용 IP
     * @return 업데이트된 행 수 (0이면 이미 rotate됨)
     */
    @Modifying
    @Query("""
        UPDATE RefreshToken r
        SET r.status = 'ROTATED',
            r.lastUsedAt = :lastUsedAt,
            r.lastUsedIp = :lastUsedIp,
            r.updatedDate = :lastUsedAt
        WHERE r.id = :tokenId
          AND r.status = 'ACTIVE'
    """)
    int markAsRotatedIfActive(@Param("tokenId") Long tokenId,
                              @Param("lastUsedAt") LocalDateTime lastUsedAt,
                              @Param("lastUsedIp") String lastUsedIp);
}
