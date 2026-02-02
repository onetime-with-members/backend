package side.onetime.barbanner;

import com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper;
import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.Schema;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.restdocs.payload.JsonFieldType;
import side.onetime.configuration.AdminControllerTestConfig;
import side.onetime.controller.BarBannerController;
import side.onetime.dto.admin.response.PageInfo;
import side.onetime.dto.barbanner.request.ExportBarBannerRequest;
import side.onetime.dto.barbanner.request.RegisterBarBannerRequest;
import side.onetime.dto.barbanner.request.UpdateBarBannerRequest;
import side.onetime.dto.barbanner.response.GetAllActivatedBarBannersResponse;
import side.onetime.dto.barbanner.response.GetAllBarBannersResponse;
import side.onetime.dto.barbanner.response.GetBarBannerResponse;
import side.onetime.exception.CustomException;
import side.onetime.exception.status.AdminErrorStatus;
import side.onetime.service.BarBannerService;

import java.util.List;

import static com.epages.restdocs.apispec.ResourceDocumentation.headerWithName;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BarBannerController.class)
public class BarBannerControllerTest extends AdminControllerTestConfig {

    @MockBean
    private BarBannerService barBannerService;

    @Test
    @DisplayName("띠배너를 등록한다.")
    public void registerBarBanner() throws Exception {
        // given
        RegisterBarBannerRequest request = new RegisterBarBannerRequest(
                "최신 소식 안내",
                "News",
                "#FFFFFF",
                "#FFFFFF",
                "https://www.link.com"
        );
        String requestContent = objectMapper.writeValueAsString(request);

        // when
        Mockito.doNothing().when(barBannerService).registerBarBanner(any(RegisterBarBannerRequest.class));

        // then
        mockMvc.perform(RestDocumentationRequestBuilders.post("/api/v1/bar-banners/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestContent))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.is_success").value(true))
                .andExpect(jsonPath("$.code").value("201"))
                .andExpect(jsonPath("$.message").value("띠배너 등록에 성공했습니다."))
                .andDo(MockMvcRestDocumentationWrapper.document("bar-banner/register",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("BarBanner API")
                                        .description("띠배너를 등록한다.")
                                        .requestFields(
                                                fieldWithPath("content_kor").type(JsonFieldType.STRING).description("한국어 내용"),
                                                fieldWithPath("content_eng").type(JsonFieldType.STRING).description("영어 내용"),
                                                fieldWithPath("background_color_code").type(JsonFieldType.STRING).description("배경 색상 코드"),
                                                fieldWithPath("text_color_code").type(JsonFieldType.STRING).description("텍스트 색상 코드"),
                                                fieldWithPath("link_url").type(JsonFieldType.STRING).optional().description("링크 URL")
                                        )
                                        .responseFields(
                                                fieldWithPath("is_success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
                                                fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
                                                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지")
                                        )
                                        .requestSchema(Schema.schema("RegisterBarBannerRequest"))
                                        .responseSchema(Schema.schema("CommonSuccessResponse"))
                                        .build()
                        )
                ));
    }

