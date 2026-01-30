package side.onetime.dto.admin.email.response;

import java.util.List;

import org.springframework.data.domain.Page;

import side.onetime.domain.EmailLog;

public record EmailLogPageResponse(
        List<EmailLogResponse> logs,
        int currentPage,
        int totalPages,
        long totalElements,
        boolean hasNext,
        boolean hasPrevious
) {
    public static EmailLogPageResponse from(Page<EmailLog> page) {
        return new EmailLogPageResponse(
                page.getContent().stream()
                        .map(EmailLogResponse::from)
                        .toList(),
                page.getNumber(),
                page.getTotalPages(),
                page.getTotalElements(),
                page.hasNext(),
                page.hasPrevious()
        );
    }
}
