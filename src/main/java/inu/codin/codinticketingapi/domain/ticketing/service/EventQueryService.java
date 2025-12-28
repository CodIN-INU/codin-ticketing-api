package inu.codin.codinticketingapi.domain.ticketing.service;

import inu.codin.codinticketingapi.domain.admin.entity.Event;
import inu.codin.codinticketingapi.domain.admin.entity.EventStatus;
import inu.codin.codinticketingapi.domain.ticketing.dto.response.EventDetailResponse;
import inu.codin.codinticketingapi.domain.ticketing.dto.response.EventPageResponse;
import inu.codin.codinticketingapi.domain.ticketing.dto.response.EventParticipationHistoryPageResponse;
import inu.codin.codinticketingapi.domain.ticketing.entity.Campus;
import inu.codin.codinticketingapi.domain.ticketing.entity.ParticipationStatus;
import inu.codin.codinticketingapi.domain.ticketing.exception.TicketingErrorCode;
import inu.codin.codinticketingapi.domain.ticketing.exception.TicketingException;
import inu.codin.codinticketingapi.domain.ticketing.repository.EventRepository;
import inu.codin.codinticketingapi.domain.ticketing.repository.ParticipationRepository;
import inu.codin.codinticketingapi.domain.user.dto.UserInfoResponse;
import inu.codin.codinticketingapi.domain.user.service.UserClientService;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class EventQueryService {

    private final EventRepository eventRepository;
    private final ParticipationRepository participationRepository;

    private final UserClientService userClientService;
    private final ParticipationService participationService;

    @Transactional(readOnly = true)
    public EventPageResponse getEventList(@NotNull Campus campus, @PositiveOrZero int pageNumber) {
        Pageable pageable = PageRequest.of(pageNumber, 10);

        return EventPageResponse.from(eventRepository.findByCampus(campus, pageable));
    }

    @Transactional(readOnly = true)
    public EventDetailResponse getEventDetail(Long eventId) {
        UserInfoResponse userInfoResponse = userClientService.fetchUser();
        // 유저 티켓팅 정보가 존재하는지 검증
        boolean isExistParticipationData =
                userInfoResponse.getDepartment() != null
                        && userInfoResponse.getStudentId() != null
                        && userInfoResponse.getName() != null
                        && userInfoResponse.getName().length() > 2;
        boolean isUserParticipatedInEvent = participationService.isUserParticipatedInEvent(eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new TicketingException(TicketingErrorCode.EVENT_NOT_FOUND));

        return EventDetailResponse.of(event, isExistParticipationData, isUserParticipatedInEvent);
    }

    @Transactional(readOnly = true)
    public EventParticipationHistoryPageResponse getUserEventList(int pageNumber) {
        if (pageNumber < 1) {
            throw new TicketingException(TicketingErrorCode.PAGE_ILLEGAL_ARGUMENT);
        }
        String userId = userClientService.fetchUser().getUserId();
        Pageable pageable = PageRequest.of(pageNumber, 10, Sort.by("createdAt").descending());
        return EventParticipationHistoryPageResponse.of(participationRepository.findHistoryByUserId(userId, pageable));
    }

    @Transactional(readOnly = true)
    public EventParticipationHistoryPageResponse getUserEventListByStatus(int pageNumber, @NotNull ParticipationStatus status) {
        if (pageNumber < 1) {
            throw new TicketingException(TicketingErrorCode.PAGE_ILLEGAL_ARGUMENT);
        }
        String userId = userClientService.fetchUser().getUserId();
        Pageable pageable = PageRequest.of(pageNumber, 10, Sort.by("createdAt").descending());
        return EventParticipationHistoryPageResponse.of(participationRepository.findHistoryByUserIdAndCanceled(userId, status, pageable));
    }
}