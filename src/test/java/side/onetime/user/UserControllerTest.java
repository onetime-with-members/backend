package side.onetime.user;

import com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper;
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
import side.onetime.auth.service.CustomUserDetailsService;
import side.onetime.configuration.ControllerTestConfig;
import side.onetime.controller.UserController;
import side.onetime.domain.enums.GuideType;
import side.onetime.domain.enums.Language;
import side.onetime.dto.user.request.*;
import side.onetime.dto.user.response.*;
import side.onetime.service.UserService;
import side.onetime.util.JwtUtil;

import static com.epages.restdocs.apispec.ResourceDocumentation.parameterWithName;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
public class UserControllerTest extends ControllerTestConfig {

    @MockBean
    private UserService userService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("유저 온보딩을 진행한다.")
    public void onboardUser() throws Exception {
        // given
        OnboardUserResponse response = new OnboardUserResponse("sampleAccessToken", "sampleRefreshToken");
        Mockito.when(userService.onboardUser(any(OnboardUserRequest.class))).thenReturn(response);

        OnboardUserRequest request = new OnboardUserRequest(
                "sampleRegisterToken",
                "UserNickname",
                true,
                true,
                false,
                "23:30",
                "07:00",
                Language.KOR
        );
        String requestContent = objectMapper.writeValueAsString(request);

        // when
        ResultActions resultActions = this.mockMvc.perform(RestDocumentationRequestBuilders.post("/api/v1/users/onboarding")
                .content(requestContent)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON));

