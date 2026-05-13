package com.green.mmg.main.address;

import com.green.mmg.common.exception.BusinessException;
import com.green.mmg.main.address.model.UserAddress;
import com.green.mmg.main.address.model.UserAddressReq;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Phase 2-Backfill-D Step D-4 + Phase 3-Backfill-A-5:
 * UserAddressService.delete + update 소유자 검증 단위 테스트.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserAddressService — 소유자 검증 (delete + update)")
class UserAddressServiceTest {

    @Mock private UserAddressRepository userAddressRepository;

    @InjectMocks
    private UserAddressService userAddressService;

    private static final long ADDRESS_ID = 555L;
    private static final long OWNER_USER_NO = 42L;
    private static final long OTHER_USER_NO = 99L;

    @Nested
    @DisplayName("delete — findById → 소유자 검증 → delete")
    class Delete {

        @Test
        @DisplayName("happy: 본인 주소 → findById → delete 호출")
        void happyPath_deletesOwnAddress() {
            UserAddress entity = newAddress(ADDRESS_ID, OWNER_USER_NO);
            when(userAddressRepository.findById(ADDRESS_ID)).thenReturn(Optional.of(entity));

            userAddressService.delete(OWNER_USER_NO, ADDRESS_ID);

            verify(userAddressRepository).findById(ADDRESS_ID);
            verify(userAddressRepository).delete(entity);
            verifyNoMoreInteractions(userAddressRepository);
        }

