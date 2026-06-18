const ui = {
    renderDashboard: async () => {
        const viewContent = document.getElementById('view-content');
        viewContent.innerHTML = `
            <div class="dashboard-grid">
                <div class="card">
                    <div class="card-icon icon-blue"><i class="fas fa-shopping-cart"></i></div>
                    <div class="card-info">
                        <h4>Today's Sales</h4>
                        <p id="dash-today-sales">Rs.0.00</p>
                    </div>
                </div>
                <div class="card">
                    <div class="card-icon icon-green"><i class="fas fa-gem"></i></div>
                    <div class="card-info">
                        <h4>Jewelry Designs</h4>
                        <p id="dash-jewelry-count">0</p>
                    </div>
                </div>
                <div class="card">
                    <div class="card-icon icon-purple"><i class="fas fa-users"></i></div>
                    <div class="card-info">
                        <h4>Total Customers</h4>
                        <p id="dash-customer-count">0</p>
                    </div>
                </div>
            </div>
            <div class="table-container">
                <div class="flex-between" style="padding: 1rem;">
                    <h3>Recent Bills</h3>
                    <button class="btn-small btn-view" onclick="app.switchView('history')">View All</button>
                </div>
                <table id="recent-bills-table">
                    <thead>
                        <tr>
                            <th>Bill ID</th>
                            <th>Customer</th>
                            <th>Total</th>
                            <th>Date</th>
                        </tr>
                    </thead>
                    <tbody></tbody>
                </table>
            </div>
        `;

        try {
            const todayStr = new Date().toLocaleDateString();
            const [jewelry, customers, bills] = await Promise.all([
                api.jewelry.getAll(),
                api.customers.getAll(),
                api.bills.getAll()
            ]);

            document.getElementById('dash-jewelry-count').textContent = jewelry.data.length;
            document.getElementById('dash-customer-count').textContent = customers.data.length;

            // Filter for today's bills only
            const todayBills = bills.data.filter(bill => new Date(bill.billDate).toLocaleDateString() === todayStr);
            const recentBills = todayBills.slice(0, 10);
            
            const tbody = document.querySelector('#recent-bills-table tbody');
            tbody.innerHTML = recentBills.length > 0 ? recentBills.map(bill => `
                <tr>
                    <td>#${bill.id}</td>
                    <td>${bill.customer ? bill.customer.name : 'Guest'}</td>
                    <td>Rs.${bill.grandTotal.toFixed(2)}</td>
                    <td>${new Date(bill.billDate).toLocaleTimeString()}</td>
                </tr>
            `).join('') : '<tr><td colspan="4" style="text-align:center;">No bills generated today</td></tr>';

            if (app.user.role === 'ADMIN') {
                const total = await api.reports.getTodayTotal();
                document.getElementById('dash-today-sales').textContent = `Rs.${total.data.toFixed(2)}`;
            } else {
                const myTotal = await api.reports.getMyTodayTotal();
                document.getElementById('dash-today-sales').textContent = `Rs.${myTotal.data.toFixed(2)}`;
            }
        } catch (e) { console.error(e); }
    },

    renderInventory: async () => {
        const viewContent = document.getElementById('view-content');
        const isAdmin = app.user.role === 'ADMIN';
        viewContent.innerHTML = `
            <div class="flex-between">
                <h3>Jewelry Inventory</h3>
                ${isAdmin ? '<button class="btn-add" onclick="ui.showJewelryModal()">+ Add New Design</button>' : ''}
            </div>
            <div class="table-container">
                <table>
                    <thead>
                        <tr>
                            <th>ID</th>
                            <th>Name</th>
                            <th>Type</th>
                            <th>Weight (g)</th>
                            <th>Stock</th>
                            <th>Making %</th>
                            ${isAdmin ? '<th>Actions</th>' : ''}
                        </tr>
                    </thead>
                    <tbody id="inventory-table-body"></tbody>
                </table>
            </div>
        `;

        const res = await api.jewelry.getAll();
        const tbody = document.getElementById('inventory-table-body');
        tbody.innerHTML = res.data.map(item => `
            <tr>
                <td>${item.id}</td>
                <td>${item.name}</td>
                <td><span class="badge">${item.type}</span></td>
                <td>${item.weight}</td>
                <td>${item.stock}</td>
                <td>${item.makingPercent}%</td>
                ${isAdmin ? `
                <td>
                    <button class="btn-small btn-edit" onclick="ui.showJewelryModal(${item.id})">Edit</button>
                    <button class="btn-small btn-add" onclick="ui.showStockModal(${item.id})">+ Stock</button>
                </td>` : ''}
            </tr>
        `).join('');
    },

    renderRates: async () => {
        const viewContent = document.getElementById('view-content');
        const isAdmin = app.user.role === 'ADMIN';
        viewContent.innerHTML = `
            <div class="flex-between">
                <h3>Daily Metal Rates (Per Gram)</h3>
                ${isAdmin ? '<button class="btn-add" onclick="ui.showRateModal()">Update Rates</button>' : ''}
            </div>
            <div class="table-container">
                <table>
                    <thead>
                        <tr>
                            <th>Metal</th>
                            <th>Price / Gram</th>
                            <th>Last Updated</th>
                        </tr>
                    </thead>
                    <tbody id="rates-table-body"></tbody>
                </table>
            </div>
        `;

        const res = await api.rates.getAll();
        const tbody = document.getElementById('rates-table-body');
        tbody.innerHTML = res.data.map(rate => `
            <tr>
                <td><strong>${rate.metalType}</strong></td>
                <td>Rs.${rate.pricePerGram.toFixed(2)}</td>
                <td>${rate.rateDate}</td>
            </tr>
        `).join('');
    },

    renderBilling: async () => {
        const viewContent = document.getElementById('view-content');
        viewContent.innerHTML = `
            <div class="billing-layout">
                <div class="bill-form-container">
                    <div class="card" style="display: block; margin-bottom: 1.5rem;">
                        <h3>Customer Information</h3>
                        <div class="flex-between" style="margin-top: 1rem; gap: 1rem;">
                            <div class="form-group" style="flex: 1;">
                                <label>Mobile Number</label>
                                <input type="text" id="bill-cust-mobile" placeholder="Enter mobile">
                            </div>
                            <button class="btn-primary" style="width: auto; margin-top: 1.5rem;" onclick="app.fetchCustomerForBill()">Find</button>
                        </div>
                        <div id="bill-cust-details" class="hidden">
                            <p><strong>Name:</strong> <span id="bill-cust-name"></span></p>
                            <p><strong>Discount:</strong> <span id="bill-cust-discount"></span>%</p>
                        </div>
                    </div>

                    <div class="card" style="display: block;">
                        <h3>Add Items</h3>
                        <div class="grid" style="display: grid; grid-template-columns: 1fr 100px auto; gap: 1rem; margin-top: 1rem;">
                            <div class="form-group">
                                <label>Select Jewelry</label>
                                <select id="bill-item-select"></select>
                            </div>
                            <div class="form-group">
                                <label>Qty</label>
                                <input type="number" id="bill-item-qty" value="1" min="1">
                            </div>
                            <button class="btn-add" style="margin-top: 1.5rem;" onclick="app.addItemToBill()">Add</button>
                        </div>
                        <table id="bill-items-table" style="margin-top: 1rem;">
                            <thead>
                                <tr>
                                    <th>Item</th>
                                    <th>Qty</th>
                                    <th>Total</th>
                                    <th></th>
                                </tr>
                            </thead>
                            <tbody></tbody>
                        </table>
                    </div>
                </div>

                <div class="bill-summary">
                    <h3>Bill Summary</h3>
                    <div id="summary-content" style="margin-top: 1.5rem;">
                        <div class="summary-item"><span>Subtotal</span><span id="sum-subtotal">Rs.0.00</span></div>
                        <div class="summary-item"><span>Discount</span><span id="sum-discount">Rs.0.00</span></div>
                        <div class="summary-item"><span>GST (3%)</span><span id="sum-gst">Rs.0.00</span></div>
                        <div class="summary-item summary-total"><span>Grand Total</span><span id="sum-grand">Rs.0.00</span></div>
                    </div>
                    <button class="btn-primary" style="margin-top: 2rem; background: var(--success-color);" onclick="app.generateBill()">Generate Bill & Download PDF</button>
                </div>
            </div>
        `;

        const jewelry = await api.jewelry.getAll();
        const select = document.getElementById('bill-item-select');
        select.innerHTML = jewelry.data.map(item => `<option value="${item.id}">${item.name} (${item.type} - ${item.weight}g)</option>`).join('');
    },

    renderHistory: async () => {
        const viewContent = document.getElementById('view-content');
        viewContent.innerHTML = `
            <div class="flex-between">
                <h3>Bill History</h3>
                <div style="display: flex; gap: 10px;">
                    <input type="text" id="bill-search-id" placeholder="Bill ID" style="padding: 5px; border-radius: 4px; border: 1px solid #ddd;">
                    <button class="btn-small btn-edit" onclick="app.searchBill()">Search</button>
                </div>
            </div>
            <div class="table-container" style="margin-top: 1.5rem;">
                <table>
                    <thead>
                        <tr>
                            <th>Bill ID</th>
                            <th>Customer</th>
                            <th>Total</th>
                            <th>Seller</th>
                            <th>Date</th>
                            <th>Action</th>
                        </tr>
                    </thead>
                    <tbody id="history-table-body">
                        <tr><td colspan="6" style="text-align:center;">Loading history...</td></tr>
                    </tbody>
                </table>
            </div>
        `;

        try {
            const res = await api.bills.getAll();
            if (!res.success) throw new Error(res.message);

            const isAdmin = app.user.role === 'ADMIN';
            let bills = res.data || [];
            
            // If STAFF, only show their own bills
            if (!isAdmin) {
                bills = bills.filter(b => b.seller && b.seller.username === app.user.username);
            }

            const tbody = document.getElementById('history-table-body');
            tbody.innerHTML = bills.length > 0 ? bills.map(bill => `
                <tr>
                    <td>#${bill.id}</td>
                    <td>${bill.customer ? bill.customer.name : 'Guest'}</td>
                    <td>Rs.${bill.grandTotal.toFixed(2)}</td>
                    <td>${bill.seller ? bill.seller.username : 'Unknown'}</td>
                    <td>${new Date(bill.billDate).toLocaleString()}</td>
                    <td>
                        <button class="btn-small btn-view" onclick="app.downloadPdf(${bill.id})">Download PDF</button>
                    </td>
                </tr>
            `).join('') : '<tr><td colspan="6" style="text-align:center;">No bills found</td></tr>';
        } catch (e) {
            console.error("Error loading bill history:", e);
            document.getElementById('history-table-body').innerHTML = `<tr><td colspan="6" style="text-align:center; color: red;">Failed to load history: ${e.message}</td></tr>`;
        }
    },

    renderCustomers: async () => {
        const viewContent = document.getElementById('view-content');
        viewContent.innerHTML = `
            <div class="flex-between">
                <h3>Customer Management</h3>
                <button class="btn-add" onclick="ui.showCustomerModal()">+ Add Customer</button>
            </div>
            <div class="table-container">
                <table>
                    <thead>
                        <tr>
                            <th>Mobile</th>
                            <th>Name</th>
                            <th>Discount %</th>
                            <th>Created</th>
                            <th>Actions</th>
                        </tr>
                    </thead>
                    <tbody id="customers-table-body"></tbody>
                </table>
            </div>
        `;

        const res = await api.customers.getAll();
        const tbody = document.getElementById('customers-table-body');
        tbody.innerHTML = res.data.map(c => `
            <tr>
                <td>${c.mobileNumber}</td>
                <td>${c.name}</td>
                <td>${c.discountPercent}%</td>
                <td>${new Date(c.createdAt).toLocaleDateString()}</td>
                <td>
                    <button class="btn-small btn-edit" onclick="ui.showCustomerModal('${c.mobileNumber}')">Edit</button>
                </td>
            </tr>
        `).join('');
    },

    renderReports: async () => {
        const viewContent = document.getElementById('view-content');
        viewContent.innerHTML = `
            <h3>Sales Reports</h3>
            <div class="dashboard-grid" style="margin-top: 1.5rem;">
                 <div class="card" style="display: block;">
                    <h4>Today's Sales</h4>
                    <p id="report-today-total" style="font-size: 1.5rem; color: var(--success-color);">Rs.0.00</p>
                </div>
                <div class="card" style="display: block;">
                    <h4>This Month's Sales</h4>
                    <p id="report-month-total" style="font-size: 1.5rem; color: #3498db;">Rs.0.00</p>
                </div>
                <div class="card" style="display: block;">
                    <h4>Total All-Time Sales</h4>
                    <p id="report-all-total" style="font-size: 1.5rem; color: #9b59b6;">Rs.0.00</p>
                </div>
            </div>
            <div class="table-container">
                <div style="padding: 1rem; border-bottom: 1px solid #eee;">
                    <h3>Sales by Seller (Today)</h3>
                </div>
                <table>
                    <thead>
                        <tr>
                            <th>Seller</th>
                            <th>Revenue</th>
                        </tr>
                    </thead>
                    <tbody id="seller-report-body"></tbody>
                </table>
            </div>

            <div class="table-container" style="margin-top: 2rem;">
                <div style="padding: 1rem; border-bottom: 1px solid #eee;">
                    <h3>Monthly Sales Breakdown</h3>
                </div>
                <table>
                    <thead>
                        <tr>
                            <th>Month</th>
                            <th>Total Revenue</th>
                        </tr>
                    </thead>
                    <tbody id="monthly-breakdown-body"></tbody>
                </table>
            </div>
        `;

        try {
            const [today, month, all, bySeller, monthlyBreakdown] = await Promise.all([
                api.reports.getTodayTotal(),
                api.reports.getMonthlyTotal(),
                api.reports.getAllTimeTotal(),
                api.reports.getTodayBySeller(),
                api.reports.getMonthlyBreakdown()
            ]);

            document.getElementById('report-today-total').textContent = `Rs.${today.data.toFixed(2)}`;
            document.getElementById('report-month-total').textContent = `Rs.${month.data.toFixed(2)}`;
            document.getElementById('report-all-total').textContent = `Rs.${all.data.toFixed(2)}`;

            const tbody = document.getElementById('seller-report-body');
            tbody.innerHTML = Object.entries(bySeller.data).map(([seller, amount]) => `
                <tr>
                    <td>${seller}</td>
                    <td>Rs.${amount.toFixed(2)}</td>
                </tr>
            `).join('');

            const mBody = document.getElementById('monthly-breakdown-body');
            mBody.innerHTML = Object.entries(monthlyBreakdown.data)
                .sort((a, b) => b[0].localeCompare(a[0])) // Sort months descending
                .map(([m, amount]) => `
                <tr>
                    <td>${m}</td>
                    <td>Rs.${amount.toFixed(2)}</td>
                </tr>
            `).join('');
        } catch (e) {
            console.error("Error fetching reports:", e);
            viewContent.innerHTML += `<div class="message error">Failed to load reports. Please ensure you are logged in as Admin.</div>`;
        }
    },

    renderStaff: async () => {
        const viewContent = document.getElementById('view-content');
        viewContent.innerHTML = `
            <div class="flex-between">
                <h3>Manage Staff</h3>
                <div style="display: flex; gap: 10px;">
                    <input type="text" id="staff-search-input" placeholder="Search Name or Mobile" style="padding: 5px; border-radius: 4px; border: 1px solid #ddd;">
                    <button class="btn-small btn-edit" onclick="ui.filterStaff()">Search</button>
                    <button class="btn-add" onclick="ui.showStaffModal()">+ Add Staff</button>
                </div>
            </div>
            <div class="table-container" style="margin-top: 1.5rem;">
                <table>
                    <thead>
                        <tr>
                            <th>Name</th>
                            <th>Mobile</th>
                            <th>Aadhaar</th>
                            <th>Gender</th>
                            <th>Salary</th>
                            <th>Login Access</th>
                            <th>Actions</th>
                        </tr>
                    </thead>
                    <tbody id="staff-table-body">
                        <tr><td colspan="7" style="text-align:center;">Loading staff...</td></tr>
                    </tbody>
                </table>
            </div>
        `;

        try {
            const res = await api.staff.getAll();
            window.allStaff = res.data || [];
            ui.renderStaffTable(window.allStaff);
        } catch (e) {
            console.error("Error loading staff:", e);
        }
    },

    filterStaff: () => {
        const query = document.getElementById('staff-search-input').value.toLowerCase();
        if (!query) {
            ui.renderStaffTable(window.allStaff);
            return;
        }
        const filtered = window.allStaff.filter(s => 
            s.name.toLowerCase().includes(query) || 
            s.mobileNumber.includes(query)
        );
        ui.renderStaffTable(filtered);
    },

    renderStaffTable: (staffList) => {
        const tbody = document.getElementById('staff-table-body');
        tbody.innerHTML = staffList.length > 0 ? staffList.map(s => `
            <tr>
                <td>${s.name}</td>
                <td>${s.mobileNumber}</td>
                <td>${s.aadhaarNumber}</td>
                <td>${s.gender}</td>
                <td>Rs.${s.salary}</td>
                <td>
                    <span class="badge" style="background: ${s.loginAccess ? 'var(--success-color)' : 'var(--error-color)'}">
                        ${s.loginAccess ? 'Yes' : 'No'}
                    </span>
                </td>
                <td>
                    <button class="btn-small btn-edit" onclick="ui.showStaffModal(${s.id})">Edit</button>
                    ${s.loginAccess ? `<button class="btn-small btn-view" onclick="ui.showResetPasswordModal(${s.id})">Reset</button>` : ''}
                </td>
            </tr>
        `).join('') : '<tr><td colspan="7" style="text-align:center;">No staff found</td></tr>';
    },

    showStaffModal: async (id = null) => {
        let s = { name: '', mobileNumber: '', aadhaarNumber: '', gender: 'Male', salary: 0, loginAccess: false, username: '', password: '' };
        if (id) {
            const res = await api.staff.getById(id);
            s = res.data;
        }

        const modalBody = document.getElementById('modal-body');
        modalBody.innerHTML = `
            <h3>${id ? 'Edit Staff' : 'Add New Staff'}</h3>
            <form id="staff-form" style="margin-top: 1rem;">
                <div class="grid" style="display: grid; grid-template-columns: 1fr 1fr; gap: 1rem;">
                    <div class="form-group">
                        <label>Full Name</label>
                        <input type="text" id="s-name" value="${s.name}" required>
                    </div>
                    <div class="form-group">
                        <label>Mobile Number</label>
                        <input type="text" id="s-mobile" value="${s.mobileNumber}" required>
                    </div>
                    <div class="form-group">
                        <label>Aadhaar Number</label>
                        <input type="text" id="s-aadhaar" value="${s.aadhaarNumber}" required>
                    </div>
                    <div class="form-group">
                        <label>Gender</label>
                        <select id="s-gender">
                            <option value="Male" ${s.gender === 'Male' ? 'selected' : ''}>Male</option>
                            <option value="Female" ${s.gender === 'Female' ? 'selected' : ''}>Female</option>
                            <option value="Other" ${s.gender === 'Other' ? 'selected' : ''}>Other</option>
                        </select>
                    </div>
                    <div class="form-group">
                        <label>Salary (Rs)</label>
                        <input type="number" id="s-salary" value="${s.salary}" required>
                    </div>
                    <div class="form-group">
                        <label>Login Access</label>
                        <select id="s-login">
                            <option value="false" ${!s.loginAccess ? 'selected' : ''}>No</option>
                            <option value="true" ${s.loginAccess ? 'selected' : ''}>Yes</option>
                        </select>
                    </div>
                </div>
                
                <div id="login-fields" class="${s.loginAccess ? '' : 'hidden'}" style="margin-top: 1rem; border-top: 1px dashed #ddd; padding-top: 1rem;">
                    <h4>Login Credentials</h4>
                    <div class="form-group" style="margin-top: 0.5rem;">
                        <label>Username</label>
                        <input type="text" id="s-username" value="${s.username || ''}">
                    </div>
                    ${!id ? `
                    <div class="form-group">
                        <label>Password</label>
                        <input type="password" id="s-password">
                    </div>` : ''}
                </div>

                <button type="submit" class="btn-primary" style="margin-top: 1.5rem;">${id ? 'Update Staff' : 'Save Staff'}</button>
            </form>
        `;

        document.getElementById('modal-container').classList.remove('hidden');

        // Toggle login fields
        document.getElementById('s-login').addEventListener('change', function() {
            const loginFields = document.getElementById('login-fields');
            if (this.value === 'true') {
                loginFields.classList.remove('hidden');
                document.getElementById('s-username').setAttribute('required', 'true');
                if(!id) document.getElementById('s-password').setAttribute('required', 'true');
            } else {
                loginFields.classList.add('hidden');
                document.getElementById('s-username').removeAttribute('required');
                if(!id) document.getElementById('s-password').removeAttribute('required');
            }
        });

        document.getElementById('staff-form').onsubmit = async (e) => {
            e.preventDefault();
            const loginAccess = document.getElementById('s-login').value === 'true';
            const data = {
                name: document.getElementById('s-name').value,
                mobileNumber: document.getElementById('s-mobile').value,
                aadhaarNumber: document.getElementById('s-aadhaar').value,
                gender: document.getElementById('s-gender').value,
                salary: parseFloat(document.getElementById('s-salary').value),
                loginAccess: loginAccess,
                username: loginAccess ? document.getElementById('s-username').value : null,
                password: loginAccess && !id ? document.getElementById('s-password').value : null
            };

            try {
                let res;
                if (id) res = await api.staff.update(id, data);
                else res = await api.staff.create(data);
                
                if (res.success) {
                    document.getElementById('modal-container').classList.add('hidden');
                    ui.renderStaff();
                } else {
                    alert(res.message || "Error saving staff details");
                }
            } catch (err) {
                console.error(err);
                alert("Error saving staff details: " + err.message);
            }
        };
    },

    showResetPasswordModal: (id) => {
        const modalBody = document.getElementById('modal-body');
        modalBody.innerHTML = `
            <h3>Reset Staff Password</h3>
            <form id="reset-password-form" style="margin-top: 1rem;">
                <div class="form-group">
                    <label>New Password</label>
                    <input type="password" id="new-password" required>
                </div>
                <button type="submit" class="btn-primary">Reset Password</button>
            </form>
        `;
        document.getElementById('modal-container').classList.remove('hidden');
        document.getElementById('reset-password-form').onsubmit = async (e) => {
            e.preventDefault();
            const newPassword = document.getElementById('new-password').value;
            try {
                await api.staff.resetPassword(id, newPassword);
                alert("Password reset successfully");
                document.getElementById('modal-container').classList.add('hidden');
            } catch (err) {
                alert("Failed to reset password");
            }
        };
    },

    showJewelryModal: async (id = null) => {
        let item = { name: '', type: 'GOLD', weight: 0, stock: 0, makingPercent: 0, companyName: '' };
        if (id) {
            const res = await api.jewelry.getById(id);
            item = res.data;
        }

        const modalBody = document.getElementById('modal-body');
        modalBody.innerHTML = `
            <h3>${id ? 'Edit Jewelry Design' : 'Add New Jewelry Design'}</h3>
            <form id="jewelry-form" style="margin-top: 1rem;">
                <div class="form-group">
                    <label>Name</label>
                    <input type="text" id="j-name" value="${item.name}" required>
                </div>
                <div class="form-group">
                    <label>Metal Type</label>
                    <select id="j-type">
                        <option value="GOLD" ${item.type === 'GOLD' ? 'selected' : ''}>GOLD</option>
                        <option value="SILVER" ${item.type === 'SILVER' ? 'selected' : ''}>SILVER</option>
                    </select>
                </div>
                <div class="form-group">
                    <label>Weight (grams)</label>
                    <input type="number" step="0.001" id="j-weight" value="${item.weight}" required>
                </div>
                <div class="form-group">
                    <label>Stock</label>
                    <input type="number" id="j-stock" value="${item.stock}" required>
                </div>
                <div class="form-group">
                    <label>Making Percent (%)</label>
                    <input type="number" step="0.1" id="j-making" value="${item.makingPercent}" required>
                </div>
                <div class="form-group">
                    <label>Company Name</label>
                    <input type="text" id="j-company" value="${item.companyName || ''}">
                </div>
                <button type="submit" class="btn-primary">${id ? 'Update' : 'Save'}</button>
            </form>
        `;

        document.getElementById('modal-container').classList.remove('hidden');
        document.getElementById('jewelry-form').onsubmit = async (e) => {
            e.preventDefault();
            const data = {
                name: document.getElementById('j-name').value,
                type: document.getElementById('j-type').value,
                weight: parseFloat(document.getElementById('j-weight').value),
                stock: parseInt(document.getElementById('j-stock').value),
                makingPercent: parseFloat(document.getElementById('j-making').value),
                companyName: document.getElementById('j-company').value
            };
            if (id) await api.jewelry.update(id, data);
            else await api.jewelry.add(data);
            document.getElementById('modal-container').classList.add('hidden');
            ui.renderInventory();
        };
    },

    showStockModal: (id) => {
        const modalBody = document.getElementById('modal-body');
        modalBody.innerHTML = `
            <h3>Add Stock</h3>
            <form id="stock-form" style="margin-top: 1rem;">
                <div class="form-group">
                    <label>Quantity to add</label>
                    <input type="number" id="s-qty" value="1" min="1" required>
                </div>
                <button type="submit" class="btn-primary">Add Stock</button>
            </form>
        `;
        document.getElementById('modal-container').classList.remove('hidden');
        document.getElementById('stock-form').onsubmit = async (e) => {
            e.preventDefault();
            const qty = document.getElementById('s-qty').value;
            await api.jewelry.addStock(id, qty);
            document.getElementById('modal-container').classList.add('hidden');
            ui.renderInventory();
        };
    },

    showRateModal: () => {
        const modalBody = document.getElementById('modal-body');
        modalBody.innerHTML = `
            <h3>Update Today's Rates</h3>
            <form id="rate-form" style="margin-top: 1rem;">
                <div class="form-group">
                    <label>Metal Type</label>
                    <select id="r-type">
                        <option value="GOLD">GOLD</option>
                        <option value="SILVER">SILVER</option>
                    </select>
                </div>
                <div class="form-group">
                    <label>Price Per Gram</label>
                    <input type="number" step="0.01" id="r-price" required>
                </div>
                <div class="form-group">
                    <label>Date</label>
                    <input type="date" id="r-date" value="${new Date().toISOString().split('T')[0]}" required>
                </div>
                <button type="submit" class="btn-primary">Update Rate</button>
            </form>
        `;
        document.getElementById('modal-container').classList.remove('hidden');
        document.getElementById('rate-form').onsubmit = async (e) => {
            e.preventDefault();
            const data = {
                metalType: document.getElementById('r-type').value,
                pricePerGram: parseFloat(document.getElementById('r-price').value),
                rateDate: document.getElementById('r-date').value
            };
            await api.rates.update(data);
            document.getElementById('modal-container').classList.add('hidden');
            ui.renderRates();
            app.updateRatesBar();
        };
    },

    showCustomerModal: async (mobile = null) => {
        let c = { name: '', mobileNumber: '', discountPercent: 0 };
        if (mobile) {
            const res = await api.customers.getByMobile(mobile);
            c = res.data;
        }

        const modalBody = document.getElementById('modal-body');
        modalBody.innerHTML = `
            <h3>${mobile ? 'Edit Customer' : 'Add New Customer'}</h3>
            <form id="customer-form" style="margin-top: 1rem;">
                <div class="form-group">
                    <label>Mobile Number</label>
                    <input type="text" id="c-mobile" value="${c.mobileNumber}" ${mobile ? 'disabled' : ''} required>
                </div>
                <div class="form-group">
                    <label>Name</label>
                    <input type="text" id="c-name" value="${c.name}" required>
                </div>
                <div class="form-group">
                    <label>Discount Percent (%)</label>
                    <input type="number" step="0.1" id="c-discount" value="${c.discountPercent}" required>
                </div>
                <button type="submit" class="btn-primary">Save Customer</button>
            </form>
        `;
        document.getElementById('modal-container').classList.remove('hidden');
        document.getElementById('customer-form').onsubmit = async (e) => {
            e.preventDefault();
            const data = {
                mobileNumber: document.getElementById('c-mobile').value,
                name: document.getElementById('c-name').value,
                discountPercent: parseFloat(document.getElementById('c-discount').value)
            };
            await api.customers.save(data);
            document.getElementById('modal-container').classList.add('hidden');
            ui.renderCustomers();
        };
    }
};
