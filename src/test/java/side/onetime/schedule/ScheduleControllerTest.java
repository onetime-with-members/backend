package side.onetime.schedule;

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
import side.onetime.controller.ScheduleController;
import side.onetime.dto.schedule.request.CreateDateScheduleRequest;
import side.onetime.dto.schedule.request.CreateDayScheduleRequest;
import side.onetime.dto.schedule.request.GetFilteredSchedulesRequest;
import side.onetime.dto.schedule.response.DateSchedule;
import side.onetime.dto.schedule.response.DaySchedule;
import side.onetime.dto.schedule.response.PerDateSchedulesResponse;
import side.onetime.dto.schedule.response.PerDaySchedulesResponse;
import side.onetime.service.ScheduleService;

import java.util.List;
import java.util.UUID;

import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ScheduleController.class)
public class ScheduleControllerTest extends ControllerTestConfig {

    @MockBean
    private ScheduleService scheduleService;

    @Test
    @DisplayName("요일 스케줄을 등록한다. (토큰 유무에 따라 로그인/비로그인 구분)")
    public void createDaySchedulesForAnonymousUser() throws Exception {
        // given
        String eventId = "123e4567-e89b-12d3-a456-426614174000";
        String memberId = "789e0123-e45b-67c8-d901-234567890abc";
        List<DaySchedule> daySchedules = List.of(
                new DaySchedule("월", List.of("09:00", "10:00"))
        );
        CreateDayScheduleRequest request = new CreateDayScheduleRequest(eventId, memberId, daySchedules);
        String requestContent = new ObjectMapper().writeValueAsString(request);

        Mockito.doNothing().when(scheduleService).createDaySchedulesForAnonymousUser(any(CreateDayScheduleRequest.class));

        // when
        ResultActions resultActions = mockMvc.perform(
                RestDocumentationRequestBuilders.post("/api/v1/schedules/day")
                        .content(requestContent)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
        );

        // then
        resultActions
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.is_success").value(true))
                .andExpect(jsonPath("$.code").value("201"))
                .andExpect(jsonPath("$.message").value("요일 스케줄 등록에 성공했습니다."))

                // docs
                .andDo(MockMvcRestDocumentationWrapper.document("schedule/create-day-anonymous",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("Schedule API")
                                        .description("요일 스케줄을 등록한다. (비로그인의 경우에는 멤버 ID가 필수 값)")
                                        .requestFields(
                                                fieldWithPath("event_id").type(JsonFieldType.STRING).description("이벤트 ID"),
                                                fieldWithPath("member_id").type(JsonFieldType.STRING).description("멤버 ID"),
                                                fieldWithPath("schedules[].time_point").type(JsonFieldType.STRING).description("요일"),
                                                fieldWithPath("schedules[].times[]").type(JsonFieldType.ARRAY).description("스케줄 시간 목록")
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
    @DisplayName("날짜 스케줄을 등록한다. (토큰 유무에 따라 로그인/비로그인 구분)")
    public void createDateSchedulesForAuthenticatedUser() throws Exception {
        // given
        String eventId = "123e4567-e89b-12d3-a456-426614174000";
        String memberId = "789e0123-e45b-67c8-d901-234567890abc";
        List<DateSchedule> dateSchedules = List.of(
                new DateSchedule("2024.12.01", List.of("09:00", "10:00"))
        );
        CreateDateScheduleRequest request = new CreateDateScheduleRequest(eventId, memberId, dateSchedules);
        String requestContent = new ObjectMapper().writeValueAsString(request);

        Mockito.doNothing().when(scheduleService).createDateSchedulesForAnonymousUser(any(CreateDateScheduleRequest.class));

        // when
        ResultActions resultActions = mockMvc.perform(
                RestDocumentationRequestBuilders.post("/api/v1/schedules/date")
                        .content(requestContent)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
        );

        // then
        resultActions
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.is_success").value(true))
                .andExpect(jsonPath("$.code").value("201"))
                .andExpect(jsonPath("$.message").value("날짜 스케줄 등록에 성공했습니다."))

                // docs
                .andDo(MockMvcRestDocumentationWrapper.document("schedule/create-date-authenticated",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("Schedule API")
                                        .description("날짜 스케줄을 등록한다. (비로그인의 경우에는 멤버 ID가 필수 값)")
                                        .requestFields(
                                                fieldWithPath("event_id").type(JsonFieldType.STRING).description("이벤트 ID"),
                                                fieldWithPath("member_id").type(JsonFieldType.STRING).optional().description("멤버 ID (로그인 유저는 필요 없음)"),
                                                fieldWithPath("schedules[].time_point").type(JsonFieldType.STRING).description("날짜"),
                                                fieldWithPath("schedules[].times[]").type(JsonFieldType.ARRAY).description("스케줄 시간 목록")
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
    @DisplayName("이벤트에 대한 모든 요일 스케줄을 조회한다.")
    public void getAllDaySchedules() throws Exception {
        // given
        String eventId = UUID.randomUUID().toString();
        List<DaySchedule> daySchedules = List.of(new DaySchedule("월", List.of("09:00", "10:00")));
        List<PerDaySchedulesResponse> responses = List.of(PerDaySchedulesResponse.of("Test Member", daySchedules));

        Mockito.when(scheduleService.getAllDaySchedules(anyString())).thenReturn(responses);

        // when
        ResultActions resultActions = mockMvc.perform(
                RestDocumentationRequestBuilders.get("/api/v1/schedules/day/{event_id}", eventId)
                        .accept(MediaType.APPLICATION_JSON)
        );

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_success").value(true))
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("전체 요일 스케줄 조회에 성공했습니다."))

                // docs
                .andDo(MockMvcRestDocumentationWrapper.document("schedule/get-all-day-schedules",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("Schedule API")
                                        .description("이벤트에 대한 모든 요일 스케줄을 조회한다.")
                                        .pathParameters(
                                                parameterWithName("event_id").description("이벤트 ID [예시 : dd099816-2b09-4625-bf95-319672c25659]")
                                        )
                                        .responseFields(
                                                fieldWithPath("is_success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
                                                fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
                                                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                                                fieldWithPath("payload[].name").type(JsonFieldType.STRING).description("멤버 이름"),
                                                fieldWithPath("payload[].schedules[].time_point").type(JsonFieldType.STRING).description("요일"),
                                                fieldWithPath("payload[].schedules[].times[]").type(JsonFieldType.ARRAY).description("스케줄 시간 목록")
                                        )
                                        .build()
                        )
                ));
    }

    @Test
    @DisplayName("개인 요일 스케줄을 조회한다. (비로그인 유저)")
    public void getMemberDaySchedules() throws Exception {
        // given
        String eventId = UUID.randomUUID().toString();
        String memberId = UUID.randomUUID().toString();
        List<DaySchedule> daySchedules = List.of(new DaySchedule("화", List.of("11:00", "12:00")));
        PerDaySchedulesResponse response = PerDaySchedulesResponse.of("Test Member", daySchedules);

        Mockito.when(scheduleService.getMemberDaySchedules(anyString(), anyString())).thenReturn(response);

        // when
        ResultActions resultActions = mockMvc.perform(
                RestDocumentationRequestBuilders.get("/api/v1/schedules/day/{event_id}/{member_id}", eventId, memberId)
                        .accept(MediaType.APPLICATION_JSON)
        );

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_success").value(true))
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("개인(비로그인) 요일 스케줄 조회에 성공했습니다."))

                // docs
                .andDo(MockMvcRestDocumentationWrapper.document("schedule/get-member-day-schedules",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("Schedule API")
                                        .description("개인 요일 스케줄을 조회한다. (비로그인 유저)")
                                        .pathParameters(
                                                parameterWithName("event_id").description("이벤트 ID [예시 : dd099816-2b09-4625-bf95-319672c25659]"),
                                                parameterWithName("member_id").description("멤버 ID [예시 : 789e0123-e45b-67c8-d901-234567890abc]")
                                        )
                                        .responseFields(
                                                fieldWithPath("is_success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
                                                fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
                                                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                                                fieldWithPath("payload.name").type(JsonFieldType.STRING).description("멤버 이름"),
                                                fieldWithPath("payload.schedules[].time_point").type(JsonFieldType.STRING).description("요일"),
                                                fieldWithPath("payload.schedules[].times[]").type(JsonFieldType.ARRAY).description("스케줄 시간 목록")
                                        )
                                        .build()
                        )
                ));
    }

    @Test
    @DisplayName("개인 요일 스케줄을 조회한다. (로그인 유저)")
    public void getUserDaySchedules() throws Exception {
        // given
        String eventId = UUID.randomUUID().toString();
        String authorizationHeader = "Bearer sampleAuthToken";
        List<DaySchedule> daySchedules = List.of(new DaySchedule("수", List.of("13:00", "14:00")));
        PerDaySchedulesResponse response = PerDaySchedulesResponse.of("Test User", daySchedules);

        Mockito.when(scheduleService.getUserDaySchedules(anyString())).thenReturn(response);

        // when
        ResultActions resultActions = mockMvc.perform(
                RestDocumentationRequestBuilders.get("/api/v1/schedules/day/{event_id}/user", eventId)
                        .header("Authorization", authorizationHeader)
                        .accept(MediaType.APPLICATION_JSON)
        );

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_success").value(true))
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("개인(로그인) 요일 스케줄 조회에 성공했습니다."))

                // docs
                .andDo(MockMvcRestDocumentationWrapper.document("schedule/get-user-day-schedules",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("Schedule API")
                                        .description("개인 요일 스케줄을 조회한다. (로그인 유저)")
                                        .pathParameters(
                                                parameterWithName("event_id").description("이벤트 ID [예시 : dd099816-2b09-4625-bf95-319672c25659]")
                                        )
                                        .responseFields(
                                                fieldWithPath("is_success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
                                                fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
                                                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                                                fieldWithPath("payload.name").type(JsonFieldType.STRING).description("사용자 이름"),
                                                fieldWithPath("payload.schedules[].time_point").type(JsonFieldType.STRING).description("요일"),
                                                fieldWithPath("payload.schedules[].times[]").type(JsonFieldType.ARRAY).description("스케줄 시간 목록")
                                        )
                                        .build()
                        )
                ));
    }

    @Test
    @DisplayName("참여자 필터링 요일 스케줄을 조회한다.")
    public void getFilteredDaySchedules() throws Exception {
        // given
        String eventId = UUID.randomUUID().toString();

        GetFilteredSchedulesRequest request = new GetFilteredSchedulesRequest(
                List.of(1L, 2L), // users
                List.of(3L)  // members
        );

        List<DaySchedule> daySchedules = List.of(new DaySchedule("월", List.of("09:00", "10:00")));
        List<PerDaySchedulesResponse> responses = List.of(PerDaySchedulesResponse.of("Test Member", daySchedules));

        Mockito.when(scheduleService.getFilteredDaySchedules(anyString(), any(GetFilteredSchedulesRequest.class))).thenReturn(responses);

        // when
        String requestContent = new ObjectMapper().writeValueAsString(request);
        ResultActions resultActions = mockMvc.perform(
                RestDocumentationRequestBuilders.post("/api/v1/schedules/day/{event_id}/filtering", eventId)
                        .content(requestContent)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
        );

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_success").value(true))
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("참여자 필터링 요일 스케줄 조회에 성공했습니다."))

                // docs
                .andDo(MockMvcRestDocumentationWrapper.document("schedule/get-filtered-day-schedules",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("Schedule API")
                                        .description("참여자 필터링 요일 스케줄을 조회한다.")
                                        .pathParameters(
                                                parameterWithName("event_id").description("이벤트 ID [예시 : dd099816-2b09-4625-bf95-319672c25659]")
                                        )
                                        .requestFields(
                                                fieldWithPath("users[]").type(JsonFieldType.ARRAY).description("조회할 유저 ID 목록"),
                                                fieldWithPath("members[]").type(JsonFieldType.ARRAY).description("조회할 멤버 ID 목록")
                                        )
                                        .responseFields(
                                                fieldWithPath("is_success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
                                                fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
                                                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                                                fieldWithPath("payload[].name").type(JsonFieldType.STRING).description("참여자 이름"),
                                                fieldWithPath("payload[].schedules[].time_point").type(JsonFieldType.STRING).description("요일"),
                                                fieldWithPath("payload[].schedules[].times[]").type(JsonFieldType.ARRAY).description("스케줄 시간 목록")
                                        )
                                        .build()
                        )
                ));
    }

    @Test
    @DisplayName("이벤트에 대한 모든 날짜 스케줄을 조회한다.")
    public void getAllDateSchedules() throws Exception {
        // given
        String eventId = UUID.randomUUID().toString();
        List<DateSchedule> dateSchedules = List.of(new DateSchedule("2024-12-01", List.of("09:00", "10:00")));
        List<PerDateSchedulesResponse> responses = List.of(PerDateSchedulesResponse.of("Test Member", dateSchedules));

        Mockito.when(scheduleService.getAllDateSchedules(any(String.class))).thenReturn(responses);

        // when
        ResultActions resultActions = mockMvc.perform(
                RestDocumentationRequestBuilders.get("/api/v1/schedules/date/{event_id}", eventId)
                        .accept(MediaType.APPLICATION_JSON)
        );

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_success").value(true))
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("전체 날짜 스케줄 조회에 성공했습니다."))

                // docs
                .andDo(MockMvcRestDocumentationWrapper.document("schedule/get-all-date-schedules",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("Schedule API")
                                        .description("이벤트에 대한 모든 날짜 스케줄을 조회한다.")
                                        .pathParameters(
                                                parameterWithName("event_id").description("이벤트 ID [예시 : dd099816-2b09-4625-bf95-319672c25659]")
                                        )
                                        .responseFields(
                                                fieldWithPath("is_success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
                                                fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
                                                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                                                fieldWithPath("payload[].name").type(JsonFieldType.STRING).description("멤버 이름"),
                                                fieldWithPath("payload[].schedules[].time_point").type(JsonFieldType.STRING).description("날짜"),
                                                fieldWithPath("payload[].schedules[].times[]").type(JsonFieldType.ARRAY).description("스케줄 시간 목록")
                                        )
                                        .build()
                        )
                ));
    }

    @Test
    @DisplayName("개인 날짜 스케줄을 조회한다. (비로그인 유저)")
    public void getMemberDateSchedules() throws Exception {
        // given
        String eventId = "123e4567-e89b-12d3-a456-426614174000";
        String memberId = "789e0123-e45b-67c8-d901-234567890abc";
        PerDateSchedulesResponse response = PerDateSchedulesResponse.of("memberName", List.of(new DateSchedule("2024.12.01", List.of("09:00", "10:00"))));

        Mockito.when(scheduleService.getMemberDateSchedules(eventId, memberId)).thenReturn(response);

        // when
        ResultActions resultActions = mockMvc.perform(
                RestDocumentationRequestBuilders.get("/api/v1/schedules/date/{event_id}/{member_id}", eventId, memberId)
                        .accept(MediaType.APPLICATION_JSON)
        );

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_success").value(true))
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("개인(비로그인) 날짜 스케줄 조회에 성공했습니다."))
                .andDo(MockMvcRestDocumentationWrapper.document("schedule/get-member-date",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("Schedule API")
                                        .description("개인 날짜 스케줄을 조회한다. (비로그인 유저)")
                                        .pathParameters(
                                                parameterWithName("event_id").description("이벤트 ID [예시 : dd099816-2b09-4625-bf95-319672c25659]"),
                                                parameterWithName("member_id").description("멤버 ID [예시 : 789e0123-e45b-67c8-d901-234567890abc]")
                                        )
                                        .responseFields(
                                                fieldWithPath("is_success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
                                                fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
                                                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                                                fieldWithPath("payload.name").type(JsonFieldType.STRING).description("멤버 이름"),
                                                fieldWithPath("payload.schedules[].time_point").type(JsonFieldType.STRING).description("날짜"),
                                                fieldWithPath("payload.schedules[].times[]").type(JsonFieldType.ARRAY).description("스케줄 시간 목록")
                                        )
                                        .build()
                        )
                ));
    }

    @Test
    @DisplayName("개인 날짜 스케줄을 조회한다. (로그인 유저)")
    public void getUserDateSchedules() throws Exception {
        // given
        String eventId = "123e4567-e89b-12d3-a456-426614174000";
        String authorizationHeader = "Bearer some_token";
        PerDateSchedulesResponse response = PerDateSchedulesResponse.of("userNickname", List.of(new DateSchedule("2024.12.01", List.of("09:00", "10:00"))));

        Mockito.when(scheduleService.getUserDateSchedules(anyString())).thenReturn(response);

        // when
        ResultActions resultActions = mockMvc.perform(
                RestDocumentationRequestBuilders.get("/api/v1/schedules/date/{event_id}/user", eventId)
                        .header("Authorization", authorizationHeader)
                        .accept(MediaType.APPLICATION_JSON)
        );

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_success").value(true))
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("개인(로그인) 날짜 스케줄 조회에 성공했습니다."))
                .andDo(MockMvcRestDocumentationWrapper.document("schedule/get-user-date",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("Schedule API")
                                        .description("개인 날짜 스케줄을 조회한다. (로그인 유저)")
                                        .pathParameters(
                                                parameterWithName("event_id").description("이벤트 ID [예시 : dd099816-2b09-4625-bf95-319672c25659]")
                                        )
                                        .responseFields(
                                                fieldWithPath("is_success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
                                                fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
                                                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                                                fieldWithPath("payload.name").type(JsonFieldType.STRING).description("유저 닉네임"),
                                                fieldWithPath("payload.schedules[].time_point").type(JsonFieldType.STRING).description("날짜"),
                                                fieldWithPath("payload.schedules[].times[]").type(JsonFieldType.ARRAY).description("스케줄 시간 목록")
                                        )
                                        .build()
                        )
                ));
    }

    @Test
    @DisplayName("참여자 필터링 날짜 스케줄을 조회한다.")
    public void getFilteredDateSchedules() throws Exception {
        // given
        String eventId = "123e4567-e89b-12d3-a456-426614174000";

        GetFilteredSchedulesRequest request = new GetFilteredSchedulesRequest(
                List.of(1L, 2L), // users
                List.of(3L)  // members
        );

        List<PerDateSchedulesResponse> responseList = List.of(
                PerDateSchedulesResponse.of("memberName1", List.of(new DateSchedule("2024.12.01", List.of("09:00", "10:00")))),
                PerDateSchedulesResponse.of("memberName2", List.of(new DateSchedule("2024.12.02", List.of("11:00", "12:00"))))
        );

        Mockito.when(scheduleService.getFilteredDateSchedules(anyString(), any(GetFilteredSchedulesRequest.class))).thenReturn(responseList);
        String requestContent = new ObjectMapper().writeValueAsString(request);

        // when
        ResultActions resultActions = mockMvc.perform(
                RestDocumentationRequestBuilders.post("/api/v1/schedules/date/{event_id}/filtering", eventId)
                        .content(requestContent)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
        );

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_success").value(true))
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("참여자 필터링 날짜 스케줄 조회에 성공했습니다."))
                .andDo(MockMvcRestDocumentationWrapper.document("schedule/get-filtered-date",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("Schedule API")
                                        .description("참여자 필터링 날짜 스케줄을 조회한다.")
                                        .pathParameters(
                                                parameterWithName("event_id").description("이벤트 ID [예시 : dd099816-2b09-4625-bf95-319672c25659]")
                                        )
                                        .requestFields(
                                                fieldWithPath("users[]").type(JsonFieldType.ARRAY).description("조회할 유저 ID 목록"),
                                                fieldWithPath("members[]").type(JsonFieldType.ARRAY).description("조회할 멤버 ID 목록")
                                        )
                                        .responseFields(
                                                fieldWithPath("is_success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
                                                fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
                                                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                                                fieldWithPath("payload[].name").type(JsonFieldType.STRING).description("참여자 이름"),
                                                fieldWithPath("payload[].schedules[].time_point").type(JsonFieldType.STRING).description("날짜"),
                                                fieldWithPath("payload[].schedules[].times[]").type(JsonFieldType.ARRAY).description("스케줄 시간 목록")
                                        )
                                        .build()
                        )
                ));
    }
}
