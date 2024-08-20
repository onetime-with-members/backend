package side.onetime.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import side.onetime.dto.EventDto;
import side.onetime.global.common.ApiResponse;
import side.onetime.global.common.constant.SuccessStatus;
import side.onetime.service.EventService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class EventController {
    private final EventService eventService;

    // 이벤트 생성 API
    @PostMapping
    public ResponseEntity<ApiResponse<EventDto.CreateEventResponse>> createEvent(
            @RequestBody EventDto.CreateEventRequest createEventRequest) {

        EventDto.CreateEventResponse createEventResponse = eventService.createEvent(createEventRequest);
        return ApiResponse.onSuccess(SuccessStatus._CREATED_EVENT, createEventResponse);
    }

    // 이벤트 조회 API
    @GetMapping("/{event_id}")
    public ResponseEntity<ApiResponse<EventDto.GetEventResponse>> getEvent(
            @PathVariable("event_id") String eventId) {

        EventDto.GetEventResponse getEventResponse = eventService.getEvent(eventId);
        return ApiResponse.onSuccess(SuccessStatus._GET_EVENT, getEventResponse);
    }

    // 참여자 조회 API
    @GetMapping("/{event_id}/participants")
    public ResponseEntity<ApiResponse<EventDto.GetParticipantsResponse>> getParticipants(
            @PathVariable("event_id") String eventId) {

        EventDto.GetParticipantsResponse getParticipantsResponse = eventService.getParticipants(eventId);
        return ApiResponse.onSuccess(SuccessStatus._GET_PARTICIPANTS, getParticipantsResponse);
    }

    // 가장 많이 되는 시간 조회 API
    @GetMapping("/{event_id}/most")
    public ResponseEntity<ApiResponse<List<EventDto.GetMostPossibleTime>>> getMostPossibleTime(
            @PathVariable("event_id") String eventId) {

        List<EventDto.GetMostPossibleTime> getMostPossibleTimes = eventService.getMostPossibleTime(eventId);
        return ApiResponse.onSuccess(SuccessStatus._GET_MOST_POSSIBLE_TIME, getMostPossibleTimes);
    }
}
