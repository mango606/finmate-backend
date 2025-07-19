-- 회원 테이블
CREATE TABLE IF NOT EXISTS tbl_member (
                                          user_id VARCHAR(20) PRIMARY KEY,
    user_password VARCHAR(255) NOT NULL,
    user_name VARCHAR(10) NOT NULL,
    user_email VARCHAR(100) NOT NULL UNIQUE,
    user_phone VARCHAR(15),
    birth_date DATE,
    gender CHAR(1),
    reg_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE
    );

-- 회원 권한 테이블
CREATE TABLE IF NOT EXISTS tbl_member_auth (
                                               user_id VARCHAR(20),
    auth VARCHAR(50) NOT NULL,
    FOREIGN KEY (user_id) REFERENCES tbl_member(user_id) ON DELETE CASCADE
    );

-- 계정 보안 테이블
CREATE TABLE IF NOT EXISTS tbl_account_security (
                                                    user_id VARCHAR(20) PRIMARY KEY,
    email_verified BOOLEAN DEFAULT FALSE,
    phone_verified BOOLEAN DEFAULT FALSE,
    two_factor_enabled BOOLEAN DEFAULT FALSE,
    account_locked BOOLEAN DEFAULT FALSE,
    login_fail_count INT DEFAULT 0,
    last_login_time TIMESTAMP,
    lock_time TIMESTAMP,
    lock_reason VARCHAR(200),
    update_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES tbl_member(user_id) ON DELETE CASCADE
    );

-- 인증 토큰 테이블
CREATE TABLE IF NOT EXISTS tbl_auth_token (
                                              token_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                              user_id VARCHAR(20) NOT NULL,
    token VARCHAR(255) NOT NULL UNIQUE,
    token_type VARCHAR(50) NOT NULL,
    expiry_time TIMESTAMP NOT NULL,
    is_used BOOLEAN DEFAULT FALSE,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    used_date TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES tbl_member(user_id) ON DELETE CASCADE
    );

-- 로그인 이력 테이블
CREATE TABLE IF NOT EXISTS tbl_login_history (
                                                 history_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                 user_id VARCHAR(20) NOT NULL,
    ip_address VARCHAR(45),
    user_agent TEXT,
    login_success BOOLEAN NOT NULL,
    failure_reason VARCHAR(200),
    login_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    location VARCHAR(100),
    device_type VARCHAR(50)
    );

-- 금융 프로필 테이블
CREATE TABLE IF NOT EXISTS tbl_financial_profile (
                                                     profile_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                     user_id VARCHAR(20) NOT NULL UNIQUE,
    income_range VARCHAR(20),
    job_type VARCHAR(30),
    monthly_income DECIMAL(15,2),
    monthly_expense DECIMAL(15,2),
    total_assets DECIMAL(15,2),
    total_debt DECIMAL(15,2),
    risk_profile VARCHAR(20),
    investment_goal VARCHAR(30),
    investment_period INT,
    emergency_fund DECIMAL(15,2),
    credit_score VARCHAR(20),
    reg_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES tbl_member(user_id) ON DELETE CASCADE
    );

-- 금융 목표 테이블
CREATE TABLE IF NOT EXISTS tbl_financial_goal (
                                                  goal_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                  user_id VARCHAR(20) NOT NULL,
    goal_name VARCHAR(50) NOT NULL,
    goal_type VARCHAR(30) NOT NULL,
    target_amount DECIMAL(15,2) NOT NULL,
    current_amount DECIMAL(15,2) DEFAULT 0,
    monthly_contribution DECIMAL(15,2),
    target_date DATE,
    start_date DATE,
    priority VARCHAR(10),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    description TEXT,
    progress_percentage DECIMAL(5,2),
    expected_months INT,
    reg_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES tbl_member(user_id) ON DELETE CASCADE
    );

-- 금융 거래 테이블
CREATE TABLE IF NOT EXISTS tbl_financial_transaction (
                                                         transaction_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                         user_id VARCHAR(20) NOT NULL,
    transaction_type VARCHAR(30) NOT NULL,
    category VARCHAR(50),
    amount DECIMAL(15,2) NOT NULL,
    description TEXT,
    transaction_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    payment_method VARCHAR(30),
    merchant VARCHAR(100),
    goal_id BIGINT,
    tags TEXT,
    memo TEXT,
    is_recurring BOOLEAN DEFAULT FALSE,
    recurring_period VARCHAR(20),
    reg_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES tbl_member(user_id) ON DELETE CASCADE,
    FOREIGN KEY (goal_id) REFERENCES tbl_financial_goal(goal_id) ON DELETE SET NULL
    );