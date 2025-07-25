SET NAMES utf8mb4;
SET character_set_client = utf8mb4;

-- 기존 데이터베이스 삭제 및 재생성
DROP DATABASE IF EXISTS finmate_db;
CREATE DATABASE finmate_db
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE finmate_db;

-- 기존 사용자 완전 삭제
DROP USER IF EXISTS 'finmate'@'localhost';
DROP USER IF EXISTS 'finmate'@'%';

-- 새 사용자 생성 (모든 호스트에서 접근 가능)
CREATE USER 'finmate'@'%' IDENTIFIED BY '1234';
CREATE USER 'finmate'@'localhost' IDENTIFIED BY '1234';

-- 비밀번호 플러그인 설정
ALTER USER 'finmate'@'%' IDENTIFIED WITH mysql_native_password BY '1234';
ALTER USER 'finmate'@'localhost' IDENTIFIED WITH mysql_native_password BY '1234';

-- 모든 권한 부여
GRANT ALL PRIVILEGES ON *.* TO 'finmate'@'%' WITH GRANT OPTION;
GRANT ALL PRIVILEGES ON *.* TO 'finmate'@'localhost' WITH GRANT OPTION;

-- 권한 새로고침
FLUSH PRIVILEGES;

-- 1. 회원 정보 테이블
CREATE TABLE tbl_member (
                            user_id VARCHAR(50) PRIMARY KEY COMMENT '사용자 ID',
                            user_password VARCHAR(128) NOT NULL COMMENT '암호화된 비밀번호',
                            user_name VARCHAR(50) NOT NULL COMMENT '사용자 이름',
                            user_email VARCHAR(100) NOT NULL UNIQUE COMMENT '이메일',
                            user_phone VARCHAR(20) COMMENT '전화번호',
                            birth_date DATE COMMENT '생년월일',
                            gender ENUM('M', 'F') COMMENT '성별',
                            reg_date DATETIME DEFAULT NOW() COMMENT '등록일',
                            update_date DATETIME DEFAULT NOW() ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일',
                            is_active BOOLEAN DEFAULT TRUE COMMENT '계정 활성화 상태'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='회원 정보';

-- 2. 사용자 권한 테이블
CREATE TABLE tbl_member_auth (
                                 user_id VARCHAR(50) NOT NULL COMMENT '사용자 ID',
                                 auth VARCHAR(50) NOT NULL COMMENT '권한 (ROLE_USER, ROLE_ADMIN)',
                                 PRIMARY KEY (user_id, auth),
                                 CONSTRAINT fk_authorities_users FOREIGN KEY (user_id) REFERENCES tbl_member (user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사용자 권한';

-- 3. 계정 보안 테이블 (AuthService에서 필요)
CREATE TABLE tbl_account_security (
                                      user_id VARCHAR(50) PRIMARY KEY COMMENT '사용자 ID',
                                      email_verified BOOLEAN DEFAULT FALSE COMMENT '이메일 인증 여부',
                                      phone_verified BOOLEAN DEFAULT FALSE COMMENT '전화번호 인증 여부',
                                      two_factor_enabled BOOLEAN DEFAULT FALSE COMMENT '2단계 인증 활성화 여부',
                                      account_locked BOOLEAN DEFAULT FALSE COMMENT '계정 잠금 여부',
                                      login_fail_count INT DEFAULT 0 COMMENT '로그인 실패 횟수',
                                      last_login_time DATETIME COMMENT '마지막 로그인 시간',
                                      lock_time DATETIME COMMENT '잠금 시간',
                                      lock_reason VARCHAR(100) COMMENT '잠금 사유',
                                      update_date DATETIME DEFAULT NOW() ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일',
                                      CONSTRAINT fk_account_security_user FOREIGN KEY (user_id) REFERENCES tbl_member(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='계정 보안';

-- 4. Refresh Token 테이블
CREATE TABLE tbl_refresh_token (
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
                                   CONSTRAINT fk_refresh_token_user_id FOREIGN KEY (user_id) REFERENCES tbl_member(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Refresh Token 관리 테이블';

-- 5. 인증 토큰 테이블
CREATE TABLE tbl_auth_token (
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
                                CONSTRAINT fk_auth_token_user_id FOREIGN KEY (user_id) REFERENCES tbl_member(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='인증 토큰 관리 테이블';

-- 6. 로그인 이력 테이블
CREATE TABLE tbl_login_history (
                                   history_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '이력 ID',
                                   user_id VARCHAR(50) NOT NULL COMMENT '사용자 ID',
                                   ip_address VARCHAR(45) COMMENT 'IP 주소',
                                   user_agent TEXT COMMENT '사용자 에이전트',
                                   login_success BOOLEAN NOT NULL COMMENT '로그인 성공 여부',
                                   failure_reason VARCHAR(255) COMMENT '실패 사유',
                                   login_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '로그인 시간',
                                   location VARCHAR(100) COMMENT '접속 위치',
                                   device_type VARCHAR(50) COMMENT '기기 유형',
                                   INDEX idx_login_history_user_id (user_id),
                                   INDEX idx_login_history_login_time (login_time),
                                   CONSTRAINT fk_login_history_user_id FOREIGN KEY (user_id) REFERENCES tbl_member(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='로그인 이력 관리 테이블';

-- 7. 금융 프로필 테이블
CREATE TABLE tbl_financial_profile (
                                       profile_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '프로필 ID',
                                       user_id VARCHAR(50) NOT NULL COMMENT '사용자 ID',
                                       income_range VARCHAR(20) NOT NULL COMMENT '소득 구간 (LOW, MEDIUM, HIGH)',
                                       job_type VARCHAR(30) NOT NULL COMMENT '직업 유형',
                                       monthly_income DECIMAL(15,2) NOT NULL COMMENT '월 소득',
                                       monthly_expense DECIMAL(15,2) NOT NULL COMMENT '월 지출',
                                       total_assets DECIMAL(15,2) DEFAULT 0 COMMENT '총 자산',
                                       total_debt DECIMAL(15,2) DEFAULT 0 COMMENT '총 부채',
                                       risk_profile VARCHAR(20) NOT NULL COMMENT '투자 성향',
                                       investment_goal VARCHAR(30) NOT NULL COMMENT '투자 목표',
                                       investment_period INT NOT NULL COMMENT '투자 기간 (개월)',
                                       emergency_fund DECIMAL(15,2) DEFAULT 0 COMMENT '비상금',
                                       credit_score VARCHAR(20) COMMENT '신용등급',
                                       reg_date DATETIME DEFAULT NOW() COMMENT '등록일',
                                       update_date DATETIME DEFAULT NOW() ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일',
                                       CONSTRAINT fk_financial_profile_user FOREIGN KEY (user_id) REFERENCES tbl_member(user_id) ON DELETE CASCADE,
                                       CONSTRAINT uk_financial_profile_user UNIQUE (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='금융 프로필';

-- 8. 금융 목표 테이블
CREATE TABLE tbl_financial_goal (
                                    goal_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '목표 ID',
                                    user_id VARCHAR(50) NOT NULL COMMENT '사용자 ID',
                                    goal_name VARCHAR(100) NOT NULL COMMENT '목표명',
                                    goal_type VARCHAR(30) NOT NULL COMMENT '목표 유형',
                                    target_amount DECIMAL(15,2) NOT NULL COMMENT '목표 금액',
                                    current_amount DECIMAL(15,2) DEFAULT 0 COMMENT '현재 금액',
                                    monthly_contribution DECIMAL(15,2) NOT NULL COMMENT '월 적립액',
                                    target_date DATE NOT NULL COMMENT '목표 달성 일자',
                                    start_date DATE NOT NULL COMMENT '시작일',
                                    priority VARCHAR(10) NOT NULL COMMENT '우선순위',
                                    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '상태',
                                    description TEXT COMMENT '설명',
                                    progress_percentage DECIMAL(5,2) DEFAULT 0 COMMENT '달성률',
                                    expected_months INT COMMENT '예상 달성 기간 (개월)',
                                    reg_date DATETIME DEFAULT NOW() COMMENT '등록일',
                                    update_date DATETIME DEFAULT NOW() ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일',
                                    CONSTRAINT fk_financial_goal_user FOREIGN KEY (user_id) REFERENCES tbl_member(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='금융 목표';

-- 인덱스 생성
CREATE INDEX idx_member_email ON tbl_member(user_email);
CREATE INDEX idx_member_phone ON tbl_member(user_phone);
CREATE INDEX idx_member_reg_date ON tbl_member(reg_date);
CREATE INDEX idx_member_active ON tbl_member(is_active);
CREATE INDEX idx_goal_user_id ON tbl_financial_goal(user_id);
CREATE INDEX idx_goal_status ON tbl_financial_goal(status);

-- 초기 사용자 데이터 삽입
-- 비밀번호는 모두 'finmate123!' 입니다.
INSERT INTO tbl_member(user_id, user_password, user_name, user_email, user_phone, birth_date, gender)
VALUES
    ('admin', '$2a$10$EsIMfxbJ6NuvwX7MDj4WqOYFzLU9U/lddCyn0nic5dFo3VfJYrXYC', '시스템관리자', 'admin@finmate.com', '010-1234-5678', '1990-01-01', 'M'),
    ('user01', '$2a$10$EsIMfxbJ6NuvwX7MDj4WqOYFzLU9U/lddCyn0nic5dFo3VfJYrXYC', '김철수', 'kimcs@finmate.com', '010-1111-2222', '1995-05-15', 'M'),
    ('user02', '$2a$10$EsIMfxbJ6NuvwX7MDj4WqOYFzLU9U/lddCyn0nic5dFo3VfJYrXYC', '이영희', 'leeyh@finmate.com', '010-3333-4444', '1992-08-20', 'F'),
    ('testuser', '$2a$10$EsIMfxbJ6NuvwX7MDj4WqOYFzLU9U/lddCyn0nic5dFo3VfJYrXYC', '테스트사용자', 'test@finmate.com', '010-9999-8888', '2000-01-01', 'F');

-- 권한 정보 삽입
INSERT INTO tbl_member_auth(user_id, auth)
VALUES
    ('admin', 'ROLE_ADMIN'),
    ('admin', 'ROLE_USER'),
    ('user01', 'ROLE_USER'),
    ('user02', 'ROLE_USER'),
    ('testuser', 'ROLE_USER');

-- 모든 사용자에 대한 계정 보안 정보 초기화
INSERT INTO tbl_account_security (user_id, email_verified, phone_verified, two_factor_enabled, account_locked, login_fail_count)
SELECT user_id, FALSE, FALSE, FALSE, FALSE, 0
FROM tbl_member;

-- 샘플 금융 목표 데이터
INSERT INTO tbl_financial_goal(user_id, goal_name, target_amount, current_amount, monthly_contribution, target_date, start_date, priority, goal_type)
VALUES
    ('user01', '비상금 마련', 5000000.00, 1500000.00, 500000.00, '2024-12-31', '2024-01-01', 'HIGH', 'SAVING'),
    ('user01', '주식 투자', 10000000.00, 3000000.00, 1000000.00, '2025-06-30', '2024-01-01', 'MEDIUM', 'INVESTMENT'),
    ('user02', '내 집 마련 자금', 300000000.00, 50000000.00, 2000000.00, '2027-12-31', '2024-01-01', 'HIGH', 'HOUSE_PURCHASE');

-- 연결 테스트 테이블
CREATE TABLE connection_test (
                                 id INT PRIMARY KEY,
                                 message VARCHAR(100),
                                 created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO connection_test (id, message) VALUES (1, 'finmate user connection OK')
    ON DUPLICATE KEY UPDATE message = 'finmate user connection OK', created_at = CURRENT_TIMESTAMP;

-- 데이터 확인
SELECT '=== 회원 정보 ===' as info;
SELECT user_id, user_name, user_email, user_phone, is_active FROM tbl_member;

SELECT '=== 권한 정보 ===' as info;
SELECT user_id, auth FROM tbl_member_auth ORDER BY user_id, auth;

SELECT '=== 계정 보안 정보 ===' as info;
SELECT user_id, email_verified, account_locked, login_fail_count FROM tbl_account_security;

-- 완료 메시지
SELECT 'FinMate 데이터베이스 초기화가 완료되었습니다!' as message;