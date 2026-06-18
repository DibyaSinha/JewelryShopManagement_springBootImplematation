const API_BASE = '/api';

const api = {
    auth: {
        login: (credentials) => fetch(`${API_BASE}/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(credentials)
        }).then(res => res.json()),
        
        register: (user) => fetch(`${API_BASE}/auth/register`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(user)
        }).then(res => res.json())
    },

    request: async (endpoint, options = {}) => {
        const token = localStorage.getItem('auth_token'); // Using basic auth for now, so we'll store "username:password" encoded
        const headers = {
            'Content-Type': 'application/json',
            ...options.headers
        };

        if (token) {
            headers['Authorization'] = `Basic ${token}`;
        }

        const response = await fetch(`${API_BASE}${endpoint}`, { ...options, headers });
        if (response.status === 401) {
            app.logout();
            throw new Error('Unauthorized');
        }
        
        if (options.responseType === 'blob') {
            return response.blob();
        }
        
        return response.json();
    },

    jewelry: {
        getAll: () => api.request('/jewelry'),
        getById: (id) => api.request(`/jewelry/${id}`),
        add: (data) => api.request('/jewelry', { method: 'POST', body: JSON.stringify(data) }),
        update: (id, data) => api.request(`/jewelry/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
        delete: (id) => api.request(`/jewelry/${id}`, { method: 'DELETE' }),
        addStock: (id, qty) => api.request(`/jewelry/${id}/stock?quantity=${qty}`, { method: 'POST' })
    },

    rates: {
        getAll: () => api.request('/rates'),
        getToday: (type) => api.request(`/rates/today/${type}`),
        update: (data) => api.request('/rates', { method: 'POST', body: JSON.stringify(data) })
    },

    bills: {
        generate: (data) => api.request('/bills', { method: 'POST', body: JSON.stringify(data) }),
        getAll: () => api.request('/bills'),
        getById: (id) => api.request(`/bills/${id}`),
        downloadPdf: (id) => api.request(`/bills/${id}/pdf`, { responseType: 'blob' })
    },

    customers: {
        getAll: () => api.request('/customers'),
        getByMobile: (mobile) => api.request(`/customers/${mobile}`),
        save: (data) => api.request('/customers', { method: 'POST', body: JSON.stringify(data) }),
        delete: (mobile) => api.request(`/customers/${mobile}`, { method: 'DELETE' })
    },

    staff: {
        getAll: () => api.request('/staff'),
        getById: (id) => api.request(`/staff/${id}`),
        create: (data) => api.request('/staff', { method: 'POST', body: JSON.stringify(data) }),
        update: (id, data) => api.request(`/staff/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
        delete: (id) => api.request(`/staff/${id}`, { method: 'DELETE' }),
        resetPassword: (id, newPassword) => api.request(`/staff/${id}/reset-password`, { method: 'PUT', body: JSON.stringify({ newPassword }) })
    },

    reports: {
        getTodayTotal: () => api.request('/reports/today-total'),
        getTodayBySeller: () => api.request('/reports/today-by-seller'),
        getMyTodayTotal: () => api.request('/reports/my-today-total'),
        getMonthlyTotal: () => api.request('/reports/monthly-total'),
        getAllTimeTotal: () => api.request('/reports/all-time-total'),
        getMonthlyBreakdown: () => api.request('/reports/monthly-breakdown')
    }
};
