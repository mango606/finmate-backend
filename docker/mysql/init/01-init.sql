CREATE DATABASE IF NOT EXISTS finmate_db
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE finmate_db;

-- 회원 테이블
CREATE TABLE IF NOT EXISTS tbl_member (
                                          user_id VARCHAR(20) PRIMARY KEY COMMENT '사용자 ID',
    user_password VARCHAR(255) NOT NULL COMMENT '암호화된 비밀번호',
    user_name VARCHAR(50) NOT NULL COMMENT '사용자 이름',
    user_email VARCHAR(100) NOT NULL UNIQUE COMMENT '이메일',
    user_phone VARCHAR(15) COMMENT '전화번호',
    birth_date DATE COMMENT '생년월일',
    gender CHAR(1) COMMENT '성별 (M/F)',
    reg_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '등록일',
    update_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일',
    is_active BOOLEAN DEFAULT TRUE COMMENT '계정 활성화 상태'
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='회원 정보';

-- 회원 권한 테이블
CREATE TABLE IF NOT EXISTS tbl_member_auth (
                                               user_id VARCHAR(20) NOT NULL COMMENT '사용자 ID',
    auth VARCHAR(50) NOT NULL COMMENT '권한',
    PRIMARY KEY (user_id, auth),
    FOREIGN KEY (user_id) REFERENCES tbl_member(user_id) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='회원 권한';

-- 인덱스 생성
CREATE INDEX idx_member_email ON tbl_member(user_email);
CREATE INDEX idx_member_reg_date ON tbl_member(reg_date);
CREATE INDEX idx_member_is_active ON tbl_member(is_active);

-- 기본 관리자 계정 생성 (비밀번호: Admin123!@#)
INSERT INTO tbl_member (user_id, user_password, user_name, user_email, user_phone, birth_date, gender)
VALUES ('admin', '$2a$10$CwTycUXWue0Thq9StjUM0uF4HTXs6BlzgQR8YxwAlm1Bs.7mKjdJ.', '관리자', 'admin@finmate.com', '010-0000-0000', '1990-01-01', 'M')
    ON DUPLICATE KEY UPDATE user_password = VALUES(user_password);

-- 관리자 권한 부여
INSERT INTO tbl_member_auth (user_id, auth) VALUES ('admin', 'ROLE_ADMIN') ON DUPLICATE KEY UPDATE auth = VALUES(auth);
INSERT INTO tbl_member_auth (user_id, auth) VALUES ('admin', 'ROLE_USER') ON DUPLICATE KEY UPDATE auth = VALUES(auth);

-- 테스트 사용자 계정 생성 (비밀번호: Test123!@#)
INSERT INTO tbl_member (user_id, user_password, user_name, user_email, user_phone, birth_date, gender)
VALUES ('testuser', '$2a$10$WZA6k.3PvP9KdD.VT9NuPefQWVUE.zH.VcLF5Kb5WB7tGl9YaZK2e', '테스트사용자', 'test@finmate.com', '010-1234-5678', '1995-05-15', 'F')
    ON DUPLICATE KEY UPDATE user_password = VALUES(user_password);

-- 테스트 사용자 권한 부여
INSERT INTO tbl_member_auth (user_id, auth) VALUES ('testuser', 'ROLE_USER') ON DUPLICATE KEY UPDATE auth = VALUES(auth);

-- 추가 더미 데이터
INSERT INTO tbl_member (user_id, user_password, user_name, user_email, user_phone, birth_date, gender) VALUES
                                                                                                           ('user001', '$2a$10$WZA6k.3PvP9KdD.VT9NuPefQWVUE.zH.VcLF5Kb5WB7tGl9YaZK2e', '김철수', 'kim@example.com', '010-1111-1111', '1992-03-10', 'M'),
                                                                                                           ('user002', '$2a$10$WZA6k.3PvP9KdD.VT9NuPefQWVUE.zH.VcLF5Kb5WB7tGl9YaZK2e', '이영희', 'lee@example.com', '010-2222-2222', '1988-07-22', 'F'),
                                                                                                           ('user003', '$2a$10$WZA6k.3PvP9KdD.VT9NuPefQWVUE.zH.VcLF5Kb5WB7tGl9YaZK2e', '박민수', 'park@example.com', '010-3333-3333', '1985-12-05', 'M')
    ON DUPLICATE KEY UPDATE user_name = VALUES(user_name);

-- 더미 사용자 권한
INSERT INTO tbl_member_auth (user_id, auth) VALUES
                                                ('user001', 'ROLE_USER'),
                                                ('user002', 'ROLE_USER'),
                                                ('user003', 'ROLE_USER')
    ON DUPLICATE KEY UPDATE auth = VALUES(auth);

-- 테이블 확인
SELECT 'tbl_member 테이블 데이터 확인' AS info;
SELECT user_id, user_name, user_email, is_active, reg_date FROM tbl_member;

SELECT 'tbl_member_auth 테이블 데이터 확인' AS info;
SELECT user_id, auth FROM tbl_member_auth ORDER BY user_id, auth;