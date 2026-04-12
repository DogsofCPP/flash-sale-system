/**
 * 秒杀系统 - API 服务模块
 * 封装所有后端 API 调用，包含 Token 自动刷新机制
 */

const API_BASE = 'http://localhost:8080/api';

// 统一响应格式处理
const ApiResponse = {
    success: (data) => ({ code: 200, data, message: 'success' }),
    error: (message, code = 500) => ({ code, data: null, message })
};

// 带自动 Refresh 的 Fetch 封装
async function fetchWithAuth(url, options = {}) {
    let accessToken = localStorage.getItem('accessToken');
    const refreshToken = localStorage.getItem('refreshToken');

    if (!options.headers) options.headers = {};
    if (accessToken) options.headers['Authorization'] = 'Bearer ' + accessToken;

    let response = await fetch(API_BASE + url, options);

    if (response.status === 401 && refreshToken) {
        console.log('Access Token 已过期，正在刷新...');
        const refreshRes = await fetch(API_BASE + '/user/refresh', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ refreshToken })
        });

        if (refreshRes.ok) {
            const tokens = await refreshRes.json();
            localStorage.setItem('accessToken', tokens.data.accessToken);
            localStorage.setItem('refreshToken', tokens.data.refreshToken);
            console.log('Token 刷新成功');

            options.headers['Authorization'] = 'Bearer ' + tokens.data.accessToken;
            response = await fetch(API_BASE + url, options);
        } else {
            console.log('Refresh Token 已失效，请重新登录');
            localStorage.removeItem('accessToken');
            localStorage.removeItem('refreshToken');
            localStorage.removeItem('userInfo');
            window.location.href = 'login.html';
            return { ok: false, status: 401 };
        }
    }
    return response;
}

// ============ 用户服务 ============

const UserAPI = {
    // 用户注册
    async register(username, password) {
        const res = await fetch(API_BASE + '/user/register', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password })
        });
        return await res.json();
    },

    // 用户登录
    async login(username, password) {
        const res = await fetch(API_BASE + '/user/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password })
        });
        return await res.json();
    },

    // 获取用户信息
    async getUserInfo() {
        const res = await fetchWithAuth('/user/info');
        return await res.json();
    },

    // 退出登录
    async logout() {
        const refreshToken = localStorage.getItem('refreshToken');
        if (!refreshToken) return { code: 200 };

        const res = await fetchWithAuth('/user/logout', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ refreshToken })
        });

        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        localStorage.removeItem('userInfo');
        return { ok: true };
    },

    // 检查登录状态
    isLoggedIn() {
        return !!localStorage.getItem('accessToken');
    },

    // 获取当前用户信息
    getCurrentUser() {
        const info = localStorage.getItem('userInfo');
        return info ? JSON.parse(info) : null;
    }
};

// ============ 商品服务 ============

const ProductAPI = {
    // 获取商品列表（游标分页）
    async list(cursor = 0, limit = 10) {
        const res = await fetchWithAuth(`/product/list?cursor=${cursor}&limit=${limit}`);
        return await res.json();
    },

    // 获取单个商品详情
    async get(id) {
        const res = await fetchWithAuth(`/product/${id}`);
        return await res.json();
    },

    // 获取秒杀商品列表
    async getSeckillProducts() {
        const res = await fetchWithAuth('/product/seckill/list');
        return await res.json();
    }
};

// ============ 订单服务 ============

const OrderAPI = {
    // 创建订单
    async create(userId, productId, amount = 1, chaosType = null) {
        const params = new URLSearchParams({
            userId, productId, amount
        });
        if (chaosType) params.append('chaosType', chaosType);

        const res = await fetchWithAuth('/order/create', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: params
        });
        return await res.json();
    },

    // 查询订单状态
    async getStatus(orderId) {
        const res = await fetchWithAuth(`/order/status/${orderId}`);
        return await res.json();
    },

    // 获取用户订单列表
    async list(userId) {
        const res = await fetchWithAuth(`/order/list/${userId}`);
        return await res.json();
    }
};

// ============ 管理员服务 ============

const AdminAPI = {
    // 添加商品
    async addProduct(name, price, stock) {
        const res = await fetchWithAuth('/product/admin/add', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name, price, stock })
        });
        return await res.json();
    },

    // 更新商品
    async updateProduct(id, updates) {
        const res = await fetchWithAuth(`/product/admin/update/${id}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(updates)
        });
        return await res.json();
    },

    // 获取系统统计
    async getStats() {
        const res = await fetchWithAuth('/admin/stats');
        return await res.json();
    }
};

// ============ 工具函数 ============

const Utils = {
    // 格式化金额
    formatPrice(price) {
        return '¥' + parseFloat(price).toFixed(2);
    },

    // 格式化日期时间
    formatDateTime(date) {
        if (!date) return '-';
        const d = new Date(date);
        return d.toLocaleString('zh-CN');
    },

    // 倒计时格式化
    formatCountdown(seconds) {
        if (seconds <= 0) return '00:00:00';
        const h = Math.floor(seconds / 3600);
        const m = Math.floor((seconds % 3600) / 60);
        const s = seconds % 60;
        return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
    },

    // 显示提示消息
    showToast(message, type = 'info') {
        const toast = document.createElement('div');
        toast.className = `toast toast-${type}`;
        toast.innerHTML = `<span class="toast-icon">${type === 'success' ? '✓' : type === 'error' ? '✕' : 'ℹ'}</span><span class="toast-message">${message}</span>`;

        let container = document.querySelector('.toast-container');
        if (!container) {
            container = document.createElement('div');
            container.className = 'toast-container';
            document.body.appendChild(container);
        }
        container.appendChild(toast);

        setTimeout(() => toast.classList.add('show'), 10);
        setTimeout(() => {
            toast.classList.remove('show');
            setTimeout(() => toast.remove(), 300);
        }, 3000);
    },

    // 显示加载状态
    showLoading(element) {
        if (typeof element === 'string') {
            element = document.querySelector(element);
        }
        if (element) {
            element.classList.add('loading');
            element.disabled = true;
        }
    },

    // 隐藏加载状态
    hideLoading(element, originalText = '') {
        if (typeof element === 'string') {
            element = document.querySelector(element);
        }
        if (element) {
            element.classList.remove('loading');
            element.disabled = false;
            if (originalText) element.textContent = originalText;
        }
    }
};

// 导出到全局
window.API_BASE = API_BASE;
window.fetchWithAuth = fetchWithAuth;
window.UserAPI = UserAPI;
window.ProductAPI = ProductAPI;
window.OrderAPI = OrderAPI;
window.AdminAPI = AdminAPI;
window.Utils = Utils;