    @Test
    @DisplayName("띠배너를 단건 조회한다.")
    public void getBarBanner() throws Exception {
        // given
        Long barBannerId = 1L;
        GetBarBannerResponse response = new GetBarBannerResponse(
                barBannerId,
                "공지사항", "Notice", "#FF5733", "#FFFFFF", true, "2025-04-01 12:00:00", "https://www.link.com"
        );

        // when
        Mockito.when(barBannerService.getBarBanner(any(Long.class))).thenReturn(response);

        // then
        mockMvc.perform(RestDocumentationRequestBuilders.get("/api/v1/bar-banners/{id}", barBannerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_success").value(true))
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("띠배너 단건 조회에 성공했습니다."))
                .andDo(MockMvcRestDocumentationWrapper.document("bar-banner/get-one",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("BarBanner API")
                                        .description("띠배너를 단건 조회한다.")
                                        .pathParameters(
                                                parameterWithName("id").description("조회할 띠배너 ID")
                                        )
                                        .responseFields(
                                                fieldWithPath("is_success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
                                                fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
                                                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                                                fieldWithPath("payload").type(JsonFieldType.OBJECT).description("띠배너 정보"),
                                                fieldWithPath("payload.id").type(JsonFieldType.NUMBER).description("띠배너 ID"),
                                                fieldWithPath("payload.content_kor").type(JsonFieldType.STRING).description("한국어 내용"),
                                                fieldWithPath("payload.content_eng").type(JsonFieldType.STRING).description("영어 내용"),
                                                fieldWithPath("payload.background_color_code").type(JsonFieldType.STRING).description("배경 색상 코드"),
                                                fieldWithPath("payload.text_color_code").type(JsonFieldType.STRING).description("텍스트 색상 코드"),
                                                fieldWithPath("payload.is_activated").type(JsonFieldType.BOOLEAN).description("활성화 여부"),
                                                fieldWithPath("payload.created_date").type(JsonFieldType.STRING).description("생성일자"),
                                                fieldWithPath("payload.link_url").type(JsonFieldType.STRING).description("링크 URL")
                                        )
                                        .responseSchema(Schema.schema("GetBarBannerResponse"))
                                        .build()
                        )
                ));
    }

    @Test
    @DisplayName("띠배너를 전체 조회한다.")
    public void getAllBarBanners() throws Exception {
        // given
        int page = 1;

        List<GetBarBannerResponse> barBanners = List.of(
                new GetBarBannerResponse(1L, "공지사항", "Notice", "#FF5733", "#FFFFFF", true, "2025-04-01 12:00:00", "https://www.link.com"),
                new GetBarBannerResponse(2L, "공지사항2", "Notice2", "#FF5733", "#FFFFFF", true, "2025-04-01 12:00:00", "https://www.link.com")
        );

        PageInfo pageInfo = PageInfo.of(1, 20, 2, 1);
        GetAllBarBannersResponse response = GetAllBarBannersResponse.of(barBanners, pageInfo);

        // when
        Mockito.when(barBannerService.getAllBarBanners(any(Pageable.class)))
                .thenReturn(response);

        // then
        mockMvc.perform(RestDocumentationRequestBuilders.get("/api/v1/bar-banners/all")
                        .param("page", String.valueOf(page)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_success").value(true))
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("띠배너 전체 조회에 성공했습니다."))
                .andExpect(jsonPath("$.payload.bar_banners[0].id").value(1L))
                .andExpect(jsonPath("$.payload.bar_banners[1].id").value(2L))
                .andExpect(jsonPath("$.payload.page_info.page").value(1))
                .andExpect(jsonPath("$.payload.page_info.size").value(20))
                .andExpect(jsonPath("$.payload.page_info.total_elements").value(2))
                .andExpect(jsonPath("$.payload.page_info.total_pages").value(1))
                .andDo(MockMvcRestDocumentationWrapper.document("bar-banner/get-all",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("BarBanner API")
                                        .description("띠배너를 전체 조회한다.")
                                        .queryParameters(
                                                parameterWithName("page").description("조회할 페이지 번호 (1부터 시작)")
                                        )
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
                                                fieldWithPath("payload.bar_banners[].link_url").type(JsonFieldType.STRING).description("링크 URL"),
                                                fieldWithPath("payload.page_info.page").type(JsonFieldType.NUMBER).description("현재 페이지 번호"),
                                                fieldWithPath("payload.page_info.size").type(JsonFieldType.NUMBER).description("페이지당 항목 수"),
                                                fieldWithPath("payload.page_info.total_elements").type(JsonFieldType.NUMBER).description("전체 항목 수"),
                                                fieldWithPath("payload.page_info.total_pages").type(JsonFieldType.NUMBER).description("전체 페이지 수")
                                        )
                                        .responseSchema(Schema.schema("GetAllBarBannersResponse"))
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
        Mockito.when(barBannerService.getAllActivatedBarBanners()).thenReturn(response);

        // then
        mockMvc.perform(RestDocumentationRequestBuilders.get("/api/v1/bar-banners/activated/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_success").value(true))
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("활성화된 띠배너 전체 조회에 성공했습니다."))
                .andDo(MockMvcRestDocumentationWrapper.document("bar-banner/get-all-activated",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("BarBanner API")
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
    @DisplayName("띠배너를 수정한다.")
    public void updateBarBanner() throws Exception {
        // given
        Long barBannerId = 1L;
        UpdateBarBannerRequest request = new UpdateBarBannerRequest(
                "수정된 내용",
                "modified content",
                "#123456",
                "#FFFFFF",
                true,
                "https://www.link.com"
        );
        String requestContent = objectMapper.writeValueAsString(request);

        // when
        Mockito.doNothing().when(barBannerService).updateBarBanner(eq(barBannerId), any(UpdateBarBannerRequest.class));

        // then
        mockMvc.perform(RestDocumentationRequestBuilders.patch("/api/v1/bar-banners/{id}", barBannerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestContent))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_success").value(true))
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("띠배너 수정에 성공했습니다."))
                .andDo(MockMvcRestDocumentationWrapper.document("bar-banner/update",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("BarBanner API")
                                        .description("띠배너를 수정한다.")
                                        .requestFields(
                                                fieldWithPath("content_kor").type(JsonFieldType.STRING).optional().description("한국어 내용"),
                                                fieldWithPath("content_eng").type(JsonFieldType.STRING).optional().description("영어 내용"),
                                                fieldWithPath("background_color_code").type(JsonFieldType.STRING).optional().description("배경 색상 코드"),
                                                fieldWithPath("text_color_code").type(JsonFieldType.STRING).optional().description("텍스트 색상 코드"),
                                                fieldWithPath("is_activated").type(JsonFieldType.BOOLEAN).optional().description("활성화 여부"),
                                                fieldWithPath("link_url").type(JsonFieldType.STRING).optional().description("링크 URL")
                                        )
                                        .responseFields(
                                                fieldWithPath("is_success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
                                                fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
                                                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지")
                                        )
                                        .requestSchema(Schema.schema("UpdateBarBannerRequest"))
                                        .responseSchema(Schema.schema("CommonSuccessResponse"))
                                        .build()
                        )
                ));
    }

    @Test
    @DisplayName("띠배너를 삭제한다.")
    public void deleteBarBanner() throws Exception {
        // given
        Long barBannerId = 1L;

        // when
        Mockito.doNothing().when(barBannerService).deleteBarBanner(eq(barBannerId));

        // then
        mockMvc.perform(RestDocumentationRequestBuilders.delete("/api/v1/bar-banners/{id}", barBannerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_success").value(true))
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("띠배너 삭제에 성공했습니다."))
                .andDo(MockMvcRestDocumentationWrapper.document("bar-banner/delete",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("BarBanner API")
                                        .description("띠배너를 삭제한다.")
                                        .responseFields(
                                                fieldWithPath("is_success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
                                                fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
                                                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지")
                                        )
                                        .responseSchema(Schema.schema("CommonSuccessResponse"))
                                        .build()
                        )
                ));
    }

    @Test
    @DisplayName("띠배너를 내보낸다.")
    public void exportBarBanners() throws Exception {
        // given
        Mockito.doNothing().when(barBannerService).exportBarBanners();

        // then
        mockMvc.perform(RestDocumentationRequestBuilders.post("/api/v1/bar-banners/export"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_success").value(true))
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("띠배너 내보내기에 성공했습니다."))
                .andDo(MockMvcRestDocumentationWrapper.document("bar-banner/export",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("BarBanner API")
                                        .description("띠배너를 내보낸다.")
                                        .responseFields(
                                                fieldWithPath("is_success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
                                                fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
                                                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지")
                                        )
                                        .responseSchema(Schema.schema("CommonSuccessResponse"))
                                        .build()
                        )
                ));
    }

    @Test
    @DisplayName("[FAILED] 띠배너 내보내기 중 연결에 실패한다.")
    public void exportBarBanners_Fail_Connection() throws Exception {
        // when
        Mockito.doThrow(new CustomException(AdminErrorStatus._FAILED_EXPORT_TRANSMISSION))
                .when(barBannerService).exportBarBanners();

        // then
        mockMvc.perform(RestDocumentationRequestBuilders.post("/api/v1/bar-banners/export"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.is_success").value(false))
                .andExpect(jsonPath("$.code").value("ADMIN-USER-011"))
                .andExpect(jsonPath("$.message").value("운영 서버로 배너 데이터 전송 중 오류가 발생했습니다."))
                .andDo(MockMvcRestDocumentationWrapper.document("bar-banner/export-fail-connection",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("BarBanner API")
                                        .responseFields(
                                                fieldWithPath("is_success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
                                                fieldWithPath("code").type(JsonFieldType.STRING).description("에러 코드"),
                                                fieldWithPath("message").type(JsonFieldType.STRING).description("에러 메시지")
                                        )
                                        .build()
                        )
                ));
    }

    @Test
    @DisplayName("띠배너를 불러온다.")
    public void importBarBanners() throws Exception {
        // given
        Mockito.doNothing().when(barBannerService).importBarBanners();

        // then
        mockMvc.perform(RestDocumentationRequestBuilders.post("/api/v1/bar-banners/import"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_success").value(true))
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("띠배너 불러오기에 성공했습니다."))
                .andDo(MockMvcRestDocumentationWrapper.document("bar-banner/import",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("BarBanner API")
                                        .description("띠배너를 불러온다.")
                                        .responseFields(
                                                fieldWithPath("is_success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
                                                fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
                                                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지")
                                        )
                                        .responseSchema(Schema.schema("CommonSuccessResponse"))
                                        .build()
                        )
                ));
    }

    @Test
    @DisplayName("띠배너 스테이징 저장을 성공한다.")
    public void saveBarBannerStaging() throws Exception {
        // given
        List<ExportBarBannerRequest> requests = List.of(
                new ExportBarBannerRequest(1L, "공지사항", "Notice", "#FF5733", "#FFFFFF", "https://www.link.com"),
                new ExportBarBannerRequest(2L, "공지사항2", "Notice2", "#FF5733", "#FFFFFF", "https://www.link.com")
        );
        String requestContent = objectMapper.writeValueAsString(requests);

        // when
        Mockito.doNothing().when(barBannerService).saveBarBannerStaging(anyString(), anyList());

        // then
        mockMvc.perform(RestDocumentationRequestBuilders.post("/api/v1/bar-banners/staging")
                        .header("X-API-KEY", "valid-api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestContent))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_success").value(true))
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("띠배너 스테이징 저장에 성공했습니다."))
                .andDo(MockMvcRestDocumentationWrapper.document("bar-banner/save-staging",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("BarBanner API")
                                        .description("띠배너 스테이징 저장을 성공한다.")
                                        .requestHeaders(
                                                headerWithName("X-API-KEY").description("API 인증키")
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

    @Test
    @DisplayName("[FAILED] 띠배너 스테이징 저장 중 유효하지 않은 API 키로 인해 실패한다.")
    public void saveBarBannerStaging_Fail_InvalidApiKey() throws Exception {
        // given
        List<ExportBarBannerRequest> requests = List.of(
                new ExportBarBannerRequest(1L, "공지사항", "Notice", "#FF5733", "#FFFFFF", "https://www.link.com"),
                new ExportBarBannerRequest(2L, "공지사항2", "Notice2", "#FF5733", "#FFFFFF", "https://www.link.com")
        );
        String requestContent = objectMapper.writeValueAsString(requests);

        // when
        Mockito.doThrow(new CustomException(AdminErrorStatus._INVALID_API_KEY))
                .when(barBannerService).saveBarBannerStaging(anyString(), anyList());

        // then
        mockMvc.perform(RestDocumentationRequestBuilders.post("/api/v1/bar-banners/staging")
                        .header("X-API-KEY", "invalid-api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestContent))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.is_success").value(false))
                .andExpect(jsonPath("$.code").value("ADMIN-USER-012"))
                .andExpect(jsonPath("$.message").value("유효하지 않은 서버 인증 키입니다."))
                .andDo(MockMvcRestDocumentationWrapper.document("bar-banner/save-staging-fail-invalid-api-key",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("BarBanner API")
                                        .responseFields(
                                                fieldWithPath("is_success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
                                                fieldWithPath("code").type(JsonFieldType.STRING).description("에러 코드"),
                                                fieldWithPath("message").type(JsonFieldType.STRING).description("에러 메시지")
                                        )
                                        .build()
                        )
                ));
    }
}