        // then
        resultActions
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.is_success").value(true))
                .andExpect(jsonPath("$.code").value("201"))
                .andExpect(jsonPath("$.message").value("유저 온보딩에 성공했습니다."))
                .andExpect(jsonPath("$.payload.access_token").value("sampleAccessToken"))
                .andExpect(jsonPath("$.payload.refresh_token").value("sampleRefreshToken"))
                .andDo(MockMvcRestDocumentationWrapper.document("user/onboard",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("User API")
                                        .description("유저 온보딩을 진행한다.")
                                        .requestFields(
                                                fieldWithPath("register_token").type(JsonFieldType.STRING).description("레지스터 토큰"),
                                                fieldWithPath("nickname").type(JsonFieldType.STRING).description("유저 닉네임"),
                                                fieldWithPath("service_policy_agreement").type(JsonFieldType.BOOLEAN).description("서비스 이용약관 동의 여부"),
                                                fieldWithPath("privacy_policy_agreement").type(JsonFieldType.BOOLEAN).description("개인정보 수집 및 이용 동의 여부"),
                                                fieldWithPath("marketing_policy_agreement").type(JsonFieldType.BOOLEAN).description("마케팅 정보 수신 동의 여부"),
                                                fieldWithPath("sleep_start_time").type(JsonFieldType.STRING).description("수면 시작 시간 (예: 23:30)"),
                                                fieldWithPath("sleep_end_time").type(JsonFieldType.STRING).description("수면 종료 시간 (예: 07:00)"),
                                                fieldWithPath("language").type(JsonFieldType.STRING).description("언어 (예: KOR, ENG)")
                                        )
                                        .responseFields(
                                                fieldWithPath("is_success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
                                                fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
                                                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                                                fieldWithPath("payload").type(JsonFieldType.OBJECT).description("응답 데이터"),
                                                fieldWithPath("payload.access_token").type(JsonFieldType.STRING).description("액세스 토큰"),
                                                fieldWithPath("payload.refresh_token").type(JsonFieldType.STRING).description("리프레쉬 토큰")
                                        )
                                        .requestSchema(Schema.schema("OnboardUserRequestSchema"))
                                        .responseSchema(Schema.schema("OnboardUserResponseSchema"))
                                        .build()
                        )
                ));
    }

    @Test
    @DisplayName("유저 정보를 조회한다.")
    public void getUserProfile() throws Exception {
        // given
        String nickname = "UserNickname";
        String email = "user@example.com";
        Language language = Language.KOR;
        String socialPlatform = "google";
        GetUserProfileResponse response = new GetUserProfileResponse(nickname, email, language, socialPlatform);

        Mockito.when(userService.getUserProfile()).thenReturn(response);

        // when
        ResultActions resultActions = this.mockMvc.perform(RestDocumentationRequestBuilders.get("/api/v1/users/profile")
                .accept(MediaType.APPLICATION_JSON));

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_success").value(true))
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("유저 정보 조회에 성공했습니다."))
                .andExpect(jsonPath("$.payload.nickname").value(nickname))
                .andExpect(jsonPath("$.payload.email").value(email))
                .andExpect(jsonPath("$.payload.language").value(language.toString()))
                .andExpect(jsonPath("$.payload.social_platform").value(socialPlatform))
                .andDo(MockMvcRestDocumentationWrapper.document("user/get-profile",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("User API")
                                        .description("유저 정보를 조회한다.")
                                        .responseFields(
                                                fieldWithPath("is_success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
                                                fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
                                                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                                                fieldWithPath("payload").type(JsonFieldType.OBJECT).description("유저 정보 데이터"),
                                                fieldWithPath("payload.nickname").type(JsonFieldType.STRING).description("유저 닉네임"),
                                                fieldWithPath("payload.email").type(JsonFieldType.STRING).description("유저 이메일"),
                                                fieldWithPath("payload.language").type(JsonFieldType.STRING).description("유저 언어"),
                                                fieldWithPath("payload.social_platform").type(JsonFieldType.STRING).description("유저 소셜 로그인 플랫폼")
                                        )
                                        .responseSchema(Schema.schema("GetUserProfileResponseSchema"))
                                        .build()
                        )
                ));
    }

    @Test
    @DisplayName("유저 정보를 수정한다.")
    public void updateUserProfile() throws Exception {
        // given
        UpdateUserProfileRequest request = new UpdateUserProfileRequest(
                "NewNickname",
                Language.ENG
        );
        Mockito.doNothing().when(userService).updateUserProfile(any(UpdateUserProfileRequest.class));
        String requestContent = objectMapper.writeValueAsString(request);

        // when
        ResultActions resultActions = this.mockMvc.perform(RestDocumentationRequestBuilders.patch("/api/v1/users/profile/action-update")
                .content(requestContent)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON));

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_success").value(true))
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("유저 정보 수정에 성공했습니다."))
                .andDo(MockMvcRestDocumentationWrapper.document("user/update-profile",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("User API")
                                        .description("유저 정보를 수정한다.")
                                        .requestFields(
                                                fieldWithPath("nickname").type(JsonFieldType.STRING).description("수정할 닉네임"),
                                                fieldWithPath("language").type(JsonFieldType.STRING).description("수정할 언어")
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
    @DisplayName("유저가 서비스를 탈퇴한다.")
    public void withdrawUser() throws Exception {
        // given
        Mockito.doNothing().when(userService).withdrawUser();

        // when
        ResultActions resultActions = this.mockMvc.perform(RestDocumentationRequestBuilders.post("/api/v1/users/action-withdraw")
                .accept(MediaType.APPLICATION_JSON));

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_success").value(true))
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("유저 서비스 탈퇴에 성공했습니다."))
                .andDo(MockMvcRestDocumentationWrapper.document("user/withdraw-service",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("User API")
                                        .description("유저가 서비스를 탈퇴한다.")
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
    @DisplayName("유저 약관 동의 여부를 조회한다.")
    public void getUserPolicyAgreement() throws Exception {
        // given
        GetUserPolicyAgreementResponse response = new GetUserPolicyAgreementResponse(true, true, false);
        Mockito.when(userService.getUserPolicyAgreement()).thenReturn(response);

        // when
        ResultActions resultActions = this.mockMvc.perform(
                RestDocumentationRequestBuilders.get("/api/v1/users/policy")
                        .accept(MediaType.APPLICATION_JSON)
        );

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_success").value(true))
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("유저 약관 동의 여부 조회에 성공했습니다."))
                .andExpect(jsonPath("$.payload.service_policy_agreement").value(true))
                .andExpect(jsonPath("$.payload.privacy_policy_agreement").value(true))
                .andExpect(jsonPath("$.payload.marketing_policy_agreement").value(false))
                .andDo(MockMvcRestDocumentationWrapper.document("user/get-policy-agreement",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("User API")
                                        .description("유저 약관 동의 여부를 조회한다.")
                                        .responseFields(
                                                fieldWithPath("is_success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
                                                fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
                                                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                                                fieldWithPath("payload").type(JsonFieldType.OBJECT).description("응답 데이터"),
                                                fieldWithPath("payload.service_policy_agreement").type(JsonFieldType.BOOLEAN).description("서비스 이용약관 동의 여부"),
                                                fieldWithPath("payload.privacy_policy_agreement").type(JsonFieldType.BOOLEAN).description("개인정보 수집 및 이용 동의 여부"),
                                                fieldWithPath("payload.marketing_policy_agreement").type(JsonFieldType.BOOLEAN).description("마케팅 정보 수신 동의 여부")
                                        )
                                        .responseSchema(Schema.schema("GetUserPolicyAgreementResponseSchema"))
                                        .build()
                        )
                ));
    }

    @Test
    @DisplayName("유저 약관 동의 여부를 수정한다.")
    public void updateUserPolicyAgreement() throws Exception {
        // given
        UpdateUserPolicyAgreementRequest request = new UpdateUserPolicyAgreementRequest(true, true, false);
        Mockito.doNothing().when(userService).updateUserPolicyAgreement(any(UpdateUserPolicyAgreementRequest.class));

        String requestContent = objectMapper.writeValueAsString(request);

        // when
        ResultActions resultActions = this.mockMvc.perform(
                RestDocumentationRequestBuilders.put("/api/v1/users/policy")
                        .content(requestContent)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
        );

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_success").value(true))
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("유저 약관 동의 여부 수정에 성공했습니다."))
                .andDo(MockMvcRestDocumentationWrapper.document("user/update-policy-agreement",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("User API")
                                        .description("유저 약관 동의 여부를 수정한다.")
                                        .requestFields(
                                                fieldWithPath("service_policy_agreement").type(JsonFieldType.BOOLEAN).description("서비스 이용약관 동의 여부"),
                                                fieldWithPath("privacy_policy_agreement").type(JsonFieldType.BOOLEAN).description("개인정보 수집 및 이용 동의 여부"),
                                                fieldWithPath("marketing_policy_agreement").type(JsonFieldType.BOOLEAN).description("마케팅 정보 수신 동의 여부")
                                        )
                                        .responseFields(
                                                fieldWithPath("is_success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
                                                fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
                                                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지")
                                        )
                                        .requestSchema(Schema.schema("UpdateUserPolicyAgreementRequestSchema"))
                                        .build()
                        )
                ));
    }

    @Test
    @DisplayName("유저 수면 시간을 조회한다.")
    public void getUserSleepTime() throws Exception {
        // given
        GetUserSleepTimeResponse response = new GetUserSleepTimeResponse("23:30", "07:00");
        Mockito.when(userService.getUserSleepTime()).thenReturn(response);

        // when
        ResultActions resultActions = this.mockMvc.perform(
                RestDocumentationRequestBuilders.get("/api/v1/users/sleep-time")
                        .accept(MediaType.APPLICATION_JSON)
        );

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_success").value(true))
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("유저 수면 시간 조회에 성공했습니다."))
                .andExpect(jsonPath("$.payload.sleep_start_time").value("23:30"))
                .andExpect(jsonPath("$.payload.sleep_end_time").value("07:00"))
                .andDo(MockMvcRestDocumentationWrapper.document("user/get-sleep-time",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("User API")
                                        .description("유저 수면 시간을 조회한다.")
                                        .responseFields(
                                                fieldWithPath("is_success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
                                                fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
                                                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                                                fieldWithPath("payload").type(JsonFieldType.OBJECT).description("응답 데이터"),
                                                fieldWithPath("payload.sleep_start_time").type(JsonFieldType.STRING).description("수면 시작 시간 (HH:mm)"),
                                                fieldWithPath("payload.sleep_end_time").type(JsonFieldType.STRING).description("수면 종료 시간 (HH:mm)")
                                        )
                                        .responseSchema(Schema.schema("GetUserSleepTimeResponseSchema"))
                                        .build()
                        )
                ));
    }

    @Test
    @DisplayName("유저 수면 시간을 수정한다.")
    public void updateUserSleepTime() throws Exception {
        // given
        UpdateUserSleepTimeRequest request = new UpdateUserSleepTimeRequest("22:00", "06:30");
        Mockito.doNothing().when(userService).updateUserSleepTime(any(UpdateUserSleepTimeRequest.class));

        String requestContent = objectMapper.writeValueAsString(request);

        // when
        ResultActions resultActions = this.mockMvc.perform(
                RestDocumentationRequestBuilders.put("/api/v1/users/sleep-time")
                        .content(requestContent)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
        );

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_success").value(true))
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("유저 수면 시간 수정에 성공했습니다."))
                .andDo(MockMvcRestDocumentationWrapper.document("user/update-sleep-time",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("User API")
                                        .description("유저 수면 시간을 수정한다.")
                                        .requestFields(
                                                fieldWithPath("sleep_start_time").type(JsonFieldType.STRING).description("수면 시작 시간 (HH:mm)"),
                                                fieldWithPath("sleep_end_time").type(JsonFieldType.STRING).description("수면 종료 시간 (HH:mm)")
                                        )
                                        .responseFields(
                                                fieldWithPath("is_success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
                                                fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
                                                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지")
                                        )
                                        .requestSchema(Schema.schema("UpdateUserSleepTimeRequestSchema"))
                                        .build()
                        )
                ));
    }

    @Test
    @DisplayName("유저가 로그아웃한다.")
    public void logoutUser() throws Exception {
        // given
        Mockito.doNothing().when(userService).logoutUser(any());

        String requestContent = """
        {
            "refresh_token": "sampleRefreshToken"
        }
    """;

        // when
        ResultActions resultActions = this.mockMvc.perform(
                RestDocumentationRequestBuilders.post("/api/v1/users/logout")
                        .content(requestContent)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
        );

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_success").value(true))
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("유저 로그아웃에 성공했습니다."))
                .andDo(MockMvcRestDocumentationWrapper.document("user/logout",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("User API")
                                        .description("유저가 로그아웃한다.")
                                        .requestFields(
                                                fieldWithPath("refresh_token").type(JsonFieldType.STRING).description("리프레쉬 토큰")
                                        )
                                        .responseFields(
                                                fieldWithPath("is_success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
                                                fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
                                                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지")
                                        )
                                        .requestSchema(Schema.schema("LogoutUserRequestSchema"))
                                        .build()
                        )
                ));
    }

    @Test
    @DisplayName("가이드 확인 여부를 저장한다.")
    public void createGuideViewStatus() throws Exception {
        // given
        CreateGuideViewStatusRequest request = new CreateGuideViewStatusRequest(GuideType.SCHEDULE_GUIDE_MODAL_001);
        String requestContent = objectMapper.writeValueAsString(request);

        // when
        Mockito.doNothing().when(userService).createGuideViewStatus(any(CreateGuideViewStatusRequest.class));

        // then
        mockMvc.perform(RestDocumentationRequestBuilders.post("/api/v1/users/guides/view-status")
                        .content(requestContent)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.is_success").value(true))
                .andExpect(jsonPath("$.code").value("201"))
                .andExpect(jsonPath("$.message").value("유저 가이드 확인 여부 저장에 성공했습니다."))
                .andDo(MockMvcRestDocumentationWrapper.document("user/create-guide-view-status",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("User API")
                                        .description("가이드 확인 여부를 저장한다.")
                                        .requestFields(
                                                fieldWithPath("guide_type").type(JsonFieldType.STRING).description("가이드 타입")
                                        )
                                        .responseFields(
                                                fieldWithPath("is_success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
                                                fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
                                                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지")
                                        )
                                        .requestSchema(Schema.schema("CreateGuideViewStatusRequestSchema"))
                                        .build()
                        )
                ));
    }

    @Test
    @DisplayName("가이드 확인 여부를 조회한다.")
    public void getGuideViewStatus() throws Exception {
        // given
        GuideType guideType = GuideType.SCHEDULE_GUIDE_MODAL_001;
        GetGuideViewStatusResponse response = GetGuideViewStatusResponse.from(true);

        // when
        Mockito.when(userService.getGuideViewStatus(any(GuideType.class))).thenReturn(response);

        // then
        mockMvc.perform(RestDocumentationRequestBuilders.get("/api/v1/users/guides/view-status")
                        .queryParam("guide_type", guideType.name())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_success").value(true))
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("유저 가이드 확인 여부 조회에 성공했습니다."))
                .andDo(MockMvcRestDocumentationWrapper.document("user/get-guide-view-status",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("User API")
                                        .description("가이드 확인 여부를 조회한다.")
                                        .queryParameters(
                                                parameterWithName("guide_type").description("가이드 타입")
                                        )
                                        .responseFields(
                                                fieldWithPath("is_success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
                                                fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
                                                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                                                fieldWithPath("payload.is_viewed").type(JsonFieldType.BOOLEAN).description("가이드 확인 여부")
                                        )
                                        .responseSchema(Schema.schema("GetGuideViewStatusResponseSchema"))
                                        .build()
                        )
                ));
    }
}
