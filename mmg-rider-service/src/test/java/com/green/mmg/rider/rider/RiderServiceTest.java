package com.green.mmg.rider.rider;

import com.green.mmg.common.dto.ResultResponse;
import com.green.mmg.common.dto.feign.UserBriefDto;
import com.green.mmg.common.exception.BusinessException;
import com.green.mmg.rider.feign.AuthInternalClient;
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
 * <p>SSE 자동화 트랙(2026-05-21) — 라이더 신원 승인/제재 흐름 영구 폐기.
 * 가입 즉시 ACTIVE 박제 (Rider 생성자), autoApprove toggle / approveRider / suspendRider 테스트 삭제.</p>
 */
@ExtendWith(MockitoExtension.class)
class RiderServiceTest {

    private static final long CALLER_USER_NO = 42L;

    @Mock private RiderRepository riderRepository;
    @Mock private AuthInternalClient authInternalClient;

    @InjectMocks private RiderService riderService;

    private static final String VALID_LICENSE_IMAGE_URL = "/uploads/rider-license/test.jpg";

    private static RiderProfileReq validReq() {
        return new RiderProfileReq(
                "11-22-333333-44",
                "2종보통",
                "MOTORBIKE",
                "신한은행",
                "110-123-456789",
                "홍길동",
                "010-1234-5678",
                VALID_LICENSE_IMAGE_URL
        );
    }

    @Nested
    @DisplayName("JoinProfile")
    class JoinProfile {

