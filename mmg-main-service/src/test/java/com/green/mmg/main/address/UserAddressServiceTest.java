package com.green.mmg.main.address;

import com.green.mmg.common.exception.BusinessException;
import com.green.mmg.main.address.model.UserAddress;
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
import static org.mockito.Mockito.*;

/**
 * Phase 2-Backfill-D Step D-4: UserAddressService.delete 소유자 검증 추가 단위 테스트.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserAddressService.delete — 소유자 검증")
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

    private static UserAddress newAddress(long addressId, Long userNo) {
        UserAddress entity = new UserAddress();
        entity.setAddressId(addressId);
        entity.setUserNo(userNo);
        entity.setAddress("테스트 주소");
        return entity;
    }
}
