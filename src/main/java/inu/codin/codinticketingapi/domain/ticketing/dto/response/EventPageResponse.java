package inu.codin.codinticketingapi.domain.ticketing.dto.response;

import inu.codin.codinticketingapi.domain.admin.entity.Event;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(description = "이벤트 페이지 목록 응답 DTO")
public class EventPageResponse {

    @Schema(description = "이벤트 목록")
    private List<EventPageDetailResponse> eventList = new ArrayList<>();

    @Schema(description = "마지막 페이지 인덱스", example = "0")
    private long lastPage;

    @Schema(description = "다음 페이지 인덱스", example = "-1")
    private long nextPage;

    public EventPageResponse(List<EventPageDetailResponse> eventList, long lastPage, long nextPage) {
        this.eventList = eventList;
        this.lastPage = lastPage;
        this.nextPage = nextPage;
    }

    public static EventPageResponse of(List<EventPageDetailResponse> eventList, long lastPage, long nextPage) {
        return new EventPageResponse(eventList, lastPage, nextPage);
    }

    public static EventPageResponse from(Page<Event> eventPage) {
        List<EventPageDetailResponse> eventList = eventPage.getContent().stream()
                .map(EventPageDetailResponse::of)
                .toList();

        return new EventPageResponse(
                eventList,
                eventPage.getTotalPages() - 1,
                eventPage.hasNext() ? eventPage.getNumber() + 1 : -1
        );
    }

    public static EventPageResponse from(Page<Event> eventPage, Map<Long, Long> waitingCountMap) {
        List<EventPageDetailResponse> eventList = eventPage.getContent().stream()
                .map(event -> EventPageDetailResponse.of(
                        event,
                        waitingCountMap.getOrDefault(event.getId(), 0L).intValue()
                ))
                .toList();

        return new EventPageResponse(
                eventList,
                eventPage.getTotalPages() - 1, // 0-based index이므로 -1
                eventPage.hasNext() ? eventPage.getNumber() + 1 : -1 // 다음 페이지가 없으면 -1
        );
    }
}