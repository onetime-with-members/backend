package side.onetime.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class RestClientConfig {

    @Value("${app.sync.target-url:}")
    private String targetUrl;

    @Bean
    public RestClient bannerClient() {
        return RestClient.builder()
                .requestFactory(new SimpleClientHttpRequestFactory() {{
                    setConnectTimeout(Duration.ofSeconds(5));
                    setReadTimeout(Duration.ofSeconds(30));
                }})
                .baseUrl(targetUrl)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
