package side.onetime.token;

import com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper;
import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.web.servlet.ResultActions;
import side.onetime.configuration.ControllerTestConfig;
import side.onetime.controller.TokenController;
import side.onetime.dto.token.request.ReissueTokenRequest;
import side.onetime.dto.token.response.ReissueTokenResponse;
import side.onetime.service.TokenService;

import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TokenController.class)
public class TokenControllerTest extends ControllerTestConfig {

    @MockBean
    private TokenService tokenService;

    @Test
    @DisplayName("액세스 토큰을 재발행한다.")
    public void reissueTokenSuccess() throws Exception {
        // given
        String oldRefreshToken = "sampleOldRefreshToken";
        String newAccessToken = "newAccessToken";
        String newRefreshToken = "newRefreshToken";
        ReissueTokenResponse response = ReissueTokenResponse.of(newAccessToken, newRefreshToken);

        Mockito.when(tokenService.reissueToken(any(ReissueTokenRequest.class)))
                .thenReturn(response);

        ReissueTokenRequest request = new ReissueTokenRequest(oldRefreshToken);
        String requestContent = new ObjectMapper().writeValueAsString(request);

        // when
        ResultActions resultActions = mockMvc.perform(
                RestDocumentationRequestBuilders.post("/api/v1/tokens/action-reissue")
                        .content(requestContent)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
        );

        // then
        resultActions
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.is_success").value(true))
                .andExpect(jsonPath("$.code").value("201"))
                .andExpect(jsonPath("$.message").value("토큰 재발행에 성공했습니다."))
                .andExpect(jsonPath("$.payload.access_token").value(newAccessToken))
                .andExpect(jsonPath("$.payload.refresh_token").value(newRefreshToken))

                // docs
                .andDo(MockMvcRestDocumentationWrapper.document("token/reissue",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("Token API")
                                        .description("액세스 토큰을 재발행한다.")
                                        .requestFields(
                                                fieldWithPath("refresh_token").type(JsonFieldType.STRING).description("기존 리프레쉬 토큰")
                                        )
                                        .responseFields(
                                                fieldWithPath("is_success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
                                                fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
                                                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                                                fieldWithPath("payload.access_token").type(JsonFieldType.STRING).description("새로운 액세스 토큰"),
                                                fieldWithPath("payload.refresh_token").type(JsonFieldType.STRING).description("새로운 리프레쉬 토큰")
                                        )
                                        .build()
                        )
                ));
    }
}
