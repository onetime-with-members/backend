package side.onetime.test;

import static com.epages.restdocs.apispec.ResourceDocumentation.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultActions;

import com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper;
import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.fasterxml.jackson.databind.ObjectMapper;

import side.onetime.configuration.ControllerTestConfig;
import side.onetime.controller.TestAuthController;
import side.onetime.dto.test.request.TestLoginRequest;
import side.onetime.dto.test.response.TestTokenResponse;
import side.onetime.exception.CustomException;
import side.onetime.exception.status.TestErrorStatus;
import side.onetime.service.TestAuthService;

@WebMvcTest(TestAuthController.class)
@TestPropertySource(properties = "test.auth.enabled=true")
public class TestAuthControllerTest extends ControllerTestConfig {

    @MockBean
    private TestAuthService testAuthService;

    @Test
    @DisplayName("테스트 로그인에 성공한다.")
    public void testLoginSuccess() throws Exception {
        // given
        String secretKey = "test-secret-key-1234567890";
        String accessToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test-access-token";
        String refreshToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test-refresh-token";
        TestTokenResponse response = TestTokenResponse.of(accessToken, refreshToken);

        Mockito.when(testAuthService.login(any(TestLoginRequest.class)))
                .thenReturn(response);

        TestLoginRequest request = new TestLoginRequest(secretKey);
        String requestContent = new ObjectMapper().writeValueAsString(request);

        // when
        ResultActions resultActions = mockMvc.perform(
                RestDocumentationRequestBuilders.post("/api/v1/test/auth/login")
                        .content(requestContent)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
        );

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_success").value(true))
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("테스트 로그인에 성공했습니다."))
                .andExpect(jsonPath("$.payload.access_token").value(accessToken))
                .andExpect(jsonPath("$.payload.refresh_token").value(refreshToken))

                // docs
                .andDo(MockMvcRestDocumentationWrapper.document("test/auth/login",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("Test API")
                                        .description("테스트 로그인 - E2E 테스트 환경에서 소셜 로그인 없이 토큰을 발급한다. (local, dev 환경에서만 동작)")
                                        .requestFields(
                                                fieldWithPath("secret_key").type(JsonFieldType.STRING).description("테스트 API 인증용 시크릿 키")
                                        )
                                        .responseFields(
                                                fieldWithPath("is_success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
                                                fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
                                                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                                                fieldWithPath("payload.access_token").type(JsonFieldType.STRING).description("액세스 토큰"),
                                                fieldWithPath("payload.refresh_token").type(JsonFieldType.STRING).description("리프레쉬 토큰")
                                        )
                                        .build()
                        )
                ));
    }

    @Test
    @DisplayName("[FAILED] 잘못된 시크릿 키로 테스트 로그인 시 실패한다.")
    public void testLoginFailWithInvalidSecretKey() throws Exception {
        // given
        String invalidSecretKey = "invalid-secret-key";

        Mockito.when(testAuthService.login(any(TestLoginRequest.class)))
                .thenThrow(new CustomException(TestErrorStatus._INVALID_SECRET_KEY));

        TestLoginRequest request = new TestLoginRequest(invalidSecretKey);
        String requestContent = new ObjectMapper().writeValueAsString(request);

        // when
        ResultActions resultActions = mockMvc.perform(
                RestDocumentationRequestBuilders.post("/api/v1/test/auth/login")
                        .content(requestContent)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
        );

        // then
        resultActions
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.is_success").value(false))
                .andExpect(jsonPath("$.code").value("TEST-001"))
                .andExpect(jsonPath("$.message").value("유효하지 않은 테스트 시크릿 키입니다."))
                .andDo(MockMvcRestDocumentationWrapper.document("test/auth/login-fail-invalid-secret-key",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("Test API")
                                        .build()
                        )
                ));
    }

    @Test
    @DisplayName("만료된 테스트 토큰 발급에 성공한다.")
    public void getExpiredTokenSuccess() throws Exception {
        // given
        String secretKey = "test-secret-key-1234567890";
        String expiredAccessToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.expired-access-token";
        TestTokenResponse response = TestTokenResponse.ofAccessToken(expiredAccessToken);

        Mockito.when(testAuthService.generateExpiredToken(any(TestLoginRequest.class)))
                .thenReturn(response);

        TestLoginRequest request = new TestLoginRequest(secretKey);
        String requestContent = new ObjectMapper().writeValueAsString(request);

        // when
        ResultActions resultActions = mockMvc.perform(
                RestDocumentationRequestBuilders.post("/api/v1/test/auth/expired-token")
                        .content(requestContent)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
        );

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_success").value(true))
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("만료된 테스트 토큰이 발급되었습니다."))
                .andExpect(jsonPath("$.payload.access_token").value(expiredAccessToken))
                .andExpect(jsonPath("$.payload.refresh_token").value(Matchers.nullValue()))

                // docs
                .andDo(MockMvcRestDocumentationWrapper.document("test/auth/expired-token",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("Test API")
                                        .description("만료된 토큰 발급 - E2E 테스트에서 401 처리, 토큰 재발급 플로우 테스트용 만료된 액세스 토큰을 발급한다. (local, dev 환경에서만 동작)")
                                        .requestFields(
                                                fieldWithPath("secret_key").type(JsonFieldType.STRING).description("테스트 API 인증용 시크릿 키")
                                        )
                                        .responseFields(
                                                fieldWithPath("is_success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
                                                fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
                                                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                                                fieldWithPath("payload.access_token").type(JsonFieldType.STRING).description("만료된 액세스 토큰"),
                                                fieldWithPath("payload.refresh_token").type(JsonFieldType.NULL).description("리프레쉬 토큰 (null)")
                                        )
                                        .build()
                        )
                ));
    }

    @Test
    @DisplayName("[FAILED] 잘못된 시크릿 키로 만료된 토큰 발급 시 실패한다.")
    public void getExpiredTokenFailWithInvalidSecretKey() throws Exception {
        // given
        String invalidSecretKey = "invalid-secret-key";

        Mockito.when(testAuthService.generateExpiredToken(any(TestLoginRequest.class)))
                .thenThrow(new CustomException(TestErrorStatus._INVALID_SECRET_KEY));

        TestLoginRequest request = new TestLoginRequest(invalidSecretKey);
        String requestContent = new ObjectMapper().writeValueAsString(request);

        // when
        ResultActions resultActions = mockMvc.perform(
                RestDocumentationRequestBuilders.post("/api/v1/test/auth/expired-token")
                        .content(requestContent)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
        );

        // then
        resultActions
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.is_success").value(false))
                .andExpect(jsonPath("$.code").value("TEST-001"))
                .andExpect(jsonPath("$.message").value("유효하지 않은 테스트 시크릿 키입니다."))
                .andDo(MockMvcRestDocumentationWrapper.document("test/auth/expired-token-fail-invalid-secret-key",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("Test API")
                                        .build()
                        )
                ));
    }
}
