package com.green.mmg.main.address;

import com.green.mmg.main.address.model.UserAddress;
import com.green.mmg.main.address.model.UserAddressReq;
import com.green.mmg.main.address.model.UserAddressRes;
import com.green.mmg.main.order.model.OrderAddressInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 3-D-B 검증: UserAddress JPA 전환 + dirty checking + Repository 위임.
 *
 * <p>Service/Repository 직접 검증 (UserAddressController는 @AuthenticationPrincipal 사용으로
 * MockMvc principal null 위험 — Phase 3-B/C에서 발견된 main-service 인증 패턴과 동일).</p>
 */
@SpringBootTest
@Transactional
@Rollback
class UserAddressIntegrationTest {

    @Autowired private UserAddressService userAddressService;
    @Autowired private UserAddressRepository userAddressRepository;

    private static final long TEST_USER_NO = 99999L;

    @Test
    @DisplayName("save → findAll: 단일 entity, defaultAd 1, ORDER BY default_ad DESC, address_id DESC")
    void save_findAll() {
        UserAddressReq req = new UserAddressReq();
        req.setAddress("부산 해운대");
        req.setAddressDetail("101호");
        req.setLatitude(35.1587);
        req.setLongitude(129.1604);
        req.setDefaultAd(1);

        userAddressService.save(TEST_USER_NO, req);
        userAddressRepository.flush();

        List<UserAddressRes> all = userAddressService.findAll(TEST_USER_NO);
        assertThat(all).hasSize(1);
        UserAddressRes res = all.get(0);
        assertThat(res.getAddress()).isEqualTo("부산 해운대");
        assertThat(res.getAddressDetail()).isEqualTo("101호");
        assertThat(res.getLatitude()).isEqualTo(35.1587);
        assertThat(res.getLongitude()).isEqualTo(129.1604);
        assertThat(res.getDefaultAd()).isEqualTo(1);
    }

    @Test
    @DisplayName("save 후 update: dirty checking으로 필드 갱신")
    void update_dirtyChecking() {
        // 1. 사전 INSERT
        UserAddressReq saveReq = new UserAddressReq();
        saveReq.setAddress("기존 주소");
        saveReq.setAddressDetail("기존 상세");
        saveReq.setLatitude(35.0);
        saveReq.setLongitude(129.0);
        saveReq.setDefaultAd(0);
        userAddressService.save(TEST_USER_NO, saveReq);
        userAddressRepository.flush();
        Long savedId = userAddressService.findAll(TEST_USER_NO).get(0).getAddressId();

        // 2. update — dirty checking
        UserAddressReq updateReq = new UserAddressReq();
        updateReq.setAddressId(savedId);
        updateReq.setAddress("변경된 주소");
        updateReq.setAddressDetail("변경된 상세");
        updateReq.setLatitude(36.0);
        updateReq.setLongitude(127.0);
        updateReq.setDefaultAd(0);
        userAddressService.update(TEST_USER_NO, updateReq);
        userAddressRepository.flush();

        UserAddress fetched = userAddressRepository.findById(savedId).orElseThrow();
        assertThat(fetched.getAddress()).isEqualTo("변경된 주소");
        assertThat(fetched.getAddressDetail()).isEqualTo("변경된 상세");
        assertThat(fetched.getLatitude()).isEqualTo(36.0);
    }

    @Test
    @DisplayName("setDefault: resetDefault + dirty checking 단일 1 셋팅")
    void setDefault_resetThenSet() {
        // 두 개 INSERT (둘 다 default 0)
        UserAddressReq r1 = makeReq("A주소", 0);
        UserAddressReq r2 = makeReq("B주소", 0);
        userAddressService.save(TEST_USER_NO, r1);
        userAddressRepository.flush();
        userAddressService.save(TEST_USER_NO, r2);
        userAddressRepository.flush();

        List<UserAddressRes> initial = userAddressService.findAll(TEST_USER_NO);
        Long firstId = initial.get(1).getAddressId();  // ORDER BY default DESC, id DESC → 더 작은 id가 두 번째

        userAddressService.setDefault(TEST_USER_NO, firstId);
        userAddressRepository.flush();

        // 검증: firstId만 default=1, 다른 건 0
        long defaultCount = userAddressRepository.findAll().stream()
                .filter(a -> a.getUserNo() == TEST_USER_NO && Integer.valueOf(1).equals(a.getDefaultAd()))
                .count();
        assertThat(defaultCount).isEqualTo(1);

        UserAddress targetAddr = userAddressRepository.findById(firstId).orElseThrow();
        assertThat(targetAddr.getDefaultAd()).isEqualTo(1);
    }

    @Test
    @DisplayName("findFirstDefaultByUserNo — OrderService 위임 (Phase 3-D-B로 OrderMapper.findDefaultAddress 대체)")
    void findFirstDefault_forOrderService() {
        Optional<OrderAddressInfo> empty = userAddressRepository.findFirstDefaultByUserNo(TEST_USER_NO);
        assertThat(empty).isEmpty();

        UserAddressReq req = makeReq("기본 배송지", 1);
        userAddressService.save(TEST_USER_NO, req);
        userAddressRepository.flush();

        Optional<OrderAddressInfo> defaultAddr = userAddressRepository.findFirstDefaultByUserNo(TEST_USER_NO);
        assertThat(defaultAddr).isPresent();
        assertThat(defaultAddr.get().getAddress()).isEqualTo("기본 배송지");
    }

    private static UserAddressReq makeReq(String address, int defaultAd) {
        UserAddressReq req = new UserAddressReq();
        req.setAddress(address);
        req.setAddressDetail("상세" + address);
        req.setLatitude(35.0);
        req.setLongitude(129.0);
        req.setDefaultAd(defaultAd);
        return req;
    }
}
