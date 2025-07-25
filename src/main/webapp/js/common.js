class FinMateCommon {
    constructor() {
        this.API_BASE_URL = '/api';
        this.setupAxios();
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

    // 인증 상태 확인
    async checkAuth() {
        const token = localStorage.getItem('accessToken');
        if (!token) {
            this.redirectToLogin();
            return false;
        }

        try {
            const response = await axios.get('/auth/status');
            return response.data.success && response.data.data.authenticated;
        } catch (error) {
            console.error('인증 확인 실패:', error);
            this.redirectToLogin();
            return false;
        }
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
            this.redirectToLogin();
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
            localStorage.removeItem('accessToken');
            localStorage.removeItem('refreshToken');
            delete axios.defaults.headers.common['Authorization'];
            this.redirectToLogin();
        }
    }

    // 로그인 페이지로 리다이렉트
    redirectToLogin() {
        if (window.location.pathname !== '/member.html') {
            window.location.href = '/member.html';
        }
    }

    // 페이지 이동
    goToPage(url) {
        window.location.href = url;
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
    formatNumber(number) {
        if (number == null) return '0';
        return new Intl.NumberFormat('ko-KR').format(number);
    }

    // 퍼센트 포맷
    formatPercent(value, decimals = 1) {
        if (value == null) return '0%';
        return `${Number(value).toFixed(decimals)}%`;
    }

    // 알림 표시 헬퍼
    showAlert(element, message, type = 'info', duration = 5000) {
        if (!element) return;

        element.className = `alert alert-${type}`;
        element.innerHTML = `<span>${this.getAlertIcon(type)}</span><span>${message}</span>`;
        element.style.display = 'flex';

        setTimeout(() => {
            element.style.display = 'none';
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
    showLoading(element) {
        if (!element) return;
        element.innerHTML = `
            <div class="loading">
                <div class="spinner"></div>
                <div>로딩 중...</div>
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

    // 쿠키 설정
    setCookie(name, value, days) {
        const expires = days ? `; expires=${new Date(Date.now() + days * 864e5).toUTCString()}` : '';
        document.cookie = `${name}=${encodeURIComponent(value)}${expires}; path=/`;
    }

    // 쿠키 가져오기
    getCookie(name) {
        return document.cookie.split('; ').reduce((r, v) => {
            const parts = v.split('=');
            return parts[0] === name ? decodeURIComponent(parts[1]) : r;
        }, '');
    }

    // 쿠키 삭제
    deleteCookie(name) {
        this.setCookie(name, '', -1);
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

    // 폼 데이터를 객체로 변환
    formDataToObject(formData) {
        const object = {};
        formData.forEach((value, key) => {
            if (object[key]) {
                if (!Array.isArray(object[key])) {
                    object[key] = [object[key]];
                }
                object[key].push(value);
            } else {
                object[key] = value;
            }
        });
        return object;
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

    // 현재 페이지 정보
    getCurrentPage() {
        const path = window.location.pathname;
        const pages = {
            '/': 'home',
            '/index.html': 'home',
            '/member.html': 'member',
            '/dashboard.html': 'dashboard',
            '/financial.html': 'financial',
            '/investment.html': 'investment',
            '/mypage.html': 'mypage'
        };
        return pages[path] || 'unknown';
    }

    // 네비게이션 메뉴 활성화
    updateNavigation() {
        const currentPage = this.getCurrentPage();
        const navItems = document.querySelectorAll('.nav-item');

        navItems.forEach(item => {
            item.classList.remove('active');
            const href = item.getAttribute('href');
            if (href && href.includes(currentPage)) {
                item.classList.add('active');
            }
        });
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
            return true;
        } catch (error) {
            console.error('클립보드 복사 실패:', error);
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

    // 이미지 미리보기
    previewImage(file, callback) {
        if (file && file.type.startsWith('image/')) {
            const reader = new FileReader();
            reader.onload = e => callback(e.target.result);
            reader.readAsDataURL(file);
        }
    }

    // 반응형 디자인 체크
    isMobile() {
        return window.innerWidth <= 768;
    }

    // 스크롤 위치 저장/복원
    saveScrollPosition() {
        sessionStorage.setItem('scrollPosition', window.pageYOffset);
    }

    restoreScrollPosition() {
        const scrollPosition = sessionStorage.getItem('scrollPosition');
        if (scrollPosition) {
            window.scrollTo(0, parseInt(scrollPosition));
            sessionStorage.removeItem('scrollPosition');
        }
    }
}

// 전역 인스턴스 생성
const finmate = new FinMateCommon();

// DOM 로드 완료 시 초기화
document.addEventListener('DOMContentLoaded', () => {
    // 네비게이션 업데이트
    finmate.updateNavigation();

    // 스크롤 위치 복원
    finmate.restoreScrollPosition();

    // 페이지 이탈 시 스크롤 위치 저장
    window.addEventListener('beforeunload', () => {
        finmate.saveScrollPosition();
    });
});

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
            getErrorMessage: finmate.getErrorMessage.bind(finmate)
        }
    };

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