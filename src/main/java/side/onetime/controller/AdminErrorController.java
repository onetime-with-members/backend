package side.onetime.controller;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;

@Controller
public class AdminErrorController implements ErrorController {

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
