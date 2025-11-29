package side.onetime.global.common.status;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import side.onetime.global.common.code.BaseCode;
import side.onetime.global.common.dto.ReasonDto;

@Getter
@AllArgsConstructor
public enum SuccessStatus implements BaseCode {
    // Global
    _OK(HttpStatus.OK, "200", "성공입니다."),
    _CREATED(HttpStatus.CREATED, "201", "생성에 성공했습니다."),
    // Event
    _CREATED_EVENT(HttpStatus.CREATED, "201", "이벤트 생성에 성공했습니다."),
    _GET_EVENT(HttpStatus.OK, "200", "이벤트 조회에 성공했습니다."),
    _GET_PARTICIPANTS(HttpStatus.OK, "200", "참여자 조회에 성공했습니다."),
    _GET_MOST_POSSIBLE_TIME(HttpStatus.OK, "200", "가장 많이 되는 시간 조회에 성공했습니다."),
    _GET_FILTERED_MOST_POSSIBLE_TIME(HttpStatus.OK, "200", "필터링한 참여자의 시간 조회에 성공했습니다."),
    _GET_USER_PARTICIPATED_EVENTS(HttpStatus.OK, "200", "유저 참여 이벤트 목록 조회에 성공했습니다."),
    _GET_PARTICIPATED_EVENTS(HttpStatus.OK, "200", "유저 참여 이벤트 목록 조회에 성공했습니다."),
    _REMOVE_USER_CREATED_EVENT(HttpStatus.OK, "200", "유저가 생성한 이벤트 삭제에 성공했습니다."),
    _MODIFY_EVENT(HttpStatus.OK, "200", "이벤트 수정에 성공했습니다."),
    _GET_EVENT_QR_CODE(HttpStatus.OK, "200", "이벤트 QR 코드 조회에 성공했습니다."),
    // Member
    _REGISTER_MEMBER(HttpStatus.CREATED, "201", "멤버 등록에 성공했습니다."),
    _LOGIN_MEMBER(HttpStatus.OK, "200", "멤버 로그인에 성공했습니다."),
    _IS_POSSIBLE_NAME(HttpStatus.OK, "200", "멤버 이름 중복 확인에 성공했습니다."),
    // Schedule
    _CREATED_DAY_SCHEDULES(HttpStatus.CREATED, "201", "요일 스케줄 등록에 성공했습니다."),
    _CREATED_DATE_SCHEDULES(HttpStatus.CREATED, "201", "날짜 스케줄 등록에 성공했습니다."),
    _GET_ALL_DAY_SCHEDULES(HttpStatus.OK, "200", "전체 요일 스케줄 조회에 성공했습니다."),
    _GET_MEMBER_DAY_SCHEDULES(HttpStatus.OK, "200", "개인(비로그인) 요일 스케줄 조회에 성공했습니다."),
    _GET_USER_DAY_SCHEDULES(HttpStatus.OK, "200", "개인(로그인) 요일 스케줄 조회에 성공했습니다."),
    _GET_ALL_DATE_SCHEDULES(HttpStatus.OK, "200", "전체 날짜 스케줄 조회에 성공했습니다."),
    _GET_MEMBER_DATE_SCHEDULES(HttpStatus.OK, "200", "개인(비로그인) 날짜 스케줄 조회에 성공했습니다."),
    _GET_USER_DATE_SCHEDULES(HttpStatus.OK, "200", "개인(로그인) 날짜 스케줄 조회에 성공했습니다."),
    _GET_FILTERED_DAY_SCHEDULES(HttpStatus.OK, "200", "참여자 필터링 요일 스케줄 조회에 성공했습니다."),
    _GET_FILTERED_DATE_SCHEDULES(HttpStatus.OK, "200", "참여자 필터링 날짜 스케줄 조회에 성공했습니다."),
    // URL
    _CONVERT_TO_SHORTEN_URL(HttpStatus.CREATED, "201", "단축 URL 변환에 성공했습니다."),
    _CONVERT_TO_ORIGINAL_URL(HttpStatus.CREATED, "201", "원본 URL 변환에 성공했습니다."),
    // Token
    _REISSUE_TOKENS(HttpStatus.CREATED, "201", "토큰 재발행에 성공했습니다."),
    // User
    _ONBOARD_USER(HttpStatus.CREATED, "201", "유저 온보딩에 성공했습니다."),
    _GET_USER_PROFILE(HttpStatus.OK, "200", "유저 정보 조회에 성공했습니다."),
    _UPDATE_USER_PROFILE(HttpStatus.OK, "200", "유저 정보 수정에 성공했습니다."),
    _WITHDRAW_USER(HttpStatus.OK, "200", "유저 서비스 탈퇴에 성공했습니다."),
    _GET_USER_POLICY_AGREEMENT(HttpStatus.OK, "200", "유저 약관 동의 여부 조회에 성공했습니다."),
    _UPDATE_USER_POLICY_AGREEMENT(HttpStatus.OK, "200", "유저 약관 동의 여부 수정에 성공했습니다."),
    _GET_USER_SLEEP_TIME(HttpStatus.OK, "200", "유저 수면 시간 조회에 성공했습니다."),
    _UPDATE_USER_SLEEP_TIME(HttpStatus.OK, "200", "유저 수면 시간 수정에 성공했습니다."),
    _LOGOUT_USER(HttpStatus.OK, "200", "유저 로그아웃에 성공했습니다."),
    _CREATE_GUIDE_VIEW_STATUS(HttpStatus.CREATED, "201", "유저 가이드 확인 여부 저장에 성공했습니다."),
    _GET_GUIDE_VIEW_STATUS(HttpStatus.OK, "200", "유저 가이드 확인 여부 조회에 성공했습니다."),
    _DELETE_GUIDE_VIEW_STATUS(HttpStatus.OK, "200", "유저 가이드 확인 여부 삭제에 성공했습니다."),
    // Fixed
    _GET_USER_FIXED_SCHEDULE(HttpStatus.OK, "200", "유저 고정 스케줄 조회에 성공했습니다."),
    _UPDATE_USER_FIXED_SCHEDULE(HttpStatus.OK, "200", "유저 고정 스케줄 수정에 성공했습니다."),
	_GET_USER_EVERYTIME_TIMETABLE(HttpStatus.OK, "200", "유저 에브리타임 시간표 조회에 성공했습니다."),
    // Admin User
    _REGISTER_ADMIN_USER(HttpStatus.CREATED, "201", "관리자 계정 등록에 성공했습니다."),
    _LOGIN_ADMIN_USER(HttpStatus.OK, "200", "관리자 계정 로그인에 성공했습니다."),
    _GET_ADMIN_USER_PROFILE(HttpStatus.OK, "200", "관리자 프로필 조회에 성공했습니다."),
    _GET_ALL_ADMIN_USER_DETAIL(HttpStatus.OK, "200", "전체 관리자 정보 조회에 성공했습니다."),
    _UPDATE_ADMIN_USER_STATUS(HttpStatus.OK, "200", "관리자 권한 수정에 성공했습니다."),
    _WITHDRAW_ADMIN_USER(HttpStatus.OK, "200", "관리자 계정 탈퇴에 성공했습니다."),
    // DashBoard
    _GET_ALL_DASHBOARD_EVENTS(HttpStatus.OK, "200", "관리자 이벤트 대시보드 정보 조회에 성공했습니다."),
    _GET_ALL_DASHBOARD_USERS(HttpStatus.OK, "200", "관리자 유저 대시보드 정보 조회에 성공했습니다."),
    // Banner
    _REGISTER_BANNER(HttpStatus.CREATED, "201", "배너 등록에 성공했습니다."),
    _REGISTER_BAR_BANNER(HttpStatus.CREATED, "201", "띠배너 등록에 성공했습니다."),
    _GET_BANNER(HttpStatus.OK, "200", "배너 단건 조회에 성공했습니다."),
    _GET_BAR_BANNER(HttpStatus.OK, "200", "띠배너 단건 조회에 성공했습니다."),
    _GET_ALL_ACTIVATED_BANNERS(HttpStatus.OK, "200", "활성화된 배너 전체 조회에 성공했습니다."),
    _GET_ALL_ACTIVATED_BAR_BANNERS(HttpStatus.OK, "200", "활성화된 띠배너 전체 조회에 성공했습니다."),
    _GET_ALL_BANNERS(HttpStatus.OK, "200", "배너 전체 조회에 성공했습니다."),
    _GET_ALL_BAR_BANNERS(HttpStatus.OK, "200", "띠배너 전체 조회에 성공했습니다."),
    _UPDATE_BANNER(HttpStatus.OK, "200", "배너 수정에 성공했습니다."),
    _UPDATE_BAR_BANNER(HttpStatus.OK, "200", "띠배너 수정에 성공했습니다."),
    _DELETE_BANNER(HttpStatus.OK, "200", "배너 삭제에 성공했습니다."),
    _DELETE_BAR_BANNER(HttpStatus.OK, "200", "띠배너 삭제에 성공했습니다."),
    _INCREASE_BANNER_CLICK_COUNT(HttpStatus.OK, "200", "배너 클릭 수 증가에 성공했습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public ReasonDto getReason() {
        return ReasonDto.builder()
                .isSuccess(true)
                .code(code)
                .message(message)
                .build();
    }

    @Override
    public ReasonDto getReasonHttpStatus() {
        return ReasonDto.builder()
                .isSuccess(true)
                .httpStatus(httpStatus)
                .code(code)
                .message(message)
                .build();
    }
}
