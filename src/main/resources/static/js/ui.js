const ui = {
    renderDashboard: async () => {
        const viewContent = document.getElementById('view-content');
        viewContent.innerHTML = `
            <div class="dashboard-grid">
                <div class="stat-card">
                    <div class="stat-icon icon-blue"><i class="fas fa-shopping-cart"></i></div>
                    <div class="stat-info">
                        <h4>Today's Sales</h4>
                        <p id="dash-today-sales">Rs.0.00</p>
                    </div>
                </div>
                <div class="stat-card">
                    <div class="stat-icon icon-green"><i class="fas fa-gem"></i></div>
                    <div class="stat-info">
                        <h4>Jewelry Designs</h4>
                        <p id="dash-jewelry-count">0</p>
                    </div>
                </div>
                <div class="stat-card">
                    <div class="stat-icon icon-purple"><i class="fas fa-users"></i></div>
                    <div class="stat-info">
                        <h4>Total Customers</h4>
                        <p id="dash-customer-count">0</p>
                    </div>
                </div>
            </div>
            <div class="table-container">
                <div class="table-container-header">
                    <h3>Recent Bills</h3>
                    <button class="btn-small btn-view" onclick="app.switchView('history')"><i class="fas fa-arrow-right"></i> View all</button>
                </div>
                <table id="recent-bills-table">
                    <thead>
                        <tr>
                            <th>Bill</th>
                            <th>Customer</th>
                            <th>Total</th>
                            <th>Time</th>
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
            `).join('') : '<tr><td colspan="4" class="table-empty">No bills generated today</td></tr>';

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
                ${isAdmin ? '<button class="btn-add" onclick="ui.showJewelryModal()"><i class="fas fa-plus"></i> Add design</button>' : ''}
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
        tbody.innerHTML = res.data.length > 0 ? res.data.map(item => `
            <tr>
                <td>${item.id}</td>
                <td>${item.name}</td>
                <td><span class="badge">${item.type}</span></td>
                <td>${item.weight}</td>
                <td>${item.stock}</td>
                <td>${item.makingPercent}%</td>
                ${isAdmin ? `
                <td>
                    <button class="btn-small btn-edit" onclick="ui.showJewelryModal(${item.id})"><i class="fas fa-pen"></i> Edit</button>
                    <button class="btn-small btn-view" onclick="ui.showStockModal(${item.id})"><i class="fas fa-plus"></i> Stock</button>
                </td>` : ''}
            </tr>
        `).join('') : `<tr><td colspan="${isAdmin ? 7 : 6}" class="table-empty">No jewelry designs yet</td></tr>`;
    },

    renderRates: async () => {
        const viewContent = document.getElementById('view-content');
        const isAdmin = app.user.role === 'ADMIN';
        viewContent.innerHTML = `
            <div class="flex-between">
                <h3>Daily Metal Rates (per gram)</h3>
                ${isAdmin ? '<button class="btn-add" onclick="ui.showRateModal()"><i class="fas fa-sync"></i> Update rates</button>' : ''}
            </div>
            <div class="table-container">
                <table>
                    <thead>
                        <tr>
                            <th>Metal</th>
                            <th>Price / gram</th>
                            <th>Last updated</th>
                        </tr>
                    </thead>
                    <tbody id="rates-table-body"></tbody>
                </table>
            </div>
        `;

        const res = await api.rates.getAll();
        const tbody = document.getElementById('rates-table-body');
        tbody.innerHTML = res.data.length > 0 ? res.data.map(rate => `
            <tr>
                <td><strong>${rate.metalType}</strong></td>
                <td>Rs.${rate.pricePerGram.toFixed(2)}</td>
                <td>${rate.rateDate}</td>
            </tr>
        `).join('') : '<tr><td colspan="3" class="table-empty">No rates recorded yet</td></tr>';
    },

    renderBilling: async () => {
        const viewContent = document.getElementById('view-content');
        viewContent.innerHTML = `
            <div class="billing-layout">
                <div class="bill-form-container">
                    <div class="panel">
                        <div class="flex-between" style="margin-bottom: 0.75rem;">
                            <h3 style="margin-bottom: 0;">Customer Information</h3>
                            <span id="cust-status-badge"></span>
                        </div>

                        <!-- Mobile search row -->
                        <div class="flex-between" style="margin-top: 0.5rem; margin-bottom: 0; align-items: flex-end; gap: 0.75rem;">
                            <div class="form-group" style="flex: 1; margin-bottom: 0;">
                                <label for="bill-cust-mobile">Mobile Number</label>
                                <input type="text" id="bill-cust-mobile" placeholder="Enter mobile number" maxlength="15" autocomplete="tel">
                            </div>
                            <button type="button" class="btn-outline" id="btn-find-cust" onclick="app.fetchCustomerForBill()">
                                <i class="fas fa-search"></i> Find
                            </button>
                        </div>

                        <!-- Existing customer details (displayed when found) -->
                        <div id="bill-cust-existing-box" class="hidden" style="margin-top: 1rem; background: var(--surface-alt); padding: 1rem; border-radius: var(--radius-md); border: 1px solid var(--border);">
                            <div class="flex-between" style="margin-bottom: 0.5rem;">
                                <span class="status-pill on"><i class="fas fa-check-circle"></i> Existing Customer</span>
                                <button type="button" class="btn-small btn-edit" onclick="app.clearCustomerSelection()"><i class="fas fa-times"></i> Change</button>
                            </div>
                            <p style="margin-bottom: 0.35rem;"><strong>Mobile Number:</strong> <span id="bill-cust-display-mobile"></span></p>
                            <p style="margin-bottom: 0.35rem;"><strong>Customer Name:</strong> <span id="bill-cust-display-name"></span></p>
                            <p id="bill-cust-display-discount-row" class="hidden" style="margin-bottom: 0;"><strong>Discount:</strong> <span id="bill-cust-display-discount">0</span>%</p>
                        </div>

                        <!-- New customer form (displayed when not found) -->
                        <div id="bill-cust-new-box" class="hidden" style="margin-top: 1rem; background: var(--surface-alt); padding: 1rem; border-radius: var(--radius-md); border: 1px solid var(--border);">
                            <div class="flex-between" style="margin-bottom: 0.5rem;">
                                <span class="status-pill off"><i class="fas fa-user-plus"></i> New Customer</span>
                                <button type="button" class="btn-small btn-edit" onclick="app.clearCustomerSelection()"><i class="fas fa-times"></i> Change</button>
                            </div>
                            <p class="text-muted" style="font-size: 0.85rem; margin-bottom: 0.75rem;">Customer not found for this mobile number. Please enter customer name.</p>
                            <p style="margin-bottom: 0.75rem;"><strong>Mobile Number:</strong> <span id="bill-cust-new-mobile-display"></span></p>
                            <div class="form-group" style="margin-bottom: 0.75rem;">
                                <label for="bill-new-cust-name">Customer Name <span style="color: var(--rosewood);">*</span></label>
                                <input type="text" id="bill-new-cust-name" placeholder="Enter customer name" required>
                            </div>
                            <div style="display: flex; gap: 0.5rem; justify-content: flex-end;">
                                <button type="button" class="btn-small btn-view" onclick="app.saveNewCustomer()"><i class="fas fa-save"></i> Save Customer</button>
                            </div>
                            <div id="bill-cust-save-msg" class="message hidden" style="margin-top: 0.5rem; padding: 0.4rem 0.75rem; font-size: 0.82rem;"></div>
                        </div>
                    </div>

                    <div class="panel">
                        <h3>Add items</h3>
                        <div class="add-item-row mt-md">
                            <div class="form-group" style="margin-bottom: 0;">
                                <label>Jewelry design</label>
                                <select id="bill-item-select"></select>
                            </div>
                            <div class="form-group" style="margin-bottom: 0;">
                                <label>Qty</label>
                                <input type="number" id="bill-item-qty" value="1" min="1">
                            </div>
                            <button class="btn-add" onclick="app.addItemToBill()"><i class="fas fa-plus"></i> Add</button>
                        </div>
                        <table id="bill-items-table" class="mt-md">
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
                    <h3>Bill summary</h3>
                    <div id="summary-content" class="mt-md">
                        <div class="summary-item"><span>Subtotal</span><span id="sum-subtotal">Rs.0.00</span></div>
                        <div class="summary-item"><span>Discount</span><span id="sum-discount">Rs.0.00</span></div>
                        <div class="summary-item"><span>GST (3%)</span><span id="sum-gst">Rs.0.00</span></div>
                        <div class="summary-total flex-between" style="margin-bottom: 0;"><span>Grand total</span><span id="sum-grand">Rs.0.00</span></div>
                    </div>
                    <button class="btn-primary btn-success mt-lg" onclick="app.generateBill()">Generate bill &amp; download PDF</button>
                </div>
            </div>
        `;

        const jewelry = await api.jewelry.getAll();
        const select = document.getElementById('bill-item-select');
        select.innerHTML = jewelry.data.map(item => `<option value="${item.id}">${item.name} (${item.type} — ${item.weight}g)</option>`).join('');

        const mobileInput = document.getElementById('bill-cust-mobile');
        if (mobileInput) {
            mobileInput.addEventListener('keydown', (e) => {
                if (e.key === 'Enter') {
                    e.preventDefault();
                    app.fetchCustomerForBill();
                }
            });
        }
    },

    renderHistory: async () => {
        const viewContent = document.getElementById('view-content');
        viewContent.innerHTML = `
            <div class="flex-between">
                <h3>Bill History</h3>
                <div class="search-inline">
                    <input type="text" id="bill-search-id" placeholder="Search by bill ID">
                    <button class="btn-small btn-edit" onclick="app.searchBill()"><i class="fas fa-search"></i> Search</button>
                </div>
            </div>
            <div class="table-container">
                <table>
                    <thead>
                        <tr>
                            <th>Bill</th>
                            <th>Customer</th>
                            <th>Total</th>
                            <th>Seller</th>
                            <th>Date</th>
                            <th>Action</th>
                        </tr>
                    </thead>
                    <tbody id="history-table-body">
                        <tr><td colspan="6" class="table-empty">Loading history…</td></tr>
                    </tbody>
                </table>
            </div>
        `;

        try {
            const res = await api.bills.getAll();
            if (!res.success) throw new Error(res.message);

            const isAdmin = app.user.role === 'ADMIN';
            let bills = res.data || [];

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
                        <button class="btn-small btn-view" onclick="app.downloadPdf(${bill.id})"><i class="fas fa-download"></i> PDF</button>
                    </td>
                </tr>
            `).join('') : '<tr><td colspan="6" class="table-empty">No bills found</td></tr>';
        } catch (e) {
            console.error("Error loading bill history:", e);
            document.getElementById('history-table-body').innerHTML = `<tr><td colspan="6" class="table-empty" style="color: var(--rosewood);">Failed to load history: ${e.message}</td></tr>`;
        }
    },

    renderCustomers: async () => {
        const viewContent = document.getElementById('view-content');
        viewContent.innerHTML = `
            <div class="flex-between">
                <h3>Customer Management</h3>
                <button class="btn-add" onclick="ui.showCustomerModal()"><i class="fas fa-plus"></i> Add customer</button>
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
        tbody.innerHTML = res.data.length > 0 ? res.data.map(c => `
            <tr>
                <td>${c.mobileNumber}</td>
                <td>${c.name}</td>
                <td>${c.discountPercent}%</td>
                <td>${new Date(c.createdAt).toLocaleDateString()}</td>
                <td>
                    <button class="btn-small btn-edit" onclick="ui.showCustomerModal('${c.mobileNumber}')"><i class="fas fa-pen"></i> Edit</button>
                </td>
            </tr>
        `).join('') : '<tr><td colspan="5" class="table-empty">No customers yet</td></tr>';
    },

    renderReports: async () => {
        const viewContent = document.getElementById('view-content');
        viewContent.innerHTML = `
            <h3>Sales Reports</h3>
            <div class="dashboard-grid mt-md">
                 <div class="panel">
                    <h4>Today's Sales</h4>
                    <p id="report-today-total" style="font-family: var(--font-display); font-size: 1.6rem; font-weight: 600; color: var(--emerald); margin-top: 0.5rem;">Rs.0.00</p>
                </div>
                <div class="panel">
                    <h4>This Month's Sales</h4>
                    <p id="report-month-total" style="font-family: var(--font-display); font-size: 1.6rem; font-weight: 600; color: var(--slate); margin-top: 0.5rem;">Rs.0.00</p>
                </div>
                <div class="panel">
                    <h4>Total All-Time Sales</h4>
                    <p id="report-all-total" style="font-family: var(--font-display); font-size: 1.6rem; font-weight: 600; color: var(--brass-dark); margin-top: 0.5rem;">Rs.0.00</p>
                </div>
            </div>
            <div class="table-container">
                <div class="table-container-header">
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

            <div class="table-container mt-lg">
                <div class="table-container-header">
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

            const sellerEntries = Object.entries(bySeller.data);
            const tbody = document.getElementById('seller-report-body');
            tbody.innerHTML = sellerEntries.length > 0 ? sellerEntries.map(([seller, amount]) => `
                <tr>
                    <td>${seller}</td>
                    <td>Rs.${amount.toFixed(2)}</td>
                </tr>
            `).join('') : '<tr><td colspan="2" class="table-empty">No sales recorded today</td></tr>';

            const monthEntries = Object.entries(monthlyBreakdown.data).sort((a, b) => b[0].localeCompare(a[0]));
            const mBody = document.getElementById('monthly-breakdown-body');
            mBody.innerHTML = monthEntries.length > 0 ? monthEntries.map(([m, amount]) => `
                <tr>
                    <td>${m}</td>
                    <td>Rs.${amount.toFixed(2)}</td>
                </tr>
            `).join('') : '<tr><td colspan="2" class="table-empty">No monthly data yet</td></tr>';
        } catch (e) {
            console.error("Error fetching reports:", e);
            viewContent.innerHTML += `<div class="message error mt-md">Failed to load reports. Please ensure you are logged in as Admin.</div>`;
        }
    },

    renderStaff: async () => {
        const viewContent = document.getElementById('view-content');
        viewContent.innerHTML = `
            <div class="flex-between">
                <h3>Manage Staff</h3>
                <div class="search-inline">
                    <input type="text" id="staff-search-input" placeholder="Search name or mobile">
                    <button class="btn-small btn-edit" onclick="ui.filterStaff()"><i class="fas fa-search"></i> Search</button>
                    <button class="btn-add" onclick="ui.showStaffModal()"><i class="fas fa-plus"></i> Add staff</button>
                </div>
            </div>
            <div class="table-container">
                <table>
                    <thead>
                        <tr>
                            <th>Name</th>
                            <th>Mobile</th>
                            <th>Aadhaar</th>
                            <th>Gender</th>
                            <th>Salary</th>
                            <th>Login access</th>
                            <th>Actions</th>
                        </tr>
                    </thead>
                    <tbody id="staff-table-body">
                        <tr><td colspan="7" class="table-empty">Loading staff…</td></tr>
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
                    <span class="status-pill ${s.loginAccess ? 'on' : 'off'}">${s.loginAccess ? 'Enabled' : 'Disabled'}</span>
                </td>
                <td>
                    <button class="btn-small btn-edit" onclick="ui.showStaffModal(${s.id})"><i class="fas fa-pen"></i> Edit</button>
                    ${s.loginAccess ? `<button class="btn-small btn-view" onclick="ui.showResetPasswordModal(${s.id})"><i class="fas fa-key"></i> Reset</button>` : ''}
                </td>
            </tr>
        `).join('') : '<tr><td colspan="7" class="table-empty">No staff found</td></tr>';
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
            <form id="jewelry-form" class="mt-md">
                <div class="form-group">
                    <label>Name</label>
                    <input type="text" id="j-name" value="${item.name}" required>
                </div>
                <div class="form-row">
                    <div class="form-group">
                        <label>Metal type</label>
                        <select id="j-type">
                            <option value="GOLD" ${item.type === 'GOLD' ? 'selected' : ''}>GOLD</option>
                            <option value="SILVER" ${item.type === 'SILVER' ? 'selected' : ''}>SILVER</option>
                        </select>
                    </div>
                    <div class="form-group">
                        <label>Weight (grams)</label>
                        <input type="number" step="0.001" id="j-weight" value="${item.weight}" required>
                    </div>
                </div>
                <div class="form-row">
                    <div class="form-group">
                        <label>Stock</label>
                        <input type="number" id="j-stock" value="${item.stock}" required>
                    </div>
                    <div class="form-group">
                        <label>Making percent (%)</label>
                        <input type="number" step="0.1" id="j-making" value="${item.makingPercent}" required>
                    </div>
                </div>
                <div class="form-group">
                    <label>Company name</label>
                    <input type="text" id="j-company" value="${item.companyName || ''}">
                </div>
                <button type="submit" class="btn-primary">${id ? 'Update design' : 'Save design'}</button>
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
            <form id="stock-form" class="mt-md">
                <div class="form-group">
                    <label>Quantity to add</label>
                    <input type="number" id="s-qty" value="1" min="1" required>
                </div>
                <button type="submit" class="btn-primary">Add stock</button>
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
            <form id="rate-form" class="mt-md">
                <div class="form-group">
                    <label>Metal type</label>
                    <select id="r-type">
                        <option value="GOLD">GOLD</option>
                        <option value="SILVER">SILVER</option>
                    </select>
                </div>
                <div class="form-group">
                    <label>Price per gram</label>
                    <input type="number" step="0.01" id="r-price" required>
                </div>
                <div class="form-group">
                    <label>Date</label>
                    <input type="date" id="r-date" value="${new Date().toISOString().split('T')[0]}" required>
                </div>
                <button type="submit" class="btn-primary">Update rate</button>
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
            <form id="customer-form" class="mt-md">
                <div class="form-group">
                    <label>Mobile number</label>
                    <input type="text" id="c-mobile" value="${c.mobileNumber}" ${mobile ? 'disabled' : ''} required>
                </div>
                <div class="form-group">
                    <label>Name</label>
                    <input type="text" id="c-name" value="${c.name}" required>
                </div>
                <div class="form-group">
                    <label>Discount percent (%)</label>
                    <input type="number" step="0.1" id="c-discount" value="${c.discountPercent}" required>
                </div>
                <button type="submit" class="btn-primary">Save customer</button>
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
            <form id="staff-form" class="mt-md">
                <div class="form-row">
                    <div class="form-group">
                        <label>Full name</label>
                        <input type="text" id="s-name" value="${s.name}" required>
                    </div>
                    <div class="form-group">
                        <label>Mobile number</label>
                        <input type="text" id="s-mobile" value="${s.mobileNumber}" required>
                    </div>
                    <div class="form-group">
                        <label>Aadhaar number</label>
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
                        <label>Login access</label>
                        <select id="s-login">
                            <option value="false" ${!s.loginAccess ? 'selected' : ''}>No</option>
                            <option value="true" ${s.loginAccess ? 'selected' : ''}>Yes</option>
                        </select>
                    </div>
                </div>

                <div id="login-fields" class="${s.loginAccess ? '' : 'hidden'}" style="margin-top: 0.5rem; border-top: 1px dashed var(--border); padding-top: 1rem;">
                    <h4>Login Credentials</h4>
                    <div class="form-group mt-md" style="margin-top: 0.75rem;">
                        <label>Username</label>
                        <input type="text" id="s-username" value="${s.username || ''}">
                    </div>
                    ${!id ? `
                    <div class="form-group">
                        <label>Password</label>
                        <input type="password" id="s-password">
                    </div>` : ''}
                </div>

                <button type="submit" class="btn-primary mt-md" style="margin-top: 0.5rem;">${id ? 'Update staff' : 'Save staff'}</button>
            </form>
        `;

        document.getElementById('modal-container').classList.remove('hidden');

        document.getElementById('s-login').addEventListener('change', function() {
            const loginFields = document.getElementById('login-fields');
            if (this.value === 'true') {
                loginFields.classList.remove('hidden');
                document.getElementById('s-username').setAttribute('required', 'true');
                if (!id) document.getElementById('s-password').setAttribute('required', 'true');
            } else {
                loginFields.classList.add('hidden');
                document.getElementById('s-username').removeAttribute('required');
                if (!id) document.getElementById('s-password').removeAttribute('required');
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
            <form id="reset-password-form" class="mt-md">
                <div class="form-group">
                    <label>New password</label>
                    <input type="password" id="new-password" required>
                </div>
                <button type="submit" class="btn-primary">Reset password</button>
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
    }
};