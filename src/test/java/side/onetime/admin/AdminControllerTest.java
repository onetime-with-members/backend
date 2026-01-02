package side.onetime.admin;

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
import side.onetime.auth.service.CustomUserDetailsService;
import side.onetime.configuration.ControllerTestConfig;
import side.onetime.controller.AdminController;
import side.onetime.domain.enums.AdminStatus;
import side.onetime.domain.enums.Category;
import side.onetime.domain.enums.Language;
import side.onetime.dto.admin.request.LoginAdminUserRequest;
import side.onetime.dto.admin.request.RegisterAdminUserRequest;
import side.onetime.dto.admin.request.UpdateAdminUserStatusRequest;
import side.onetime.dto.admin.response.*;
import side.onetime.service.AdminService;
import side.onetime.util.JwtUtil;

import java.util.List;

import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminController.class)
public class AdminControllerTest extends ControllerTestConfig {

    @MockBean
    private AdminService adminService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("관리자 계정 회원가입을 진행한다.")
    public void registerAdminUser() throws Exception {
        // given
        RegisterAdminUserRequest request = new RegisterAdminUserRequest(
                "관리자 이름",
                "admin@example.com",
                "Password123!"
        );
        String requestContent = objectMapper.writeValueAsString(request);

        // when
        Mockito.doNothing().when(adminService).registerAdminUser(any(RegisterAdminUserRequest.class));

        // then
        mockMvc.perform(RestDocumentationRequestBuilders.post("/api/v1/admin/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestContent))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.is_success").value(true))
                .andExpect(jsonPath("$.code").value("201"))
                .andExpect(jsonPath("$.message").value("관리자 계정 등록에 성공했습니다."))
                .andDo(MockMvcRestDocumentationWrapper.document("admin/register",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("Admin API")
                                        .description("관리자 회원가입을 진행한다.")
                                        .requestFields(
                                                fieldWithPath("name").type(JsonFieldType.STRING).description("이름"),
                                                fieldWithPath("email").type(JsonFieldType.STRING).description("이메일"),
                                                fieldWithPath("password").type(JsonFieldType.STRING).description("비밀번호")
                                        )
                                        .responseFields(
                                                fieldWithPath("is_success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
                                                fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
                                                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지")
                                        )
                                        .requestSchema(Schema.schema("RegisterAdminUserRequest"))
                                        .responseSchema(Schema.schema("CommonSuccessResponse"))
                                        .build()
                        )
                ));
    }

