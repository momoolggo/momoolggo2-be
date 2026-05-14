package com.green.mmg.main.address;

import com.green.mmg.main.address.model.UserAddress;
import com.green.mmg.main.address.model.UserAddressRes;
import com.green.mmg.main.internal.dto.UserDefaultAddressRes;
import com.green.mmg.main.order.model.OrderAddressInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Phase 3-D-B: UserAddress 단순 CRUD JPA 전환 + OrderService.findDefaultAddress 위임.
 * UserAddressMapper / Address.xml 제거 (전 SQL 단순 CRUD — 보존 정책 예외, decisions.md 등재).
 */
public interface UserAddressRepository extends JpaRepository<UserAddress, Long> {

    /** 기존 findAllByUserNo: ORDER BY default_ad DESC, address_id DESC + 응답 DTO 매핑 */
    @Query("""
            SELECT new com.green.mmg.main.address.model.UserAddressRes(
                a.addressId, a.address, a.addressDetail, a.latitude, a.longitude, a.defaultAd)
            FROM UserAddress a
            WHERE a.userNo IN (:userNos)
            ORDER BY a.defaultAd DESC, a.addressId DESC
            """)
    List<UserAddressRes> findAllByUserNo(@Param("userNo") long userNo);

    /** 기존 resetDefault: 같은 user의 default_ad 모두 0 */
    @Modifying
    @Query("UPDATE UserAddress a SET a.defaultAd = 0 WHERE a.userNo = :userNo")
    int resetDefault(@Param("userNo") long userNo);

    /** OrderService 위임 (Phase 3-C 잔존 OrderMapper.findDefaultAddress 대체) */
    @Query("""
            SELECT new com.green.mmg.main.order.model.OrderAddressInfo(a.address, a.addressDetail)
            FROM UserAddress a
            WHERE a.userNo = :userNo AND a.defaultAd = 1
            ORDER BY a.addressId DESC
            """)

    List<OrderAddressInfo> findDefaultByUserNo(@Param("userNo") Long userNo);

    default Optional<OrderAddressInfo> findFirstDefaultByUserNo(Long userNo) {
        List<OrderAddressInfo> list = findDefaultByUserNo(userNo);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Query("SELECT COUNT(DISTINCT a.userNo) FROM UserAddress a WHERE a.userNo IN :userNos AND a.address LIKE %:district%")
    long countDistinctByUserNosAndDistrict(@Param("userNos") List<Long> userNos, @Param("district") String district);

    @Query("""
        SELECT new com.green.mmg.main.internal.dto.UserDefaultAddressRes(
            a.userNo,
            a.address,
            a.addressDetail
        )
        FROM UserAddress a
        WHERE a.userNo IN :userNos
        AND a.defaultAd = 1
        """)
    List<UserDefaultAddressRes> findDefaultAddressesByUserNos(@Param("userNos") List<Long> userNos);

}


