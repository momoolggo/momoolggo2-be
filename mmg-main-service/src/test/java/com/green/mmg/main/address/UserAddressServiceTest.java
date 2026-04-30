package com.green.mmg.main.address;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.EmptyResultDataAccessException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Phase 2-Backfill-C: UserAddressService.delete 단위 테스트 — 현재 동작 동결.
 *
 * <p><b>권한 분기 부재를 명시적으로 동결한다.</b><br>
 * {@code delete(long addressId)}는 {@code userNo} 파라미터를 받지 않아
 * 다른 사용자 주소 삭제 시도를 막을 수 없다. 이는 알려진 보안 부채이며
 * <b>Phase 2-Backfill-D에서 userNo 파라미터 추가 + JWT principal 사용 + 권한 분기를 추가할 예정</b>이다.<br>
 * 본 테스트는 현재 동작을 회귀 방지를 위해 그대로 동결한다 (D 단계에서 함께 갱신).</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserAddressService.delete — 단위 테스트 (현재 동작 동결)")
class UserAddressServiceTest {

    @Mock private UserAddressRepository userAddressRepository;

    @InjectMocks
    private UserAddressService userAddressService;

    private static final long ADDRESS_ID = 555L;

    @Nested
    @DisplayName("delete — Repository 단순 위임")
    class Delete {

        @Test
        @DisplayName("happy: deleteById(addressId) 단일 호출 + 권한 검증 호출 일체 없음 (분기 부재 동결)")
        void happyPath_delegatesToDeleteById() {
            userAddressService.delete(ADDRESS_ID);

            verify(userAddressRepository).deleteById(ADDRESS_ID);
            // 권한 분기 부재 동결: userNo 기반 조회/검증/findById 등 후속 호출 일체 없음
            // Phase 2-Backfill-D에서 userNo 파라미터 + 권한 분기 추가 시 본 verify는 갱신 필요
            verifyNoMoreInteractions(userAddressRepository);
        }

        @Test
        @DisplayName("Repository EmptyResultDataAccessException → 호출자에게 그대로 propagate (별도 wrapping 없음)")
        void repositoryException_propagatesAsIs() {
            doThrow(new EmptyResultDataAccessException(1))
                    .when(userAddressRepository).deleteById(ADDRESS_ID);

            assertThatThrownBy(() -> userAddressService.delete(ADDRESS_ID))
                    .isInstanceOf(EmptyResultDataAccessException.class);

            verify(userAddressRepository).deleteById(ADDRESS_ID);
            verifyNoMoreInteractions(userAddressRepository);
        }
    }
}
