package side.onetime.auth.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Public API임을 명시적으로 선언하는 마커 어노테이션.
 * - 이 어노테이션 자체는 권한 체크를 수행하지 않습니다 (SpEL 없음).
 * - 실제 허용은 SecurityConfig의 URL 레벨에서 처리됩니다.
 *
 * [사용 목적]
 * - 코드에서 "이 API는 의도적으로 Public"임을 명시
 * - 자동화 테스트에서 어노테이션 누락 검증에 활용
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PublicApi {
}
