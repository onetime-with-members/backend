package side.onetime.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 이메일 템플릿 코드
 * DB email_templates.code 컬럼 값과 매핑
 *
 * API에서 특정 시점에 즉시 트리거되는 이메일(회원가입, 이벤트 확정 등)은
 * 배치가 아닌 API → SQS 직접 발행 방식으로 처리되며,
 * 해당 템플릿 코드를 이 enum에 추가하여 관리한다.
 *
 * @see side.onetime.service.EmailService
 */
@Getter
@RequiredArgsConstructor
public enum EmailTemplateCode {
    WELCOME("WELCOME"),
    ;

    private final String code;
}
