package side.onetime.security;

import static org.assertj.core.api.Assertions.*;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import side.onetime.auth.annotation.IsAdmin;
import side.onetime.auth.annotation.IsMasterAdmin;
import side.onetime.auth.annotation.IsUser;
import side.onetime.auth.annotation.PublicApi;
import side.onetime.configuration.DatabaseTestConfig;

/**
 * 모든 API 엔드포인트에 보안 어노테이션이 적용되어 있는지 검증하는 테스트.
 * - 이 테스트는 CI/CD 파이프라인에서 자동으로 어노테이션 누락을 검출합니다.
 */
@SpringBootTest
class SecurityAnnotationTest extends DatabaseTestConfig {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    @DisplayName("모든 API 엔드포인트는 권한 어노테이션이 있어야 한다")
    void allApiEndpointsShouldHaveSecurityAnnotation() {
        Map<String, Object> controllers = applicationContext.getBeansWithAnnotation(RestController.class);

        List<String> missingAnnotations = new ArrayList<>();

        for (Object controller : controllers.values()) {
            Class<?> clazz = AopUtils.getTargetClass(controller);

            // 클래스 레벨에 보안 어노테이션이 있으면 스킵
            if (hasSecurityAnnotation(clazz)) {
                continue;
            }

            for (Method method : clazz.getDeclaredMethods()) {
                if (!hasRequestMapping(method)) {
                    continue;
                }
                if (!isApiEndpoint(method, clazz)) {
                    continue;
                }

                if (!hasSecurityAnnotation(method)) {
                    missingAnnotations.add(clazz.getSimpleName() + "." + method.getName());
                }
            }
        }

        assertThat(missingAnnotations)
                .as("다음 메서드에 보안 어노테이션(@PublicApi, @IsUser, @IsAdmin 등)이 누락됨")
                .isEmpty();
    }

    private boolean hasSecurityAnnotation(AnnotatedElement element) {
        return element.isAnnotationPresent(PublicApi.class)
                || element.isAnnotationPresent(IsUser.class)
                || element.isAnnotationPresent(IsAdmin.class)
                || element.isAnnotationPresent(IsMasterAdmin.class)
                || element.isAnnotationPresent(PreAuthorize.class);
    }

    private boolean hasRequestMapping(Method method) {
        return method.isAnnotationPresent(RequestMapping.class)
                || method.isAnnotationPresent(GetMapping.class)
                || method.isAnnotationPresent(PostMapping.class)
                || method.isAnnotationPresent(PutMapping.class)
                || method.isAnnotationPresent(PatchMapping.class)
                || method.isAnnotationPresent(DeleteMapping.class);
    }

    private boolean isApiEndpoint(Method method, Class<?> clazz) {
        RequestMapping classMapping = clazz.getAnnotation(RequestMapping.class);
        if (classMapping != null) {
            String[] paths = classMapping.value();
            for (String path : paths) {
                if (path.startsWith("/api/") || path.startsWith("/admin")) {
                    return true;
                }
            }
        }
        return false;
    }
}
