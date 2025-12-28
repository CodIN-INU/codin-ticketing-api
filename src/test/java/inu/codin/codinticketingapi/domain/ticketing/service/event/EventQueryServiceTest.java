package inu.codin.codinticketingapi.domain.ticketing.service.event;

import inu.codin.codinticketingapi.domain.admin.entity.Event;
import inu.codin.codinticketingapi.domain.ticketing.dto.response.EventDetailResponse;
import inu.codin.codinticketingapi.domain.ticketing.dto.response.EventPageResponse;
import inu.codin.codinticketingapi.domain.ticketing.dto.response.EventParticipationHistoryDto;
import inu.codin.codinticketingapi.domain.ticketing.dto.response.EventParticipationHistoryPageResponse;
import inu.codin.codinticketingapi.domain.ticketing.entity.Campus;
import inu.codin.codinticketingapi.domain.ticketing.entity.Department;
import inu.codin.codinticketingapi.domain.ticketing.entity.ParticipationStatus;
import inu.codin.codinticketingapi.domain.ticketing.entity.Stock;
import inu.codin.codinticketingapi.domain.ticketing.exception.TicketingErrorCode;
import inu.codin.codinticketingapi.domain.ticketing.exception.TicketingException;
import inu.codin.codinticketingapi.domain.ticketing.repository.EventRepository;
import inu.codin.codinticketingapi.domain.ticketing.repository.ParticipationRepository;
import inu.codin.codinticketingapi.domain.ticketing.service.EventQueryService;
import inu.codin.codinticketingapi.domain.ticketing.service.ParticipationService;
import inu.codin.codinticketingapi.domain.user.dto.UserInfoResponse;
import inu.codin.codinticketingapi.domain.user.service.UserClientService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventQueryServiceTest {

    private static final String TEST_USER_ID_1 = "user123";
    private static final String TEST_USER_ID_2 = "user456";
    private static final Long TEST_EVENT_ID = 999L;
    private static final String TEST_TITLE = "test-title";
    private static final String TEST_IMAGE_URL = "http://test-image.com";
    private static final String TEST_LOCATION = "test-location";
    private static final String TEST_DESCRIPTION = "test-description";
    private static final int TEST_INITIAL_STOCK = 100;
    private static final LocalDateTime START_TIME = LocalDateTime.now().plusDays(1);
    private static final LocalDateTime END_TIME = LocalDateTime.now().plusDays(1).plusHours(2);
    private static final String TEST_USER_NAME = "test-user-name";
    private static final String TEST_STUDENT_ID = "201901536";
    private static final String TEST_TARGET = "test-target";
    private static final int TEST_DEFAULT_PAGE_NUM = 1;
    private static final int PAGE_SIZE = 10;
    private static final Sort UNSORT = Sort.unsorted();
    private static final Sort DESC_SORT = Sort.by("createdAt").descending();
    private static final Campus SONGDO = Campus.SONGDO_CAMPUS;
    private static final Campus MICHUHOL = Campus.MICHUHOL_CAMPUS;

    @InjectMocks
    private EventQueryService eventQueryService;

    @Mock
    private EventRepository eventRepository;
    @Mock
    private ParticipationRepository participationRepository;
    @Mock
    private UserClientService userClientService;
    @Mock
    private ParticipationService participationService;

    @Test
    @DisplayName("이벤트 리스트 반환 - 정상, 페이징 메타 정보 검증")
    void getEventList_성공() {
        // given
        int page = 0;
        Pageable pageable = PageRequest.of(page, PAGE_SIZE, UNSORT);
        Event mockEvent = getMockEvent();
        Page<Event> mockPage = new PageImpl<>(List.of(mockEvent), pageable, 1);
        // when
        when(eventRepository.findByCampus(SONGDO, pageable)).thenReturn(mockPage);
        // then
        EventPageResponse result = eventQueryService.getEventList(SONGDO, page);
        assertThat(result.getEventList()).hasSize(1);
        assertThat(result.getLastPage()).isEqualTo(0);
        assertThat(result.getNextPage()).isEqualTo(-1);
        verify(eventRepository).findByCampus(SONGDO, pageable);
    }

    @Test
    @DisplayName("이벤트 리스트 반환 - 빈 리스트")
    void getEventList_빈리스트() {
        // given
        int page = 0;
        Pageable pageable = PageRequest.of(page, PAGE_SIZE, UNSORT);
        Page<Event> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
        given(eventRepository.findByCampus(MICHUHOL, pageable)).willReturn(emptyPage);
        // when
        EventPageResponse result = eventQueryService.getEventList(MICHUHOL, page);
        // then
        assertThat(result.getEventList()).isEmpty();
        assertThat(result.getLastPage()).isEqualTo(-1);
        assertThat(result.getNextPage()).isEqualTo(-1);
        verify(eventRepository).findByCampus(MICHUHOL, pageable);
    }

    @Test
    @DisplayName("이벤트 리스트 반환 - Campus NULL 일때")
    void getEventList_campusNull() {
        // when & then
        assertThatThrownBy(() -> eventQueryService.getEventList(null, 0))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("이벤트 리스트 반환 - 페이지 음수 예외")
    void getEventList_페이지음수예외처리() {
        // when & then
        assertThatThrownBy(() -> eventQueryService.getEventList(SONGDO, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("이벤트 리스트 반환 – 기본 페이지 크기(10) 로 호출되는지 검증")
    void getEventList_기본페이지크기() {
        // given
        int page = 2;
        Pageable expected = PageRequest.of(page, PAGE_SIZE, UNSORT);
        given(eventRepository.findByCampus(eq(SONGDO), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(), expected, 0));
        // when
        eventQueryService.getEventList(SONGDO, page);
        // then
        verify(eventRepository, times(1))
                .findByCampus(eq(SONGDO), argThat(p -> p.getPageSize() == PAGE_SIZE && p.getPageNumber() == page));
    }

    @Test
    @DisplayName("이벤트 상세 조회 - 성공")
    void getEventDetail_성공() {
        // given
        UserInfoResponse userInfo = createUserInfo();
        Event mockEvent = getMockEvent();
        given(userClientService.fetchUser()).willReturn(userInfo);
        given(participationService.isUserParticipatedInEvent(TEST_EVENT_ID)).willReturn(false);
        given(eventRepository.findById(TEST_EVENT_ID)).willReturn(Optional.of(mockEvent));
        // when
        EventDetailResponse result = eventQueryService.getEventDetail(TEST_EVENT_ID);
        // then
        assertThat(result.getEventId()).isEqualTo(TEST_EVENT_ID);
        assertThat(result.getEventTitle()).isEqualTo(TEST_TITLE);
        verify(userClientService).fetchUser();
        verify(participationService).isUserParticipatedInEvent(TEST_EVENT_ID);
        verify(eventRepository).findById(TEST_EVENT_ID);
    }

    @Test
    @DisplayName("이벤트 상세 조회 - 이벤트 없음 예외")
    void getEventDetail_이벤트존재X() {
        // given
        UserInfoResponse userInfo = createUserInfo();
        given(userClientService.fetchUser()).willReturn(userInfo);
        given(eventRepository.findById(TEST_EVENT_ID)).willReturn(Optional.empty());
        // when & then
        assertThatThrownBy(() -> eventQueryService.getEventDetail(TEST_EVENT_ID))
                .isInstanceOf(TicketingException.class)
                .hasFieldOrPropertyWithValue("errorCode", TicketingErrorCode.EVENT_NOT_FOUND);
        verify(userClientService).fetchUser();
        verify(eventRepository).findById(TEST_EVENT_ID);
    }

    @Test
    @DisplayName("사용자 이벤트 참여 내역 조회 - 정상, 페이징 메타 정보 검증")
    void getUserEventList_성공() {
        // given
        int pageNumber = 1;
        Pageable pageable = PageRequest.of(pageNumber, PAGE_SIZE, DESC_SORT);
        UserInfoResponse userInfo = getUserInfoResponse();
        EventParticipationHistoryDto historyDto = getEventParticipationHistoryDto();
        Page<EventParticipationHistoryDto> mockPage = new PageImpl<>(List.of(historyDto), pageable, 1);
        given(userClientService.fetchUser()).willReturn(userInfo);
        given(participationRepository.findHistoryByUserId(TEST_USER_ID_1, pageable)).willReturn(mockPage);
        // when
        EventParticipationHistoryPageResponse result = eventQueryService.getUserEventList(pageNumber);
        // then
        assertThat(result.getEventList()).hasSize(1);
        assertThat(result.getLastPage()).isEqualTo(pageNumber);
        assertThat(result.getNextPage()).isEqualTo(-1);
        verify(userClientService).fetchUser();
        verify(participationRepository).findHistoryByUserId(TEST_USER_ID_1, pageable);
    }

    @Test
    @DisplayName("사용자 이벤트 참여 내역 조회 - 페이지 예외")
    void getUserEventList_페이지음수() {
        // when & then
        assertThatThrownBy(() -> eventQueryService.getUserEventList(0))
                .isInstanceOf(TicketingException.class);
    }

    @Test
    @DisplayName("사용자 이벤트 참여 내역 조회 – 기본 페이지 크기(10)로 호출되는지 검증")
    void getUserEventList_기본페이지크기() {
        // given
        given(userClientService.fetchUser()).willReturn(getUserInfoResponse());
        Pageable expected = PageRequest.of(TEST_DEFAULT_PAGE_NUM, PAGE_SIZE, DESC_SORT);
        given(participationRepository.findHistoryByUserId(eq(TEST_USER_ID_1), eq(expected)))
                .willReturn(new PageImpl<>(List.of(), expected, 0));
        // when
        eventQueryService.getUserEventList(TEST_DEFAULT_PAGE_NUM);
        // then
        verify(participationRepository)
                .findHistoryByUserId(eq(TEST_USER_ID_1), eq(expected));
    }

    @Test
    @DisplayName("상태별 사용자 이벤트 참여 내역 조회 - 완료, 취소, 대기")
    void getUserEventListByStatus_상태별() {
        Pageable pageable = PageRequest.of(TEST_DEFAULT_PAGE_NUM, PAGE_SIZE, DESC_SORT);
        UserInfoResponse userInfo = UserInfoResponse.builder().userId(TEST_USER_ID_2).build();

        for (ParticipationStatus status : ParticipationStatus.values()) {
            // given
            Page<EventParticipationHistoryDto> mockPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
            given(userClientService.fetchUser()).willReturn(userInfo);
            given(participationRepository.findHistoryByUserIdAndCanceled(TEST_USER_ID_2, status, pageable)).willReturn(mockPage);
            // when
            EventParticipationHistoryPageResponse result = eventQueryService.getUserEventListByStatus(TEST_DEFAULT_PAGE_NUM, status);
            // then
            assertThat(result.getEventList()).isEmpty();
            assertThat(result.getLastPage()).isEqualTo(0);
            assertThat(result.getNextPage()).isEqualTo(-1);
            verify(userClientService).fetchUser();
            verify(participationRepository).findHistoryByUserIdAndCanceled(TEST_USER_ID_2, status, pageable);
            // reset
            reset(userClientService, participationRepository);
        }
    }

    @Test
    @DisplayName("상태별 사용자 이벤트 참여 내역 조회 – Status Null")
    void getUserEventListByStatus_statusNull() {
        // given
        UserInfoResponse mockUser = getUserInfoResponse();
        given(userClientService.fetchUser()).willReturn(mockUser);
        // when & then
        assertThatThrownBy(() -> eventQueryService.getUserEventListByStatus(TEST_DEFAULT_PAGE_NUM, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("상태별 사용자 이벤트 참여 내역 조회 – 기본 페이지 크기(10)로 호출되는지 검증")
    void getUserEventListByStatus_기본페이지크기() {
        // given
        ParticipationStatus status = ParticipationStatus.COMPLETED;
        given(userClientService.fetchUser()).willReturn(getUserInfoResponse());

        Pageable expected = PageRequest.of(TEST_DEFAULT_PAGE_NUM, PAGE_SIZE, DESC_SORT);
        given(participationRepository.findHistoryByUserIdAndCanceled(eq(TEST_USER_ID_1), eq(status), eq(expected)))
                .willReturn(new PageImpl<>(List.of(), expected, 0));
        // when
        eventQueryService.getUserEventListByStatus(TEST_DEFAULT_PAGE_NUM, status);
        // then
        verify(participationRepository).findHistoryByUserIdAndCanceled(
                eq(TEST_USER_ID_1),
                eq(status),
                eq(expected)
        );
    }

    private static UserInfoResponse getUserInfoResponse() {
        return UserInfoResponse.builder().userId(TEST_USER_ID_1).build();
    }

    private Event getMockEvent() {
        Stock stock = Stock.builder().initialStock(TEST_INITIAL_STOCK).build();
        return Event.builder()
                .id(TEST_EVENT_ID)
                .title(TEST_TITLE)
                .campus(SONGDO)
                .eventTime(START_TIME)
                .eventEndTime(END_TIME)
                .description(TEST_DESCRIPTION)
                .locationInfo(TEST_LOCATION)
                .target(TEST_TARGET)
                .eventImageUrl(TEST_IMAGE_URL)
                .stock(stock)
                .build();
    }

    private EventParticipationHistoryDto getEventParticipationHistoryDto() {
        return EventParticipationHistoryDto.builder()
                .eventId(TEST_EVENT_ID)
                .title(TEST_TITLE)
                .eventImageUrl(TEST_IMAGE_URL)
                .locationInfo(TEST_LOCATION)
                .eventTime(START_TIME)
                .eventEndTime(END_TIME)
                .status(ParticipationStatus.WAITING)
                .build();
    }

    private UserInfoResponse createUserInfo() {
        return UserInfoResponse.builder()
                .userId(TEST_USER_ID_1)
                .name(TEST_USER_NAME)
                .studentId(TEST_STUDENT_ID)
                .department(Department.COMPUTER_SCI)
                .build();
    }
}