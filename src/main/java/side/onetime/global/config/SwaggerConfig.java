package side.onetime.global.config;

import java.io.InputStream;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import lombok.extern.slf4j.Slf4j;
import side.onetime.exception.CustomException;
import side.onetime.global.common.status.ErrorStatus;

@Slf4j
@Configuration
public class SwaggerConfig {
	
	@Bean
	public OpenAPI customOpenAPI() {
		OpenAPI openAPI = new OpenAPI()
			.info(new Info()
				.title("OneTime API Documentation")
				.version("v1.6.9")
				.description("Spring REST Docs with Swagger UI.")
				.contact(new Contact()
					.name("Sangho Han")
					.url("https://github.com/bbbang105")
					.email("hchsa77@gmail.com"))
			);
		
		try {
			ClassPathResource resource = new ClassPathResource("static/docs/open-api-3.0.1.json");
			
			// 파일이 존재할 때만 병합 로직 수행 (테스트 환경 대응)
			if (resource.exists()) {
				ObjectMapper swaggerMapper = Json.mapper();
				try (InputStream inputStream = resource.getInputStream()) {
					OpenAPI restDocsOpenAPI = swaggerMapper.readValue(inputStream, OpenAPI.class);
					
					if (restDocsOpenAPI.getPaths() != null) {
						openAPI.setPaths(restDocsOpenAPI.getPaths());
					}
					
					Components components = restDocsOpenAPI.getComponents() != null
						? restDocsOpenAPI.getComponents()
						: new Components();
					
					addSecuritySchemes(components);
					openAPI.components(components);
				}
			} else {
				// 파일이 없으면 기본 Security 설정만 추가 (테스트 실행 중에는 파일이 없을 수 있음)
				log.warn("OpenAPI JSON 파일을 찾을 수 없어 병합을 건너뜁니다. (테스트 환경에서는 정상)");
				Components components = new Components();
				addSecuritySchemes(components);
				openAPI.components(components);
			}
			
			// Security Requirement 공통 추가
			openAPI.addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
			
		} catch (Exception e) {
			// 파일 읽기 중 에러가 난 경우에만 예외 발생
			log.error("Swagger 설정 중 오류가 발생했습니다.", e);
			throw new CustomException(ErrorStatus._FAILED_TRANSLATE_SWAGGER);
		}
		
		return openAPI;
	}
	
	private void addSecuritySchemes(Components components) {
		SecurityScheme bearerAuth = new SecurityScheme()
			.type(SecurityScheme.Type.HTTP)
			.scheme("bearer")
			.bearerFormat("JWT");
		components.addSecuritySchemes("bearerAuth", bearerAuth);
	}
}
