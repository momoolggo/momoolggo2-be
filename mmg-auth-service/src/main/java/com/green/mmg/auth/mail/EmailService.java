package com.green.mmg.auth.mail;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {
    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${spring.mail.username:no-reply@momoolggo.local}")
    private String from;

    public void sendPasswordResetCode(String to, String name, String code) {
        String subject = "[뭐물꼬] 비밀번호 재설정 인증코드";
        String text = """
                 안녕하세요, %s님 
                 뭐물꼬 계정의 비밀번호 재설정 인증코드는 다음과 같습니다.
                 
                 인증코드: %s
                 
                 인증코드는 5분 동안만 유효합니다. 시간 내에 인증해주세요.
                 
                 비밀번호 재설정을 요청하지 않았다면, 이 이메일을 무시하셔도 됩니다.
                 
                 감사합니다.
                 
                 뭐물꼬 momoolggo
                """.formatted(name, code);

        send(to, subject, text);
    }

    public void sendApprovalRejected(String to, String name, String role, String reason){
        String roleName = "OWNER".equals(role) ? "사장" : "RIDER".equals(role) ?  "라이더" : "회원";
        String rejectionReason = reason == null || reason.isBlank()
                ? "반려 사유가 입력되지 않았습니다."
                : reason;

        String subject = "[뭐물꼬] %s 회원가입 신청 반려 안내".formatted(roleName);
        String text = """
                안녕하세요, %s님
                뭐물꼬 %s 회원가입 신청이 반려되었습니다.
                
                반려 사유는 다음과 같습니다.
                
                반려 사유: %s
                
                내용을 확인하신 뒤, 다시 신청해 주세요.
                
                감사합니다.
                
                뭐물꼬 momoolggo
                """.formatted(name, roleName, rejectionReason);

        send(to, subject, text);
    }

    private void send(String to, String subject, String text){
        if (to == null || to.isBlank()) {
            log.warn("메일 수신자가 없어 발송을 건너뜁니다. subject={}", subject);
            return;
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.warn("메일 설정이 없어 발송을 건너뜁니다. to={}, subject={}", to, subject);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);

        mailSender.send(message);
    }

}
