package side.onetime.global.config;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;

import feign.RequestInterceptor;

@Configuration
@EnableFeignClients(basePackages = "side.onetime.infra")
public class FeignConfig {

	/**
	 * Everytime API용 Request Interceptor
	 */
	@Bean
	public RequestInterceptor everytimeRequestInterceptor() {
		return requestTemplate -> {
			// "everytime"으로 시작하는 Feign Client에만 헤더 적용
			if (requestTemplate.feignTarget().name().startsWith("everytime")) {
				requestTemplate.header(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
				requestTemplate.header(HttpHeaders.REFERER, "https://everytime.kr/");
			}
		};
	}
}
