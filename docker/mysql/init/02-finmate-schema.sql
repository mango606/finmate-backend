-- FinMate 제테크 금융 서비스 데이터베이스 스키마

-- 기존 테이블 삭제 (개발 환경에서만 사용)
DROP TABLE IF EXISTS tbl_financial_transaction;
DROP TABLE IF EXISTS tbl_financial_goal;
DROP TABLE IF EXISTS tbl_financial_profile;
DROP TABLE IF EXISTS tbl_auth_token;
DROP TABLE IF EXISTS tbl_login_history;
DROP TABLE IF EXISTS tbl_account_security;

-- 1. 금융 프로필 테이블
CREATE TABLE tbl_financial_profile (
                                       profile_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '프로필 ID',
                                       user_id VARCHAR(50) NOT NULL COMMENT '사용자 ID',
                                       income_range VARCHAR(20) NOT NULL COMMENT '소득 구간 (LOW, MEDIUM, HIGH)',
                                       job_type VARCHAR(30) NOT NULL COMMENT '직업 유형 (EMPLOYEE, SELF_EMPLOYED, FREELANCER, STUDENT, RETIRED)',
                                       monthly_income DECIMAL(15,2) NOT NULL COMMENT '월 소득',
                                       monthly_expense DECIMAL(15,2) NOT NULL COMMENT '월 지출',
                                       total_assets DECIMAL(15,2) DEFAULT 0 COMMENT '총 자산',
                                       total_debt DECIMAL(15,2) DEFAULT 0 COMMENT '총 부채',
                                       risk_profile VARCHAR(20) NOT NULL COMMENT '투자 성향 (CONSERVATIVE, MODERATE, AGGRESSIVE)',
                                       investment_goal VARCHAR(30) NOT NULL COMMENT '투자 목표 (SAVING, RETIREMENT, EDUCATION, HOUSE, BUSINESS)',
                                       investment_period INT NOT NULL COMMENT '투자 기간 (개월)',
                                       emergency_fund DECIMAL(15,2) DEFAULT 0 COMMENT '비상금',
                                       credit_score VARCHAR(20) COMMENT '신용등급 (EXCELLENT, GOOD, FAIR, POOR)',
                                       reg_date DATETIME DEFAULT NOW() COMMENT '등록일',
                                       update_date DATETIME DEFAULT NOW() ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일',

                                       CONSTRAINT fk_financial_profile_user FOREIGN KEY (user_id) REFERENCES tbl_member(user_id) ON DELETE CASCADE,
                                       CONSTRAINT uk_financial_profile_user UNIQUE (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='금융 프로필';

-- 2. 금융 목표 테이블
CREATE TABLE tbl_financial_goal (
                                    goal_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '목표 ID',
                                    user_id VARCHAR(50) NOT NULL COMMENT '사용자 ID',
                                    goal_name VARCHAR(100) NOT NULL COMMENT '목표명',
                                    goal_type VARCHAR(30) NOT NULL COMMENT '목표 유형 (SAVING, INVESTMENT, DEBT_REPAYMENT, HOUSE_PURCHASE, RETIREMENT)',
                                    target_amount DECIMAL(15,2) NOT NULL COMMENT '목표 금액',
                                    current_amount DECIMAL(15,2) DEFAULT 0 COMMENT '현재 금액',
                                    monthly_contribution DECIMAL(15,2) NOT NULL COMMENT '월 적립액',
                                    target_date DATE NOT NULL COMMENT '목표 달성 일자',
                                    start_date DATE NOT NULL COMMENT '시작일',
                                    priority VARCHAR(10) NOT NULL COMMENT '우선순위 (HIGH, MEDIUM, LOW)',
                                    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '상태 (ACTIVE, COMPLETED, PAUSED, CANCELLED)',
                                    description TEXT COMMENT '설명',
                                    progress_percentage DECIMAL(5,2) DEFAULT 0 COMMENT '달성률',
                                    expected_months INT COMMENT '예상 달성 기간 (개월)',
                                    reg_date DATETIME DEFAULT NOW() COMMENT '등록일',
                                    update_date DATETIME DEFAULT NOW() ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일',

                                    CONSTRAINT fk_financial_goal_user FOREIGN KEY (user_id) REFERENCES tbl_member(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='금융 목표';

-- 3. 금융 거래 테이블
CREATE TABLE tbl_financial_transaction (
                                           transaction_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '거래 ID',
                                           user_id VARCHAR(50) NOT NULL COMMENT '사용자 ID',
                                           transaction_type VARCHAR(20) NOT NULL COMMENT '거래 유형 (INCOME, EXPENSE, INVESTMENT, SAVING, LOAN_REPAYMENT)',
                                           category VARCHAR(50) NOT NULL COMMENT '카테고리',
                                           amount DECIMAL(15,2) NOT NULL COMMENT '거래 금액',
                                           description VARCHAR(200) COMMENT '설명',
                                           transaction_date DATETIME NOT NULL COMMENT '거래 일시',
                                           payment_method VARCHAR(30) COMMENT '결제 수단 (CARD, CASH, TRANSFER, INVESTMENT_ACCOUNT)',
                                           merchant VARCHAR(100) COMMENT '거래처/상호',
                                           goal_id BIGINT COMMENT '연관된 목표 ID',
                                           tags VARCHAR(200) COMMENT '태그 (콤마로 구분)',
                                           memo TEXT COMMENT '메모',
                                           is_recurring BOOLEAN DEFAULT FALSE COMMENT '정기 거래 여부',
                                           recurring_period VARCHAR(20) COMMENT '정기 거래 주기 (DAILY, WEEKLY, MONTHLY, YEARLY)',
                                           reg_date DATETIME DEFAULT NOW() COMMENT '등록일',
                                           update_date DATETIME DEFAULT NOW() ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일',

                                           CONSTRAINT fk_financial_transaction_user FOREIGN KEY (user_id) REFERENCES tbl_member(user_id) ON DELETE CASCADE,
                                           CONSTRAINT fk_financial_transaction_goal FOREIGN KEY (goal_id) REFERENCES tbl_financial_goal(goal_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='금융 거래';

-- 4. 인증 토큰 테이블
CREATE TABLE tbl_auth_token (
                                token_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '토큰 ID',
                                user_id VARCHAR(50) NOT NULL COMMENT '사용자 ID',
                                token VARCHAR(255) NOT NULL COMMENT '토큰 값',
                                token_type VARCHAR(30) NOT NULL COMMENT '토큰 유형 (PASSWORD_RESET, EMAIL_VERIFICATION, ACCESS_TOKEN)',
                                expiry_time DATETIME NOT NULL COMMENT '만료 시간',
                                is_used BOOLEAN DEFAULT FALSE COMMENT '사용 여부',
                                created_date DATETIME DEFAULT NOW() COMMENT '생성일',
                                used_date DATETIME COMMENT '사용일',

                                CONSTRAINT fk_auth_token_user FOREIGN KEY (user_id) REFERENCES tbl_member(user_id) ON DELETE CASCADE,
                                CONSTRAINT uk_auth_token UNIQUE (token)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='인증 토큰';

-- 5. 로그인 이력 테이블
CREATE TABLE tbl_login_history (
                                   history_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '이력 ID',
                                   user_id VARCHAR(50) NOT NULL COMMENT '사용자 ID',
                                   ip_address VARCHAR(45) NOT NULL COMMENT 'IP 주소',
                                   user_agent TEXT COMMENT '사용자 에이전트',
                                   login_success BOOLEAN NOT NULL COMMENT '로그인 성공 여부',
                                   failure_reason VARCHAR(100) COMMENT '실패 사유',
                                   login_time DATETIME DEFAULT NOW() COMMENT '로그인 시간',
                                   location VARCHAR(100) COMMENT '접속 위치',
                                   device_type VARCHAR(30) COMMENT '기기 유형',

                                   CONSTRAINT fk_login_history_user FOREIGN KEY (user_id) REFERENCES tbl_member(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='로그인 이력';

-- 6. 계정 보안 테이블
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

-- 7. 투자 포트폴리오 테이블
CREATE TABLE tbl_investment_portfolio (
                                          portfolio_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '포트폴리오 ID',
                                          user_id VARCHAR(50) NOT NULL COMMENT '사용자 ID',
                                          portfolio_name VARCHAR(100) NOT NULL COMMENT '포트폴리오명',
                                          total_investment DECIMAL(15,2) DEFAULT 0 COMMENT '총 투자금액',
                                          current_value DECIMAL(15,2) DEFAULT 0 COMMENT '현재 평가액',
                                          profit_loss DECIMAL(15,2) DEFAULT 0 COMMENT '손익',
                                          profit_rate DECIMAL(5,2) DEFAULT 0 COMMENT '수익률',
                                          portfolio_type VARCHAR(30) NOT NULL COMMENT '포트폴리오 유형 (STOCKS, FUNDS, BONDS, MIXED)',
                                          risk_level VARCHAR(20) NOT NULL COMMENT '위험도 (LOW, MEDIUM, HIGH)',
                                          status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '상태 (ACTIVE, INACTIVE, CLOSED)',
                                          reg_date DATETIME DEFAULT NOW() COMMENT '등록일',
                                          update_date DATETIME DEFAULT NOW() ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일',

                                          CONSTRAINT fk_investment_portfolio_user FOREIGN KEY (user_id) REFERENCES tbl_member(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='투자 포트폴리오';

-- 8. 투자 상품 테이블
CREATE TABLE tbl_investment_product (
                                        product_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '상품 ID',
                                        portfolio_id BIGINT NOT NULL COMMENT '포트폴리오 ID',
                                        product_code VARCHAR(20) NOT NULL COMMENT '상품 코드',
                                        product_name VARCHAR(100) NOT NULL COMMENT '상품명',
                                        product_type VARCHAR(30) NOT NULL COMMENT '상품 유형 (STOCK, FUND, BOND, ETF)',
                                        quantity DECIMAL(15,6) NOT NULL COMMENT '보유 수량',
                                        purchase_price DECIMAL(15,2) NOT NULL COMMENT '매입 단가',
                                        current_price DECIMAL(15,2) DEFAULT 0 COMMENT '현재 단가',
                                        purchase_amount DECIMAL(15,2) NOT NULL COMMENT '매입 금액',
                                        current_value DECIMAL(15,2) DEFAULT 0 COMMENT '현재 평가액',
                                        profit_loss DECIMAL(15,2) DEFAULT 0 COMMENT '손익',
                                        profit_rate DECIMAL(5,2) DEFAULT 0 COMMENT '수익률',
                                        purchase_date DATE NOT NULL COMMENT '매입일',
                                        reg_date DATETIME DEFAULT NOW() COMMENT '등록일',
                                        update_date DATETIME DEFAULT NOW() ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일',

                                        CONSTRAINT fk_investment_product_portfolio FOREIGN KEY (portfolio_id) REFERENCES tbl_investment_portfolio(portfolio_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='투자 상품';

-- 9. 금융 교육 콘텐츠 테이블
CREATE TABLE tbl_education_content (
                                       content_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '콘텐츠 ID',
                                       title VARCHAR(200) NOT NULL COMMENT '제목',
                                       content_type VARCHAR(30) NOT NULL COMMENT '콘텐츠 유형 (ARTICLE, VIDEO, QUIZ, COURSE)',
                                       category VARCHAR(50) NOT NULL COMMENT '카테고리',
                                       level VARCHAR(20) NOT NULL COMMENT '난이도 (BEGINNER, INTERMEDIATE, ADVANCED)',
                                       content TEXT NOT NULL COMMENT '내용',
                                       summary TEXT COMMENT '요약',
                                       thumbnail_url VARCHAR(255) COMMENT '썸네일 URL',
                                       video_url VARCHAR(255) COMMENT '동영상 URL',
                                       duration INT COMMENT '소요 시간 (분)',
                                       view_count INT DEFAULT 0 COMMENT '조회수',
                                       like_count INT DEFAULT 0 COMMENT '좋아요 수',
                                       tags VARCHAR(200) COMMENT '태그',
                                       is_active BOOLEAN DEFAULT TRUE COMMENT '활성화 여부',
                                       reg_date DATETIME DEFAULT NOW() COMMENT '등록일',
                                       update_date DATETIME DEFAULT NOW() ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='금융 교육 콘텐츠';

-- 10. 학습 진도 테이블
CREATE TABLE tbl_learning_progress (
                                       progress_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '진도 ID',
                                       user_id VARCHAR(50) NOT NULL COMMENT '사용자 ID',
                                       content_id BIGINT NOT NULL COMMENT '콘텐츠 ID',
                                       progress_rate DECIMAL(5,2) DEFAULT 0 COMMENT '진도율',
                                       completion_status VARCHAR(20) DEFAULT 'IN_PROGRESS' COMMENT '완료 상태 (IN_PROGRESS, COMPLETED, PAUSED)',
                                       last_position INT DEFAULT 0 COMMENT '마지막 위치',
                                       quiz_score INT COMMENT '퀴즈 점수',
                                       start_date DATETIME DEFAULT NOW() COMMENT '시작일',
                                       completion_date DATETIME COMMENT '완료일',
                                       update_date DATETIME DEFAULT NOW() ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일',

                                       CONSTRAINT fk_learning_progress_user FOREIGN KEY (user_id) REFERENCES tbl_member(user_id) ON DELETE CASCADE,
                                       CONSTRAINT fk_learning_progress_content FOREIGN KEY (content_id) REFERENCES tbl_education_content(content_id) ON DELETE CASCADE,
                                       CONSTRAINT uk_learning_progress UNIQUE (user_id, content_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='학습 진도';

-- 인덱스 생성
CREATE INDEX idx_financial_profile_user ON tbl_financial_profile(user_id);
CREATE INDEX idx_financial_goal_user ON tbl_financial_goal(user_id);
CREATE INDEX idx_financial_goal_status ON tbl_financial_goal(status);
CREATE INDEX idx_financial_goal_type ON tbl_financial_goal(goal_type);
CREATE INDEX idx_financial_transaction_user ON tbl_financial_transaction(user_id);
CREATE INDEX idx_financial_transaction_date ON tbl_financial_transaction(transaction_date);
CREATE INDEX idx_financial_transaction_type ON tbl_financial_transaction(transaction_type);
CREATE INDEX idx_financial_transaction_category ON tbl_financial_transaction(category);
CREATE INDEX idx_auth_token_user ON tbl_auth_token(user_id);
CREATE INDEX idx_auth_token_type ON tbl_auth_token(token_type);
CREATE INDEX idx_auth_token_expiry ON tbl_auth_token(expiry_time);
CREATE INDEX idx_login_history_user ON tbl_login_history(user_id);
CREATE INDEX idx_login_history_time ON tbl_login_history(login_time);
CREATE INDEX idx_investment_portfolio_user ON tbl_investment_portfolio(user_id);
CREATE INDEX idx_investment_product_portfolio ON tbl_investment_product(portfolio_id);
CREATE INDEX idx_education_content_category ON tbl_education_content(category);
CREATE INDEX idx_education_content_level ON tbl_education_content(level);
CREATE INDEX idx_learning_progress_user ON tbl_learning_progress(user_id);

-- 기존 회원 테이블에 계정 보안 정보 추가
INSERT INTO tbl_account_security (user_id, email_verified, phone_verified, two_factor_enabled, account_locked, login_fail_count)
SELECT user_id, FALSE, FALSE, FALSE, FALSE, 0
FROM tbl_member
WHERE user_id NOT IN (SELECT user_id FROM tbl_account_security);

-- 샘플 교육 콘텐츠 데이터
INSERT INTO tbl_education_content (title, content_type, category, level, content, summary, duration, tags) VALUES
                                                                                                               ('제테크 시작하기', 'ARTICLE', 'BASIC', 'BEGINNER', '제테크의 기본 개념과 시작 방법에 대해 알아보겠습니다.', '제테크 입문자를 위한 기초 가이드', 15, '제테크,기초,입문'),
                                                                                                               ('투자 성향 파악하기', 'QUIZ', 'INVESTMENT', 'BEGINNER', '나의 투자 성향을 파악해보는 퀴즈입니다.', '투자 성향 테스트', 10, '투자,성향,테스트'),
                                                                                                               ('주식 투자 기초', 'VIDEO', 'STOCK', 'INTERMEDIATE', '주식 투자의 기본 원리와 방법을 설명합니다.', '주식 투자 기초 강의', 30, '주식,투자,기초'),
                                                                                                               ('펀드 투자 가이드', 'COURSE', 'FUND', 'INTERMEDIATE', '펀드 투자의 모든 것을 배우는 과정입니다.', '펀드 투자 완벽 가이드', 120, '펀드,투자,가이드'),
                                                                                                               ('은퇴 준비 전략', 'ARTICLE', 'RETIREMENT', 'ADVANCED', '은퇴 후 안정적인 생활을 위한 재정 계획 방법입니다.', '은퇴 준비 재정 계획', 45, '은퇴,재정,계획');

-- 트리거 생성 (금융 목표 달성률 자동 계산)
DELIMITER //
CREATE TRIGGER tr_financial_goal_progress
    BEFORE UPDATE ON tbl_financial_goal
    FOR EACH ROW
BEGIN
    IF NEW.target_amount > 0 THEN
        SET NEW.progress_percentage = (NEW.current_amount / NEW.target_amount) * 100;
END IF;
END//
DELIMITER ;

-- 트리거 생성 (투자 상품 수익률 자동 계산)
DELIMITER //
CREATE TRIGGER tr_investment_product_profit
    BEFORE UPDATE ON tbl_investment_product
    FOR EACH ROW
BEGIN
    SET NEW.current_value = NEW.quantity * NEW.current_price;
    SET NEW.profit_loss = NEW.current_value - NEW.purchase_amount;
    IF NEW.purchase_amount > 0 THEN
        SET NEW.profit_rate = (NEW.profit_loss / NEW.purchase_amount) * 100;
END IF;
END//
DELIMITER ;

-- 뷰 생성 (사용자 금융 현황 요약)
CREATE VIEW vw_user_financial_summary AS
SELECT
    m.user_id,
    m.user_name,
    m.user_email,
    fp.monthly_income,
    fp.monthly_expense,
    fp.total_assets,
    fp.total_debt,
    fp.emergency_fund,
    fp.risk_profile,
    COUNT(fg.goal_id) as total_goals,
    SUM(CASE WHEN fg.status = 'ACTIVE' THEN 1 ELSE 0 END) as active_goals,
    SUM(CASE WHEN fg.status = 'COMPLETED' THEN 1 ELSE 0 END) as completed_goals,
    SUM(fg.target_amount) as total_target_amount,
    SUM(fg.current_amount) as total_current_amount,
    AVG(fg.progress_percentage) as avg_progress
FROM tbl_member m
         LEFT JOIN tbl_financial_profile fp ON m.user_id = fp.user_id
         LEFT JOIN tbl_financial_goal fg ON m.user_id = fg.user_id
WHERE m.is_active = TRUE
GROUP BY m.user_id;

-- 권한 설정
GRANT SELECT, INSERT, UPDATE, DELETE ON finmate_db.* TO 'finmate'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON finmate_db.* TO 'finmate'@'localhost';
FLUSH PRIVILEGES;

-- 완료 메시지
SELECT '🎉 FinMate 제테크 금융 서비스 데이터베이스 스키마 생성이 완료되었습니다!' as message;