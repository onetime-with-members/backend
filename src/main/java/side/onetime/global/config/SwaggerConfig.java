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
import side.onetime.exception.CustomException;
import side.onetime.global.common.status.ErrorStatus;

@Configuration
public class SwaggerConfig {

	@Bean
	public OpenAPI customOpenAPI() {
		OpenAPI openAPI = new OpenAPI()
			.info(new Info()
				.title("OneTime API Documentation")
				.version("v1.6.2")
				.description("Spring REST Docs with Swagger UI.")
				.contact(new Contact()
					.name("Sangho Han")
					.url("https://github.com/bbbang105")
					.email("hchsa77@gmail.com"))
			);
		try {
			// ✅ Swagger 전용 ObjectMapper 사용
			ObjectMapper swaggerMapper = Json.mapper();
			ClassPathResource resource = new ClassPathResource("static/docs/open-api-3.0.1.json");
			try (InputStream inputStream = resource.getInputStream()) {
				// REST Docs에서 생성한 open-api JSON -> OpenAPI 객체로 변환
				OpenAPI restDocsOpenAPI = swaggerMapper.readValue(inputStream, OpenAPI.class);

				// REST Docs Paths 적용
				if (restDocsOpenAPI.getPaths() != null) {
					openAPI.setPaths(restDocsOpenAPI.getPaths());
				}

				// REST Docs Components + Security 병합
				Components components = restDocsOpenAPI.getComponents() != null
					? restDocsOpenAPI.getComponents()
					: new Components();

				SecurityScheme bearerAuth = new SecurityScheme()
					.type(SecurityScheme.Type.HTTP)
					.scheme("bearer")
					.bearerFormat("JWT");

				components.addSecuritySchemes("bearerAuth", bearerAuth);

				openAPI.components(components)
					.addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
			}
		} catch (Exception e) {
			throw new CustomException(ErrorStatus._FAILED_TRANSLATE_SWAGGER);
		}

		return openAPI;
	}
}