        @Test
        @DisplayName("happy: 신규 Rider INSERT — Rider 생성자에서 status=ACTIVE 직접 박제 (SSE 자동화 트랙)")
        void happy_savesAsActive() {
            when(riderRepository.existsByUserNo(CALLER_USER_NO)).thenReturn(false);
            ArgumentCaptor<Rider> riderCaptor = ArgumentCaptor.forClass(Rider.class);
            when(riderRepository.save(riderCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

            RiderProfileRes res = riderService.joinProfile(CALLER_USER_NO, validReq());

            verify(riderRepository, times(1)).save(any(Rider.class));

            Rider captured = riderCaptor.getValue();
            assertThat(captured.getUserNo()).isEqualTo(CALLER_USER_NO);
            assertThat(captured.getLicenseNo()).isEqualTo("11-22-333333-44");
            assertThat(captured.getVehicleType()).isEqualTo(VehicleType.MOTORBIKE);
            assertThat(captured.getStatus()).isEqualTo(RiderStatus.ACTIVE);
            assertThat(res.status()).isEqualTo("ACTIVE");
            assertThat(res.userNo()).isEqualTo(CALLER_USER_NO);
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
                    "신한은행", "110-123-456789", "홍길동",
                    "010-1234-5678", VALID_LICENSE_IMAGE_URL
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

            ArgumentCaptor<Rider> riderCaptor = ArgumentCaptor.forClass(Rider.class);
            when(riderRepository.save(riderCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

            for (VehicleType type : VehicleType.values()) {
                RiderProfileReq req = new RiderProfileReq(
                        "11-22-333333-44", "1종보통", type.name(),
                        "신한은행", "110-123-456789", "홍길동",
                        "010-1234-5678", VALID_LICENSE_IMAGE_URL);
                riderService.joinProfile(CALLER_USER_NO, req);
            }

            verify(riderRepository, times(4)).save(any(Rider.class));
            assertThat(riderCaptor.getAllValues())
                    .extracting(Rider::getVehicleType)
                    .containsExactly(VehicleType.WALK, VehicleType.BICYCLE, VehicleType.MOTORBIKE, VehicleType.CAR);
        }

        @Test
        @DisplayName("phone 미입력 시 auth tel 자동 조회 — rider.phone에 auth tel 저장")
        void noPhone_fetchesFromAuth() {
            when(riderRepository.existsByUserNo(CALLER_USER_NO)).thenReturn(false);
            ArgumentCaptor<Rider> riderCaptor = ArgumentCaptor.forClass(Rider.class);
            when(riderRepository.save(riderCaptor.capture())).thenAnswer(i -> i.getArgument(0));

            UserBriefDto brief = new UserBriefDto(CALLER_USER_NO, "홍길동", "010-9999-8888", "");
            when(authInternalClient.getUser(CALLER_USER_NO))
                    .thenReturn(new ResultResponse<>("유저 조회 완료", brief));

            RiderProfileReq req = new RiderProfileReq(
                    "11-22-333333-44", "2종보통", "MOTORBIKE",
                    "신한은행", "110-123-456789", "홍길동", null, VALID_LICENSE_IMAGE_URL);
            riderService.joinProfile(CALLER_USER_NO, req);

            assertThat(riderCaptor.getValue().getPhone()).isEqualTo("010-9999-8888");
            verify(authInternalClient, times(1)).getUser(CALLER_USER_NO);
        }

        @Test
        @DisplayName("phone 미입력 + auth 호출 실패 시 — phone null로 가입 정상 완료 (auth 장애 격리)")
        void noPhone_authFails_registersWithNullPhone() {
            when(riderRepository.existsByUserNo(CALLER_USER_NO)).thenReturn(false);
            ArgumentCaptor<Rider> riderCaptor = ArgumentCaptor.forClass(Rider.class);
            when(riderRepository.save(riderCaptor.capture())).thenAnswer(i -> i.getArgument(0));
            when(authInternalClient.getUser(CALLER_USER_NO)).thenThrow(new RuntimeException("auth down"));

            RiderProfileReq req = new RiderProfileReq(
                    "11-22-333333-44", "2종보통", "MOTORBIKE",
                    "신한은행", "110-123-456789", "홍길동", null, VALID_LICENSE_IMAGE_URL);
            riderService.joinProfile(CALLER_USER_NO, req);

            assertThat(riderCaptor.getValue().getPhone()).isNull();
            verify(riderRepository, times(1)).save(any(Rider.class));
        }

        @Test
        @DisplayName("필수 필드 blank (licenseNo 빈 문자열): BusinessException BAD_REQUEST + save 미호출")
        void blankLicenseNo_throwsBadRequest() {
            when(riderRepository.existsByUserNo(CALLER_USER_NO)).thenReturn(false);

            RiderProfileReq req = new RiderProfileReq(
                    "  ", "1종보통", "CAR",
                    "신한은행", "110-123-456789", "홍길동",
                    "010-1234-5678", VALID_LICENSE_IMAGE_URL
            );

            assertThatThrownBy(() -> riderService.joinProfile(CALLER_USER_NO, req))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("licenseNo는 필수 입력값입니다.")
                    .extracting(e -> ((BusinessException) e).getStatus())
                    .isEqualTo(HttpStatus.BAD_REQUEST);

            verify(riderRepository, never()).save(any(Rider.class));
        }

        @Test
        @DisplayName("licenseImageUrl blank: BusinessException BAD_REQUEST + save 미호출 (2026-05-28 트랙)")
        void blankLicenseImageUrl_throwsBadRequest() {
            when(riderRepository.existsByUserNo(CALLER_USER_NO)).thenReturn(false);

            RiderProfileReq req = new RiderProfileReq(
                    "11-22-333333-44", "1종보통", "CAR",
                    "신한은행", "110-123-456789", "홍길동",
                    "010-1234-5678", null
            );

            assertThatThrownBy(() -> riderService.joinProfile(CALLER_USER_NO, req))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("licenseImageUrl는 필수 입력값입니다.")
                    .extracting(e -> ((BusinessException) e).getStatus())
                    .isEqualTo(HttpStatus.BAD_REQUEST);

            verify(riderRepository, never()).save(any(Rider.class));
        }
    }

    @Nested
    @DisplayName("FindProfile")
    class FindProfile {

        @Test
        @DisplayName("happy: 본인 rider 조회 → RiderProfileRes 반환 (가입 즉시 ACTIVE)")
        void happy_returnsDto() {
            Rider rider = new Rider(CALLER_USER_NO, "11-22-333333-44", "2종보통", VehicleType.MOTORBIKE,
                    "신한은행", "110-123-456789", "홍길동");

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