        @Test
        @DisplayName("403: 다른 사용자 주소 삭제 시도 → FORBIDDEN '본인 주소만 삭제 가능합니다.' + delete 미호출")
        void otherUserAddress_throwsForbiddenAndShortCircuits() {
            UserAddress entity = newAddress(ADDRESS_ID, OWNER_USER_NO);  // 주소는 OWNER 소유
            when(userAddressRepository.findById(ADDRESS_ID)).thenReturn(Optional.of(entity));

            assertThatThrownBy(() -> userAddressService.delete(OTHER_USER_NO, ADDRESS_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("본인 주소만 삭제 가능합니다.")
                    .extracting("status").isEqualTo(HttpStatus.FORBIDDEN);

            verify(userAddressRepository, never()).delete(any(UserAddress.class));
            verify(userAddressRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("404: 존재하지 않는 주소 → NOT_FOUND '주소를 찾을 수 없습니다.' + delete 미호출")
        void addressNotFound_throwsNotFound() {
            when(userAddressRepository.findById(ADDRESS_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userAddressService.delete(OWNER_USER_NO, ADDRESS_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("주소를 찾을 수 없습니다.")
                    .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);

            verify(userAddressRepository, never()).delete(any(UserAddress.class));
            verify(userAddressRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("403: 주소의 userNo가 null인 비정상 데이터 → FORBIDDEN (방어적)")
        void addressWithNullUserNo_throwsForbidden() {
            UserAddress entity = newAddress(ADDRESS_ID, null);  // userNo == null
            when(userAddressRepository.findById(ADDRESS_ID)).thenReturn(Optional.of(entity));

            assertThatThrownBy(() -> userAddressService.delete(OWNER_USER_NO, ADDRESS_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("본인 주소만 삭제 가능합니다.");

            verify(userAddressRepository, never()).delete(any(UserAddress.class));
        }
    }

    @Nested
    @DisplayName("update — Phase 3-A-5 소유자 검증 (delete 패턴 일관 적용)")
    class Update {

        @Test
        @DisplayName("happy: 본인 주소 수정 → findById → dirty checking (필드 갱신)")
        void happyPath_updatesOwnAddress() {
            UserAddress entity = newAddress(ADDRESS_ID, OWNER_USER_NO);
            UserAddressReq req = newUpdateReq(ADDRESS_ID, "변경된 주소", 0);
            when(userAddressRepository.findById(ADDRESS_ID)).thenReturn(Optional.of(entity));

            userAddressService.update(OWNER_USER_NO, req);

            assertThat(entity.getAddress()).isEqualTo("변경된 주소");
            // defaultAd != 1 → resetDefault 미호출
            verify(userAddressRepository, never()).resetDefault(anyLong());
        }

        @Test
        @DisplayName("happy: defaultAd=1 → resetDefault(callerUserNo) → flush → dirty checking")
        void happyPath_setDefault_callsResetWithCaller() {
            UserAddress entity = newAddress(ADDRESS_ID, OWNER_USER_NO);
            UserAddressReq req = newUpdateReq(ADDRESS_ID, "주소", 1);
            when(userAddressRepository.findById(ADDRESS_ID)).thenReturn(Optional.of(entity));

            userAddressService.update(OWNER_USER_NO, req);

            verify(userAddressRepository).resetDefault(OWNER_USER_NO);
            verify(userAddressRepository).flush();
            assertThat(entity.getDefaultAd()).isEqualTo(1);
        }

        @Test
        @DisplayName("403: 다른 사용자 주소 수정 시도 → FORBIDDEN '본인 주소만 수정 가능합니다.' + 필드 미변경 + resetDefault 미호출")
        void otherUserAddress_throwsForbiddenAndShortCircuits() {
            UserAddress entity = newAddress(ADDRESS_ID, OWNER_USER_NO);  // 주소는 OWNER 소유
            UserAddressReq req = newUpdateReq(ADDRESS_ID, "위조 시도", 1);
            when(userAddressRepository.findById(ADDRESS_ID)).thenReturn(Optional.of(entity));

            assertThatThrownBy(() -> userAddressService.update(OTHER_USER_NO, req))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("본인 주소만 수정 가능합니다.")
                    .extracting("status").isEqualTo(HttpStatus.FORBIDDEN);

            assertThat(entity.getAddress()).isEqualTo("테스트 주소");  // 미변경 동결
            verify(userAddressRepository, never()).resetDefault(anyLong());
            verify(userAddressRepository, never()).flush();
        }

        @Test
        @DisplayName("404: 존재하지 않는 주소 수정 시도 → NOT_FOUND '주소를 찾을 수 없습니다.' + resetDefault 미호출")
        void addressNotFound_throwsNotFound() {
            UserAddressReq req = newUpdateReq(ADDRESS_ID, "x", 1);
            when(userAddressRepository.findById(ADDRESS_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userAddressService.update(OWNER_USER_NO, req))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("주소를 찾을 수 없습니다.")
                    .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);

            verify(userAddressRepository, never()).resetDefault(anyLong());
        }
    }

    @Nested
    @DisplayName("save — defaultAd 분기 (1: resetDefault+flush 후 save / 0|null: save만)")
    class Save {

        @Test
        @DisplayName("happy: defaultAd=1 → resetDefault(userNo) → flush → save (InOrder 동결)")
        void defaultAd1_resetsThenSaves() {
            UserAddressReq req = newSaveReq("새 주소", "상세", 35.0, 129.0, 1);

            userAddressService.save(OWNER_USER_NO, req);

            InOrder inOrder = inOrder(userAddressRepository);
            inOrder.verify(userAddressRepository).resetDefault(OWNER_USER_NO);
            inOrder.verify(userAddressRepository).flush();

            ArgumentCaptor<UserAddress> captor = ArgumentCaptor.forClass(UserAddress.class);
            inOrder.verify(userAddressRepository).save(captor.capture());

            UserAddress saved = captor.getValue();
            assertThat(saved.getUserNo()).isEqualTo(OWNER_USER_NO);
            assertThat(saved.getAddress()).isEqualTo("새 주소");
            assertThat(saved.getAddressDetail()).isEqualTo("상세");
            assertThat(saved.getLatitude()).isEqualTo(35.0);
            assertThat(saved.getLongitude()).isEqualTo(129.0);
            assertThat(saved.getDefaultAd()).isEqualTo(1);
        }

        @Test
        @DisplayName("happy: defaultAd=0 → resetDefault/flush 미호출, save만")
        void defaultAd0_savesWithoutReset() {
            UserAddressReq req = newSaveReq("일반 주소", "상세2", 36.5, 127.5, 0);

            userAddressService.save(OWNER_USER_NO, req);

            verify(userAddressRepository, never()).resetDefault(anyLong());
            verify(userAddressRepository, never()).flush();

            ArgumentCaptor<UserAddress> captor = ArgumentCaptor.forClass(UserAddress.class);
            verify(userAddressRepository).save(captor.capture());
            assertThat(captor.getValue().getDefaultAd()).isEqualTo(0);
            assertThat(captor.getValue().getUserNo()).isEqualTo(OWNER_USER_NO);
        }

        @Test
        @DisplayName("happy: defaultAd=null → resetDefault/flush 미호출, save만 (가드 동결)")
        void defaultAdNull_savesWithoutReset() {
            UserAddressReq req = newSaveReq("주소", "상세3", 37.0, 128.0, null);

            userAddressService.save(OWNER_USER_NO, req);

            verify(userAddressRepository, never()).resetDefault(anyLong());
            verify(userAddressRepository, never()).flush();

            ArgumentCaptor<UserAddress> captor = ArgumentCaptor.forClass(UserAddress.class);
            verify(userAddressRepository).save(captor.capture());
            assertThat(captor.getValue().getDefaultAd()).isNull();
        }
    }

    @Nested
    @DisplayName("setDefault — resetDefault → flush → findById → dirty checking")
    class SetDefault {

        @Test
        @DisplayName("happy: 기존 주소 → resetDefault → flush → findById → defaultAd=1 (InOrder + dirty)")
        void happyPath_setsDefaultViaDirtyChecking() {
            UserAddress entity = newAddress(ADDRESS_ID, OWNER_USER_NO);
            entity.setDefaultAd(0);  // 초기값 일반
            when(userAddressRepository.findById(ADDRESS_ID)).thenReturn(Optional.of(entity));

            userAddressService.setDefault(OWNER_USER_NO, ADDRESS_ID);

            InOrder inOrder = inOrder(userAddressRepository);
            inOrder.verify(userAddressRepository).resetDefault(OWNER_USER_NO);
            inOrder.verify(userAddressRepository).flush();
            inOrder.verify(userAddressRepository).findById(ADDRESS_ID);
            // dirty checking: entity.defaultAd가 1로 변경됐는지 직접 검증
            assertThat(entity.getDefaultAd()).isEqualTo(1);
            verifyNoMoreInteractions(userAddressRepository);
        }

        @Test
        @DisplayName("404: 존재하지 않는 주소 → NOT_FOUND '주소를 찾을 수 없습니다.' (resetDefault/flush는 이미 호출됐음 — 동작 동결)")
        void addressNotFound_throwsNotFound() {
            when(userAddressRepository.findById(ADDRESS_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userAddressService.setDefault(OWNER_USER_NO, ADDRESS_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("주소를 찾을 수 없습니다.")
                    .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);

            // resetDefault/flush는 findById 이전에 호출됨 → 트랜잭션 롤백 의존
            verify(userAddressRepository).resetDefault(OWNER_USER_NO);
            verify(userAddressRepository).flush();
            verify(userAddressRepository).findById(ADDRESS_ID);
        }
    }

    private static UserAddressReq newSaveReq(String address, String detail, Double lat, Double lng, Integer defaultAd) {
        UserAddressReq req = new UserAddressReq();
        req.setAddress(address);
        req.setAddressDetail(detail);
        req.setLatitude(lat);
        req.setLongitude(lng);
        req.setDefaultAd(defaultAd);
        return req;
    }

    private static UserAddress newAddress(long addressId, Long userNo) {
        UserAddress entity = new UserAddress();
        entity.setAddressId(addressId);
        entity.setUserNo(userNo);
        entity.setAddress("테스트 주소");
        return entity;
    }

    private static UserAddressReq newUpdateReq(long addressId, String address, int defaultAd) {
        UserAddressReq req = new UserAddressReq();
        req.setAddressId(addressId);
        req.setAddress(address);
        req.setAddressDetail("상세");
        req.setLatitude(35.0);
        req.setLongitude(129.0);
        req.setDefaultAd(defaultAd);
        return req;
    }
}
