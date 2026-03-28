package side.onetime.kakao;

import com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper;
import com.epages.restdocs.apispec.ResourceDocumentation;
import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.Schema;
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
import side.onetime.controller.KakaoController;
import side.onetime.dto.kakao.api.KakaoCalendarEventResponse;
import side.onetime.dto.kakao.request.CreateKakaoCalendarEventRequest;
import side.onetime.dto.kakao.request.KakaoTokenRequest;
import side.onetime.dto.kakao.response.KakaoTokenResponse;
import side.onetime.service.KakaoService;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(KakaoController.class)
public class KakaoControllerTest extends ControllerTestConfig {

    @MockBean
    private KakaoService kakaoService;

    @Test
    @DisplayName("카카오 인증 페이지 URL을 조회 및 리다이렉트한다.")
    public void getAuthorizeUrl() throws Exception {
        // given
        String authorizeUrl = "https://kauth.kakao.com/oauth/authorize?client_id=...&redirect_uri=...&response_type=code";
        Mockito.when(kakaoService.getAuthorizeUrl()).thenReturn(authorizeUrl);

        // when
        ResultActions resultActions = this.mockMvc.perform(RestDocumentationRequestBuilders.get("/api/v1/kakao/authorize-url")
                .accept(MediaType.APPLICATION_JSON));

        // then
        resultActions
                .andExpect(status().isFound())
                .andDo(MockMvcRestDocumentationWrapper.document("kakao/get-authorize-url",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        ResourceDocumentation.resource(
                                ResourceSnippetParameters.builder()
                                        .tag("Kakao API")
                                        .description("카카오 인증 페이지 URL을 조회 및 리다이렉트한다.")
                                        .build()
                        )
                ));
    }

    @Test
    @DisplayName("카카오 토큰을 발급한다.")
    public void getKakaoToken() throws Exception {
        // given
        String code = "sample_auth_code";
        String accessToken = "sample_access_token";
        KakaoTokenResponse response = new KakaoTokenResponse(accessToken);

        Mockito.when(kakaoService.getKakaoToken(anyString())).thenReturn(response);

        KakaoTokenRequest request = new KakaoTokenRequest(code);
        String requestContent = objectMapper.writeValueAsString(request);

        // when
        ResultActions resultActions = this.mockMvc.perform(RestDocumentationRequestBuilders.post("/api/v1/kakao/token")
                .content(requestContent)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON));

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_success").value(true))
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("카카오 토큰 발급에 성공했습니다."))
                .andExpect(jsonPath("$.payload.access_token").value(accessToken))
                .andDo(MockMvcRestDocumentationWrapper.document("kakao/get-token",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        ResourceDocumentation.resource(
                                ResourceSnippetParameters.builder()
                                        .tag("Kakao API")
                                        .description("인가 코드를 사용하여 카카오 토큰을 발급받습니다.")
                                        .requestFields(
                                                fieldWithPath("code").type(JsonFieldType.STRING).description("카카오 인가 코드")
                                        )
                                        .responseFields(
                                                fieldWithPath("is_success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
                                                fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
                                                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                                                fieldWithPath("payload").type(JsonFieldType.OBJECT).description("응답 데이터"),
                                                fieldWithPath("payload.access_token").type(JsonFieldType.STRING).description("액세스 토큰")
                                        )
                                        .requestSchema(Schema.schema("KakaoTokenRequestSchema"))
                                        .responseSchema(Schema.schema("KakaoTokenResponseSchema"))
                                        .build()
                        )
                ));
    }

    @Test
    @DisplayName("카카오 톡캘린더 이벤트를 생성한다.")
    public void createCalendarEvent() throws Exception {
        // given
        UUID eventId = UUID.randomUUID();
        String accessToken = "sample_access_token";

        String kakaoCalendarEventId = "sample_kakao_calendar_event_id";
        KakaoCalendarEventResponse response = new KakaoCalendarEventResponse(kakaoCalendarEventId);

        Mockito.when(kakaoService.createCalendarEvent(any(CreateKakaoCalendarEventRequest.class))).thenReturn(response);

        CreateKakaoCalendarEventRequest request = new CreateKakaoCalendarEventRequest(
                accessToken,
                eventId,
                "OneTime에 의해 추가된 일정입니다.",
                List.of(30, 1440),
                "LAVENDER",
                "FREQ=WEEKLY",
                "Asia/Seoul"
        );
        String requestContent = objectMapper.writeValueAsString(request);

        // when
        ResultActions resultActions = this.mockMvc.perform(RestDocumentationRequestBuilders.post("/api/v1/kakao/calendar/confirmation")
                .content(requestContent)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON));

        // then
        resultActions
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.is_success").value(true))
                .andExpect(jsonPath("$.code").value("201"))
                .andExpect(jsonPath("$.message").value("카카오 캘린더 일정 생성에 성공했습니다."))
                .andExpect(jsonPath("$.payload.event_id").value(kakaoCalendarEventId));

        // docs
        resultActions.andDo(MockMvcRestDocumentationWrapper.document("kakao/create-calendar-event",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                ResourceDocumentation.resource(
                        ResourceSnippetParameters.builder()
                                .tag("Kakao API")
                                .description("확정 정보를 기반으로 카카오 톡캘린더 일정을 생성합니다.")
                                .requestFields(
                                        fieldWithPath("access_token").type(JsonFieldType.STRING).description("카카오 액세스 토큰"),
                                        fieldWithPath("event_id").type(JsonFieldType.STRING).description("이벤트 ID (UUID)"),
                                        fieldWithPath("description").type(JsonFieldType.STRING).description("일정 설명 (기본값 있음)").optional(),
                                        fieldWithPath("reminders").type(JsonFieldType.ARRAY).description("리마인더 설정 (분 단위 리스트, 기본값 있음)").optional(),
                                        fieldWithPath("color").type(JsonFieldType.STRING).description("색상 텍스트 (기본값 있음)").optional(),
                                        fieldWithPath("rrule").type(JsonFieldType.STRING).description("반복 설정 (RRULE, 기본값 있음)").optional(),
                                        fieldWithPath("time_zone").type(JsonFieldType.STRING).description("타임존 (기본값: Asia/Seoul)").optional()
                                )
                                .responseFields(
                                        fieldWithPath("is_success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
                                        fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
                                        fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                                        fieldWithPath("payload").type(JsonFieldType.OBJECT).description("응답 데이터"),
                                        fieldWithPath("payload.event_id").type(JsonFieldType.STRING).description("생성된 카카오 이벤트 ID")
                                )
                                .requestSchema(Schema.schema("CreateKakaoCalendarEventRequestSchema"))
                                .responseSchema(Schema.schema("KakaoCalendarEventResponseSchema"))
                                .build()
                )
        ));
    }
}
