package side.onetime.member;

import com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper;
import com.epages.restdocs.apispec.ResourceSnippetParameters;
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
import side.onetime.controller.MemberController;
import side.onetime.dto.member.request.IsDuplicateRequest;
import side.onetime.dto.member.request.LoginMemberRequest;
import side.onetime.dto.member.request.RegisterMemberRequest;
import side.onetime.dto.member.response.IsDuplicateResponse;
import side.onetime.dto.member.response.LoginMemberResponse;
import side.onetime.dto.member.response.RegisterMemberResponse;
import side.onetime.dto.member.response.ScheduleResponse;
import side.onetime.service.MemberService;

import java.util.List;

import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MemberController.class)
public class MemberControllerTest extends ControllerTestConfig {

    @MockBean
    private MemberService memberService;

    @Test
    @DisplayName("멤버를 등록한다.")
    public void registerMember() throws Exception {
        // given
        RegisterMemberRequest request = new RegisterMemberRequest(
                "123e4567-e89b-12d3-a456-426614174000",
                "newMember",
                "1234",
                List.of(
                        new ScheduleResponse("2024.12.01", List.of("09:00", "10:00")),
                        new ScheduleResponse("2024.12.02", List.of("11:00", "12:00"))
                )
        );
        RegisterMemberResponse response = new RegisterMemberResponse("789e0123-e45b-67c8-d901-234567890abc", "CATEGORY");

        Mockito.when(memberService.registerMember(any(RegisterMemberRequest.class))).thenReturn(response);

        // when
        ResultActions result = mockMvc.perform(
                RestDocumentationRequestBuilders.post("/api/v1/members/action-register")
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
        );

        // then
        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.is_success").value(true))
                .andExpect(jsonPath("$.code").value("201"))
                .andExpect(jsonPath("$.message").value("멤버 등록에 성공했습니다."))
                .andDo(MockMvcRestDocumentationWrapper.document("member/register",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("Member API")
                                        .description("멤버를 등록한다.")
                                        .requestFields(
                                                fieldWithPath("event_id").type(JsonFieldType.STRING).description("이벤트 ID"),
                                                fieldWithPath("name").type(JsonFieldType.STRING).description("멤버 이름"),
                                                fieldWithPath("pin").type(JsonFieldType.STRING).description("멤버 PIN"),
                                                fieldWithPath("schedules").type(JsonFieldType.ARRAY).description("스케줄 목록"),
                                                fieldWithPath("schedules[].time_point").type(JsonFieldType.STRING).description("스케줄 날짜"),
                                                fieldWithPath("schedules[].times").type(JsonFieldType.ARRAY).description("시간 목록")
                                        )
                                        .responseFields(
                                                fieldWithPath("is_success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
                                                fieldWithPath("code").type(JsonFieldType.STRING).description("HTTP 상태 코드"),
                                                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                                                fieldWithPath("payload.member_id").type(JsonFieldType.STRING).description("멤버 ID"),
                                                fieldWithPath("payload.category").type(JsonFieldType.STRING).description("이벤트 카테고리")
                                        )
                                        .build()
                        )
                ));
    }

    @Test
    @DisplayName("멤버 로그인을 진행한다.")
    public void loginMember() throws Exception {
        // given
        LoginMemberRequest request = new LoginMemberRequest(
                "123e4567-e89b-12d3-a456-426614174000",
                "existingMember",
                "1234"
        );
        LoginMemberResponse response = new LoginMemberResponse("789e0123-e45b-67c8-d901-234567890abc", "DATE");

        Mockito.when(memberService.loginMember(any(LoginMemberRequest.class))).thenReturn(response);

        // when
        ResultActions result = mockMvc.perform(
                RestDocumentationRequestBuilders.post("/api/v1/members/action-login")
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
        );

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.is_success").value(true))
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("멤버 로그인에 성공했습니다."))
                .andDo(MockMvcRestDocumentationWrapper.document("member/login",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("Member API")
                                        .description("멤버 로그인을 진행한다.")
                                        .requestFields(
                                                fieldWithPath("event_id").type(JsonFieldType.STRING).description("이벤트 ID"),
                                                fieldWithPath("name").type(JsonFieldType.STRING).description("멤버 이름"),
                                                fieldWithPath("pin").type(JsonFieldType.STRING).description("멤버 PIN")
                                        )
                                        .responseFields(
                                                fieldWithPath("is_success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
                                                fieldWithPath("code").type(JsonFieldType.STRING).description("HTTP 상태 코드"),
                                                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                                                fieldWithPath("payload.member_id").type(JsonFieldType.STRING).description("멤버 ID"),
                                                fieldWithPath("payload.category").type(JsonFieldType.STRING).description("이벤트 카테고리")
                                        )
                                        .build()
                        )
                ));
    }

    @Test
    @DisplayName("멤버 이름 중복 확인을 진행한다.")
    public void isDuplicate() throws Exception {
        // given
        IsDuplicateRequest request = new IsDuplicateRequest(
                "123e4567-e89b-12d3-a456-426614174000",
                "duplicateCheckName"
        );
        IsDuplicateResponse response = new IsDuplicateResponse(true);

        Mockito.when(memberService.isDuplicate(any(IsDuplicateRequest.class))).thenReturn(response);

        // when
        ResultActions result = mockMvc.perform(
                RestDocumentationRequestBuilders.post("/api/v1/members/name/action-check")
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
        );

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.is_success").value(true))
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("멤버 이름 중복 확인에 성공했습니다."))
                .andDo(MockMvcRestDocumentationWrapper.document("member/check-duplicate",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("Member API")
                                        .description("멤버 이름 중복 확인을 진행한다.")
                                        .requestFields(
                                                fieldWithPath("event_id").type(JsonFieldType.STRING).description("이벤트 ID"),
                                                fieldWithPath("name").type(JsonFieldType.STRING).description("확인할 멤버 이름")
                                        )
                                        .responseFields(
                                                fieldWithPath("is_success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
                                                fieldWithPath("code").type(JsonFieldType.STRING).description("HTTP 상태 코드"),
                                                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                                                fieldWithPath("payload.is_possible").type(JsonFieldType.BOOLEAN).description("이름 사용 가능 여부")
                                        )
                                        .build()
                        )
                ));
    }
}
