package inu.codin.codinticketingapi.domain.ticketing.service;

import inu.codin.codinticketingapi.domain.admin.entity.Event;
import inu.codin.codinticketingapi.domain.admin.entity.EventStatus;
import inu.codin.codinticketingapi.domain.ticketing.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class EventCommandService {

    private final EventRepository eventRepository;

    @Transactional
    public void changeAllActiveEventsToUpcoming() {
        List<Event> activeEvents = eventRepository.findByEventStatus(EventStatus.ACTIVE);
        if (activeEvents.isEmpty()) {
            log.info("상태를 변경할 활성 이벤트가 없습니다.");
            return;
        }
        // 각 이벤트의 상태를 UPCOMING으로 변경
        activeEvents.forEach(event -> {
            log.info("이벤트 ID: {}, '{}'의 상태를 ACTIVE에서 UPCOMING으로 변경합니다.", event.getId(), event.getTitle());
            event.updateStatus(EventStatus.UPCOMING);
        });
    }

    @Transactional
    public void restoreUpcomingEventsToActive() {
        List<Event> upcomingEvents = eventRepository.findAllLiveEvent(EventStatus.UPCOMING, LocalDateTime.now());
        if (upcomingEvents.isEmpty()) {
            log.info("ACTIVE로 복구할 UPCOMING 상태의 이벤트가 없습니다.");
            return;
        }
        upcomingEvents.forEach(event -> {
            log.info("Redis 복구로 인해 이벤트 ID: {}의 상태를 ACTIVE로 복구합니다.", event.getId());
            event.updateStatus(EventStatus.ACTIVE);
        });
    }
}
