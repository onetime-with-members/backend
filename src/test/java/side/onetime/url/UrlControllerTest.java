package side.onetime.url;

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
import side.onetime.controller.UrlController;
import side.onetime.dto.url.request.ConvertToOriginalUrlRequest;
import side.onetime.dto.url.request.ConvertToShortenUrlRequest;
import side.onetime.dto.url.response.ConvertToOriginalUrlResponse;
import side.onetime.dto.url.response.ConvertToShortenUrlResponse;
import side.onetime.repository.EventRepository;
import side.onetime.service.UrlService;

import java.util.UUID;

import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UrlController.class)
public class UrlControllerTest extends ControllerTestConfig {

    @MockBean
    private UrlService urlService;

    @MockBean
    private EventRepository eventRepository;

    @Test
    @DisplayName("원본 URL을 단축 URL로 변환한다.")
    public void convertToShortenUrl() throws Exception {
        // given
        String originalUrl = "https://example.com/event/123e4567-e89b-12d3-a456-426614174000";
        ConvertToShortenUrlRequest request = new ConvertToShortenUrlRequest(originalUrl);
        String shortenUrl = "https://short.ly/abc123";

        Mockito.when(eventRepository.existsByEventId(any(UUID.class))).thenReturn(true);
        Mockito.when(urlService.convertToShortenUrl(any(ConvertToShortenUrlRequest.class)))
                .thenReturn(ConvertToShortenUrlResponse.of(shortenUrl));

        String requestContent = new ObjectMapper().writeValueAsString(request);

        // when
        ResultActions resultActions = this.mockMvc.perform(RestDocumentationRequestBuilders.post("/api/v1/urls/action-shorten")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestContent)
                .accept(MediaType.APPLICATION_JSON));

        // then
        resultActions
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.is_success").value(true))
                .andExpect(jsonPath("$.code").value("201"))
                .andExpect(jsonPath("$.message").value("단축 URL 변환에 성공했습니다."))
                .andExpect(jsonPath("$.payload.shorten_url").value(shortenUrl))

                // docs
                .andDo(MockMvcRestDocumentationWrapper.document("url/convert-to-shorten",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("URL API")
                                        .description("원본 URL을 단축 URL로 변환한다.")
                                        .requestFields(
                                                fieldWithPath("original_url").type(JsonFieldType.STRING).description("단축할 원본 URL")
                                        )
                                        .responseFields(
                                                fieldWithPath("is_success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
                                                fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
                                                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                                                fieldWithPath("payload").type(JsonFieldType.OBJECT).description("응답 데이터"),
                                                fieldWithPath("payload.shorten_url").type(JsonFieldType.STRING).description("생성된 단축 URL")
                                        )
                                        .build()
                        )
                ));
    }

    @Test
    @DisplayName("단축 URL을 원본 URL로 변환한다.")
    public void convertToOriginalUrl() throws Exception {
        // given
        String shortenUrl = "https://short.ly/abc123";
        ConvertToOriginalUrlRequest request = new ConvertToOriginalUrlRequest(shortenUrl);
        String originalUrl = "https://example.com/event/123e4567-e89b-12d3-a456-426614174000";

        Mockito.when(urlService.convertToOriginalUrl(any(ConvertToOriginalUrlRequest.class)))
                .thenReturn(ConvertToOriginalUrlResponse.of(originalUrl));
        Mockito.when(eventRepository.existsByEventId(any(UUID.class))).thenReturn(true);

        String requestContent = new ObjectMapper().writeValueAsString(request);

        // when
        ResultActions resultActions = this.mockMvc.perform(RestDocumentationRequestBuilders.post("/api/v1/urls/action-original")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestContent)
                .accept(MediaType.APPLICATION_JSON));

        // then
        resultActions
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.is_success").value(true))
                .andExpect(jsonPath("$.code").value("201"))
                .andExpect(jsonPath("$.message").value("원본 URL 변환에 성공했습니다."))
                .andExpect(jsonPath("$.payload.original_url").value(originalUrl))

                // docs
                .andDo(MockMvcRestDocumentationWrapper.document("url/convert-to-original",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("URL API")
                                        .description("단축 URL을 원본 URL로 변환한다.")
                                        .requestFields(
                                                fieldWithPath("shorten_url").type(JsonFieldType.STRING).description("복원할 단축 URL")
                                        )
                                        .responseFields(
                                                fieldWithPath("is_success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
                                                fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
                                                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                                                fieldWithPath("payload").type(JsonFieldType.OBJECT).description("응답 데이터"),
                                                fieldWithPath("payload.original_url").type(JsonFieldType.STRING).description("복원된 원본 URL")
                                        )
                                        .build()
                        )
                ));
    }
}
