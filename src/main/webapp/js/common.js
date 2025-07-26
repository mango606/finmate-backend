class FinMateCommon {
    constructor() {
        this.API_BASE_URL = '/api';
        this.setupAxios();
        this.initializeNavigation();
    }

    // Axios 기본 설정
    setupAxios() {
        axios.defaults.baseURL = this.API_BASE_URL;
        axios.defaults.headers.common['Content-Type'] = 'application/json';
        axios.defaults.withCredentials = true;

        // 토큰 설정
        const token = localStorage.getItem('accessToken');
        if (token) {
            axios.defaults.headers.common['Authorization'] = `Bearer ${token}`;
        }

        // 응답 인터셉터 - 토큰 만료 처리
        axios.interceptors.response.use(
            response => response,
            async error => {
                if (error.response?.status === 401) {
                    const refreshToken = localStorage.getItem('refreshToken');
                    if (refreshToken) {
                        try {
                            const response = await axios.post('/auth/refresh', {
                                refreshToken: refreshToken
                            });

                            if (response.data.success) {
                                const newToken = response.data.data.accessToken;
                                localStorage.setItem('accessToken', newToken);
                                axios.defaults.headers.common['Authorization'] = `Bearer ${newToken}`;

                                // 원래 요청 재시도
                                error.config.headers['Authorization'] = `Bearer ${newToken}`;
                                return axios.request(error.config);
                            }
                        } catch (refreshError) {
                            console.error('토큰 갱신 실패:', refreshError);
                        }
                    }

                    // 토큰 갱신 실패 시 로그인 페이지로 리다이렉트
                    this.logout();
                }
                return Promise.reject(error);
            }
        );
    }

    // 네비게이션 초기화
    initializeNavigation() {
        // 페이지별 경로 매핑
        this.pageRoutes = {
            '/': 'home',
            '/index.html': 'home',
            '/member.html': 'member',
            '/dashboard.html': 'dashboard',
            '/financial.html': 'financial',
            '/investment.html': 'investment',
            '/portfolio.html': 'portfolio',
            '/market.html': 'market',
            '/calculator.html': 'calculator',
            '/education.html': 'education',
            '/mypage.html': 'mypage',
            '/admindashboard.html': 'admin'
        };

        // 로그인이 필요한 페이지들
        this.protectedPages = [
            '/dashboard.html',
            '/financial.html',
            '/investment.html',
            '/portfolio.html',
            '/mypage.html',
            '/admindashboard.html'
        ];

        // 관리자 권한이 필요한 페이지들
        this.adminPages = [
            '/admindashboard.html'
        ];
    }

    // 인증 상태 확인
    async checkAuth() {
        const token = localStorage.getItem('accessToken');
        if (!token) {
            return { authenticated: false, user: null };
        }

        try {
            const response = await axios.get('/auth/me');
            if (response.data.success) {
                return {
                    authenticated: true,
                    user: response.data.data
                };
            }
        } catch (error) {
            console.error('인증 확인 실패:', error);
            this.clearAuthData();
        }

        return { authenticated: false, user: null };
    }

    // 페이지 접근 권한 확인
    async checkPageAccess(path = window.location.pathname) {
        const authStatus = await this.checkAuth();

        // 로그인이 필요한 페이지 체크
        if (this.protectedPages.includes(path) && !authStatus.authenticated) {
            this.redirectToLogin();
            return false;
        }

        // 관리자 권한이 필요한 페이지 체크
        if (this.adminPages.includes(path)) {
            if (!authStatus.authenticated) {
                this.redirectToLogin();
                return false;
            }

            if (!authStatus.user?.authorities?.includes('ROLE_ADMIN')) {
                this.showAlert('관리자 권한이 필요합니다.', 'error');
                this.goToPage('/dashboard.html');
                return false;
            }
        }

        return { allowed: true, user: authStatus.user };
    }

    // 사용자 정보 로드
    async loadUserInfo() {
        try {
            const response = await axios.get('/auth/me');
            if (response.data.success) {
                return response.data.data;
            }
            throw new Error('사용자 정보 로드 실패');
        } catch (error) {
            console.error('사용자 정보 로드 실패:', error);
            this.clearAuthData();
            return null;
        }
    }

    // 로그아웃
    async logout() {
        try {
            await axios.post('/auth/logout');
        } catch (error) {
            console.warn('로그아웃 API 호출 실패:', error);
        } finally {
            this.clearAuthData();
            this.redirectToLogin();
        }
    }

    // 인증 데이터 클리어
    clearAuthData() {
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        delete axios.defaults.headers.common['Authorization'];
    }

    // 로그인 페이지로 리다이렉트
    redirectToLogin() {
        if (window.location.pathname !== '/member.html') {
            window.location.href = '/member.html';
        }
    }

    // 페이지 이동 (권한 체크 포함)
    async goToPage(url) {
        // 로그인이 필요한 페이지들에 대한 체크
        if (this.protectedPages.includes(url)) {
            const authStatus = await this.checkAuth();
            if (!authStatus.authenticated) {
                this.redirectToLogin();
                return;
            }
        }

        window.location.href = url;
    }

    // 현재 페이지 확인
    getCurrentPage() {
        const path = window.location.pathname;
        return this.pageRoutes[path] || 'unknown';
    }

    // 네비게이션 메뉴 업데이트
    updateNavigation() {
        const currentPage = this.getCurrentPage();
        const navItems = document.querySelectorAll('.nav-item');

        navItems.forEach(item => {
            item.classList.remove('active');
            const href = item.getAttribute('href');

            if (href) {
                const pageName = this.pageRoutes[href];
                if (pageName === currentPage) {
                    item.classList.add('active');
                }
            }
        });
    }

    // 사용자 메뉴 생성
    createUserMenu(user, container) {
        if (!container) return;

        if (user) {
            container.innerHTML = `
                <div class="user-info" onclick="finmate.toggleUserDropdown()">
                    <div class="avatar">${user.userName ? user.userName.charAt(0) : 'U'}</div>
                    <span>${user.userName}님</span>
                    <div class="user-dropdown" id="userDropdown">
                        <a href="/mypage.html" class="dropdown-item">👤 마이페이지</a>
                        <a href="/financial.html" class="dropdown-item">💰 금융관리</a>
                        ${user.authorities?.includes('ROLE_ADMIN') ?
                '<a href="/admindashboard.html" class="dropdown-item">🛠️ 관리자</a>' : ''}
                        <a href="#" onclick="finmate.logout()" class="dropdown-item">🚪 로그아웃</a>
                    </div>
                </div>
            `;
        } else {
            container.innerHTML = `
                <div style="display: flex; gap: 15px;">
                    <a href="/member.html" class="btn btn-outline">로그인</a>
                    <a href="/member.html" class="btn btn-primary">회원가입</a>
                </div>
            `;
        }
    }

    // 사용자 드롭다운 토글
    toggleUserDropdown() {
        const dropdown = document.getElementById('userDropdown');
        if (dropdown) {
            dropdown.classList.toggle('show');
        }
    }

    // 통화 포맷
    formatCurrency(amount) {
        if (amount == null) return '₩0';
        return new Intl.NumberFormat('ko-KR', {
            style: 'currency',
            currency: 'KRW',
            minimumFractionDigits: 0
        }).format(amount);
    }

    // 날짜 포맷
    formatDate(dateString) {
        if (!dateString) return '';
        return new Date(dateString).toLocaleDateString('ko-KR');
    }

    // 날짜시간 포맷
    formatDateTime(dateString) {
        if (!dateString) return '';
        return new Date(dateString).toLocaleString('ko-KR');
    }

    // 숫자 포맷 (천단위 구분)
    formatNumber(number, decimals = 0) {
        if (number == null) return '0';
        return new Intl.NumberFormat('ko-KR', {
            minimumFractionDigits: 0,
            maximumFractionDigits: decimals
        }).format(number);
    }

    // 퍼센트 포맷
    formatPercent(value, decimals = 1) {
        if (value == null) return '0%';
        return `${Number(value).toFixed(decimals)}%`;
    }

    // 알림 표시 헬퍼
    showAlert(message, type = 'info', duration = 5000) {
        // 기존 알림 제거
        const existingAlert = document.querySelector('.finmate-alert');
        if (existingAlert) {
            existingAlert.remove();
        }

        // 새 알림 생성
        const alert = document.createElement('div');
        alert.className = `finmate-alert alert alert-${type}`;
        alert.innerHTML = `
            <span>${this.getAlertIcon(type)}</span>
            <span>${message}</span>
            <button onclick="this.parentElement.remove()" style="margin-left: auto; background: none; border: none; font-size: 1.2rem; cursor: pointer;">&times;</button>
        `;
        alert.style.cssText = `
            position: fixed;
            top: 20px;
            right: 20px;
            z-index: 9999;
            max-width: 400px;
            padding: 15px 20px;
            border-radius: 10px;
            box-shadow: 0 4px 15px rgba(0,0,0,0.2);
            display: flex;
            align-items: center;
            gap: 10px;
            animation: slideInRight 0.3s ease-out;
        `;

        // 타입별 스타일 적용
        const styles = {
            success: 'background: #d4edda; color: #155724; border: 1px solid #c3e6cb;',
            error: 'background: #f8d7da; color: #721c24; border: 1px solid #f5c6cb;',
            warning: 'background: #fff3cd; color: #856404; border: 1px solid #ffeaa7;',
            info: 'background: #d1ecf1; color: #0c5460; border: 1px solid #bee5eb;'
        };
        alert.style.cssText += styles[type] || styles.info;

        document.body.appendChild(alert);

        // 자동 제거
        setTimeout(() => {
            if (alert.parentElement) {
                alert.style.animation = 'slideOutRight 0.3s ease-out';
                setTimeout(() => alert.remove(), 300);
            }
        }, duration);
    }

    // 알림 아이콘 반환
    getAlertIcon(type) {
        const icons = {
            success: '✅',
            error: '❌',
            warning: '⚠️',
            info: 'ℹ️'
        };
        return icons[type] || 'ℹ️';
    }

    // 로딩 상태 표시
    showLoading(element, message = '로딩 중...') {
        if (!element) return;
        element.innerHTML = `
            <div class="loading">
                <div class="spinner"></div>
                <div>${message}</div>
            </div>
        `;
    }

    // 에러 메시지 추출
    getErrorMessage(error) {
        if (error.response?.data?.message) {
            return error.response.data.message;
        }
        if (error.message) {
            return error.message;
        }
        return '알 수 없는 오류가 발생했습니다.';
    }

    // 디바운스 함수
    debounce(func, wait) {
        let timeout;
        return function executedFunction(...args) {
            const later = () => {
                clearTimeout(timeout);
                func(...args);
            };
            clearTimeout(timeout);
            timeout = setTimeout(later, wait);
        };
    }

    // 로컬 스토리지 안전한 저장
    setStorage(key, value) {
        try {
            localStorage.setItem(key, JSON.stringify(value));
        } catch (error) {
            console.error('로컬 스토리지 저장 실패:', error);
        }
    }

    // 로컬 스토리지 안전한 조회
    getStorage(key) {
        try {
            const item = localStorage.getItem(key);
            return item ? JSON.parse(item) : null;
        } catch (error) {
            console.error('로컬 스토리지 조회 실패:', error);
            return null;
        }
    }

    // URL 파라미터 파싱
    getUrlParams() {
        const params = {};
        const urlParams = new URLSearchParams(window.location.search);
        for (const [key, value] of urlParams) {
            params[key] = value;
        }
        return params;
    }

    // 브라우저 알림 권한 요청
    async requestNotificationPermission() {
        if ('Notification' in window) {
            const permission = await Notification.requestPermission();
            return permission === 'granted';
        }
        return false;
    }

    // 브라우저 알림 표시
    showNotification(title, options = {}) {
        if ('Notification' in window && Notification.permission === 'granted') {
            new Notification(title, {
                icon: '/favicon.ico',
                badge: '/favicon.ico',
                ...options
            });
        }
    }

    // 클립보드에 복사
    async copyToClipboard(text) {
        try {
            await navigator.clipboard.writeText(text);
            this.showAlert('클립보드에 복사되었습니다.', 'success');
            return true;
        } catch (error) {
            console.error('클립보드 복사 실패:', error);
            this.showAlert('클립보드 복사에 실패했습니다.', 'error');
            return false;
        }
    }

    // 파일 다운로드
    downloadFile(data, filename, type = 'application/octet-stream') {
        const blob = new Blob([data], { type });
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = filename;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
    }

    // 반응형 디자인 체크
    isMobile() {
        return window.innerWidth <= 768;
    }

    // 서버 상태 확인
    async checkServerStatus() {
        try {
            const response = await axios.get('/ping');
            return response.data.success;
        } catch (error) {
            console.error('서버 연결 실패:', error);
            return false;
        }
    }

    // 페이지 초기화
    async initializePage() {
        try {
            // 네비게이션 업데이트
            this.updateNavigation();

            // 권한 체크
            const access = await this.checkPageAccess();
            if (!access) return;

            // 사용자 메뉴 업데이트
            const userMenuContainer = document.querySelector('.user-menu-container');
            if (userMenuContainer) {
                this.createUserMenu(access.user, userMenuContainer);
            }

            // 서버 상태 확인
            const serverStatus = await this.checkServerStatus();
            if (!serverStatus) {
                this.showAlert('서버와의 연결이 불안정합니다.', 'warning');
            }

            return access.user;
        } catch (error) {
            console.error('페이지 초기화 실패:', error);
            return null;
        }
    }
}

