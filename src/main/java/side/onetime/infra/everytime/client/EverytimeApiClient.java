package side.onetime.infra.everytime.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
	name = "everytimeApi",
	url = "https://api.everytime.kr"
)
public interface EverytimeApiClient {

	@PostMapping(
		value = "/find/timetable/table/friend",
		consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
	)
	String getUserTimetable(
		@RequestParam("identifier") String identifier
	);
}
