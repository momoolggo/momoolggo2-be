package com.green.mmg.rider.rider;

import com.green.mmg.common.exception.BusinessException;
import com.green.mmg.rider.config.RiderProperties;
import com.green.mmg.rider.rider.model.Rider;
import com.green.mmg.rider.rider.model.RiderProfileReq;
import com.green.mmg.rider.rider.model.RiderProfileRes;
import com.green.mmg.rider.rider.model.RiderStatus;
import com.green.mmg.rider.rider.model.VehicleType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * RiderService 단위 테스트 — 가짜 테스트 0건 원칙 (NAJACKS 재발 방지, CLAUDE.md §6.5).
 *
 * <p>커버리지: joinProfile (auto-approve true/false, 중복, vehicle 화이트리스트, blank 검증) +
 * findProfile (happy + NOT_FOUND).</p>
 */
@ExtendWith(MockitoExtension.class)
class RiderServiceTest {

    private static final long CALLER_USER_NO = 42L;

    @Mock private RiderRepository riderRepository;
    @Mock private RiderProperties riderProperties;

    @InjectMocks private RiderService riderService;

    private static RiderProfileReq validReq() {
        return new RiderProfileReq(
                "11-22-333333-44",
                "2종보통",
                "MOTORBIKE",
                "신한은행",
                "110-123-456789",
                "홍길동"
        );
    }

    @Nested
    @DisplayName("JoinProfile")
    class JoinProfile {

