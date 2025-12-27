package side.onetime.banner;

import com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper;
import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.Schema;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.restdocs.payload.JsonFieldType;
import side.onetime.auth.service.CustomUserDetailsService;
import side.onetime.configuration.ControllerTestConfig;
import side.onetime.controller.BannerController;
import side.onetime.dto.admin.response.GetAllActivatedBannersResponse;
import side.onetime.dto.admin.response.GetAllActivatedBarBannersResponse;
import side.onetime.dto.admin.response.GetBannerResponse;
import side.onetime.dto.admin.response.GetBarBannerResponse;
import side.onetime.service.BannerService;
import side.onetime.util.JwtUtil;

import java.util.List;

import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BannerController.class)
public class BannerControllerTest extends ControllerTestConfig {

    @MockBean
    private BannerService bannerService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("활성화된 배너를 전체 조회한다.")
    public void getAllActivatedBanners() throws Exception {
        // given
        List<GetBannerResponse> banners = List.of(
                new GetBannerResponse(1L, "OneTime", "OneTime's Title", "OneTime's Sub Title", "OneTime's Button Text", "#FFFFFF", "https://www.image.com", true, "2025-08-26 12:00:00", "https://www.link.com", 1L),
                new GetBannerResponse(2L, "OneTime2", "OneTime's Title2", "OneTime's Sub Title2", "OneTime's Button Text2", "#000000", "https://www.image.com", true, "2025-08-27 12:00:00", "https://www.link.com", 1L)
        );
        GetAllActivatedBannersResponse response = GetAllActivatedBannersResponse.from(banners);

        // when
        Mockito.when(bannerService.getAllActivatedBanners()).thenReturn(response);

        // then
        mockMvc.perform(RestDocumentationRequestBuilders.get("/api/v1/banners/activated/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_success").value(true))
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("활성화된 배너 전체 조회에 성공했습니다."))
                .andDo(MockMvcRestDocumentationWrapper.document("banner/activated-banner-get-all",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("Banner API")
                                        .description("활성화된 배너를 전체 조회한다.")
                                        .responseFields(
                                                fieldWithPath("is_success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
                                                fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
                                                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                                                fieldWithPath("payload").type(JsonFieldType.OBJECT).description("페이로드 객체"),
                                                fieldWithPath("payload.banners").type(JsonFieldType.ARRAY).description("배너 목록"),
                                                fieldWithPath("payload.banners[].id").type(JsonFieldType.NUMBER).description("배너 ID"),
                                                fieldWithPath("payload.banners[].organization").type(JsonFieldType.STRING).description("조직명"),
                                                fieldWithPath("payload.banners[].title").type(JsonFieldType.STRING).description("제목"),
                                                fieldWithPath("payload.banners[].sub_title").type(JsonFieldType.STRING).description("부제목"),
                                                fieldWithPath("payload.banners[].button_text").type(JsonFieldType.STRING).description("버튼 텍스트"),
                                                fieldWithPath("payload.banners[].color_code").type(JsonFieldType.STRING).description("색상 코드"),
                                                fieldWithPath("payload.banners[].image_url").type(JsonFieldType.STRING).description("배너 이미지 URL"),
                                                fieldWithPath("payload.banners[].is_activated").type(JsonFieldType.BOOLEAN).description("활성화 여부"),
                                                fieldWithPath("payload.banners[].created_date").type(JsonFieldType.STRING).description("생성일자"),
                                                fieldWithPath("payload.banners[].link_url").type(JsonFieldType.STRING).description("링크 URL"),
                                                fieldWithPath("payload.banners[].click_count").type(JsonFieldType.NUMBER).description("클릭 수")
                                        )
                                        .responseSchema(Schema.schema("GetAllActivatedBannersResponse"))
                                        .build()
                        )
                ));
    }

    @Test
    @DisplayName("활성화된 띠배너를 전체 조회한다.")
    public void getAllActivatedBarBanners() throws Exception {
        // given
        List<GetBarBannerResponse> barBanners = List.of(
                new GetBarBannerResponse(1L, "공지사항", "Notice", "#FF5733", "#FFFFFF", true, "2025-04-01 12:00:00", "https://www.link.com"),
                new GetBarBannerResponse(2L, "공지사항2", "Notice2", "#FF5733", "#FFFFFF", true, "2025-04-01 12:00:00", "https://www.link.com")
        );
        GetAllActivatedBarBannersResponse response = GetAllActivatedBarBannersResponse.from(barBanners);

        // when
        Mockito.when(bannerService.getAllActivatedBarBanners()).thenReturn(response);

        // then
        mockMvc.perform(RestDocumentationRequestBuilders.get("/api/v1/bar-banners/activated/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_success").value(true))
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("활성화된 띠배너 전체 조회에 성공했습니다."))
                .andDo(MockMvcRestDocumentationWrapper.document("banner/activated-bar-banner-get-all",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("Banner API")
                                        .description("활성화된 띠배너를 전체 조회한다.")
                                        .responseFields(
                                                fieldWithPath("is_success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
                                                fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
                                                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                                                fieldWithPath("payload").type(JsonFieldType.OBJECT).description("페이로드 객체"),
                                                fieldWithPath("payload.bar_banners").type(JsonFieldType.ARRAY).description("띠배너 목록"),
                                                fieldWithPath("payload.bar_banners[].id").type(JsonFieldType.NUMBER).description("띠배너 ID"),
                                                fieldWithPath("payload.bar_banners[].content_kor").type(JsonFieldType.STRING).description("한국어 내용"),
                                                fieldWithPath("payload.bar_banners[].content_eng").type(JsonFieldType.STRING).description("영어 내용"),
                                                fieldWithPath("payload.bar_banners[].background_color_code").type(JsonFieldType.STRING).description("배경 색상 코드"),
                                                fieldWithPath("payload.bar_banners[].text_color_code").type(JsonFieldType.STRING).description("텍스트 색상 코드"),
                                                fieldWithPath("payload.bar_banners[].is_activated").type(JsonFieldType.BOOLEAN).description("활성화 여부"),
                                                fieldWithPath("payload.bar_banners[].created_date").type(JsonFieldType.STRING).description("생성일자"),
                                                fieldWithPath("payload.bar_banners[].link_url").type(JsonFieldType.STRING).description("링크 URL")
                                        )
                                        .responseSchema(Schema.schema("GetAllActivatedBarBannersResponse"))
                                        .build()
                        )
                ));
    }

    @Test
    @DisplayName("배너 클릭 수가 1 증가한다.")
    public void increaseBannerClickCount() throws Exception {
        // given
        Long bannerId = 1L;

        // when
        Mockito.doNothing().when(bannerService).increaseBannerClickCount(any(Long.class));

        // then
        mockMvc.perform(RestDocumentationRequestBuilders.patch("/api/v1/banners/{id}/clicks", bannerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_success").value(true))
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("배너 클릭 수 증가에 성공했습니다."))
                .andDo(MockMvcRestDocumentationWrapper.document("banner/increase-click-count",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("Banner API")
                                        .description("배너 클릭 수를 1 증가한다.")
                                        .pathParameters(
                                                parameterWithName("id").description("클릭한 배너 ID")
                                        )
                                        .responseFields(
                                                fieldWithPath("is_success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
                                                fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
                                                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지")
                                        )
                                        .build()
                        )
                ));
    }
}
