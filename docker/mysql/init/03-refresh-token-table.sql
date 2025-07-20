USE finmate_db;

-- Refresh Token 테이블 생성
CREATE TABLE IF NOT EXISTS tbl_refresh_token (
                                                 token_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '토큰 ID',
                                                 user_id VARCHAR(50) NOT NULL COMMENT '사용자 ID',
    refresh_token VARCHAR(255) NOT NULL UNIQUE COMMENT 'Refresh Token 값',
    expiry_time DATETIME NOT NULL COMMENT '만료 시간',
    is_valid BOOLEAN DEFAULT TRUE COMMENT '유효 여부',
    ip_address VARCHAR(45) COMMENT '발급 IP 주소',
    user_agent TEXT COMMENT '사용자 에이전트',
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '생성일',
    last_used_date DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '마지막 사용일',

    INDEX idx_refresh_token_user_id (user_id),
    INDEX idx_refresh_token_token (refresh_token),
    INDEX idx_refresh_token_expiry_time (expiry_time),
    INDEX idx_refresh_token_is_valid (is_valid),
    INDEX idx_refresh_token_created_date (created_date),

    CONSTRAINT fk_refresh_token_user_id
    FOREIGN KEY (user_id) REFERENCES tbl_member(user_id) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
    COMMENT='Refresh Token 관리 테이블';

-- 인증 토큰 테이블 생성 (비밀번호 재설정, 이메일 인증 등)
CREATE TABLE IF NOT EXISTS tbl_auth_token (
                                              token_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '토큰 ID',
                                              user_id VARCHAR(50) NOT NULL COMMENT '사용자 ID',
    token VARCHAR(255) NOT NULL COMMENT '토큰 값',
    token_type VARCHAR(50) NOT NULL COMMENT '토큰 유형 (PASSWORD_RESET, EMAIL_VERIFICATION)',
    expiry_time DATETIME NOT NULL COMMENT '만료 시간',
    is_used BOOLEAN DEFAULT FALSE COMMENT '사용 여부',
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '생성일',
    used_date DATETIME NULL COMMENT '사용일',

    INDEX idx_auth_token_user_id (user_id),
    INDEX idx_auth_token_token (token),
    INDEX idx_auth_token_type (token_type),
    INDEX idx_auth_token_expiry_time (expiry_time),
    INDEX idx_auth_token_is_used (is_used),

    CONSTRAINT fk_auth_token_user_id
    FOREIGN KEY (user_id) REFERENCES tbl_member(user_id) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
    COMMENT='인증 토큰 관리 테이블 (비밀번호 재설정, 이메일 인증 등)';

-- 로그인 이력 테이블 생성
CREATE TABLE IF NOT EXISTS tbl_login_history (
                                                 history_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '이력 ID',
                                                 user_id VARCHAR(50) NOT NULL COMMENT '사용자 ID',
    ip_address VARCHAR(45) COMMENT 'IP 주소',
    user_agent TEXT COMMENT '사용자 에이전트',
    login_success BOOLEAN NOT NULL COMMENT '로그인 성공 여부',
    failure_reason VARCHAR(255) COMMENT '실패 사유',
    login_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '로그인 시간',
    location VARCHAR(100) COMMENT '접속 위치 (선택적)',
    device_type VARCHAR(50) COMMENT '기기 유형 (선택적)',

    INDEX idx_login_history_user_id (user_id),
    INDEX idx_login_history_login_time (login_time),
    INDEX idx_login_history_login_success (login_success),
    INDEX idx_login_history_ip_address (ip_address),
    INDEX idx_login_history_user_time (user_id, login_time),

    CONSTRAINT fk_login_history_user_id
    FOREIGN KEY (user_id) REFERENCES tbl_member(user_id) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
    COMMENT='로그인 이력 관리 테이블';

-- 계정 보안 정보 테이블 생성
CREATE TABLE IF NOT EXISTS tbl_account_security (
                                                    user_id VARCHAR(50) PRIMARY KEY COMMENT '사용자 ID',
    email_verified BOOLEAN DEFAULT FALSE COMMENT '이메일 인증 여부',
    phone_verified BOOLEAN DEFAULT FALSE COMMENT '전화번호 인증 여부',
    two_factor_enabled BOOLEAN DEFAULT FALSE COMMENT '2단계 인증 활성화 여부',
    account_locked BOOLEAN DEFAULT FALSE COMMENT '계정 잠금 여부',
    login_fail_count INT DEFAULT 0 COMMENT '로그인 실패 횟수',
    last_login_time DATETIME NULL COMMENT '마지막 로그인 시간',
    lock_time DATETIME NULL COMMENT '잠금 시간',
    lock_reason VARCHAR(255) COMMENT '잠금 사유',
    update_date DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일',

    CONSTRAINT fk_account_security_user_id
    FOREIGN KEY (user_id) REFERENCES tbl_member(user_id) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
    COMMENT='계정 보안 정보 테이블';

-- 기존 회원에 대한 계정 보안 정보 초기화
INSERT IGNORE INTO tbl_account_security (user_id, email_verified, phone_verified, two_factor_enabled, account_locked, login_fail_count)
SELECT user_id, FALSE, FALSE, FALSE, FALSE, 0
FROM tbl_member
WHERE user_id NOT IN (SELECT user_id FROM tbl_account_security);

-- 성능 최적화를 위한 추가 복합 인덱스
CREATE INDEX idx_refresh_token_user_valid ON tbl_refresh_token(user_id, is_valid, expiry_time);
CREATE INDEX idx_login_history_user_success_time ON tbl_login_history(user_id, login_success, login_time);

-- 테이블 생성 완료 로그
SELECT 'Refresh Token 및 인증 관련 테이블 생성 완료' AS message;

-- 데이터 정리용 이벤트 스케줄러 생성 (선택적)
-- 주의: 이벤트 스케줄러는 프로덕션 환경에서만 활성화하는 것을 권장

/*
-- 이벤트 스케줄러 활성화 확인
SHOW VARIABLES LIKE 'event_scheduler';

-- 만료된 토큰 정리 이벤트 (매일 새벽 2시)
DELIMITER //
CREATE EVENT IF NOT EXISTS evt_cleanup_expired_tokens
ON SCHEDULE EVERY 1 DAY
STARTS (TIMESTAMP(CURRENT_DATE) + INTERVAL 2 HOUR)
ON COMPLETION PRESERVE
ENABLE
COMMENT '만료된 토큰 및 오래된 로그 정리'
DO
BEGIN
    -- 만료된 Refresh Token 삭제
    DELETE FROM tbl_refresh_token
    WHERE expiry_time < NOW() OR is_valid = FALSE;

    -- 만료된 인증 토큰 삭제 (7일 이상 된 것)
    DELETE FROM tbl_auth_token
    WHERE expiry_time < DATE_SUB(NOW(), INTERVAL 7 DAY);

    -- 오래된 로그인 이력 삭제 (90일 이상 된 것)
    DELETE FROM tbl_login_history
    WHERE login_time < DATE_SUB(NOW(), INTERVAL 90 DAY);

    -- 정리 결과 로그 기록
    INSERT INTO tbl_login_history (user_id, login_success, failure_reason, login_time)
    VALUES ('SYSTEM', TRUE, 'TOKEN_CLEANUP_COMPLETED', NOW());
END//
DELIMITER ;

-- 이벤트 스케줄러 활성화 (필요한 경우)
-- SET GLOBAL event_scheduler = ON;
*/