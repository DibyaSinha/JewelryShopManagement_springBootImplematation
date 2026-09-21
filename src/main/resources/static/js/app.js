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

        const userInfo = document.getElementById('user-info');
        if (userInfo) {
            userInfo.setAttribute('data-initial', (app.user.username || '?').charAt(0).toUpperCase());
        }

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
                if (!document.getElementById('rate-warning-banner')) {
                    const warningDiv = document.createElement('div');
                    warningDiv.id = 'rate-warning-banner';
                    warningDiv.className = 'alert-banner';
                    warningDiv.innerHTML = `<i class="fas fa-exclamation-triangle"></i> <span><strong>Action required:</strong> update the daily rates for ${missingRates.join(' and ')} before proceeding with billing.</span>`;

                    document.querySelector('.content').insertBefore(warningDiv, document.getElementById('view-content'));
                }
            } else {
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
            <li class="active" data-view="dashboard"><i class="fas fa-chart-line"></i> <span>Dashboard</span></li>
        `;

        if (isAdmin) {
            html += `
                <li data-view="staff"><i class="fas fa-user-tie"></i> <span>Manage Staff</span></li>
                <li data-view="inventory"><i class="fas fa-gem"></i> <span>Jewelry Designs</span></li>
                <li data-view="rates"><i class="fas fa-coins"></i> <span>Metal Rates</span></li>
                <li data-view="reports"><i class="fas fa-chart-pie"></i> <span>Sales Reports</span></li>
                <li data-view="history"><i class="fas fa-history"></i> <span>Bill History</span></li>
                <li data-view="customers"><i class="fas fa-users"></i> <span>Manage Customers</span></li>
            `;
        } else {
            html += `
                <li data-view="billing"><i class="fas fa-file-invoice-dollar"></i> <span>Create Bill</span></li>
                <li data-view="inventory"><i class="fas fa-boxes"></i> <span>View Stock</span></li>
                <li data-view="history"><i class="fas fa-history"></i> <span>My Bills</span></li>
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
                            <button class="btn-small btn-view" onclick="app.downloadPdf(${bill.id})"><i class="fas fa-download"></i> PDF</button>
                        </td>
                    </tr>
                `;
            } else {
                tbody.innerHTML = '<tr><td colspan="6" class="table-empty">Bill not found</td></tr>';
            }
        } catch (e) {
            alert('Bill not found');
        }
    },

    logout: () => {
        fetch('/api/auth/logout', { method: 'POST', credentials: 'same-origin' }).catch(() => {});
        localStorage.removeItem('auth_token');
        localStorage.removeItem('user');
        app.user = null;

        const usernameInput = document.getElementById('username');
        const passwordInput = document.getElementById('password');
        if (usernameInput) usernameInput.value = '';
        if (passwordInput) {
            passwordInput.value = '';
            passwordInput.setAttribute('type', 'password');
        }

        const togglePassword = document.getElementById('togglePassword');
        if (togglePassword) {
            togglePassword.classList.remove('fa-eye');
            togglePassword.classList.add('fa-eye-slash');
        }

        const warningBanner = document.getElementById('rate-warning-banner');
        if (warningBanner) warningBanner.remove();

        app.showLogin();
    },

    switchView: (view) => {
        app.currentView = view;
        document.querySelectorAll('.sidebar-nav li').forEach(li => {
            li.classList.remove('active');
            if (li.getAttribute('data-view') === view) li.classList.add('active');
        });

        document.getElementById('view-title').textContent = view.charAt(0).toUpperCase() + view.slice(1);

        switch (view) {
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
        app.searchedMobile = null;
    },

    clearCustomerSelection: () => {
        app.billCustomer = null;
        app.searchedMobile = null;
        const mobileInput = document.getElementById('bill-cust-mobile');
        if (mobileInput) {
            mobileInput.value = '';
            mobileInput.disabled = false;
            mobileInput.focus();
        }
        const existingBox = document.getElementById('bill-cust-existing-box');
        if (existingBox) existingBox.classList.add('hidden');
        const newBox = document.getElementById('bill-cust-new-box');
        if (newBox) newBox.classList.add('hidden');
        const newNameInput = document.getElementById('bill-new-cust-name');
        if (newNameInput) newNameInput.value = '';
        const msg = document.getElementById('bill-cust-save-msg');
        if (msg) msg.classList.add('hidden');
        app.calculateBill();
    },

    fetchCustomerForBill: async () => {
        const mobileInput = document.getElementById('bill-cust-mobile');
        const mobile = mobileInput ? mobileInput.value.trim() : '';
        if (!mobile) {
            alert('Please enter a mobile number');
            if (mobileInput) mobileInput.focus();
            return;
        }

        app.searchedMobile = mobile;
        const existingBox = document.getElementById('bill-cust-existing-box');
        const newBox = document.getElementById('bill-cust-new-box');
        const msg = document.getElementById('bill-cust-save-msg');
        if (msg) msg.classList.add('hidden');

        try {
            const res = await api.customers.getByMobile(mobile);
            if (res.success && res.data) {
                // Existing customer found
                app.billCustomer = res.data;
                if (newBox) newBox.classList.add('hidden');
                if (existingBox) {
                    existingBox.classList.remove('hidden');
                    document.getElementById('bill-cust-display-mobile').textContent = res.data.mobileNumber;
                    document.getElementById('bill-cust-display-name').textContent = res.data.name;
                    const discountRow = document.getElementById('bill-cust-display-discount-row');
                    if (discountRow) {
                        if (res.data.discountPercent && res.data.discountPercent > 0) {
                            discountRow.classList.remove('hidden');
                            document.getElementById('bill-cust-display-discount').textContent = res.data.discountPercent;
                        } else {
                            discountRow.classList.add('hidden');
                        }
                    }
                }
                app.calculateBill();
            } else {
                // Customer not found -> prompt for new customer
                app.showNewCustomerPrompt(mobile);
            }
        } catch (e) {
            // Customer not found or 404 response
            app.showNewCustomerPrompt(mobile);
        }
    },

    showNewCustomerPrompt: (mobile) => {
        app.billCustomer = null;
        const existingBox = document.getElementById('bill-cust-existing-box');
        if (existingBox) existingBox.classList.add('hidden');

        const newBox = document.getElementById('bill-cust-new-box');
        if (newBox) {
            newBox.classList.remove('hidden');
            document.getElementById('bill-cust-new-mobile-display').textContent = mobile;
            const nameInput = document.getElementById('bill-new-cust-name');
            if (nameInput) {
                nameInput.value = '';
                nameInput.focus();
                nameInput.onkeydown = (e) => {
                    if (e.key === 'Enter') {
                        e.preventDefault();
                        app.saveNewCustomer();
                    }
                };
            }
        }
        app.calculateBill();
    },

    saveNewCustomer: async () => {
        const mobile = app.searchedMobile || (document.getElementById('bill-cust-mobile') ? document.getElementById('bill-cust-mobile').value.trim() : '');
        const nameInput = document.getElementById('bill-new-cust-name');
        const name = nameInput ? nameInput.value.trim() : '';

        if (!mobile) {
            alert('Please enter a mobile number first');
            return false;
        }

        if (!name) {
            alert('Please enter the customer name');
            if (nameInput) nameInput.focus();
            return false;
        }

        try {
            const customerData = {
                mobileNumber: mobile,
                name: name,
                discountPercent: 0.0
            };
            const res = await api.customers.save(customerData);
            if (res.success && res.data) {
                app.billCustomer = res.data;
                const newBox = document.getElementById('bill-cust-new-box');
                if (newBox) newBox.classList.add('hidden');

                const existingBox = document.getElementById('bill-cust-existing-box');
                if (existingBox) {
                    existingBox.classList.remove('hidden');
                    document.getElementById('bill-cust-display-mobile').textContent = res.data.mobileNumber;
                    document.getElementById('bill-cust-display-name').textContent = res.data.name;
                    const discountRow = document.getElementById('bill-cust-display-discount-row');
                    if (discountRow) discountRow.classList.add('hidden');
                }
                app.calculateBill();
                return true;
            } else {
                alert(res.message || 'Failed to save customer');
                return false;
            }
        } catch (e) {
            alert('Error saving customer: ' + e.message);
            return false;
        }
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
        tbody.innerHTML = app.billItems.length > 0 ? app.billItems.map((item, index) => `
            <tr>
                <td>${item.name}</td>
                <td>${item.quantity}</td>
                <td>Rs.${item.total.toFixed(2)}</td>
                <td><button class="btn-small btn-delete" onclick="app.removeItemFromBill(${index})">&times;</button></td>
            </tr>
        `).join('') : '<tr><td colspan="4" class="table-empty">No items added yet</td></tr>';
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

        const subEl = document.getElementById('sum-subtotal');
        if (subEl) subEl.textContent = `Rs.${subtotal.toFixed(2)}`;
        const discEl = document.getElementById('sum-discount');
        if (discEl) discEl.textContent = `Rs.${discountAmount.toFixed(2)}`;
        const gstEl = document.getElementById('sum-gst');
        if (gstEl) gstEl.textContent = `Rs.${gst.toFixed(2)}`;
        const grandEl = document.getElementById('sum-grand');
        if (grandEl) grandEl.textContent = `Rs.${grand.toFixed(2)}`;
    },

    generateBill: async () => {
        if (app.billItems.length === 0) {
            alert('Add items to bill first');
            return;
        }

        // If new customer form is open and customer not yet saved:
        const newBox = document.getElementById('bill-cust-new-box');
        if (newBox && !newBox.classList.contains('hidden') && !app.billCustomer) {
            const nameInput = document.getElementById('bill-new-cust-name');
            const name = nameInput ? nameInput.value.trim() : '';
            if (!name) {
                alert('Customer name is required for new customer');
                if (nameInput) nameInput.focus();
                return;
            }
            const saved = await app.saveNewCustomer();
            if (!saved) return;
        }

        // If user entered a mobile number but never clicked Find:
        const mobileInput = document.getElementById('bill-cust-mobile');
        const enteredMobile = mobileInput ? mobileInput.value.trim() : '';
        if (enteredMobile && !app.billCustomer) {
            await app.fetchCustomerForBill();
            if (!app.billCustomer) {
                const nameInput = document.getElementById('bill-new-cust-name');
                if (!nameInput || !nameInput.value.trim()) {
                    alert('Customer name is required for new customer');
                    if (nameInput) nameInput.focus();
                    return;
                }
                const saved = await app.saveNewCustomer();
                if (!saved) return;
            }
        }

        const data = {
            customerMobile: app.billCustomer ? app.billCustomer.mobileNumber : null,
            customerName: app.billCustomer ? app.billCustomer.name : null,
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