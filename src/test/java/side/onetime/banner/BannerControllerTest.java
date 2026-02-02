package side.onetime.banner;

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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.web.multipart.MultipartFile;
import side.onetime.configuration.AdminControllerTestConfig;
import side.onetime.controller.BannerController;
import side.onetime.dto.admin.response.PageInfo;
import side.onetime.dto.banner.request.*;
import side.onetime.dto.banner.response.*;
import side.onetime.exception.CustomException;
import side.onetime.exception.status.AdminErrorStatus;
import side.onetime.service.BannerService;

import java.util.List;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.headerWithName;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BannerController.class)
public class BannerControllerTest extends AdminControllerTestConfig {

    @MockBean
    private BannerService bannerService;

    @Test
    @DisplayName("배너를 등록한다.")
    public void registerBanner() throws Exception {
        // given
        RegisterBannerRequest request = new RegisterBannerRequest(
                "OneTime",
                "OneTime's Title",
                "OneTime's Sub Title",
                "OneTime's Button Text",
                "#FFFFFF",
                "https://www.link.com"
        );
        String requestContent = objectMapper.writeValueAsString(request);

        // when
        Mockito.doNothing().when(bannerService).registerBanner(any(RegisterBannerRequest.class), any(MultipartFile.class));

        // then
        mockMvc.perform(RestDocumentationRequestBuilders.multipart("/api/v1/banners/register")
                        .file(new MockMultipartFile("request", "", "application/json", requestContent.getBytes()))
                        .file(new MockMultipartFile("image_file", "banner.png", "image/png", "banner-image-content".getBytes())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.is_success").value(true))
                .andExpect(jsonPath("$.code").value("201"))
                .andExpect(jsonPath("$.message").value("배너 등록에 성공했습니다."))
                .andDo(MockMvcRestDocumentationWrapper.document("banner/register",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("Banner API")
                                        .description("배너를 등록한다.")
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
        Mockito.doNothing().when(bannerService).registerBarBanner(any(RegisterBarBannerRequest.class));

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
    @DisplayName("배너를 단건 조회한다.")
    public void getBanner() throws Exception {
        // given
        Long bannerId = 1L;
        GetBannerResponse response = new GetBannerResponse(
                bannerId, "OneTime", "OneTime's Title", "OneTime's Sub Title", "OneTime's Button Text", "#FFFFFF", "https://www.image.com", true, "2025-08-26 12:00:00", "https://www.link.com", 1L
        );

        // when
        Mockito.when(bannerService.getBanner(any(Long.class))).thenReturn(response);

        // then
        mockMvc.perform(RestDocumentationRequestBuilders.get("/api/v1/banners/{id}", bannerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_success").value(true))
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("배너 단건 조회에 성공했습니다."))
                .andDo(MockMvcRestDocumentationWrapper.document("banner/get-one",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("Banner API")
                                        .description("배너를 단건 조회한다.")
                                        .pathParameters(
                                                parameterWithName("id").description("조회할 배너 ID")
                                        )
                                        .responseFields(
                                                fieldWithPath("is_success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
                                                fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
                                                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                                                fieldWithPath("payload").type(JsonFieldType.OBJECT).description("배너 정보"),
                                                fieldWithPath("payload.id").type(JsonFieldType.NUMBER).description("배너 ID"),
                                                fieldWithPath("payload.organization").type(JsonFieldType.STRING).description("조직명"),
                                                fieldWithPath("payload.title").type(JsonFieldType.STRING).description("제목"),
                                                fieldWithPath("payload.sub_title").type(JsonFieldType.STRING).description("부제목"),
                                                fieldWithPath("payload.button_text").type(JsonFieldType.STRING).description("버튼 텍스트"),
                                                fieldWithPath("payload.color_code").type(JsonFieldType.STRING).description("색상 코드"),
                                                fieldWithPath("payload.image_url").type(JsonFieldType.STRING).description("배너 이미지 URL"),
                                                fieldWithPath("payload.is_activated").type(JsonFieldType.BOOLEAN).description("활성화 여부"),
                                                fieldWithPath("payload.created_date").type(JsonFieldType.STRING).description("생성일자"),
                                                fieldWithPath("payload.link_url").type(JsonFieldType.STRING).description("링크 URL"),
                                                fieldWithPath("payload.click_count").type(JsonFieldType.NUMBER).description("클릭 수")
                                        )
                                        .responseSchema(Schema.schema("GetBannerResponse"))
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
        Mockito.when(bannerService.getBarBanner(any(Long.class))).thenReturn(response);

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
    @DisplayName("배너를 전체 조회한다.")
    public void getAllBanners() throws Exception {
        // given
        int page = 1;

        List<GetBannerResponse> banners = List.of(
                new GetBannerResponse(1L, "OneTime", "OneTime's Title", "OneTime's Sub Title", "OneTime's Button Text", "#FFFFFF", "https://www.image.com", true, "2025-08-26 12:00:00", "https://www.link.com", 1L),
                new GetBannerResponse(2L, "OneTime2", "OneTime's Title2", "OneTime's Sub Title2", "OneTime's Button Text2", "#000000", "https://www.image.com", true, "2025-08-27 12:00:00", "https://www.link.com", 1L)
        );

        PageInfo pageInfo = PageInfo.of(1, 20, 2, 1);
        GetAllBannersResponse response = GetAllBannersResponse.of(banners, pageInfo);

        // when
        Mockito.when(bannerService.getAllBanners(any(Pageable.class)))
                .thenReturn(response);

        // then
        mockMvc.perform(RestDocumentationRequestBuilders.get("/api/v1/banners/all")
                        .param("page", String.valueOf(page)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_success").value(true))
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("배너 전체 조회에 성공했습니다."))
                .andExpect(jsonPath("$.payload.banners[0].id").value(1L))
                .andExpect(jsonPath("$.payload.banners[1].id").value(2L))
                .andExpect(jsonPath("$.payload.page_info.page").value(1))
                .andExpect(jsonPath("$.payload.page_info.size").value(20))
                .andExpect(jsonPath("$.payload.page_info.total_elements").value(2))
                .andExpect(jsonPath("$.payload.page_info.total_pages").value(1))
                .andDo(MockMvcRestDocumentationWrapper.document("banner/get-all",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("Banner API")
                                        .description("배너를 전체 조회한다.")
                                        .queryParameters(
                                                parameterWithName("page").description("조회할 페이지 번호 (1부터 시작)")
                                        )
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
                                                fieldWithPath("payload.banners[].click_count").type(JsonFieldType.NUMBER).description("클릭 수"),
                                                fieldWithPath("payload.page_info.page").type(JsonFieldType.NUMBER).description("현재 페이지 번호"),
                                                fieldWithPath("payload.page_info.size").type(JsonFieldType.NUMBER).description("페이지당 항목 수"),
                                                fieldWithPath("payload.page_info.total_elements").type(JsonFieldType.NUMBER).description("전체 항목 수"),
                                                fieldWithPath("payload.page_info.total_pages").type(JsonFieldType.NUMBER).description("전체 페이지 수")
                                        )
                                        .responseSchema(Schema.schema("GetAllBannersResponse"))
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
        Mockito.when(bannerService.getAllBarBanners(any(Pageable.class)))
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
                .andDo(MockMvcRestDocumentationWrapper.document("banner/get-all-activated",
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
    @DisplayName("배너를 수정한다.")
    public void updateBanner() throws Exception {
        // given
        Long bannerId = 1L;

        UpdateBannerRequest request = new UpdateBannerRequest(
                "Modified OneTime",
                "Modified OneTime's Title",
                "Modified OneTime's Sub Title",
                "Modified OneTime's Button Text",
                "#000000",
                true,
                "https://www.link.com"
        );
        String requestContent = objectMapper.writeValueAsString(request);

        // when
        Mockito.doNothing().when(bannerService).updateBanner(any(Long.class), any(UpdateBannerRequest.class), any(MultipartFile.class));

        // then
        mockMvc.perform(RestDocumentationRequestBuilders.multipart("/api/v1/banners/{id}", bannerId)
                        .file(new MockMultipartFile("request", "", "application/json", requestContent.getBytes()))
                        .file(new MockMultipartFile("image_file", "banner.png", "image/png", "banner-image-content".getBytes()))
                        .with(r -> {
                            r.setMethod("PATCH");
                            return r;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_success").value(true))
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("배너 수정에 성공했습니다."))
                .andDo(document("banner/update",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("Banner API")
                                        .description("배너를 수정한다.")
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
        Mockito.doNothing().when(bannerService).updateBarBanner(eq(barBannerId), any(UpdateBarBannerRequest.class));

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
    @DisplayName("배너를 삭제한다.")
    public void deleteBanner() throws Exception {
        // given
        Long bannerId = 1L;

        // when
        Mockito.doNothing().when(bannerService).deleteBanner(eq(bannerId));

        // then
        mockMvc.perform(RestDocumentationRequestBuilders.delete("/api/v1/banners/{id}", bannerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_success").value(true))
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("배너 삭제에 성공했습니다."))
                .andDo(MockMvcRestDocumentationWrapper.document("banner/delete",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("Banner API")
                                        .description("배너를 삭제한다.")
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
    @DisplayName("띠배너를 삭제한다.")
    public void deleteBarBanner() throws Exception {
        // given
        Long barBannerId = 1L;

        // when
        Mockito.doNothing().when(bannerService).deleteBarBanner(eq(barBannerId));

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

    @Test
    @DisplayName("배너를 내보낸다.")
    public void exportBanners() throws Exception {
        // given
        Mockito.doNothing().when(bannerService).exportBanners();

        // then
        mockMvc.perform(RestDocumentationRequestBuilders.post("/api/v1/banners/export"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_success").value(true))
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("배너 내보내기에 성공했습니다."))
                .andDo(MockMvcRestDocumentationWrapper.document("banner/export",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("Banner API")
                                        .description("배너를 내보낸다.")
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
    @DisplayName("[FAILED] 배너 내보내기 중 연결에 실패한다.")
    public void exportBanners_Fail_Connection() throws Exception {
        // when
        Mockito.doThrow(new CustomException(AdminErrorStatus._FAILED_EXPORT_TRANSMISSION))
                .when(bannerService).exportBanners();

        // then
        mockMvc.perform(RestDocumentationRequestBuilders.post("/api/v1/banners/export"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.is_success").value(false))
                .andExpect(jsonPath("$.code").value("ADMIN-USER-011"))
                .andExpect(jsonPath("$.message").value("운영 서버로 배너 데이터 전송 중 오류가 발생했습니다."))
                .andDo(MockMvcRestDocumentationWrapper.document("banner/export-fail-connection",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("Banner API")
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
    @DisplayName("띠배너를 내보낸다.")
    public void exportBarBanners() throws Exception {
        // given
        Mockito.doNothing().when(bannerService).exportBarBanners();

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
                .when(bannerService).exportBarBanners();

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
    @DisplayName("배너를 불러온다.")
    public void importBanners() throws Exception {
        // given
        Mockito.doNothing().when(bannerService).importBanners();

        // then
        mockMvc.perform(RestDocumentationRequestBuilders.post("/api/v1/banners/import"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_success").value(true))
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("배너 불러오기에 성공했습니다."))
                .andDo(MockMvcRestDocumentationWrapper.document("banner/import",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("Banner API")
                                        .description("배너를 불러온다.")
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
    @DisplayName("띠배너를 불러온다.")
    public void importBarBanners() throws Exception {
        // given
        Mockito.doNothing().when(bannerService).importBarBanners();

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
    @DisplayName("배너를 스테이징에 저장한다.")
    public void saveBannerStaging() throws Exception {
        // given
        String apiKey = "api-key-0123456789";
        List<ExportBannerRequest> requests = List.of(
                new ExportBannerRequest(
                        1L,
                        "OneTime",
                        "OneTime's Title",
                        "OneTime's Sub Title",
                        "OneTime's Button Text",
                        "#FFFFFF",
                        "https://www.image.com",
                        "https://www.link.com"
                ),
                new ExportBannerRequest(
                        2L,
                        "OneTime2",
                        "OneTime's Title2",
                        "OneTime's Sub Title2",
                        "OneTime's Button Text2",
                        "#000000",
                        "https://www.image2.com",
                        "https://www.link2.com"
                )
        );
        String requestContent = objectMapper.writeValueAsString(requests);

        // when
        Mockito.doNothing().when(bannerService).saveBannerStaging(eq(apiKey), anyList());

        // then
        mockMvc.perform(RestDocumentationRequestBuilders.post("/api/v1/banners/staging")
                        .header("X-API-KEY", apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestContent))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_success").value(true))
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("배너 스테이징 저장에 성공했습니다."))
                .andDo(MockMvcRestDocumentationWrapper.document("banner/staging-save",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("Banner API")
                                        .description("배너를 스테이징에 저장한다.")
                                        .requestHeaders(
                                                headerWithName("X-API-KEY").description("서버 간 인증을 위한 API Key")
                                        )
                                        .requestFields(
                                                fieldWithPath("[].bannerId").type(JsonFieldType.NUMBER).description("배너 ID"),
                                                fieldWithPath("[].organization").type(JsonFieldType.STRING).description("조직명"),
                                                fieldWithPath("[].title").type(JsonFieldType.STRING).description("제목"),
                                                fieldWithPath("[].subTitle").type(JsonFieldType.STRING).description("부제목"),
                                                fieldWithPath("[].buttonText").type(JsonFieldType.STRING).description("버튼 텍스트"),
                                                fieldWithPath("[].colorCode").type(JsonFieldType.STRING).description("색상 코드"),
                                                fieldWithPath("[].imageUrl").type(JsonFieldType.STRING).description("이미지 URL"),
                                                fieldWithPath("[].linkUrl").type(JsonFieldType.STRING).description("링크 URL")
                                        )
                                        .responseFields(
                                                fieldWithPath("is_success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
                                                fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
                                                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지")
                                        )
                                        .requestSchema(Schema.schema("ExportBannerRequestList"))
                                        .responseSchema(Schema.schema("CommonSuccessResponse"))
                                        .build()
                        )
                ));
    }

    @Test
    @DisplayName("[FAILED] 유효하지 않은 API 키로 배너 스테이징 저장에 실패한다.")
    public void saveBannerStaging_Fail_InvalidApiKey() throws Exception {
        // given
        String invalidApiKey = "invalid-api-key";
        List<ExportBannerRequest> requests = List.of(
                new ExportBannerRequest(
                        1L,
                        "OneTime",
                        "OneTime's Title",
                        "OneTime's Sub Title",
                        "OneTime's Button Text",
                        "#FFFFFF",
                        "https://www.image.com",
                        "https://www.link.com"
                )
        );
        String requestContent = objectMapper.writeValueAsString(requests);

        // when
        Mockito.doThrow(new CustomException(AdminErrorStatus._INVALID_API_KEY))
                .when(bannerService).saveBannerStaging(eq(invalidApiKey), anyList());

        // then
        mockMvc.perform(RestDocumentationRequestBuilders.post("/api/v1/banners/staging")
                        .header("X-API-KEY", invalidApiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestContent))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.is_success").value(false))
                .andExpect(jsonPath("$.code").value("ADMIN-USER-012"))
                .andExpect(jsonPath("$.message").value("유효하지 않은 서버 인증 키입니다."))
                .andDo(MockMvcRestDocumentationWrapper.document("banner/staging-save/fail-invalid-apikey",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("Banner API")
                                        .requestHeaders(
                                                headerWithName("X-API-KEY").description("서버 간 인증을 위한 API Key")
                                        )
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
    @DisplayName("띠배너를 스테이징에 저장한다.")
    public void saveBarBannerStaging() throws Exception {
        // given
        String apiKey = "api-key-0123456789";
        List<ExportBarBannerRequest> requests = List.of(
                new ExportBarBannerRequest(
                        1L,
                        "한국어 내용",
                        "English Content",
                        "#FFFFFF",
                        "#000000",
                        "https://www.link.com"
                )
        );
        String requestContent = objectMapper.writeValueAsString(requests);

        // when
        Mockito.doNothing().when(bannerService).saveBarBannerStaging(eq(apiKey), anyList());

        // then
        mockMvc.perform(RestDocumentationRequestBuilders.post("/api/v1/bar-banners/staging")
                        .header("X-API-KEY", apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestContent))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_success").value(true))
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("띠배너 스테이징 저장에 성공했습니다."))
                .andDo(MockMvcRestDocumentationWrapper.document("bar-banner/staging-save",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("BarBanner API")
                                        .description("띠배너를 스테이징에 저장한다.")
                                        .requestHeaders(
                                                headerWithName("X-API-KEY").description("서버 간 인증을 위한 API Key")
                                        )
                                        .requestFields(
                                                fieldWithPath("[].barBannerId").type(JsonFieldType.NUMBER).description("띠배너 ID"),
                                                fieldWithPath("[].contentKor").type(JsonFieldType.STRING).description("한국어 내용"),
                                                fieldWithPath("[].contentEng").type(JsonFieldType.STRING).description("영어 내용"),
                                                fieldWithPath("[].backgroundColorCode").type(JsonFieldType.STRING).description("배경색 코드"),
                                                fieldWithPath("[].textColorCode").type(JsonFieldType.STRING).description("텍스트색 코드"),
                                                fieldWithPath("[].linkUrl").type(JsonFieldType.STRING).description("링크 URL")
                                        )
                                        .responseFields(
                                                fieldWithPath("is_success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
                                                fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
                                                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지")
                                        )
                                        .requestSchema(Schema.schema("ExportBarBannerRequestList"))
                                        .responseSchema(Schema.schema("CommonSuccessResponse"))
                                        .build()
                        )
                ));
    }

    @Test
    @DisplayName("[FAILED] 유효하지 않은 API 키로 띠배너 스테이징 저장에 실패한다.")
    public void saveBarBannerStaging_Fail_InvalidApiKey() throws Exception {
        // given
        String invalidApiKey = "invalid-api-key";
        List<ExportBarBannerRequest> requests = List.of(
                new ExportBarBannerRequest(
                        1L,
                        "한국어 내용",
                        "English Content",
                        "#FFFFFF",
                        "#000000",
                        "https://www.link.com"
                )
        );
        String requestContent = objectMapper.writeValueAsString(requests);

        // when
        Mockito.doThrow(new CustomException(AdminErrorStatus._INVALID_API_KEY))
                .when(bannerService).saveBarBannerStaging(eq(invalidApiKey), anyList());

        // then
        mockMvc.perform(RestDocumentationRequestBuilders.post("/api/v1/bar-banners/staging")
                        .header("X-API-KEY", invalidApiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestContent))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.is_success").value(false))
                .andExpect(jsonPath("$.code").value("ADMIN-USER-012"))
                .andExpect(jsonPath("$.message").value("유효하지 않은 서버 인증 키입니다."))
                .andDo(MockMvcRestDocumentationWrapper.document("bar-banner/staging-save/fail-invalid-apikey",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("BarBanner API")
                                        .requestHeaders(
                                                headerWithName("X-API-KEY").description("서버 간 인증을 위한 API Key")
                                        )
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