// CSS 스타일 추가
const finmateStyles = `
    @keyframes slideInRight {
        from { transform: translateX(100%); opacity: 0; }
        to { transform: translateX(0); opacity: 1; }
    }
    
    @keyframes slideOutRight {
        from { transform: translateX(0); opacity: 1; }
        to { transform: translateX(100%); opacity: 0; }
    }
    
    .user-dropdown {
        position: absolute;
        top: 100%;
        right: 0;
        background: white;
        border-radius: 10px;
        box-shadow: 0 5px 20px rgba(0,0,0,0.2);
        min-width: 200px;
        z-index: 1000;
        display: none;
        overflow: hidden;
    }
    
    .user-dropdown.show {
        display: block;
    }
    
    .dropdown-item {
        display: block;
        padding: 12px 20px;
        color: #333;
        text-decoration: none;
        border-bottom: 1px solid #eee;
        transition: all 0.3s;
    }
    
    .dropdown-item:hover {
        background: #f8f9fa;
    }
    
    .dropdown-item:last-child {
        border-bottom: none;
    }
    
    .loading {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        padding: 50px;
        gap: 15px;
    }
    
    .spinner {
        width: 40px;
        height: 40px;
        border: 4px solid #f3f3f3;
        border-top: 4px solid #667eea;
        border-radius: 50%;
        animation: spin 1s linear infinite;
    }
    
    @keyframes spin {
        0% { transform: rotate(0deg); }
        100% { transform: rotate(360deg); }
    }
`;

