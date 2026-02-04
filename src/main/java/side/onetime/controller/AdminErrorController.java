package side.onetime.controller;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 어드민 에러 페이지 컨트롤러
 * 어드민 페이지에서 발생한 에러를 커스텀 에러 페이지로 처리
 */
@Controller
public class AdminErrorController implements ErrorController {

    /**
     * HTML 에러 페이지 렌더링
     * 어드민 페이지 404 에러 시 커스텀 404 페이지 반환
     */
    @RequestMapping(value = "/error", produces = MediaType.TEXT_HTML_VALUE)
    public String handleErrorHtml(HttpServletRequest request, Model model) {
        String requestUri = (String) request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);

        // Admin 페이지 요청이고 404인 경우
        if (requestUri != null && requestUri.startsWith("/admin")) {
            if (status != null && Integer.parseInt(status.toString()) == HttpStatus.NOT_FOUND.value()) {
                model.addAttribute("currentUri", requestUri);
                model.addAttribute("currentPage", "");
                model.addAttribute("pageTitle", "404 Not Found");
                return "admin/404";
            }
        }

        // 그 외 에러
        return "error";
    }
}