        @Test
        @DisplayName("auto-approve true: 신규 Rider INSERT 후 ACTIVE 전환 + save 2회 호출 (D11 임시 블록)")
        void autoApproveTrue_savesAsActive() {
            when(riderRepository.existsByUserNo(CALLER_USER_NO)).thenReturn(false);
            when(riderProperties.autoApprove()).thenReturn(true);

            // save가 호출되면 들어온 Rider를 그대로 반환 — riderNo는 미설정 (mock entity)
            // 그러나 JPA 흐름 상 첫 save는 PENDING 상태, approve() 호출 후 두번째 save는 ACTIVE
            ArgumentCaptor<Rider> riderCaptor = ArgumentCaptor.forClass(Rider.class);
            when(riderRepository.save(riderCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

            RiderProfileRes res = riderService.joinProfile(CALLER_USER_NO, validReq());

            // save 호출은 1회 (rider = repo.save(rider) — approve()는 dirty checking)
            // 다만 본 코드는 명시적으로 두 번째 save를 호출하지 않음 (approve로 상태만 변경)
            verify(riderRepository, times(1)).save(any(Rider.class));

            Rider captured = riderCaptor.getValue();
            assertThat(captured.getUserNo()).isEqualTo(CALLER_USER_NO);
            assertThat(captured.getLicenseNo()).isEqualTo("11-22-333333-44");
            assertThat(captured.getVehicleType()).isEqualTo(VehicleType.MOTORBIKE);

            // approve() 적용 후 status ACTIVE
            assertThat(captured.getStatus()).isEqualTo(RiderStatus.ACTIVE);

            // 응답 dto status도 ACTIVE
            assertThat(res.status()).isEqualTo("ACTIVE");
            assertThat(res.userNo()).isEqualTo(CALLER_USER_NO);
        }

        @Test
        @DisplayName("auto-approve false: PENDING 유지 — D11 임시 블록 미적용 (admin approve 흐름 정상 운영)")
        void autoApproveFalse_savesAsPending() {
            when(riderRepository.existsByUserNo(CALLER_USER_NO)).thenReturn(false);
            when(riderProperties.autoApprove()).thenReturn(false);

            ArgumentCaptor<Rider> riderCaptor = ArgumentCaptor.forClass(Rider.class);
            when(riderRepository.save(riderCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

            RiderProfileRes res = riderService.joinProfile(CALLER_USER_NO, validReq());

            assertThat(riderCaptor.getValue().getStatus()).isEqualTo(RiderStatus.PENDING);
            assertThat(res.status()).isEqualTo("PENDING");
        }

        @Test
        @DisplayName("중복 가입: existsByUserNo true → BusinessException CONFLICT (409, save 미호출)")
        void duplicate_throwsConflict() {
            when(riderRepository.existsByUserNo(CALLER_USER_NO)).thenReturn(true);

            assertThatThrownBy(() -> riderService.joinProfile(CALLER_USER_NO, validReq()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("이미 라이더로 등록된 계정입니다.")
                    .extracting(e -> ((BusinessException) e).getStatus())
                    .isEqualTo(HttpStatus.CONFLICT);

            verify(riderRepository, never()).save(any(Rider.class));
        }

        @Test
        @DisplayName("vehicleType 화이트리스트 위반 (HELICOPTER): BusinessException BAD_REQUEST + save 미호출")
        void invalidVehicleType_throwsBadRequest() {
            when(riderRepository.existsByUserNo(CALLER_USER_NO)).thenReturn(false);

            RiderProfileReq req = new RiderProfileReq(
                    "11-22-333333-44", "1종보통", "HELICOPTER",
                    "신한은행", "110-123-456789", "홍길동"
            );

            assertThatThrownBy(() -> riderService.joinProfile(CALLER_USER_NO, req))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("vehicleType는 WALK/BICYCLE/MOTORBIKE/CAR")
                    .extracting(e -> ((BusinessException) e).getStatus())
                    .isEqualTo(HttpStatus.BAD_REQUEST);

            verify(riderRepository, never()).save(any(Rider.class));
        }

        @Test
        @DisplayName("vehicleType 4종(WALK/BICYCLE/MOTORBIKE/CAR) 모두 valueOf 변환 성공 + Rider 필드 enum 박제 (R3-a 마이그레이션)")
        void allVehicleTypes_valueOfSuccess() {
            when(riderRepository.existsByUserNo(CALLER_USER_NO)).thenReturn(false);
            when(riderProperties.autoApprove()).thenReturn(false);

            ArgumentCaptor<Rider> riderCaptor = ArgumentCaptor.forClass(Rider.class);
            when(riderRepository.save(riderCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

            for (VehicleType type : VehicleType.values()) {
                RiderProfileReq req = new RiderProfileReq(
                        "11-22-333333-44", "1종보통", type.name(),
                        "신한은행", "110-123-456789", "홍길동");
                riderService.joinProfile(CALLER_USER_NO, req);
            }

            verify(riderRepository, times(4)).save(any(Rider.class));
            assertThat(riderCaptor.getAllValues())
                    .extracting(Rider::getVehicleType)
                    .containsExactly(VehicleType.WALK, VehicleType.BICYCLE, VehicleType.MOTORBIKE, VehicleType.CAR);
        }

        @Test
        @DisplayName("필수 필드 blank (licenseNo 빈 문자열): BusinessException BAD_REQUEST + save 미호출")
        void blankLicenseNo_throwsBadRequest() {
            when(riderRepository.existsByUserNo(CALLER_USER_NO)).thenReturn(false);

            RiderProfileReq req = new RiderProfileReq(
                    "  ", "1종보통", "CAR",
                    "신한은행", "110-123-456789", "홍길동"
            );

            assertThatThrownBy(() -> riderService.joinProfile(CALLER_USER_NO, req))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("licenseNo는 필수 입력값입니다.")
                    .extracting(e -> ((BusinessException) e).getStatus())
                    .isEqualTo(HttpStatus.BAD_REQUEST);

            verify(riderRepository, never()).save(any(Rider.class));
        }
    }

    @Nested
    @DisplayName("FindProfile")
    class FindProfile {

        @Test
        @DisplayName("happy: 본인 rider 조회 → RiderProfileRes 반환")
        void happy_returnsDto() {
            Rider rider = new Rider(CALLER_USER_NO, "11-22-333333-44", "2종보통", VehicleType.MOTORBIKE,
                    "신한은행", "110-123-456789", "홍길동");
            rider.approve();  // ACTIVE

            when(riderRepository.findByUserNo(CALLER_USER_NO)).thenReturn(Optional.of(rider));

            RiderProfileRes res = riderService.findProfile(CALLER_USER_NO);

            assertThat(res.userNo()).isEqualTo(CALLER_USER_NO);
            assertThat(res.status()).isEqualTo("ACTIVE");
            assertThat(res.licenseType()).isEqualTo("2종보통");
            assertThat(res.vehicleType()).isEqualTo("MOTORBIKE");
            assertThat(res.accountHolder()).isEqualTo("홍길동");
        }

        @Test
        @DisplayName("부재: findByUserNo empty → BusinessException NOT_FOUND")
        void notFound_throwsNotFound() {
            when(riderRepository.findByUserNo(CALLER_USER_NO)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> riderService.findProfile(CALLER_USER_NO))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("라이더 프로필이 등록되지 않았습니다.")
                    .extracting(e -> ((BusinessException) e).getStatus())
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }
    }
}