// 스타일을 head에 추가
const styleSheet = document.createElement('style');
styleSheet.textContent = finmateStyles;
document.head.appendChild(styleSheet);

// 전역 인스턴스 생성
const finmate = new FinMateCommon();

// 외부 클릭 시 드롭다운 닫기
document.addEventListener('click', (event) => {
    const userMenu = document.querySelector('.user-info');
    const dropdown = document.getElementById('userDropdown');

    if (dropdown && userMenu && !userMenu.contains(event.target)) {
        dropdown.classList.remove('show');
    }
});

// DOM 로드 완료 시 초기화
document.addEventListener('DOMContentLoaded', async () => {
    // 페이지 초기화
    await finmate.initializePage();
});

// Vue.js 전역 믹스인 (Vue.js가 있는 경우)
if (typeof Vue !== 'undefined' && Vue.createApp) {
    const globalMixin = {
        data() {
            return {
                finmate: finmate
            };
        },
        methods: {
            formatCurrency: finmate.formatCurrency.bind(finmate),
            formatDate: finmate.formatDate.bind(finmate),
            formatDateTime: finmate.formatDateTime.bind(finmate),
            formatNumber: finmate.formatNumber.bind(finmate),
            formatPercent: finmate.formatPercent.bind(finmate),
            goToPage: finmate.goToPage.bind(finmate),
            getErrorMessage: finmate.getErrorMessage.bind(finmate),
            showAlert: finmate.showAlert.bind(finmate)
        }
    };

    // Vue 앱 생성 시 자동으로 믹스인 추가
    const originalCreateApp = Vue.createApp;
    Vue.createApp = function(options) {
        if (options.mixins) {
            options.mixins.push(globalMixin);
        } else {
            options.mixins = [globalMixin];
        }
        return originalCreateApp.call(this, options);
    };
}

window.finmate = finmate;