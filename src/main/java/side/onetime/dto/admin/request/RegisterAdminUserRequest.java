package side.onetime.dto.admin.request;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import side.onetime.domain.AdminUser;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record RegisterAdminUserRequest(
        @NotBlank(message = "이름은 필수 값입니다.")
        @Size(max = 50, message = "이름은 최대 50자까지 가능합니다.")
        String name,

        @NotBlank(message = "이메일은 필수 값입니다.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        String email,

        @NotBlank(message = "비밀번호는 필수 값입니다.")
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$",
                message = "비밀번호는 8자 이상이며 영문자와 숫자를 모두 포함해야 합니다."
        )
        String password
) {

        public AdminUser toEntity(String encodedPassword) {
                return AdminUser.builder()
                        .name(name)
                        .email(email)
                        .password(encodedPassword)
                        .build();
        }
}
