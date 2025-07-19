-- 테스트용 관리자 계정
INSERT INTO tbl_member (user_id, user_password, user_name, user_email, user_phone, birth_date, gender, is_active)
VALUES ('admin', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', '관리자', 'admin@finmate.com', '010-0000-0000', '1980-01-01', 'M', true);

INSERT INTO tbl_member_auth (user_id, auth) VALUES ('admin', 'ROLE_ADMIN');
INSERT INTO tbl_member_auth (user_id, auth) VALUES ('admin', 'ROLE_USER');

-- 테스트용 일반 사용자
INSERT INTO tbl_member (user_id, user_password, user_name, user_email, user_phone, birth_date, gender, is_active)
VALUES ('testuser', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', '테스트사용자', 'test@finmate.com', '010-1234-5678', '1990-01-01', 'M', true);

INSERT INTO tbl_member_auth (user_id, auth) VALUES ('testuser', 'ROLE_USER');

-- 계정 보안 정보 초기화
INSERT INTO tbl_account_security (user_id, email_verified, phone_verified, two_factor_enabled, account_locked, login_fail_count)
VALUES ('admin', true, true, false, false, 0);

INSERT INTO tbl_account_security (user_id, email_verified, phone_verified, two_factor_enabled, account_locked, login_fail_count)
VALUES ('testuser', false, false, false, false, 0);