package com.green.mmg.main.address;

import com.green.mmg.common.exception.BusinessException;
import com.green.mmg.main.address.model.UserAddress;
import com.green.mmg.main.address.model.UserAddressReq;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
