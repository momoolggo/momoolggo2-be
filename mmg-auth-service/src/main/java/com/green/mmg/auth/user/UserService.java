package com.green.mmg.auth.user;

import com.green.mmg.auth.feign.AdminSettlementClient;
import com.green.mmg.auth.feign.MainOwnerProfileClient;
import com.green.mmg.auth.feign.MainUserCleanupClient;
import com.green.mmg.auth.feign.RiderUserClient;
import com.green.mmg.auth.feign.dto.OwnerProfileCreateReq;
import com.green.mmg.auth.feign.dto.OwnerWithdrawCheckRes;
import com.green.mmg.auth.mail.EmailService;
import com.green.mmg.auth.token.RefreshTokenStore;
import com.green.mmg.auth.user.model.*;
import com.green.mmg.common.constants.ConstJwt;
import com.green.mmg.common.exception.BusinessException;
import com.green.mmg.common.jwt.JwtTokenManager;
import com.green.mmg.common.jwt.JwtTokenProvider;
import com.green.mmg.common.model.JwtUser;
import com.green.mmg.common.util.MyCookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Phase 3-A: MyBatis → JPA 전환 (단순 CRUD).
 * UserMapper 제거됨 — 모든 쿼리 UserRepository로 이동.
 * Phase 3-B 이후 복잡 쿼리 필요시 UserMapper.java + User.xml 재도입 가능 (mybatis starter 영구 유지).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private static final List<String> FIND_AUTH_ROLES = List.of("CUSTOMER", "OWNER", "RIDER");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenManager jwtTokenManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final MyCookieUtil myCookieUtil;
    private final ConstJwt constJwt;
    private final RefreshTokenStore refreshTokenStore;  // Phase 4-C: RT revoke 가능성 보장

    private static final Duration RESET_PW_CODE_TTL = Duration.ofMinutes(5);
    private static final String RESET_PW_CODE_PREFIX = "auth:reset-pw:";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final StringRedisTemplate stringRedisTemplate;
    private final EmailService emailService;
    private final MainOwnerProfileClient mainOwnerProfileClient;

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String STATUS_SUSPENDED = "SUSPENDED";
    private static final String STATUS_WITHDRAWN = "WITHDRAWN";

    private final MainUserCleanupClient mainUserCleanupClient;
    private final RiderUserClient riderUserClient;
    private final AdminSettlementClient adminSettlementClient;

    // ── 아이디 중복확인
    @Transactional(readOnly = true)
    public boolean checkId(String userId) {
        return !userRepository.existsByUserId(userId);
    }

    @Transactional(readOnly = true)
    public boolean checkEmail(String email) {
        String normalizedEmail = normalizeEmail(email);
        validateRequired(normalizedEmail, "이메일은 필수입니다.");
        return !userRepository.existsByEmail(normalizedEmail);
    }

    // ── 회원가입 (옵션 D-1: BFF 패턴)
    // 가입 후 즉시 AT/RT 발급. user_address 등록은 프론트가 별도 POST /api/address 호출 (main-service).
    @Transactional
    public UserSigninRes signup(UserSignupReq req, HttpServletResponse res) {
        // 작업 C (2026-05-18): 이용약관 동의 검증 — 미동의 시 가입 차단 (이력 저장 X)
        if (req.getAgreedToTerms() == null || !req.getAgreedToTerms()) {
            throw new BusinessException("이용약관에 동의해야 회원가입이 가능합니다.", HttpStatus.BAD_REQUEST);
        }

        String role = (req.getRole() == null || req.getRole().isBlank()) ? "CUSTOMER" : req.getRole();
        String email = normalizeEmail(req.getEmail());
        validateRequiredEmail(role, email);
        validateDuplicateEmail(email, null);

        if ("OWNER".equals(role)) {
            validateOwnerSignupRequired(req);
        }
        User user = new User();
        user.setUserId(req.getUserId());
        user.setUserPw(passwordEncoder.encode(req.getUserPw()));
        user.setName(req.getName());
        user.setBirth(req.getBirth());
        user.setGender(req.getGender() == null ? 0 : req.getGender());
        user.setTel(req.getTel());
        user.setEmail(email);
        user.setRole(role);

        if ("CUSTOMER".equals(role)) {
            user.setRank("BRONZE");
            user.setGreen(0);
            user.setKind(0);
        }

        if("OWNER".equals(role) || "RIDER".equals(role)) {
            user.setStatus("PENDING");
        } else {
            user.setStatus("ACTIVE");
        }

        User saved = userRepository.save(user);

        if ("OWNER".equals(role)) {
            mainOwnerProfileClient.createOwnerProfile(new OwnerProfileCreateReq(
                    saved.getUserNo(),
                    buildStoreAddress(req),
                    req.getBusinessNumber(),
                    req.getBusinessLicenseUrl(),
                    req.getMailOrderLicenseUrl(),
                    req.getBankName(),
                    req.getAccountNumber(),
                    req.getAccountHolder()
            ));
        }

        long atExpiresAt = 0L;

        if ("ACTIVE".equals(saved.getStatus()) || "RIDER".equals(saved.getRole())) {
            JwtUser jwtUser = new JwtUser(saved.getUserNo(), saved.getRole(), saved.getStatus(), saved.getName());
            issueAndStoreTokens(res, jwtUser);
            atExpiresAt = System.currentTimeMillis() + constJwt.getAccessTokenValidityMilliseconds();
        } else {
            jwtTokenManager.signOut(res);
        }

        return new UserSigninRes(saved.getUserNo(), saved.getName(), saved.getRole(), atExpiresAt, null);
    }

    // ── 로그인 (조회 + JWT 발급, DB 변경 없음)
    @Transactional(readOnly = true)
    public UserSigninRes signin(UserSigninReq req, HttpServletResponse res) {
        User user = userRepository.findByUserId(req.getUserId())
                .orElseThrow(() -> new BusinessException("아이디 또는 비밀번호가 틀렸습니다.", HttpStatus.UNAUTHORIZED));

        if (!passwordEncoder.matches(req.getUserPw(), user.getUserPw())) {
            throw new BusinessException("아이디 또는 비밀번호가 틀렸습니다.", HttpStatus.UNAUTHORIZED);
        }

        if (!STATUS_ACTIVE.equals(user.getStatus())) {
            if (STATUS_WITHDRAWN.equals(user.getStatus())) {
                boolean recoverable = user.getWithdrawnAt() != null
                        && user.getWithdrawnAt().isAfter(LocalDateTime.now().minusDays(14));
                String msg = recoverable
                        ? "탈퇴한 계정입니다. 14일 이내 복구 가능합니다."
                        : "탈퇴한 계정입니다. 복구 기간이 지났습니다.";
                throw new BusinessException(msg, HttpStatus.FORBIDDEN);
            }
            throw inactiveAccountException(user.getStatus(), HttpStatus.FORBIDDEN);
        }

        JwtUser jwtUser = new JwtUser(user.getUserNo(), user.getRole(), user.getStatus(), user.getName());
        issueAndStoreTokens(res, jwtUser);

        return new UserSigninRes(user.getUserNo(), user.getName(), user.getRole(),
                System.currentTimeMillis() + constJwt.getAccessTokenValidityMilliseconds(), null);
    }
    /**
     * Phase 4-C: AT/RT 발급 + RT를 Redis에 저장. signup/signin 공통.
     *
     * <p>순서: 쿠키 세팅 → Redis 저장 (사용자 결정 — Redis 좀비 데이터 회피).
     * Redis 저장 실패(RedisConnectionFailureException 등) 시 그대로 throw —
     * D1 결정(정합성 우선): login 자체 실패. best-effort 회피.</p>
     */
    private void issueAndStoreTokens(HttpServletResponse res, JwtUser jwtUser) {
        String accessToken = jwtTokenProvider.generateAccessToken(jwtUser);
        String refreshToken = jwtTokenProvider.generateRefreshToken(jwtUser);
        jwtTokenManager.setAccessTokenInCookie(res, accessToken);
        jwtTokenManager.setRefreshTokenInCookie(res, refreshToken);
        refreshTokenStore.save(jwtUser.getSignedUserNo(), refreshToken,
                Duration.ofMillis(constJwt.getRefreshTokenValidityMilliseconds()));
    }

    // ── 로그아웃 (Phase 4-C: Redis RT 삭제 + 쿠키 만료)
    // D1-bis: Redis delete 실패 시 best-effort — 쿠키 만료는 진행, 로그만 남김.
    // 종료점이라 정합성 살짝 깨져도 RT 자연 만료로 결국 무효화. UX 마이너스 회피.
    public void signout(long userNo, HttpServletResponse res) {
        try {
            refreshTokenStore.delete(userNo);
        } catch (Exception e) {
            log.warn("signout: RT 저장소 삭제 실패 (best-effort 진행) — userNo={}, cause={}",
                    userNo, e.getMessage());
        }
        jwtTokenManager.signOut(res);
    }

    // ── AT 재발급 (RT 검증 후 새 AT만 발급, RT는 그대로)
    // RT 부재 → BusinessException(401) / RT 만료·위변조 → JwtException → GlobalExceptionHandler가 401로 응답
    // Phase 4-C: 저장소 RT 비교 추가 — signout 후 또는 위조 시도 시 401 (revoke 가능성 보장)
    public void reissue(HttpServletRequest req, HttpServletResponse res) {
        String cookieRefreshToken = myCookieUtil.getValue(req, constJwt.getRefreshTokenCookieName());
        if (cookieRefreshToken == null) {
            throw new BusinessException("리프레시 토큰이 없습니다.", HttpStatus.UNAUTHORIZED);
        }
        JwtUser jwtUser = jwtTokenProvider.getJwtUserFromToken(cookieRefreshToken);

        // Phase 4-C: 저장 RT == 쿠키 RT 비교. 부재(signout 후)/불일치(위조) 시 401, 재로그인 강제.
        String storedRefreshToken = refreshTokenStore.get(jwtUser.getSignedUserNo())
                .orElseThrow(() -> new BusinessException(
                        "리프레시 토큰이 만료되었거나 로그아웃되었습니다.", HttpStatus.UNAUTHORIZED));
        if (!cookieRefreshToken.equals(storedRefreshToken)) {
            throw new BusinessException(
                    "리프레시 토큰이 유효하지 않습니다. 재로그인이 필요합니다.", HttpStatus.UNAUTHORIZED);
        }

        User user = userRepository.findById(jwtUser.getSignedUserNo())
                .orElseThrow(() -> new BusinessException("회원 정보를 찾을 수 없습니다.", HttpStatus.UNAUTHORIZED));

        if (!STATUS_ACTIVE.equals(user.getStatus())) {
            throw inactiveAccountException(user.getStatus(), HttpStatus.UNAUTHORIZED);
        }

        JwtUser activeJwtUser = new JwtUser(user.getUserNo(), user.getRole(), user.getStatus(), user.getName());
        jwtTokenManager.setAccessTokenInCookie(res, activeJwtUser);
    }

    // 내 정보 조회
    @Transactional(readOnly = true)
    public UserGetRes getUser(Long userNo) {
        User user = findActiveUser(userNo, HttpStatus.FORBIDDEN);
        return new UserGetRes(user.getUserId(), user.getName(), user.getTel(), user.getEmail(), user.getGender(), user.getBirth(),
                user.getGreen() == null ? 0 : user.getGreen());
    }

    // 내 정보 수정 — JPA dirty checking (기존 MyBatis <if> 동적 UPDATE와 동일 의미)
    @Transactional
    public void updateUser(Long userNo, UserUpdateReq req) {
        User user = findActiveUser(userNo, HttpStatus.FORBIDDEN);

        if (req.getName() != null && !req.getName().isBlank())  user.setName(req.getName());
        if (req.getTel() != null && !req.getTel().isBlank())    user.setTel(req.getTel());
        if (req.getEmail() != null && !req.getEmail().isBlank()) {
            String email = normalizeEmail(req.getEmail());
            validateDuplicateEmail(email, userNo);
            user.setEmail(email);
        }
        if (req.getGender() != null)                             user.setGender(req.getGender());
        if (req.getBirth() != null && !req.getBirth().isBlank()) user.setBirth(req.getBirth());
        if (req.getUserPw() != null && !req.getUserPw().isBlank())
            user.setUserPw(passwordEncoder.encode(req.getUserPw()));
    }

    @Transactional(readOnly = true)
    public UserFindIdRes findId(UserFindIdReq req) {
        validateRequired(req.getName(), "이름은 필수입니다.");
        validateRequired(req.getTel(), "연락처는 필수입니다.");

        String email = normalizeEmail(req.getEmail());
        User user = email == null
                ? userRepository.findFirstByNameAndTelAndRoleIn(req.getName(), req.getTel(), FIND_AUTH_ROLES)
                .orElseThrow(() -> new BusinessException("일치하는 회원 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND))
                : userRepository.findFirstByNameAndTelAndEmailAndRoleIn(req.getName(), req.getTel(), email, FIND_AUTH_ROLES)
                .orElseThrow(() -> new BusinessException("일치하는 회원 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        return new UserFindIdRes(user.getUserId());
    }

    @Transactional
    public void sendResetPasswordCode(UserResetPwCodeReq req) {
        validateRequired(req.getUserId(), "아이디는 필수입니다.");
        validateRequired(req.getName(), "이름은 필수입니다.");
        validateRequired(req.getTel(), "연락처는 필수입니다.");
        validateRequired(req.getEmail(), "이메일은 필수입니다.");

        User user = findResetPasswordTarget(req.getUserId(), req.getName(), req.getTel(), req.getEmail());

        String code = generateResetPasswordCode();
        stringRedisTemplate.opsForValue()
                .set(resetPasswordCodeKey(user.getUserNo()), code, RESET_PW_CODE_TTL);

        emailService.sendPasswordResetCode(user.getEmail(), user.getName(), code);
    }

    @Transactional
    public void resetPassword(UserResetPwReq req) {
        validateRequired(req.getUserId(), "아이디는 필수입니다.");
        validateRequired(req.getName(), "이름은 필수입니다.");
        validateRequired(req.getTel(), "연락처는 필수입니다.");
        validateRequired(req.getEmail(), "이메일은 필수입니다.");
        validateRequired(req.getVerificationCode(), "인증코드는 필수입니다.");
        validateRequired(req.getNewPassword(), "새 비밀번호는 필수입니다.");

        User user = findResetPasswordTarget(req.getUserId(), req.getName(), req.getTel(), req.getEmail());

        String key = resetPasswordCodeKey(user.getUserNo());
        String savedCode = stringRedisTemplate.opsForValue().get(key);

        if (savedCode == null || !savedCode.equals(req.getVerificationCode())) {
            throw new BusinessException("인증코드가 일치하지 않거나 만료되었습니다.", HttpStatus.BAD_REQUEST);
        }

        user.setUserPw(passwordEncoder.encode(req.getNewPassword()));
        stringRedisTemplate.delete(key);
    }

    private User findResetPasswordTarget(String userId, String name, String tel, String email) {
        String normalizedEmail = normalizeEmail(email);

        return userRepository.findByUserIdAndNameAndTelAndEmailAndRoleIn(
                        userId, name, tel, normalizedEmail, FIND_AUTH_ROLES)
                .orElseThrow(() -> new BusinessException("일치하는 회원 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
    }

    private String generateResetPasswordCode() {
        return String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
    }

    private String resetPasswordCodeKey(Long userNo) {
        return RESET_PW_CODE_PREFIX + userNo;
    }
    private void validateRequiredEmail(String role, String email) {
        if (FIND_AUTH_ROLES.contains(role) && email == null) {
            throw new BusinessException("이메일은 필수입니다.", HttpStatus.BAD_REQUEST);
        }
    }

    private void validateDuplicateEmail(String email, Long currentUserNo) {
        if (email == null) {
            return;
        }
        userRepository.findByEmail(email)
                .filter(user -> currentUserNo == null || user.getUserNo() != currentUserNo)
                .ifPresent(user -> {
                    throw new BusinessException("이미 사용 중인 이메일입니다.", HttpStatus.CONFLICT);
                });
    }

    private void validateRequired(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(message, HttpStatus.BAD_REQUEST);
        }
    }

    private void validateOwnerSignupRequired(UserSignupReq req) {
        validateRequired(req.getAddress(), "주소는 필수입니다.");
        validateRequired(req.getBusinessNumber(), "사업자 등록 번호는 필수입니다.");
        validateRequired(req.getBusinessLicenseUrl(), "영업 신고증은 필수입니다.");
        validateRequired(req.getMailOrderLicenseUrl(), "통신판매업 신고증은 필수입니다.");
        validateRequired(req.getBankName(), "정산 은행은 필수입니다.");
        validateRequired(req.getAccountNumber(), "정산 계좌번호는 필수입니다.");
        validateRequired(req.getAccountHolder(), "예금주는 필수입니다.");
    }

    private String buildStoreAddress(UserSignupReq req) {
        if (req.getAddressDetail() == null || req.getAddressDetail().isBlank()) {
            return req.getAddress();
        }
        return req.getAddress() + " " + req.getAddressDetail();
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        return email.trim();
    }

    @Transactional(readOnly = true)
    public UserSigninRes getMe(Long userNo) {
        User user = findActiveUser(userNo, HttpStatus.FORBIDDEN);
        return new UserSigninRes(user.getUserNo(), user.getName(), user.getRole(), 0L, null);
    }

    // 탈퇴
    @Transactional
    public void withdraw(Long userNo, UserWithdrawReq req, HttpServletResponse res) {
        User user = userRepository.findById(userNo)
                .orElseThrow(() -> new BusinessException("회원 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        if (STATUS_WITHDRAWN.equals(user.getStatus())) {
            throw new BusinessException("이미 탈퇴한 계정입니다.", HttpStatus.BAD_REQUEST);
        }

        if (!STATUS_ACTIVE.equals(user.getStatus())) {
            throw inactiveAccountException(user.getStatus(), HttpStatus.FORBIDDEN);
        }

        if ("ADMIN".equals(user.getRole())) {
            throw new BusinessException("관리자 계정은 탈퇴할 수 없습니다", HttpStatus.FORBIDDEN);
        }

        if (req == null || req.getUserPw() == null || req.getUserPw().isBlank()) {
            throw new BusinessException("비밀번호를 입력해 주세요.", HttpStatus.BAD_REQUEST);
        }

        if (!passwordEncoder.matches(req.getUserPw(), user.getUserPw())) {
            throw new BusinessException("비밀번호가 일치하지 않습니다.", HttpStatus.UNAUTHORIZED);
        }

        if (hasActiveWork(user)) {
            if ("OWNER".equals(user.getRole())) {
                throw new BusinessException("운영 중인 가게, 진행 중인 주문 또는 미정산 내역이 있어 탈퇴할 수 없습니다. 정리 후 다시 시도해 주세요.", HttpStatus.BAD_REQUEST);
            }
            throw new BusinessException("진행 중인 주문 또는 업무가 있어 탈퇴할 수 없습니다. 완료 또는 취소·환불 처리 후 다시 시도해 주세요.", HttpStatus.BAD_REQUEST);
        }

        user.setStatus(STATUS_WITHDRAWN);
        user.setWithdrawnAt(LocalDateTime.now());
        user.setProcessMemo("회원 본인 탈퇴");

        mainUserCleanupClient.cleanupWithdrawnUser(userNo);

        signout(userNo, res);
    }

    private boolean hasActiveWork(User user) {
        try {
            Boolean result;

            if ("CUSTOMER".equals(user.getRole())) {
                result = mainUserCleanupClient.hasActiveOrders(user.getUserNo()).getResultData();
            } else if ("OWNER".equals(user.getRole())) {
                return hasOwnerWithdrawBlocker(user.getUserNo());
            } else if ("RIDER".equals(user.getRole())) {
                result = riderUserClient.hasActiveWork(user.getUserNo()).getResultData();
            } else {
                return false;
            }

            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.warn("회원탈퇴 진행 중 업무 확인 실패: userNo={}, role={}, cause={}",
                    user.getUserNo(), user.getRole(), e.getMessage());
            throw new BusinessException("진행 중인 업무 확인에 실패했습니다. 잠시 후 다시 시도해 주세요.", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    private boolean hasOwnerWithdrawBlocker(Long ownerNo) {
        OwnerWithdrawCheckRes mainCheck = mainUserCleanupClient.checkOwnerWithdraw(ownerNo).getResultData();
        if (mainCheck == null) {
            throw new BusinessException("사장 탈퇴 가능 여부 확인에 실패했습니다. 잠시 후 다시 시도해 주세요.", HttpStatus.SERVICE_UNAVAILABLE);
        }

        boolean hasUnpaidSettlement = false;
        List<Long> storeIds = mainCheck.getStoreIds() == null ? List.of() : mainCheck.getStoreIds();
        if (!storeIds.isEmpty()) {
            hasUnpaidSettlement = Boolean.TRUE.equals(
                    adminSettlementClient.hasUnpaidStoreSettlement(storeIds).getResultData()
            );
        }

        return mainCheck.isHasActiveStore() || mainCheck.isHasActiveOrders() || hasUnpaidSettlement;
    }

    // 계정 복구 (탈퇴 후 14일 이내)
    @Transactional
    public UserSigninRes recover(UserRecoverReq req, HttpServletResponse res) {
        User user = userRepository.findByUserId(req.getUserId())
                .orElseThrow(() -> new BusinessException("아이디 또는 비밀번호가 틀렸습니다.", HttpStatus.UNAUTHORIZED));

        if (!passwordEncoder.matches(req.getUserPw(), user.getUserPw())) {
            throw new BusinessException("아이디 또는 비밀번호가 틀렸습니다.", HttpStatus.UNAUTHORIZED);
        }

        if (!STATUS_WITHDRAWN.equals(user.getStatus())) {
            throw new BusinessException("탈퇴한 계정이 아닙니다.", HttpStatus.BAD_REQUEST);
        }

        if (user.getWithdrawnAt() == null || user.getWithdrawnAt().isBefore(LocalDateTime.now().minusDays(14))) {
            throw new BusinessException("복구 기간(14일)이 지났습니다. 새로 가입해 주세요.", HttpStatus.GONE);
        }

        user.setStatus(STATUS_ACTIVE);
        user.setWithdrawnAt(null);
        user.setProcessMemo("계정 복구");

        JwtUser jwtUser = new JwtUser(user.getUserNo(), user.getRole(), user.getStatus(), user.getName());
        issueAndStoreTokens(res, jwtUser);

        return new UserSigninRes(user.getUserNo(), user.getName(), user.getRole(),
                System.currentTimeMillis() + constJwt.getAccessTokenValidityMilliseconds(), null);
    }

    private User findActiveUser(Long userNo, HttpStatus inactiveStatus) {
        User user = userRepository.findById(userNo)
                .orElseThrow(() -> new BusinessException("회원 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        if (!STATUS_ACTIVE.equals(user.getStatus())) {
            throw inactiveAccountException(user.getStatus(), inactiveStatus);
        }

        return user;
    }

    private BusinessException inactiveAccountException(String status, HttpStatus httpStatus) {
        if (STATUS_PENDING.equals(status)) {
            return new BusinessException("관리자 승인 후 이용 가능합니다.", httpStatus);
        }
        if (STATUS_REJECTED.equals(status)) {
            return new BusinessException("가입 신청이 반려되었습니다.", httpStatus);
        }
        if (STATUS_SUSPENDED.equals(status)) {
            return new BusinessException("정지된 계정입니다.", httpStatus);
        }
        if (STATUS_WITHDRAWN.equals(status)) {
            return new BusinessException("탈퇴한 계정입니다.", httpStatus);
        }
        return new BusinessException("이용할 수 없는 계정 상태입니다.", httpStatus);
    }
}
