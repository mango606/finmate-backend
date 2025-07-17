SET NAMES utf8mb4;
SET character_set_client = utf8mb4;

-- 데이터베이스 사용
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

-- 기존 테이블 삭제 (있을 경우)
DROP TABLE IF EXISTS tbl_member_auth;
DROP TABLE IF EXISTS tbl_member;
DROP TABLE IF EXISTS tbl_financial_goal;

-- 회원 정보 테이블
CREATE TABLE tbl_member
(
    user_id      VARCHAR(50) PRIMARY KEY COMMENT '사용자 ID',
    user_password VARCHAR(128) NOT NULL COMMENT '암호화된 비밀번호',
    user_name    VARCHAR(50)  NOT NULL COMMENT '사용자 이름',
    user_email   VARCHAR(100) NOT NULL UNIQUE COMMENT '이메일',
    user_phone   VARCHAR(20) COMMENT '전화번호',
    birth_date   DATE COMMENT '생년월일',
    gender       ENUM('M', 'F') COMMENT '성별',
    reg_date     DATETIME DEFAULT NOW() COMMENT '등록일',
    update_date  DATETIME DEFAULT NOW() ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일',
    is_active    BOOLEAN DEFAULT TRUE COMMENT '계정 활성화 상태'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='회원 정보';

-- 사용자 권한 테이블
CREATE TABLE tbl_member_auth
(
    user_id VARCHAR(50) NOT NULL COMMENT '사용자 ID',
    auth    VARCHAR(50) NOT NULL COMMENT '권한 (ROLE_USER, ROLE_ADMIN)',
    PRIMARY KEY (user_id, auth),
    CONSTRAINT fk_authorities_users FOREIGN KEY (user_id) REFERENCES tbl_member (user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사용자 권한';

-- 금융 목표 테이블
CREATE TABLE tbl_financial_goal
(
    goal_id        BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '목표 ID',
    user_id        VARCHAR(50) NOT NULL COMMENT '사용자 ID',
    goal_name      VARCHAR(100) NOT NULL COMMENT '목표명',
    target_amount  DECIMAL(15,2) NOT NULL COMMENT '목표 금액',
    current_amount DECIMAL(15,2) DEFAULT 0 COMMENT '현재 금액',
    target_date    DATE COMMENT '목표 달성 일자',
    goal_type      VARCHAR(20) NOT NULL COMMENT '목표 유형 (SAVING, INVESTMENT, etc.)',
    goal_status    ENUM('ACTIVE', 'COMPLETED', 'CANCELLED') DEFAULT 'ACTIVE' COMMENT '목표 상태',
    reg_date       DATETIME DEFAULT NOW() COMMENT '등록일',
    update_date    DATETIME DEFAULT NOW() ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일',
    CONSTRAINT fk_goal_user FOREIGN KEY (user_id) REFERENCES tbl_member (user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='금융 목표';

-- 인덱스 생성
CREATE INDEX idx_member_email ON tbl_member(user_email);
CREATE INDEX idx_member_phone ON tbl_member(user_phone);
CREATE INDEX idx_member_reg_date ON tbl_member(reg_date);
CREATE INDEX idx_member_active ON tbl_member(is_active);
CREATE INDEX idx_goal_user_id ON tbl_financial_goal(user_id);
CREATE INDEX idx_goal_status ON tbl_financial_goal(goal_status);
CREATE INDEX idx_goal_type ON tbl_financial_goal(goal_type);

-- 초기 사용자 데이터 삽입
-- 비밀번호는 모두 'finmate123!' 입니다.
INSERT INTO tbl_member(user_id, user_password, user_name, user_email, user_phone, birth_date, gender)
VALUES
    ('admin', '$2a$10$EsIMfxbJ6NuvwX7MDj4WqOYFzLU9U/lddCyn0nic5dFo3VfJYrXYC', '시스템관리자', 'admin@finmate.com', '010-1234-5678', '1990-01-01', 'M'),
    ('user01', '$2a$10$EsIMfxbJ6NuvwX7MDj4WqOYFzLU9U/lddCyn0nic5dFo3VfJYrXYC', '김철수', 'kimcs@finmate.com', '010-1111-2222', '1995-05-15', 'M'),
    ('user02', '$2a$10$EsIMfxbJ6NuvwX7MDj4WqOYFzLU9U/lddCyn0nic5dFo3VfJYrXYC', '이영희', 'leeyh@finmate.com', '010-3333-4444', '1992-08-20', 'F'),
    ('user03', '$2a$10$EsIMfxbJ6NuvwX7MDj4WqOYFzLU9U/lddCyn0nic5dFo3VfJYrXYC', '박민수', 'parkms@finmate.com', '010-5555-6666', '1988-12-10', 'M'),
    ('testuser', '$2a$10$EsIMfxbJ6NuvwX7MDj4WqOYFzLU9U/lddCyn0nic5dFo3VfJYrXYC', '테스트사용자', 'test@finmate.com', '010-9999-8888', '2000-01-01', 'F');

-- 권한 정보 삽입
INSERT INTO tbl_member_auth(user_id, auth)
VALUES
    ('admin', 'ROLE_ADMIN'),
    ('admin', 'ROLE_USER'),
    ('user01', 'ROLE_USER'),
    ('user02', 'ROLE_USER'),
    ('user03', 'ROLE_USER'),
    ('testuser', 'ROLE_USER');

-- 샘플 금융 목표 데이터 (선택사항)
INSERT INTO tbl_financial_goal(user_id, goal_name, target_amount, current_amount, target_date, goal_type)
VALUES
    ('user01', '비상금 마련', 5000000.00, 1500000.00, '2024-12-31', 'SAVING'),
    ('user01', '주식 투자', 10000000.00, 3000000.00, '2025-06-30', 'INVESTMENT'),
    ('user02', '내 집 마련 자금', 300000000.00, 50000000.00, '2027-12-31', 'SAVING'),
    ('user03', '펀드 투자', 20000000.00, 8000000.00, '2025-12-31', 'INVESTMENT');

-- 연결 테스트 테이블
CREATE TABLE IF NOT EXISTS connection_test (
                                               id INT PRIMARY KEY,
                                               message VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

INSERT INTO connection_test (id, message) VALUES (1, 'finmate user connection OK')
    ON DUPLICATE KEY UPDATE message = 'finmate user connection OK', created_at = CURRENT_TIMESTAMP;

-- 권한 확인
SELECT 'finmate 사용자 권한 확인' as info;
SHOW GRANTS FOR 'finmate'@'%';
SHOW GRANTS FOR 'finmate'@'localhost';

-- 데이터 확인
SELECT '=== 회원 정보 ===' as info;
SELECT user_id, user_name, user_email, user_phone, is_active FROM tbl_member;

SELECT '=== 권한 정보 ===' as info;
SELECT user_id, auth FROM tbl_member_auth ORDER BY user_id, auth;

-- 완료 메시지
SELECT 'FinMate 데이터베이스 초기화가 완료되었습니다!' as message;