    @Test
    @DisplayName("관리자 계정 로그인을 진행한다.")
    public void loginAdminUser() throws Exception {
        // given
        LoginAdminUserRequest request = new LoginAdminUserRequest(
                "admin@example.com",
                "Password123!"
        );
        String requestContent = objectMapper.writeValueAsString(request);
        String tempAccessToken = "temp.jwt.access.token";

        // when
        Mockito.when(adminService.loginAdminUser(any(LoginAdminUserRequest.class)))
                .thenReturn(LoginAdminUserResponse.of(tempAccessToken));

        // then
        mockMvc.perform(RestDocumentationRequestBuilders.post("/api/v1/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestContent))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_success").value(true))
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("관리자 계정 로그인에 성공했습니다."))
                .andExpect(jsonPath("$.payload.access_token").value(tempAccessToken))
                .andDo(MockMvcRestDocumentationWrapper.document("admin/login",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("Admin API")
                                        .description("관리자 계정 로그인을 진행한다.")
                                        .requestFields(
                                                fieldWithPath("email").type(JsonFieldType.STRING).description("이메일"),
                                                fieldWithPath("password").type(JsonFieldType.STRING).description("비밀번호")
                                        )
                                        .responseFields(
                                                fieldWithPath("is_success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
                                                fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
                                                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                                                fieldWithPath("payload").type(JsonFieldType.OBJECT).description("응답 데이터"),
                                                fieldWithPath("payload.access_token").type(JsonFieldType.STRING).description("액세스 토큰")
                                        )
                                        .requestSchema(Schema.schema("LoginAdminUserRequest"))
                                        .responseSchema(Schema.schema("LoginAdminUserResponse"))
                                        .build()
                        )
                ));
    }

    @Test
    @DisplayName("관리자 프로필 조회를 진행한다.")
    public void getAdminUserProfile() throws Exception {
        // given
        String accessToken = "Bearer temp.jwt.access.token";

        GetAdminUserProfileResponse response = new GetAdminUserProfileResponse(
                "관리자 이름",
                "admin@example.com"
        );

        // when
        Mockito.when(adminService.getAdminUserProfile(any(String.class)))
                .thenReturn(response);

        // then
        mockMvc.perform(RestDocumentationRequestBuilders.get("/api/v1/admin/profile")
                        .header("Authorization", accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_success").value(true))
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("관리자 프로필 조회에 성공했습니다."))
                .andExpect(jsonPath("$.payload.name").value("관리자 이름"))
                .andExpect(jsonPath("$.payload.email").value("admin@example.com"))
                .andDo(MockMvcRestDocumentationWrapper.document("admin/profile",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("Admin API")
                                        .description("관리자 프로필 조회를 진행한다.")
                                        .responseFields(
                                                fieldWithPath("is_success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
                                                fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
                                                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                                                fieldWithPath("payload").type(JsonFieldType.OBJECT).description("응답 데이터"),
                                                fieldWithPath("payload.name").type(JsonFieldType.STRING).description("관리자 이름"),
                                                fieldWithPath("payload.email").type(JsonFieldType.STRING).description("관리자 이메일")
                                        )
                                        .responseSchema(Schema.schema("GetAdminUserProfileResponse"))
                                        .build()
                        )
                ));
    }

    @Test
    @DisplayName("전체 관리자 정보를 조회한다.")
    public void getAllAdminUserDetail() throws Exception {
        // given
        String accessToken = "Bearer temp.jwt.access.token";

        List<AdminUserDetailResponse> response = List.of(
                new AdminUserDetailResponse(1L, "마스터 관리자", "master@example.com", AdminStatus.MASTER),
                new AdminUserDetailResponse(2L, "일반 관리자", "admin@example.com", AdminStatus.APPROVED)
        );

        // when
        Mockito.when(adminService.getAllAdminUserDetail(any(String.class)))
                .thenReturn(response);

        // then
        mockMvc.perform(RestDocumentationRequestBuilders.get("/api/v1/admin/all")
                        .header("Authorization", accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_success").value(true))
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("전체 관리자 정보 조회에 성공했습니다."))
                .andExpect(jsonPath("$.payload[0].id").value(1L))
                .andExpect(jsonPath("$.payload[0].name").value("마스터 관리자"))
                .andExpect(jsonPath("$.payload[0].email").value("master@example.com"))
                .andExpect(jsonPath("$.payload[0].admin_status").value("MASTER"))
                .andDo(MockMvcRestDocumentationWrapper.document("admin/get-all",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("Admin API")
                                        .description("전체 관리자 정보를 조회한다. (마스터 관리자만 가능)")
                                        .responseFields(
                                                fieldWithPath("is_success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
                                                fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
                                                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                                                fieldWithPath("payload").type(JsonFieldType.ARRAY).description("응답 데이터"),
                                                fieldWithPath("payload[].id").type(JsonFieldType.NUMBER).description("관리자 ID"),
                                                fieldWithPath("payload[].name").type(JsonFieldType.STRING).description("관리자 이름"),
                                                fieldWithPath("payload[].email").type(JsonFieldType.STRING).description("관리자 이메일"),
                                                fieldWithPath("payload[].admin_status").type(JsonFieldType.STRING).description("관리자 상태 (MASTER, APPROVED, PENDING_APPROVAL)")
                                        )
                                        .responseSchema(Schema.schema("GetAllAdminUserDetailResponse"))
                                        .build()
                        )
                ));
    }

    @Test
    @DisplayName("관리자 권한을 수정한다.")
    public void updateAdminUserStatus() throws Exception {
        // given
        String accessToken = "Bearer temp.jwt.access.token";
        String requestContent = objectMapper.writeValueAsString(
                new UpdateAdminUserStatusRequest(2L, AdminStatus.APPROVED)
        );

        // when
        Mockito.doNothing().when(adminService).updateAdminUserStatus(any(String.class), any(UpdateAdminUserStatusRequest.class));

        // then
        mockMvc.perform(RestDocumentationRequestBuilders.patch("/api/v1/admin/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", accessToken)
                        .content(requestContent))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_success").value(true))
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("관리자 권한 수정에 성공했습니다."))
                .andDo(MockMvcRestDocumentationWrapper.document("admin/update-status",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("Admin API")
                                        .description("관리자 권한을 수정한다. (마스터 관리자만 가능)")
                                        .requestFields(
                                                fieldWithPath("id").type(JsonFieldType.NUMBER).description("수정 대상 관리자 ID"),
                                                fieldWithPath("admin_status").type(JsonFieldType.STRING).description("변경할 관리자 상태 (MASTER, APPROVED, PENDING_APPROVAL)")
                                        )
                                        .responseFields(
                                                fieldWithPath("is_success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
                                                fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
                                                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지")
                                        )
                                        .requestSchema(Schema.schema("UpdateAdminUserStatusRequest"))
                                        .responseSchema(Schema.schema("CommonSuccessResponse"))
                                        .build()
                        )
                ));
    }

    @Test
    @DisplayName("관리자 계정을 탈퇴한다.")
    public void withdrawAdminUser() throws Exception {
        // given
        String accessToken = "Bearer temp.jwt.access.token";

        // when
        Mockito.doNothing().when(adminService).withdrawAdminUser(any(String.class));

        // then
        mockMvc.perform(RestDocumentationRequestBuilders.post("/api/v1/admin/withdraw")
                        .header("Authorization", accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_success").value(true))
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("관리자 계정 탈퇴에 성공했습니다."))
                .andDo(MockMvcRestDocumentationWrapper.document("admin/withdraw",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("Admin API")
                                        .description("관리자 계정을 탈퇴한다.")
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
    @DisplayName("관리자 이벤트 대시보드 정보를 조회한다.")
    public void getAllDashboardEvents() throws Exception {
        // given
        String accessToken = "Bearer temp.jwt.access.token";

        List<DashboardEvent> events = List.of(
                new DashboardEvent(
                        100L, "1", "이벤트 제목", "10:00", "12:00",
                        Category.DATE, 10, "2025-03-01 12:00:00",
                        List.of("2025.04.01")
                )
        );

        PageInfo pageInfo = PageInfo.of(1, 20, 1, 1);
        GetAllDashboardEventsResponse response = GetAllDashboardEventsResponse.of(events, pageInfo);

        // when
        Mockito.when(adminService.getAllDashboardEvents(any(String.class), any(Pageable.class), any(String.class), any(String.class)))
                .thenReturn(response);

        // then
        mockMvc.perform(RestDocumentationRequestBuilders.get("/api/v1/admin/dashboard/events")
                        .header("Authorization", accessToken)
                        .param("page", "1")
                        .param("keyword", "created_date")
                        .param("sorting", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_success").value(true))
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("관리자 이벤트 대시보드 정보 조회에 성공했습니다."))
                .andExpect(jsonPath("$.payload.events[0].id").value(100L))
                .andExpect(jsonPath("$.payload.events[0].event_id").value("1"))
                .andExpect(jsonPath("$.payload.events[0].title").value("이벤트 제목"))
                .andExpect(jsonPath("$.payload.events[0].participant_count").value(10))
                .andExpect(jsonPath("$.payload.page_info.page").value(1))
                .andExpect(jsonPath("$.payload.page_info.size").value(20))
                .andExpect(jsonPath("$.payload.page_info.total_elements").value(1))
                .andExpect(jsonPath("$.payload.page_info.total_pages").value(1))
                .andDo(MockMvcRestDocumentationWrapper.document("admin/dashboard-events",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("Admin API")
                                        .description("관리자 이벤트 대시보드 정보를 조회한다.")
                                        .queryParameters(
                                                parameterWithName("page").description("조회할 페이지 번호 (1부터 시작)"),
                                                parameterWithName("keyword").description("정렬 기준 필드명 (예: created_date, end_time 등)"),
                                                parameterWithName("sorting").description("정렬 방향 (asc 또는 desc)")
                                        )
                                        .responseFields(
                                                fieldWithPath("is_success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
                                                fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
                                                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                                                fieldWithPath("payload").type(JsonFieldType.OBJECT).description("페이로드 객체"),
                                                fieldWithPath("payload.events").type(JsonFieldType.ARRAY).description("이벤트 리스트"),
                                                fieldWithPath("payload.events[].id").type(JsonFieldType.NUMBER).description("이벤트 내부 ID"),
                                                fieldWithPath("payload.events[].event_id").type(JsonFieldType.STRING).description("이벤트 외부 식별자 (UUID)"),
                                                fieldWithPath("payload.events[].title").type(JsonFieldType.STRING).description("이벤트 제목"),
                                                fieldWithPath("payload.events[].start_time").type(JsonFieldType.STRING).description("시작 시간"),
                                                fieldWithPath("payload.events[].end_time").type(JsonFieldType.STRING).description("종료 시간"),
                                                fieldWithPath("payload.events[].category").type(JsonFieldType.STRING).description("카테고리 (DATE 또는 DAY)"),
                                                fieldWithPath("payload.events[].participant_count").type(JsonFieldType.NUMBER).description("참여 인원 수"),
                                                fieldWithPath("payload.events[].created_date").type(JsonFieldType.STRING).description("생성일자"),
                                                fieldWithPath("payload.events[].ranges").type(JsonFieldType.ARRAY).description("이벤트 날짜 또는 요일 범위"),
                                                fieldWithPath("payload.page_info.page").type(JsonFieldType.NUMBER).description("현재 페이지 번호"),
                                                fieldWithPath("payload.page_info.size").type(JsonFieldType.NUMBER).description("페이지당 항목 수"),
                                                fieldWithPath("payload.page_info.total_elements").type(JsonFieldType.NUMBER).description("전체 항목 수"),
                                                fieldWithPath("payload.page_info.total_pages").type(JsonFieldType.NUMBER).description("전체 페이지 수")
                                        )
                                        .responseSchema(Schema.schema("GetAllDashboardEventsResponse"))
                                        .build()
                        )
                ));
    }

    @Test
    @DisplayName("관리자 유저 대시보드 정보를 조회한다.")
    public void getAllDashboardUsers() throws Exception {
        // given
        String accessToken = "Bearer temp.jwt.access.token";

        List<DashboardUser> users = List.of(
                new DashboardUser(
                        1L,
                        "홍길동",
                        "hong@example.com",
                        "길동이",
                        "KAKAO",
                        "kakao_123",
                        true,
                        true,
                        false,
                        "23:00",
                        "07:00",
                        Language.KOR,
                        3,
                        "2025-03-01 12:00:00"
                )
        );

        PageInfo pageInfo = PageInfo.of(1, 20, 1, 1);
        GetAllDashboardUsersResponse response = GetAllDashboardUsersResponse.of(users, pageInfo);

        // when
        Mockito.when(adminService.getAllDashboardUsers(any(String.class), any(Pageable.class), any(String.class), any(String.class)))
                .thenReturn(response);

        // then
        mockMvc.perform(RestDocumentationRequestBuilders.get("/api/v1/admin/dashboard/users")
                        .header("Authorization", accessToken)
                        .param("page", "1")
                        .param("keyword", "created_date")
                        .param("sorting", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_success").value(true))
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("관리자 유저 대시보드 정보 조회에 성공했습니다."))
                .andExpect(jsonPath("$.payload.users[0].name").value("홍길동"))
                .andExpect(jsonPath("$.payload.users[0].participation_count").value(3))
                .andExpect(jsonPath("$.payload.page_info.page").value(1))
                .andExpect(jsonPath("$.payload.page_info.size").value(20))
                .andExpect(jsonPath("$.payload.page_info.total_elements").value(1))
                .andExpect(jsonPath("$.payload.page_info.total_pages").value(1))
                .andDo(MockMvcRestDocumentationWrapper.document("admin/dashboard-users",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("Admin API")
                                        .description("관리자 유저 대시보드 정보를 조회한다.")
                                        .queryParameters(
                                                parameterWithName("page").description("조회할 페이지 번호 (1부터 시작)"),
                                                parameterWithName("keyword").description("정렬 기준 필드명 (예: created_date, email 등)"),
                                                parameterWithName("sorting").description("정렬 방향 (asc 또는 desc)")
                                        )
                                        .responseFields(
                                                fieldWithPath("is_success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
                                                fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
                                                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                                                fieldWithPath("payload").type(JsonFieldType.OBJECT).description("페이로드 객체"),
                                                fieldWithPath("payload.users").type(JsonFieldType.ARRAY).description("사용자 리스트"),
                                                fieldWithPath("payload.users[].id").type(JsonFieldType.NUMBER).description("사용자 ID"),
                                                fieldWithPath("payload.users[].name").type(JsonFieldType.STRING).description("이름"),
                                                fieldWithPath("payload.users[].email").type(JsonFieldType.STRING).description("이메일"),
                                                fieldWithPath("payload.users[].nickname").type(JsonFieldType.STRING).description("닉네임"),
                                                fieldWithPath("payload.users[].provider").type(JsonFieldType.STRING).description("소셜 로그인 제공자"),
                                                fieldWithPath("payload.users[].provider_id").type(JsonFieldType.STRING).description("소셜 로그인 식별자"),
                                                fieldWithPath("payload.users[].service_policy_agreement").type(JsonFieldType.BOOLEAN).description("서비스 정책 동의 여부"),
                                                fieldWithPath("payload.users[].privacy_policy_agreement").type(JsonFieldType.BOOLEAN).description("개인정보 정책 동의 여부"),
                                                fieldWithPath("payload.users[].marketing_policy_agreement").type(JsonFieldType.BOOLEAN).description("마케팅 정책 동의 여부"),
                                                fieldWithPath("payload.users[].sleep_start_time").type(JsonFieldType.STRING).description("수면 시작 시간"),
                                                fieldWithPath("payload.users[].sleep_end_time").type(JsonFieldType.STRING).description("수면 종료 시간"),
                                                fieldWithPath("payload.users[].language").type(JsonFieldType.STRING).description("선호 언어"),
                                                fieldWithPath("payload.users[].participation_count").type(JsonFieldType.NUMBER).description("참여한 이벤트 수"),
                                                fieldWithPath("payload.users[].created_date").type(JsonFieldType.STRING).description("생성일자"),
                                                fieldWithPath("payload.page_info.page").type(JsonFieldType.NUMBER).description("현재 페이지 번호"),
                                                fieldWithPath("payload.page_info.size").type(JsonFieldType.NUMBER).description("페이지당 항목 수"),
                                                fieldWithPath("payload.page_info.total_elements").type(JsonFieldType.NUMBER).description("전체 항목 수"),
                                                fieldWithPath("payload.page_info.total_pages").type(JsonFieldType.NUMBER).description("전체 페이지 수")
                                        )
                                        .responseSchema(Schema.schema("GetAllDashboardUsersResponse"))
                                        .build()
                        )
                ));
    }
}
