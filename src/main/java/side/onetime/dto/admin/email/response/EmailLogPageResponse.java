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

    public static EmailLogPageResponse of(List<EmailLog> logs, int page, int size, long totalElements) {
        int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;
        return new EmailLogPageResponse(
                logs.stream()
                        .map(EmailLogResponse::from)
                        .toList(),
                page,
                totalPages,
                totalElements,
                page < totalPages - 1,
                page > 0
        );
    }
}
