package com.green.mmg.auth.user;

import com.green.mmg.auth.user.model.User;
import com.green.mmg.common.dto.feign.UserBriefDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUserId(String userId);

    boolean existsByUserId(String userId);

    /** Internal API 단건 — UserBriefDto.address는 항상 "" (main이 자체 채움) */
    @Query("""
            SELECT new com.green.mmg.common.dto.feign.UserBriefDto(u.userNo, u.name, u.tel, '')
            FROM User u
            WHERE u.userNo = :userNo
            """)
    Optional<UserBriefDto> findBriefByUserNo(@Param("userNo") long userNo);

    /** Internal API batch — N+1 회피 */
    @Query("""
            SELECT new com.green.mmg.common.dto.feign.UserBriefDto(u.userNo, u.name, u.tel, '')
            FROM User u
            WHERE u.userNo IN :ids
            """)
    List<UserBriefDto> findBriefsByUserNos(@Param("ids") List<Long> ids);

    @Query("""
            SELECT u
            FROM User u
            WHERE u.userNo = :userNo
            """)

    Optional<User> findInternalUserDetailByUserNo(@Param("userNo") long userNo);

    @Query("""
        SELECT u
        FROM User u
        WHERE (:role IS NULL OR u.role = :role)
        ORDER BY u.createdAt DESC
""")
    Page<User> findAllByRole(@Param("role") String role, Pageable pageable);

    @Query("""
        SELECT u
        FROM User u
        WHERE u.status = 'PENDING'
        AND u.role IN ('OWNER', 'RIDER')
        ORDER BY u.createdAt DESC
            
""")
    List<User> findPendingUsers();

    long countByRole(String role);
    long countByCreatedAtBetween(Date start, Date end);

    @Query("SELECT u.userNo FROM User u WHERE u.role = :role")
    List<Long> findAllUserNosByRole(@Param("role") String role);

}
