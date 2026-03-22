package side.onetime.security;

import static org.assertj.core.api.Assertions.*;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
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

/**
 * 모든 API 엔드포인트에 보안 어노테이션이 적용되어 있는지 검증하는 테스트.
 * - Spring 컨텍스트 없이 클래스패스 스캔만으로 검증하여 빠르게 실행됩니다.
 * - 이 테스트는 CI/CD 파이프라인에서 자동으로 어노테이션 누락을 검출합니다.
 */
class SecurityAnnotationTest {

    private static final String BASE_PACKAGE = "side.onetime";

    @Test
    @DisplayName("모든 API 엔드포인트는 권한 어노테이션이 있어야 한다")
    void allApiEndpointsShouldHaveSecurityAnnotation() {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));

        Set<BeanDefinition> controllerBeans = scanner.findCandidateComponents(BASE_PACKAGE);

        List<String> missingAnnotations = new ArrayList<>();

        for (BeanDefinition bd : controllerBeans) {
            Class<?> clazz;
            try {
                clazz = Class.forName(bd.getBeanClassName());
            } catch (ClassNotFoundException e) {
                continue;
            }

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
                    String httpMethod = getHttpMethod(method);
                    String path = resolveEndpointPath(method, clazz);
                    missingAnnotations.add(String.format("%s %s (%s.%s)",
                            httpMethod, path, clazz.getSimpleName(), method.getName()));
                }
            }
        }

        if (!missingAnnotations.isEmpty()) {
            String report = "\n\n=== 보안 어노테이션 누락 API (" + missingAnnotations.size() + "건) ===\n"
                    + missingAnnotations.stream()
                        .map(api -> "  - " + api)
                        .reduce((a, b) -> a + "\n" + b)
                        .orElse("")
                    + "\n" + "=".repeat(50) + "\n";
            fail(report);
        }
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

    private String getHttpMethod(Method method) {
        if (method.isAnnotationPresent(GetMapping.class)) return "GET";
        if (method.isAnnotationPresent(PostMapping.class)) return "POST";
        if (method.isAnnotationPresent(PutMapping.class)) return "PUT";
        if (method.isAnnotationPresent(PatchMapping.class)) return "PATCH";
        if (method.isAnnotationPresent(DeleteMapping.class)) return "DELETE";
        return "REQUEST";
    }

    private String resolveEndpointPath(Method method, Class<?> clazz) {
        String basePath = "";
        RequestMapping classMapping = clazz.getAnnotation(RequestMapping.class);
        if (classMapping != null && classMapping.value().length > 0) {
            basePath = classMapping.value()[0];
        }

        String methodPath = "";
        if (method.isAnnotationPresent(GetMapping.class)) {
            String[] values = method.getAnnotation(GetMapping.class).value();
            methodPath = values.length > 0 ? values[0] : "";
        } else if (method.isAnnotationPresent(PostMapping.class)) {
            String[] values = method.getAnnotation(PostMapping.class).value();
            methodPath = values.length > 0 ? values[0] : "";
        } else if (method.isAnnotationPresent(PutMapping.class)) {
            String[] values = method.getAnnotation(PutMapping.class).value();
            methodPath = values.length > 0 ? values[0] : "";
        } else if (method.isAnnotationPresent(PatchMapping.class)) {
            String[] values = method.getAnnotation(PatchMapping.class).value();
            methodPath = values.length > 0 ? values[0] : "";
        } else if (method.isAnnotationPresent(DeleteMapping.class)) {
            String[] values = method.getAnnotation(DeleteMapping.class).value();
            methodPath = values.length > 0 ? values[0] : "";
        }

        return basePath + methodPath;
    }
}
