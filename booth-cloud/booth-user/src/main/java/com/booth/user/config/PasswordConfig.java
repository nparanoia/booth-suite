package com.booth.user.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PasswordConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CredentialChecker credentialChecker(PasswordEncoder passwordEncoder) {
        return new CredentialChecker(passwordEncoder);
    }

    /**
     * 账号不存在时也做一次 BCrypt 比对，让“账号不存在”和“密码错误”的耗时一致，
     * 防止通过响应时间探测哪些用户名已注册。
     */
    public static class CredentialChecker {

        private final PasswordEncoder passwordEncoder;
        private final String dummyHash;

        CredentialChecker(PasswordEncoder passwordEncoder) {
            this.passwordEncoder = passwordEncoder;
            this.dummyHash = passwordEncoder.encode("dummy-password-for-timing");
        }

        public boolean matches(String rawPassword, String storedHashOrNull) {
            if (storedHashOrNull == null) {
                passwordEncoder.matches(rawPassword, dummyHash);
                return false;
            }
            return passwordEncoder.matches(rawPassword, storedHashOrNull);
        }
    }
}
