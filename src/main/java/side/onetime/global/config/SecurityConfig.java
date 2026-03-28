package side.onetime.global.config;

import java.util.Arrays;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HttpBasicConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import lombok.RequiredArgsConstructor;
import side.onetime.auth.exception.CustomAccessDeniedHandler;
import side.onetime.auth.exception.CustomAuthenticationEntryPoint;
import side.onetime.auth.handler.OAuthLoginFailureHandler;
import side.onetime.auth.handler.OAuthLoginSuccessHandler;
import side.onetime.global.filter.JwtFilter;

@RequiredArgsConstructor
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
	
	private final JwtFilter jwtFilter;
	private final OAuthLoginSuccessHandler oAuthLoginSuccessHandler;
	private final OAuthLoginFailureHandler oAuthLoginFailureHandler;
	private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
	private final CustomAccessDeniedHandler customAccessDeniedHandler;
	
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	private static final String[] SWAGGER_URLS = {
		"/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html"
	};

	/**
	 * [GET 요청]에 한해서만 Public인 URL
	 */
	private static final String[] PUBLIC_GET_URLS = {
		"/api/v1/events/**",            // 이벤트 조회, QR, 참여자 조회 등
		"/api/v1/schedules/**",         // 스케줄 조회
		"/api/v1/banners/activated/all",
		"/api/v1/bar-banners/activated/all",
		"/api/v1/kakao/authorize-url"
	};
	
	/**
	 * [POST 요청]에 한해서만 Public인 URL
	 */
	private static final String[] PUBLIC_POST_URLS = {
		"/api/v1/users/onboarding",
		"/api/v1/admin/login",
		"/api/v1/admin/register",
		"/api/v1/members/action-register",
		"/api/v1/members/action-login",
		"/api/v1/members/name/action-check",
		"/api/v1/tokens/action-reissue",
		"/api/v1/urls/action-shorten",
		"/api/v1/urls/action-original",
		"/api/v1/events",                         // 익명 이벤트 생성
		"/api/v1/events/*/most/filtering",        // 필터링 조회
		"/api/v1/schedules/day",                  // 스케줄 등록
		"/api/v1/schedules/date",                 // 스케줄 등록
		"/api/v1/schedules/day/*/filtering",      // 스케줄 필터링
		"/api/v1/schedules/date/*/filtering",     // 스케줄 필터링
        "/api/v1/banners/staging",
        "/api/v1/bar-banners/staging",
        "/api/v1/kakao/**",
	};

	/**
	 * [PATCH 요청]에 한해서만 Public인 URL
	 */
	private static final String[] PUBLIC_PATCH_URLS = {
		"/api/v1/banners/*/clicks",
		"/api/v1/events/*"
	};
	
	/**
	 * [PUT 요청]에 한해서만 Public인 URL
	 */
	private static final String[] PUBLIC_PUT_URLS = {
		"/api/v1/events/*/confirm",        		  // 이벤트 확정
	};
	
	private static final String[] ALLOWED_ORIGINS = {
		"http://localhost:5173",
		"http://localhost:3000",
		"https://onetime-with-members.com",
		"https://www.onetime-with-members.com",
		"https://1-ti.me",
		"https://dev-app.onetime-with-members.workers.dev",
		"https://admin.onetime-with-members.workers.dev",
		"https://dev-admin.onetime-with-members.workers.dev",
		"https://discord.onetime.run",
	};

	@Bean
	CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOrigins(Arrays.asList(ALLOWED_ORIGINS));
		config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
		config.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept", "Set-Cookie"));
		config.setAllowCredentials(true);
		config.setExposedHeaders(Arrays.asList("Authorization", "Set-Cookie"));
		config.setMaxAge(3600L);
		
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return source;
	}

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
		httpSecurity
			.httpBasic(HttpBasicConfigurer::disable)
			.cors(corsConfigurer -> corsConfigurer.configurationSource(corsConfigurationSource()))
			.csrf(AbstractHttpConfigurer::disable)
			.authorizeHttpRequests(authorize -> authorize
				// 1. 인프라 & Swagger (메서드 상관없이 허용)
				.requestMatchers(SWAGGER_URLS).permitAll()
				.requestMatchers("/actuator/health", "/", "/api/v1/test/**").permitAll()
				// 2. GET Public
				.requestMatchers(HttpMethod.GET, PUBLIC_GET_URLS).permitAll()
				// 3. POST Public
				.requestMatchers(HttpMethod.POST, PUBLIC_POST_URLS).permitAll()
				// 4. PATCH Public
				.requestMatchers(HttpMethod.PATCH, PUBLIC_PATCH_URLS).permitAll()
				// 5. PUT Public
				.requestMatchers(HttpMethod.PUT, PUBLIC_PUT_URLS).permitAll()
				// 6. Safety-Net: 위에서 허용되지 않은 /api/** 요청은 인증 필요
				.requestMatchers("/api/**").authenticated()
				// 7. 어드민 페이지: 로그인/정적 리소스 허용, 나머지 인증 필요
				.requestMatchers("/admin/login", "/admin/css/**", "/admin/js/**", "/admin/vendor/**", "/admin/manifest.json").permitAll()
				.requestMatchers("/admin/**").authenticated()
				// 8. 그 외 (정적 리소스 등)
				.anyRequest().permitAll()
			)
			.oauth2Login(oauth -> oauth
				.successHandler(oAuthLoginSuccessHandler)
				.failureHandler(oAuthLoginFailureHandler)
			)
			.exceptionHandling(exception -> exception
				.authenticationEntryPoint(customAuthenticationEntryPoint)
				.accessDeniedHandler(customAccessDeniedHandler)
			)
			.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
		
		return httpSecurity.build();
	}
}
