const app = {
    user: null,
    currentView: 'dashboard',
    billItems: [],
    billCustomer: null,

    init: () => {
        app.checkAuth();
        app.bindEvents();
    },

    checkAuth: () => {
        // Force login page on launch/restart
        app.logout();
    },

    bindEvents: () => {
        document.getElementById('login-form').addEventListener('submit', app.handleLogin);
        document.getElementById('logout-btn').addEventListener('click', app.logout);
        
        // Password toggle logic
        const togglePassword = document.getElementById('togglePassword');
        const passwordInput = document.getElementById('password');
        
        togglePassword.addEventListener('click', function() {
            const type = passwordInput.getAttribute('type') === 'password' ? 'text' : 'password';
            passwordInput.setAttribute('type', type);
            this.classList.toggle('fa-eye');
            this.classList.toggle('fa-eye-slash');
        });

        document.querySelectorAll('.sidebar-nav li').forEach(li => {
            li.addEventListener('click', () => {
                const view = li.getAttribute('data-view');
                if (view) app.switchView(view);
            });
        });

        document.querySelector('.close-modal').onclick = () => {
            document.getElementById('modal-container').classList.add('hidden');
        };

        window.onclick = (event) => {
            if (event.target == document.getElementById('modal-container')) {
                document.getElementById('modal-container').classList.add('hidden');
            }
        };
    },

    handleLogin: async (e) => {
        e.preventDefault();
        const username = document.getElementById('username').value;
        const password = document.getElementById('password').value;
        const msg = document.getElementById('login-message');

        try {
            const res = await api.auth.login({ username, password });
            if (res.success) {
                app.user = res.data;
                const token = btoa(`${username}:${password}`);
                localStorage.setItem('auth_token', token);
                localStorage.setItem('user', JSON.stringify(app.user));
                app.showApp();
            } else {
                msg.textContent = res.message;
                msg.className = 'message error';
            }
        } catch (err) {
            msg.textContent = 'Invalid credentials or server error';
            msg.className = 'message error';
        }
    },

    showLogin: () => {
        document.getElementById('login-container').classList.remove('hidden');
        document.getElementById('app-container').classList.add('hidden');
    },

    showApp: () => {
        document.getElementById('login-container').classList.add('hidden');
        document.getElementById('app-container').classList.remove('hidden');
        document.getElementById('user-display-name').textContent = app.user.username;
        document.getElementById('user-role').textContent = app.user.role;
        
        app.generateSidebar();
        app.updateRatesBar().then(() => {
            if (app.user.role === 'ADMIN') {
                app.checkDailyRates();
            }
        });
        app.switchView('dashboard');
    },

    checkDailyRates: async () => {
        try {
            const types = ['GOLD', 'SILVER'];
            const ratePromises = types.map(t => api.rates.getToday(t).catch(() => null));
            const rates = await Promise.all(ratePromises);
            
            const missingRates = types.filter((type, i) => !rates[i] || !rates[i].data);
            
            if (missingRates.length > 0) {
                // If the warning doesn't exist, create it
                if (!document.getElementById('rate-warning-banner')) {
                    const header = document.querySelector('.content-header');
                    const warningDiv = document.createElement('div');
                    warningDiv.id = 'rate-warning-banner';
                    warningDiv.className = 'message error';
                    warningDiv.style.position = 'absolute';
                    warningDiv.style.top = '80px';
                    warningDiv.style.left = '50%';
                    warningDiv.style.transform = 'translateX(-50%)';
                    warningDiv.style.zIndex = '1000';
                    warningDiv.style.width = '80%';
                    warningDiv.style.textAlign = 'center';
                    warningDiv.style.boxShadow = '0 4px 6px rgba(0,0,0,0.1)';
                    warningDiv.innerHTML = `<i class="fas fa-exclamation-triangle"></i> <strong>Action Required:</strong> Please update the daily rates for ${missingRates.join(' and ')} before proceeding with billing.`;
                    
                    document.querySelector('.content').insertBefore(warningDiv, document.getElementById('view-content'));
                }
            } else {
                // Remove warning if rates are updated
                const warningBanner = document.getElementById('rate-warning-banner');
                if (warningBanner) {
                    warningBanner.remove();
                }
            }
        } catch (e) {
            console.error("Failed to check daily rates:", e);
        }
    },

    generateSidebar: () => {
        const menu = document.getElementById('sidebar-menu');
        const isAdmin = app.user.role === 'ADMIN';
        
        let html = `
            <li class="active" data-view="dashboard"><i class="fas fa-chart-line"></i> Dashboard</li>
        `;

        if (isAdmin) {
            html += `
                <li data-view="staff"><i class="fas fa-user-tie"></i> Manage Staff</li>
                <li data-view="inventory"><i class="fas fa-gem"></i> Jewelry Designs</li>
                <li data-view="rates"><i class="fas fa-coins"></i> Metal Rates</li>
                <li data-view="reports"><i class="fas fa-chart-pie"></i> Sales Reports</li>
                <li data-view="history"><i class="fas fa-history"></i> Bill History</li>
                <li data-view="customers"><i class="fas fa-users"></i> Manage Customers</li>
            `;
        } else {
            html += `
                <li data-view="billing"><i class="fas fa-file-invoice-dollar"></i> Create Bill</li>
                <li data-view="inventory"><i class="fas fa-boxes"></i> View Stock</li>
                <li data-view="history"><i class="fas fa-history"></i> My Bills</li>
            `;
        }

        menu.innerHTML = html;

        // Re-bind events to new sidebar items
        menu.querySelectorAll('li').forEach(li => {
            li.addEventListener('click', () => {
                const view = li.getAttribute('data-view');
                const action = li.getAttribute('data-action');
                if (view) app.switchView(view);
                if (action === 'add-staff') ui.showStaffModal();
            });
        });
    },

    searchBill: async () => {
        const id = document.getElementById('bill-search-id').value;
        if (!id) {
            ui.renderHistory();
            return;
        }
        try {
            const res = await api.bills.getById(id);
            const tbody = document.getElementById('history-table-body');
            if (res.success && res.data) {
                const bill = res.data;
                tbody.innerHTML = `
                    <tr>
                        <td>#${bill.id}</td>
                        <td>${bill.customer ? bill.customer.name : 'Guest'}</td>
                        <td>Rs.${bill.grandTotal.toFixed(2)}</td>
                        <td>${bill.seller.username}</td>
                        <td>${new Date(bill.billDate).toLocaleString()}</td>
                        <td>
                            <button class="btn-small btn-view" onclick="app.downloadPdf(${bill.id})">Download PDF</button>
                        </td>
                    </tr>
                `;
            } else {
                tbody.innerHTML = '<tr><td colspan="6" style="text-align:center;">Bill not found</td></tr>';
            }
        } catch (e) {
            alert('Bill not found');
        }
    },

    logout: () => {
        localStorage.removeItem('auth_token');
        localStorage.removeItem('user');
        app.user = null;
        
        // Clear login form inputs
        const usernameInput = document.getElementById('username');
        const passwordInput = document.getElementById('password');
        if (usernameInput) usernameInput.value = '';
        if (passwordInput) {
            passwordInput.value = '';
            passwordInput.setAttribute('type', 'password'); // Reset to password type
        }
        
        // Reset toggle icon if it exists
        const togglePassword = document.getElementById('togglePassword');
        if (togglePassword) {
            togglePassword.classList.remove('fa-eye');
            togglePassword.classList.add('fa-eye-slash');
        }

        app.showLogin();
    },

    switchView: (view) => {
        app.currentView = view;
        document.querySelectorAll('.sidebar-nav li').forEach(li => {
            li.classList.remove('active');
            if (li.getAttribute('data-view') === view) li.classList.add('active');
        });

        document.getElementById('view-title').textContent = view.charAt(0).toUpperCase() + view.slice(1);
        
        switch(view) {
            case 'dashboard': ui.renderDashboard(); break;
            case 'inventory': ui.renderInventory(); break;
            case 'rates': ui.renderRates(); break;
            case 'billing': app.resetBill(); ui.renderBilling(); break;
            case 'history': ui.renderHistory(); break;
            case 'customers': ui.renderCustomers(); break;
            case 'reports': ui.renderReports(); break;
            case 'staff': ui.renderStaff(); break;
        }
    },

    updateRatesBar: async () => {
        try {
            const bar = document.getElementById('today-rates-bar');
            const types = ['GOLD', 'SILVER'];
            const ratePromises = types.map(t => api.rates.getToday(t).catch(() => null));
            const rates = await Promise.all(ratePromises);

            bar.innerHTML = types.map((type, i) => {
                const rate = rates[i];
                return `<div class="rate-item">${type}: <strong>${rate ? 'Rs.' + rate.data.pricePerGram : 'Not set'}</strong></div>`;
            }).join('');
        } catch (e) { console.error(e); }
    },

    // Billing Logic
    resetBill: () => {
        app.billItems = [];
        app.billCustomer = null;
    },

    fetchCustomerForBill: async () => {
        const mobile = document.getElementById('bill-cust-mobile').value;
        if (!mobile) return;
        try {
            const res = await api.customers.getByMobile(mobile);
            if (res.success) {
                app.billCustomer = res.data;
                document.getElementById('bill-cust-details').classList.remove('hidden');
                document.getElementById('bill-cust-name').textContent = res.data.name;
                document.getElementById('bill-cust-discount').textContent = res.data.discountPercent;
                app.calculateBill();
            } else {
                alert('Customer not found');
            }
        } catch (e) { alert('Customer not found'); }
    },

    addItemToBill: async () => {
        const id = document.getElementById('bill-item-select').value;
        const qty = parseInt(document.getElementById('bill-item-qty').value);
        
        const res = await api.jewelry.getById(id);
        const item = res.data;

        if (item.stock < qty) {
            alert('Insufficient stock!');
            return;
        }

        // Fetch current rate
        try {
            const rateRes = await api.rates.getToday(item.type);
            const rate = rateRes.data.pricePerGram;
            
            const baseAmount = item.weight * rate * qty;
            const makingCharge = baseAmount * (item.makingPercent / 100);
            const total = baseAmount + makingCharge;

            app.billItems.push({
                jewelryId: item.id,
                name: item.name,
                quantity: qty,
                total: total
            });

            app.renderBillTable();
            app.calculateBill();
        } catch (e) {
            alert('Rate not set for ' + item.type + ' today!');
        }
    },

    renderBillTable: () => {
        const tbody = document.querySelector('#bill-items-table tbody');
        tbody.innerHTML = app.billItems.map((item, index) => `
            <tr>
                <td>${item.name}</td>
                <td>${item.quantity}</td>
                <td>Rs.${item.total.toFixed(2)}</td>
                <td><button onclick="app.removeItemFromBill(${index})">&times;</button></td>
            </tr>
        `).join('');
    },

    removeItemFromBill: (index) => {
        app.billItems.splice(index, 1);
        app.renderBillTable();
        app.calculateBill();
    },

    calculateBill: () => {
        const subtotal = app.billItems.reduce((sum, item) => sum + item.total, 0);
        const discountPercent = app.billCustomer ? app.billCustomer.discountPercent : 0;
        const discountAmount = subtotal * (discountPercent / 100);
        const taxable = subtotal - discountAmount;
        const gst = taxable * 0.03;
        const grand = taxable + gst;

        document.getElementById('sum-subtotal').textContent = `Rs.${subtotal.toFixed(2)}`;
        document.getElementById('sum-discount').textContent = `Rs.${discountAmount.toFixed(2)}`;
        document.getElementById('sum-gst').textContent = `Rs.${gst.toFixed(2)}`;
        document.getElementById('sum-grand').textContent = `Rs.${grand.toFixed(2)}`;
    },

    generateBill: async () => {
        if (app.billItems.length === 0) {
            alert('Add items to bill first');
            return;
        }

        const data = {
            customerMobile: app.billCustomer ? app.billCustomer.mobileNumber : null,
            items: app.billItems.map(i => ({ jewelryId: i.jewelryId, quantity: i.quantity }))
        };

        try {
            const res = await api.bills.generate(data);
            if (res.success) {
                alert('Bill generated successfully!');
                app.downloadPdf(res.data.id);
                app.switchView('history');
            } else {
                alert('Error: ' + res.message);
            }
        } catch (e) {
            alert('Failed to generate bill: ' + e.message);
        }
    },

    downloadPdf: async (id) => {
        try {
            const blob = await api.bills.downloadPdf(id);
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = `bill_${id}.pdf`;
            document.body.appendChild(a);
            a.click();
            window.URL.revokeObjectURL(url);
            document.body.removeChild(a);
        } catch (e) {
            alert('Failed to download PDF');
        }
    }
};

app.init();
