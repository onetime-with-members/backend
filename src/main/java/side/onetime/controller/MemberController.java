package side.onetime.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import side.onetime.auth.annotation.PublicApi;
import side.onetime.dto.member.request.IsDuplicateRequest;
import side.onetime.dto.member.request.LoginMemberRequest;
import side.onetime.dto.member.request.RegisterMemberRequest;
import side.onetime.dto.member.response.IsDuplicateResponse;
import side.onetime.dto.member.response.LoginMemberResponse;
import side.onetime.dto.member.response.RegisterMemberResponse;
import side.onetime.global.common.ApiResponse;
import side.onetime.global.common.status.SuccessStatus;
import side.onetime.service.MemberService;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    /**
     * 멤버 등록 API.
     *
     * 이 API는 새로운 멤버를 등록합니다. 멤버가 속한 이벤트 ID와 이름, PIN, 스케줄 목록을 받습니다.
     *
     * @param registerMemberRequest 등록할 멤버 정보 (이벤트 ID, 이름, PIN, 스케줄 목록)
     * @return 성공 여부와 등록된 멤버 정보 (멤버 ID, 이벤트 카테고리)
     */
    @PublicApi
    @PostMapping("/action-register")
    public ResponseEntity<ApiResponse<RegisterMemberResponse>> registerMember(
            @Valid @RequestBody RegisterMemberRequest registerMemberRequest) {

        RegisterMemberResponse registerMemberResponse = memberService.registerMember(registerMemberRequest);
        return ApiResponse.onSuccess(SuccessStatus._REGISTER_MEMBER, registerMemberResponse);
    }

    /**
     * 멤버 로그인 API.
     *
     * 이 API는 멤버의 로그인 정보를 확인하고, 로그인에 성공한 경우 멤버의 정보를 반환합니다.
     *
     * @param loginMemberRequest 로그인할 멤버 정보 (이벤트 ID, 이름, PIN)
     * @return 성공 여부와 로그인된 멤버 정보 (멤버 ID, 이벤트 카테고리)
     */
    @PublicApi
    @PostMapping("/action-login")
    public ResponseEntity<ApiResponse<LoginMemberResponse>> loginMember(
            @Valid @RequestBody LoginMemberRequest loginMemberRequest) {

        LoginMemberResponse loginMemberResponse = memberService.loginMember(loginMemberRequest);
        return ApiResponse.onSuccess(SuccessStatus._LOGIN_MEMBER, loginMemberResponse);
    }

    /**
     * 이름 중복 확인 API.
     *
     * 이 API는 특정 이벤트에서 지정한 이름이 중복되는지 확인합니다.
     *
     * @param isDuplicateRequest 중복 확인할 정보 (이벤트 ID, 확인할 이름)
     * @return 성공 여부와 이름 사용 가능 여부 (isPossible 필드)
     */
    @PublicApi
    @PostMapping("/name/action-check")
    public ResponseEntity<ApiResponse<IsDuplicateResponse>> isDuplicate(
            @Valid @RequestBody IsDuplicateRequest isDuplicateRequest) {

        IsDuplicateResponse isDuplicateResponse = memberService.isDuplicate(isDuplicateRequest);
        return ApiResponse.onSuccess(SuccessStatus._IS_POSSIBLE_NAME, isDuplicateResponse);
    }
}
