package side.onetime.global.config;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import lombok.RequiredArgsConstructor;
import side.onetime.global.config.sync.BannerSyncProperties;

@Configuration
@RequiredArgsConstructor
public class RestClientConfig {
	
	private final BannerSyncProperties syncProperties;
	
	@Bean
	public RestClient bannerClient() {
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(Duration.ofSeconds(5));
		factory.setReadTimeout(Duration.ofSeconds(30));
		
		return RestClient.builder()
			.requestFactory(factory)
			.baseUrl(syncProperties.targetUrl())
			.defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
			.build();
	}
}
