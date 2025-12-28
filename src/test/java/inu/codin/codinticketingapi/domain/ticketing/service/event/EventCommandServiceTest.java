package inu.codin.codinticketingapi.domain.ticketing.service.event;

import inu.codin.codinticketingapi.domain.admin.entity.Event;
import inu.codin.codinticketingapi.domain.admin.entity.EventStatus;
import inu.codin.codinticketingapi.domain.ticketing.entity.Campus;
import inu.codin.codinticketingapi.domain.ticketing.entity.Stock;
import inu.codin.codinticketingapi.domain.ticketing.repository.EventRepository;
import inu.codin.codinticketingapi.domain.ticketing.service.EventCommandService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventCommandServiceTest {

    @InjectMocks
    private EventCommandService eventCommandService;

    @Mock
    private EventRepository eventRepository;

    private static final Long TEST_EVENT_ID_1 = 1L;
    private static final Long TEST_EVENT_ID_2 = 2L;
    private static final String TEST_TITLE_1 = "테스트 이벤트 1";
    private static final String TEST_TITLE_2 = "테스트 이벤트 2";
    private static final String TEST_IMAGE_URL = "http://test-image.com";
    private static final String TEST_LOCATION = "테스트 장소";
    private static final String TEST_DESCRIPTION = "테스트 설명";
    private static final String TEST_TARGET = "테스트 대상";
    private static final int TEST_INITIAL_STOCK = 100;

    @Test
    @DisplayName("모든 ACTIVE 이벤트를 UPCOMING으로 변경 - 정상")
    void changeAllActiveEventsToUpcoming_성공() {
        // given
        Event event1 = createMockEvent(TEST_EVENT_ID_1, TEST_TITLE_1, EventStatus.ACTIVE);
        Event event2 = createMockEvent(TEST_EVENT_ID_2, TEST_TITLE_2, EventStatus.ACTIVE);
        List<Event> activeEvents = List.of(event1, event2);

        given(eventRepository.findByEventStatus(EventStatus.ACTIVE)).willReturn(activeEvents);

        // when
        eventCommandService.changeAllActiveEventsToUpcoming();

        // then
        verify(eventRepository).findByEventStatus(EventStatus.ACTIVE);
        verify(event1).updateStatus(EventStatus.UPCOMING);
        verify(event2).updateStatus(EventStatus.UPCOMING);
    }

    @Test
    @DisplayName("모든 ACTIVE 이벤트를 UPCOMING으로 변경 - 이벤트 없음")
    void changeAllActiveEventsToUpcoming_이벤트없음() {
        // given
        given(eventRepository.findByEventStatus(EventStatus.ACTIVE)).willReturn(Collections.emptyList());

        // when
        eventCommandService.changeAllActiveEventsToUpcoming();

        // then
        verify(eventRepository).findByEventStatus(EventStatus.ACTIVE);
        verify(eventRepository, never()).save(any());
    }

    @Test
    @DisplayName("모든 ACTIVE 이벤트를 UPCOMING으로 변경 - 단일 이벤트")
    void changeAllActiveEventsToUpcoming_단일이벤트() {
        // given
        Event event = createMockEvent(TEST_EVENT_ID_1, TEST_TITLE_1, EventStatus.ACTIVE);
        List<Event> activeEvents = List.of(event);

        given(eventRepository.findByEventStatus(EventStatus.ACTIVE)).willReturn(activeEvents);

        // when
        eventCommandService.changeAllActiveEventsToUpcoming();

        // then
        verify(eventRepository).findByEventStatus(EventStatus.ACTIVE);
        verify(event).updateStatus(EventStatus.UPCOMING);
    }

    @Test
    @DisplayName("UPCOMING 이벤트를 ACTIVE로 복구 - 정상")
    void restoreUpcomingEventsToActive_성공() {
        // given
        LocalDateTime now = LocalDateTime.now();
        Event event1 = createMockEvent(TEST_EVENT_ID_1, TEST_TITLE_1, EventStatus.UPCOMING);
        Event event2 = createMockEvent(TEST_EVENT_ID_2, TEST_TITLE_2, EventStatus.UPCOMING);
        List<Event> upcomingEvents = List.of(event1, event2);

        given(eventRepository.findAllLiveEvent(eq(EventStatus.UPCOMING), any(LocalDateTime.class)))
                .willReturn(upcomingEvents);

        // when
        eventCommandService.restoreUpcomingEventsToActive();

        // then
        verify(eventRepository).findAllLiveEvent(eq(EventStatus.UPCOMING), any(LocalDateTime.class));
        verify(event1).updateStatus(EventStatus.ACTIVE);
        verify(event2).updateStatus(EventStatus.ACTIVE);
    }

    @Test
    @DisplayName("UPCOMING 이벤트를 ACTIVE로 복구 - 이벤트 없음")
    void restoreUpcomingEventsToActive_이벤트없음() {
        // given
        given(eventRepository.findAllLiveEvent(eq(EventStatus.UPCOMING), any(LocalDateTime.class)))
                .willReturn(Collections.emptyList());

        // when
        eventCommandService.restoreUpcomingEventsToActive();

        // then
        verify(eventRepository).findAllLiveEvent(eq(EventStatus.UPCOMING), any(LocalDateTime.class));
        verify(eventRepository, never()).save(any());
    }

    @Test
    @DisplayName("UPCOMING 이벤트를 ACTIVE로 복구 - 단일 이벤트")
    void restoreUpcomingEventsToActive_단일이벤트() {
        // given
        Event event = createMockEvent(TEST_EVENT_ID_1, TEST_TITLE_1, EventStatus.UPCOMING);
        List<Event> upcomingEvents = List.of(event);

        given(eventRepository.findAllLiveEvent(eq(EventStatus.UPCOMING), any(LocalDateTime.class)))
                .willReturn(upcomingEvents);

        // when
        eventCommandService.restoreUpcomingEventsToActive();

        // then
        verify(eventRepository).findAllLiveEvent(eq(EventStatus.UPCOMING), any(LocalDateTime.class));
        verify(event).updateStatus(EventStatus.ACTIVE);
    }

    @Test
    @DisplayName("UPCOMING 이벤트를 ACTIVE로 복구 - 여러 이벤트")
    void restoreUpcomingEventsToActive_여러이벤트() {
        // given
        Event event1 = createMockEvent(TEST_EVENT_ID_1, TEST_TITLE_1, EventStatus.UPCOMING);
        Event event2 = createMockEvent(TEST_EVENT_ID_2, TEST_TITLE_2, EventStatus.UPCOMING);
        Event event3 = createMockEvent(3L, "테스트 이벤트 3", EventStatus.UPCOMING);
        List<Event> upcomingEvents = List.of(event1, event2, event3);

        given(eventRepository.findAllLiveEvent(eq(EventStatus.UPCOMING), any(LocalDateTime.class)))
                .willReturn(upcomingEvents);

        // when
        eventCommandService.restoreUpcomingEventsToActive();

        // then
        verify(eventRepository).findAllLiveEvent(eq(EventStatus.UPCOMING), any(LocalDateTime.class));
        verify(event1).updateStatus(EventStatus.ACTIVE);
        verify(event2).updateStatus(EventStatus.ACTIVE);
        verify(event3).updateStatus(EventStatus.ACTIVE);
    }

    private Event createMockEvent(Long eventId, String title, EventStatus status) {
        Stock stock = Stock.builder()
                .initialStock(TEST_INITIAL_STOCK)
                .build();

        Event event = spy(Event.builder()
                .id(eventId)
                .title(title)
                .campus(Campus.SONGDO_CAMPUS)
                .eventTime(LocalDateTime.now().plusHours(1))
                .eventEndTime(LocalDateTime.now().plusHours(3))
                .description(TEST_DESCRIPTION)
                .locationInfo(TEST_LOCATION)
                .target(TEST_TARGET)
                .eventImageUrl(TEST_IMAGE_URL)
                .stock(stock)
                .build());

        event.updateStatus(status);
        return event;
    }
}
