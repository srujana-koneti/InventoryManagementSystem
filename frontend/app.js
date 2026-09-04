document.addEventListener("DOMContentLoaded", () => {
    // Application State
    let currentUser = null;
    let products = [];
    let pendingRequests = [];
    let transactions = [];
    let selectedProduct = null;

    // Views & Layout
    const loginView = document.getElementById("loginView");
    const dashboardView = document.getElementById("dashboardView");
    const workspaceGrid = document.querySelector(".workspace-grid");
    const sideColumn = document.querySelector(".side-column");

    // Login Elements
    const loginForm = document.getElementById("loginForm");
    const loginUsernameInput = document.getElementById("loginUsername");
    const loginPasswordInput = document.getElementById("loginPassword");
    const loginAlert = document.getElementById("loginAlert");
    const fillManagerBtn = document.getElementById("fillManagerBtn");
    const fillStaffBtn = document.getElementById("fillStaffBtn");

    // User Profile in Header
    const userBadgeName = document.getElementById("userBadgeName");
    const userBadgeRole = document.getElementById("userBadgeRole");
    const logoutBtn = document.getElementById("logoutBtn");

    // Dashboard Metric Elements
    const metricTotalProducts = document.getElementById("metricTotalProducts");
    const metricTotalStock = document.getElementById("metricTotalStock");
    const metricLowStock = document.getElementById("metricLowStock");
    const metricPendingRequests = document.getElementById("metricPendingRequests");

    // Table & List Elements
    const productsTableBody = document.getElementById("productsTableBody");
    const transactionsTableBody = document.getElementById("transactionsTableBody");
    const queueContainer = document.getElementById("queueContainer");
    const processAlert = document.getElementById("processAlert");
    const globalNotification = document.getElementById("globalNotification");
    const incomingRequestsCard = document.getElementById("incomingRequestsCard");

    // Controls
    const searchInput = document.getElementById("searchInput");
    const filterSelect = document.getElementById("filterSelect");
    const refreshBtn = document.getElementById("refreshBtn");
    const processNextBtn = document.getElementById("processNextBtn");

    // Add Product Modal Elements
    const openAddProductModalBtn = document.getElementById("openAddProductModalBtn");
    const closeModalBtn = document.getElementById("closeModalBtn");
    const cancelAddBtn = document.getElementById("cancelAddBtn");
    const addProductModal = document.getElementById("addProductModal");
    const addProductForm = document.getElementById("addProductForm");
    const modalAlert = document.getElementById("modalAlert");

    // Delete Product Modal Elements
    const openDeleteProductModalBtn = document.getElementById("openDeleteProductModalBtn");
    const closeDeleteModalBtn = document.getElementById("closeDeleteModalBtn");
    const cancelDeleteBtn = document.getElementById("cancelDeleteBtn");
    const deleteProductModal = document.getElementById("deleteProductModal");
    const deleteProductForm = document.getElementById("deleteProductForm");
    const deleteProductSelect = document.getElementById("deleteProductSelect");
    const deleteModalAlert = document.getElementById("deleteModalAlert");

    // Row Action Selection Modal Elements
    const rowActionModal = document.getElementById("rowActionModal");
    const actionModalTitle = document.getElementById("actionModalTitle");
    const actionModalSubtitle = document.getElementById("actionModalSubtitle");
    const selectRequestStockBtn = document.getElementById("selectRequestStockBtn");
    const selectUpdateProductBtn = document.getElementById("selectUpdateProductBtn");
    const closeRowActionModalBtn = document.getElementById("closeRowActionModalBtn");
    const cancelRowActionBtn = document.getElementById("cancelRowActionBtn");

    // Request Stock Modal Elements (Option 1)
    const stockRequestModal = document.getElementById("stockRequestModal");
    const requestStockTargetLabel = document.getElementById("requestStockTargetLabel");
    const requestStockModalAlert = document.getElementById("requestStockModalAlert");
    const stockRequestModalForm = document.getElementById("stockRequestModalForm");
    const reqModalProductId = document.getElementById("reqModalProductId");
    const reqModalType = document.getElementById("reqModalType");
    const reqModalQuantity = document.getElementById("reqModalQuantity");
    const closeRequestStockModalBtn = document.getElementById("closeRequestStockModalBtn");
    const cancelRequestStockBtn = document.getElementById("cancelRequestStockBtn");

    // Update Product Modal Elements (Option 2)
    const updateProductModal = document.getElementById("updateProductModal");
    const updateProductTargetLabel = document.getElementById("updateProductTargetLabel");
    const updateModalAlert = document.getElementById("updateModalAlert");
    const updateProductForm = document.getElementById("updateProductForm");
    const updateProdId = document.getElementById("updateProdId");
    const updateProdName = document.getElementById("updateProdName");
    const updateProdCategory = document.getElementById("updateProdCategory");
    const updateProdPrice = document.getElementById("updateProdPrice");
    const updateProdQty = document.getElementById("updateProdQty");
    const updateProdReorder = document.getElementById("updateProdReorder");
    const closeUpdateModalBtn = document.getElementById("closeUpdateModalBtn");
    const cancelUpdateBtn = document.getElementById("cancelUpdateBtn");

    let notificationTimeout = null;

    // =========================================================================
    // Authentication & State Management
    // =========================================================================

    function showLogin() {
        if (loginView) loginView.style.display = "flex";
        if (dashboardView) dashboardView.style.display = "none";
        hideLoginAlert();
        closeAllModals();
    }

    function showDashboard(user) {
        currentUser = user;
        if (loginView) loginView.style.display = "none";
        if (dashboardView) dashboardView.style.display = "block";
        applyRoleUI(user);
        closeAllModals();
        loadData();
    }

    function showLoginAlert(message) {
        if (!loginAlert) return;
        loginAlert.textContent = message;
        loginAlert.className = "alert-box danger";
        loginAlert.classList.remove("hidden");
        loginAlert.style.display = "block";
    }

    function hideLoginAlert() {
        if (!loginAlert) return;
        loginAlert.classList.add("hidden");
        loginAlert.style.display = "none";
        loginAlert.textContent = "";
    }

    // Role-Based UI Adjustments (RBAC)
    function applyRoleUI(user) {
        if (!user) return;

        if (userBadgeName) userBadgeName.textContent = user.displayName || user.username;

        const catalogTip = document.getElementById("catalogTip");

        if (user.role === "MANAGER") {
            // Inventory Manager View: Full access
            if (userBadgeRole) {
                userBadgeRole.textContent = "Inventory Manager";
                userBadgeRole.className = "badge badge-info";
            }
            if (openAddProductModalBtn) openAddProductModalBtn.style.display = "inline-flex";
            if (openDeleteProductModalBtn) openDeleteProductModalBtn.style.display = "inline-flex";
            if (sideColumn) sideColumn.style.display = "flex";
            if (incomingRequestsCard) incomingRequestsCard.style.display = "block";
            if (workspaceGrid) workspaceGrid.classList.remove("full-width");
            if (selectUpdateProductBtn) selectUpdateProductBtn.style.display = "flex";
            if (catalogTip) {
                catalogTip.innerHTML = `💡 Tip: Click on any product row below to <strong>Request Stock</strong> or <strong>Update Details</strong>.`;
            }
        } else if (user.role === "STAFF") {
            // Staff View: Restricted access
            if (userBadgeRole) {
                userBadgeRole.textContent = "Staff Member";
                userBadgeRole.className = "badge badge-warning";
            }
            // Hide Add/Remove buttons for Staff
            if (openAddProductModalBtn) openAddProductModalBtn.style.display = "none";
            if (openDeleteProductModalBtn) openDeleteProductModalBtn.style.display = "none";
            // Hide Incoming Requests queue entirely and expand table to full width
            if (incomingRequestsCard) incomingRequestsCard.style.display = "none";
            if (sideColumn) sideColumn.style.display = "none";
            if (workspaceGrid) workspaceGrid.classList.add("full-width");
            // Completely hide Update Product action from Staff interface
            if (selectUpdateProductBtn) selectUpdateProductBtn.style.display = "none";
            if (catalogTip) {
                catalogTip.innerHTML = `💡 Tip: Click on any product row below to <strong>Request Stock</strong>.`;
            }
            closeUpdateProductModal();
        }
    }

    // Login Form Submit Handler
    if (loginForm) {
        loginForm.addEventListener("submit", async (e) => {
            e.preventDefault();
            hideLoginAlert();

            const username = loginUsernameInput.value.trim();
            const password = loginPasswordInput.value.trim();

            if (!username || !password) {
                showLoginAlert("Please enter both username/email and password.");
                return;
            }

            try {
                const res = await fetch("/api/auth/login", {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({ username, password })
                });

                const data = await res.json();

                if (res.ok && data.success) {
                    sessionStorage.setItem("currentUser", JSON.stringify(data.user));
                    showDashboard(data.user);
                    showGlobalNotification(`Welcome back, ${data.user.displayName}!`, "success");
                } else {
                    showLoginAlert(data.message || "Invalid username or password.");
                }
            } catch (err) {
                console.error("Login request error:", err);
                showLoginAlert("Network error connecting to login service.");
            }
        });
    }

    // Demo Quick-Fill Buttons
    if (fillManagerBtn) {
        fillManagerBtn.addEventListener("click", () => {
            loginUsernameInput.value = "Manager";
            loginPasswordInput.value = "manager@123";
            loginForm.dispatchEvent(new Event("submit"));
        });
    }

    if (fillStaffBtn) {
        fillStaffBtn.addEventListener("click", () => {
            loginUsernameInput.value = "STAFF";
            loginPasswordInput.value = "staff";
            loginForm.dispatchEvent(new Event("submit"));
        });
    }

    // Logout Handler
    if (logoutBtn) {
        logoutBtn.addEventListener("click", () => {
            sessionStorage.removeItem("currentUser");
            currentUser = null;
            if (loginForm) loginForm.reset();
            showLogin();
        });
    }

    // =========================================================================
    // UI Helpers & Alerts
    // =========================================================================

    function showGlobalNotification(message, type = "success") {
        if (!globalNotification) return;
        if (notificationTimeout) clearTimeout(notificationTimeout);
        globalNotification.textContent = message;
        globalNotification.className = `alert-box ${type}`;
        globalNotification.classList.remove("hidden");
        globalNotification.style.display = "block";

        notificationTimeout = setTimeout(() => {
            globalNotification.classList.add("hidden");
            globalNotification.style.display = "none";
        }, 6000);
    }

    function showModalAlert(el, message, type = "danger") {
        if (!el) return;
        el.textContent = message;
        el.className = `alert-box ${type}`;
        el.classList.remove("hidden");
        el.style.display = "block";
    }

    function hideModalAlert(el) {
        if (!el) return;
        el.classList.add("hidden");
        el.style.display = "none";
        el.textContent = "";
    }

    // =========================================================================
    // Modal Handlers (Close All Helper)
    // =========================================================================

    function closeAllModals() {
        closeAddModal();
        closeDeleteModal();
        closeRowActionModal();
        closeStockRequestModal();
        closeUpdateProductModal();
    }

    function openAddModal() {
        if (addProductForm) addProductForm.reset();
        hideModalAlert(modalAlert);
        if (addProductModal) {
            addProductModal.classList.remove("hidden");
            addProductModal.style.display = "flex";
        }
        const idInput = document.getElementById("newProdId");
        if (idInput) idInput.focus();
    }

    function closeAddModal() {
        if (addProductModal) {
            addProductModal.classList.add("hidden");
            addProductModal.style.display = "none";
        }
        hideModalAlert(modalAlert);
    }

    function openDeleteModal() {
        if (deleteProductForm) deleteProductForm.reset();
        hideModalAlert(deleteModalAlert);
        populateDeleteDropdown();
        if (deleteProductModal) {
            deleteProductModal.classList.remove("hidden");
            deleteProductModal.style.display = "flex";
        }
        if (deleteProductSelect) deleteProductSelect.focus();
    }

    function closeDeleteModal() {
        if (deleteProductModal) {
            deleteProductModal.classList.add("hidden");
            deleteProductModal.style.display = "none";
        }
        hideModalAlert(deleteModalAlert);
    }

    // Row Action Selector Modal Handlers
    function openRowActionModal(product) {
        if (!product) return;
        selectedProduct = product;

        // Strict RBAC: Staff bypasses the choice menu and opens Request Stock directly!
        if (currentUser && currentUser.role === "STAFF") {
            openStockRequestModal(product);
            return;
        }

        if (actionModalTitle) {
            actionModalTitle.textContent = `📦 Product Actions: ${product.id} - ${product.name}`;
        }
        if (actionModalSubtitle) {
            actionModalSubtitle.innerHTML = `Current Stock: <strong>${product.quantity}</strong> | Price: <strong>$${product.price.toFixed(2)}</strong> | Reorder Level: <strong>${product.reorderLevel}</strong>`;
        }

        if (rowActionModal) {
            rowActionModal.classList.remove("hidden");
            rowActionModal.style.display = "flex";
        }
    }

    function closeRowActionModal() {
        if (rowActionModal) {
            rowActionModal.classList.add("hidden");
            rowActionModal.style.display = "none";
        }
    }

    // Option 1: Request Stock Modal Handlers
    function openStockRequestModal(product) {
        closeRowActionModal();
        selectedProduct = product;
        if (stockRequestModalForm) stockRequestModalForm.reset();
        hideModalAlert(requestStockModalAlert);

        if (reqModalProductId) reqModalProductId.value = product.id;
        if (reqModalType) reqModalType.value = "ADD";
        if (reqModalQuantity) reqModalQuantity.value = 5;

        if (requestStockTargetLabel) {
            requestStockTargetLabel.innerHTML = `Product: <strong>${escapeHtml(product.id)} - ${escapeHtml(product.name)}</strong> (Current Stock: ${product.quantity})`;
        }

        if (stockRequestModal) {
            stockRequestModal.classList.remove("hidden");
            stockRequestModal.style.display = "flex";
        }
        if (reqModalQuantity) reqModalQuantity.focus();
    }

    function closeStockRequestModal() {
        if (stockRequestModal) {
            stockRequestModal.classList.add("hidden");
            stockRequestModal.style.display = "none";
        }
        hideModalAlert(requestStockModalAlert);
    }

    // Option 2: Update Product Modal Handlers
    function openUpdateProductModal(product) {
        if (!product) return;
        // Strict Access Restriction: Only Inventory Managers can edit products
        if (!currentUser || currentUser.role !== "MANAGER") {
            alert("Access denied. Updating product details is strictly restricted to Inventory Managers.");
            return;
        }

        closeRowActionModal();
        selectedProduct = product;
        hideModalAlert(updateModalAlert);

        if (updateProdId) updateProdId.value = product.id;
        if (updateProdName) updateProdName.value = product.name;
        if (updateProdCategory) updateProdCategory.value = product.category || "General";
        if (updateProdPrice) updateProdPrice.value = product.price.toFixed(2);
        if (updateProdQty) updateProdQty.value = product.quantity;
        if (updateProdReorder) updateProdReorder.value = product.reorderLevel;

        if (updateProductTargetLabel) {
            updateProductTargetLabel.innerHTML = `Editing: <strong>${escapeHtml(product.id)} - ${escapeHtml(product.name)}</strong>`;
        }

        if (updateProductModal) {
            updateProductModal.classList.remove("hidden");
            updateProductModal.style.display = "flex";
        }
        if (updateProdName) updateProdName.focus();
    }

    function closeUpdateProductModal() {
        if (updateProductModal) {
            updateProductModal.classList.add("hidden");
            updateProductModal.style.display = "none";
        }
        hideModalAlert(updateModalAlert);
    }

    // Option Button Listeners
    if (selectRequestStockBtn) {
        selectRequestStockBtn.addEventListener("click", () => {
            if (selectedProduct) openStockRequestModal(selectedProduct);
        });
    }

    if (selectUpdateProductBtn) {
        selectUpdateProductBtn.addEventListener("click", () => {
            if (currentUser && currentUser.role !== "MANAGER") {
                alert("Updating product details is restricted to Inventory Managers. Staff can submit stock requests using Option A.");
                return;
            }
            if (selectedProduct) openUpdateProductModal(selectedProduct);
        });
    }

    // Bind Close / Cancel Modal Buttons
    if (openAddProductModalBtn) openAddProductModalBtn.addEventListener("click", openAddModal);
    if (closeModalBtn) closeModalBtn.addEventListener("click", closeAddModal);
    if (cancelAddBtn) cancelAddBtn.addEventListener("click", closeAddModal);

    if (openDeleteProductModalBtn) openDeleteProductModalBtn.addEventListener("click", openDeleteModal);
    if (closeDeleteModalBtn) closeDeleteModalBtn.addEventListener("click", closeDeleteModal);
    if (cancelDeleteBtn) cancelDeleteBtn.addEventListener("click", closeDeleteModal);

    if (closeRowActionModalBtn) closeRowActionModalBtn.addEventListener("click", closeRowActionModal);
    if (cancelRowActionBtn) cancelRowActionBtn.addEventListener("click", closeRowActionModal);

    if (closeRequestStockModalBtn) closeRequestStockModalBtn.addEventListener("click", closeStockRequestModal);
    if (cancelRequestStockBtn) cancelRequestStockBtn.addEventListener("click", closeStockRequestModal);

    if (closeUpdateModalBtn) closeUpdateModalBtn.addEventListener("click", closeUpdateProductModal);
    if (cancelUpdateBtn) cancelUpdateBtn.addEventListener("click", closeUpdateProductModal);

    // Close Modals on Overlay Click
    [addProductModal, deleteProductModal, rowActionModal, stockRequestModal, updateProductModal].forEach(modal => {
        if (modal) {
            modal.addEventListener("click", (e) => {
                if (e.target === modal) closeAllModals();
            });
        }
    });

    // Close Modals on ESC Key
    document.addEventListener("keydown", (e) => {
        if (e.key === "Escape") {
            closeAllModals();
        }
    });

    // =========================================================================
    // Data Loading & Rendering
    // =========================================================================

    async function loadData() {
        try {
            const [productsRes, requestsRes, txnsRes] = await Promise.all([
                fetch("/api/products"),
                fetch("/api/requests"),
                fetch("/api/transactions")
            ]);

            if (productsRes.ok) products = await productsRes.json();
            if (requestsRes.ok) pendingRequests = await requestsRes.json();
            if (txnsRes.ok) transactions = await txnsRes.json();

            updateMetrics();
            renderProducts();
            renderQueue();
            renderTransactions();
            populateDeleteDropdown();
            loadSuppliers();
        } catch (err) {
            console.error("Failed to load inventory data:", err);
            showGlobalNotification("Error connecting to server. Is the Java HTTP server running?", "danger");
        }
    }

    function updateMetrics() {
        const totalItems = products.length;
        const totalStockUnits = products.reduce((sum, p) => sum + p.quantity, 0);
        const lowStockCount = products.filter(p => p.isLowStock).length;
        const pendingCount = pendingRequests.length;

        if (metricTotalProducts) metricTotalProducts.textContent = totalItems;
        if (metricTotalStock) metricTotalStock.textContent = totalStockUnits.toLocaleString();
        if (metricLowStock) metricLowStock.textContent = lowStockCount;
        if (metricPendingRequests) metricPendingRequests.textContent = pendingCount;
    }

    function renderProducts() {
        if (!productsTableBody) return;
        const query = searchInput ? searchInput.value.trim().toLowerCase() : "";
        const filter = filterSelect ? filterSelect.value : "ALL";

        const filtered = products.filter(p => {
            const matchesQuery = !query || 
                p.name.toLowerCase().includes(query) ||
                p.category.toLowerCase().includes(query) ||
                p.id.toLowerCase().includes(query);

            let matchesFilter = true;
            if (filter === "LOW_STOCK") {
                matchesFilter = p.isLowStock && p.quantity > 0;
            } else if (filter === "OUT_OF_STOCK") {
                matchesFilter = p.quantity === 0;
            } else if (filter === "NORMAL") {
                matchesFilter = !p.isLowStock && p.quantity > 0;
            }

            return matchesQuery && matchesFilter;
        });

        if (filtered.length === 0) {
            productsTableBody.innerHTML = `<tr><td colspan="7" class="text-center">No matching products found.</td></tr>`;
            return;
        }

        productsTableBody.innerHTML = filtered.map(p => {
            let badgeClass = "badge-success";
            let statusText = "Normal";

            if (p.quantity === 0) {
                badgeClass = "badge-danger";
                statusText = "Out of Stock";
            } else if (p.isLowStock) {
                badgeClass = "badge-warning";
                statusText = "Low Stock";
            }

            const isStaff = currentUser && currentUser.role === "STAFF";
            const rowTitle = isStaff ? "Click to Request Stock" : "Click to Request Stock or Update Details";

            return `
                <tr data-id="${escapeHtml(p.id)}" title="${rowTitle}">
                    <td><strong>${escapeHtml(p.id)}</strong></td>
                    <td>${escapeHtml(p.name)}</td>
                    <td>${escapeHtml(p.category)}</td>
                    <td>$${p.price.toFixed(2)}</td>
                    <td><strong>${p.quantity}</strong></td>
                    <td>${p.reorderLevel}</td>
                    <td><span class="badge ${badgeClass}">${statusText}</span></td>
                </tr>
            `;
        }).join("");

        // Attach click handlers to each row based on user role
        productsTableBody.querySelectorAll("tr[data-id]").forEach(tr => {
            tr.addEventListener("click", () => {
                const pId = tr.getAttribute("data-id");
                const item = products.find(p => p.id === pId);
                if (item) {
                    if (currentUser && currentUser.role === "STAFF") {
                        // STAFF: Open Request Stock modal immediately without choice popup
                        openStockRequestModal(item);
                    } else {
                        // INVENTORY MANAGER: Open popup with both options
                        openRowActionModal(item);
                    }
                }
            });
        });
    }

    function populateDeleteDropdown() {
        if (!deleteProductSelect) return;
        const currentVal = deleteProductSelect.value;
        deleteProductSelect.innerHTML = `<option value="">-- Select Product --</option>` +
            products.map(p => `<option value="${escapeHtml(p.id)}">${escapeHtml(p.id)} - ${escapeHtml(p.name)}</option>`).join("");
        if (currentVal && products.some(p => p.id === currentVal)) {
            deleteProductSelect.value = currentVal;
        }
    }

    // =========================================================================
    // Forms & Actions
    // =========================================================================

    // Add Product Form Submit
    if (addProductForm) {
        addProductForm.addEventListener("submit", async (e) => {
            e.preventDefault();
            hideModalAlert(modalAlert);

            const id = document.getElementById("newProdId").value.trim();
            const name = document.getElementById("newProdName").value.trim();
            const category = document.getElementById("newProdCategory").value.trim() || "General";
            const price = parseFloat(document.getElementById("newProdPrice").value);
            const quantity = parseInt(document.getElementById("newProdQty").value, 10);
            const reorderLevel = parseInt(document.getElementById("newProdReorder").value, 10);

            if (!id) {
                showModalAlert(modalAlert, "Product ID cannot be empty.", "danger");
                return;
            }
            if (!name) {
                showModalAlert(modalAlert, "Product Name cannot be empty.", "danger");
                return;
            }
            if (isNaN(price) || price <= 0) {
                showModalAlert(modalAlert, "Price must be strictly greater than 0.", "danger");
                return;
            }
            if (isNaN(quantity) || quantity < 0) {
                showModalAlert(modalAlert, "Initial stock quantity must not be negative.", "danger");
                return;
            }
            if (isNaN(reorderLevel) || reorderLevel < 0) {
                showModalAlert(modalAlert, "Reorder level must not be negative.", "danger");
                return;
            }

            try {
                const res = await fetch("/api/products", {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({ id, name, category, price, quantity, reorderLevel })
                });

                const data = await res.json();

                if (res.ok && data.success) {
                    closeAddModal();
                    addProductForm.reset();
                    showGlobalNotification(data.message || `Product ${id} added successfully.`, "success");
                    await loadData();
                } else {
                    showModalAlert(modalAlert, data.message || data.error || "Failed to add product.", "danger");
                }
            } catch (err) {
                console.error("Error adding product:", err);
                showModalAlert(modalAlert, "Network error adding product: " + err.message, "danger");
            }
        });
    }

    // Delete Product Form Submit
    if (deleteProductForm) {
        deleteProductForm.addEventListener("submit", async (e) => {
            e.preventDefault();
            hideModalAlert(deleteModalAlert);

            const productId = deleteProductSelect.value;
            if (!productId) {
                showModalAlert(deleteModalAlert, "Please select a product to delete.", "danger");
                return;
            }

            const selectedOption = deleteProductSelect.options[deleteProductSelect.selectedIndex];
            const selectedText = selectedOption ? selectedOption.textContent : productId;

            const confirmed = confirm(`Are you sure you want to delete ${selectedText}?`);
            if (!confirmed) return;

            try {
                const res = await fetch(`/api/products/${encodeURIComponent(productId)}`, {
                    method: "DELETE"
                });
                const data = await res.json();

                if (res.ok && data.success) {
                    closeDeleteModal();
                    showGlobalNotification(data.message || `Product ${productId} deleted successfully.`, "success");
                    await loadData();
                } else {
                    showModalAlert(deleteModalAlert, data.message || data.error || `Failed to delete product ${productId}.`, "danger");
                }
            } catch (err) {
                console.error("Error deleting product:", err);
                showModalAlert(deleteModalAlert, "Network error deleting product: " + err.message, "danger");
            }
        });
    }

    // Option 1: Request Stock Form Submit
    if (stockRequestModalForm) {
        stockRequestModalForm.addEventListener("submit", async (e) => {
            e.preventDefault();
            hideModalAlert(requestStockModalAlert);

            const productId = reqModalProductId ? reqModalProductId.value : "";
            const type = reqModalType ? reqModalType.value : "ADD";
            const quantity = parseInt(reqModalQuantity ? reqModalQuantity.value : "0", 10);

            if (!productId) {
                showModalAlert(requestStockModalAlert, "No product selected for this request.", "danger");
                return;
            }
            if (isNaN(quantity) || quantity <= 0) {
                showModalAlert(requestStockModalAlert, "Please enter a valid quantity greater than 0.", "danger");
                return;
            }

            try {
                const res = await fetch("/api/requests", {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({ productId, type, quantity })
                });

                const data = await res.json();

                if (res.ok) {
                    closeStockRequestModal();
                    showGlobalNotification(`Stock request for ${productId} (${type} ${quantity}) submitted successfully.`, "success");
                    await loadData();
                } else {
                    showModalAlert(requestStockModalAlert, data.error || data.message || "Failed to submit request.", "danger");
                }
            } catch (err) {
                console.error("Failed to submit request:", err);
                showModalAlert(requestStockModalAlert, "Error submitting request: " + err.message, "danger");
            }
        });
    }

    // Option 2: Update Product Form Submit
    if (updateProductForm) {
        updateProductForm.addEventListener("submit", async (e) => {
            e.preventDefault();
            hideModalAlert(updateModalAlert);

            if (!currentUser || currentUser.role !== "MANAGER") {
                showModalAlert(updateModalAlert, "Access denied. Product updating is strictly restricted to Inventory Managers.", "danger");
                return;
            }

            const productId = updateProdId ? updateProdId.value.trim() : "";
            const name = updateProdName ? updateProdName.value.trim() : "";
            const category = updateProdCategory ? updateProdCategory.value.trim() : "General";
            const price = parseFloat(updateProdPrice ? updateProdPrice.value : "0");
            const quantity = parseInt(updateProdQty ? updateProdQty.value : "0", 10);
            const reorderLevel = parseInt(updateProdReorder ? updateProdReorder.value : "0", 10);

            if (!productId) {
                showModalAlert(updateModalAlert, "Product ID cannot be missing.", "danger");
                return;
            }
            if (!name) {
                showModalAlert(updateModalAlert, "Product Name cannot be empty.", "danger");
                return;
            }
            if (isNaN(price) || price <= 0) {
                showModalAlert(updateModalAlert, "Price must be strictly greater than 0.", "danger");
                return;
            }
            if (isNaN(quantity) || quantity < 0) {
                showModalAlert(updateModalAlert, "Stock quantity must not be negative.", "danger");
                return;
            }
            if (isNaN(reorderLevel) || reorderLevel < 0) {
                showModalAlert(updateModalAlert, "Reorder level must not be negative.", "danger");
                return;
            }

            try {
                const res = await fetch(`/api/products/${encodeURIComponent(productId)}`, {
                    method: "PUT",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({ name, category, price, quantity, reorderLevel })
                });

                const data = await res.json();

                if (res.ok && data.success) {
                    closeUpdateProductModal();
                    showGlobalNotification(data.message || `Product ${productId} updated successfully.`, "success");
                    await loadData();
                } else {
                    showModalAlert(updateModalAlert, data.message || data.error || `Failed to update product ${productId}.`, "danger");
                }
            } catch (err) {
                console.error("Error updating product:", err);
                showModalAlert(updateModalAlert, "Network error updating product: " + err.message, "danger");
            }
        });
    }

    // Render Requests Queue (Manager view)
    function renderQueue() {
        if (!queueContainer) return;
        if (pendingRequests.length === 0) {
            queueContainer.innerHTML = `<p class="empty-state">No pending requests in queue.</p>`;
            return;
        }

        queueContainer.innerHTML = pendingRequests.map((req, idx) => {
            const isRemove = req.type.toUpperCase() === "REMOVE";
            const badgeClass = isRemove ? "badge-warning" : "badge-info";
            const pos = idx === 0 ? "Next" : `#${idx + 1}`;

            return `
                <div class="queue-item ${isRemove ? 'remove' : ''}">
                    <div>
                        <span class="queue-pos">${pos}</span>
                        <strong>${escapeHtml(req.requestId)}</strong>: ${escapeHtml(req.productId)}
                    </div>
                    <div>
                        <span class="badge ${badgeClass}">${escapeHtml(req.type)} ${req.quantity}</span>
                    </div>
                </div>
            `;
        }).join("");
    }

    // Render Transaction Log
    function renderTransactions() {
        if (!transactionsTableBody) return;
        if (transactions.length === 0) {
            transactionsTableBody.innerHTML = `<tr><td colspan="7" class="text-center">No transactions recorded yet.</td></tr>`;
            return;
        }

        const sortedTxns = [...transactions].reverse();

        transactionsTableBody.innerHTML = sortedTxns.map(t => {
            const isAdd = t.type === "ADD_STOCK";
            const badgeClass = isAdd ? "badge-success" : "badge-warning";
            const actionText = isAdd ? "+ ADD STOCK" : "- REMOVE STOCK";

            return `
                <tr>
                    <td><code>${escapeHtml(t.transactionId)}</code></td>
                    <td>${escapeHtml(t.timestamp)}</td>
                    <td><strong>${escapeHtml(t.productId)}</strong></td>
                    <td>${escapeHtml(t.productName)}</td>
                    <td><span class="badge ${badgeClass}">${actionText}</span></td>
                    <td>${t.quantity}</td>
                    <td><strong>${t.resultingStock}</strong></td>
                </tr>
            `;
        }).join("");
    }

    // Process Next Request (Manager only)
    if (processNextBtn) {
        processNextBtn.addEventListener("click", async () => {
            try {
                processNextBtn.disabled = true;
                const res = await fetch("/api/requests/process", { method: "POST" });
                const data = await res.json();

                if (processAlert) {
                    processAlert.textContent = data.result || "Request processed.";
                    processAlert.className = "alert-box info";
                    processAlert.classList.remove("hidden");
                    processAlert.style.display = "block";
                }

                await loadData();
            } catch (err) {
                console.error("Error processing request:", err);
                showGlobalNotification("Error processing next request: " + err.message, "danger");
            } finally {
                processNextBtn.disabled = false;
            }
        });
    }

    // Search and Filter Listeners
    if (searchInput) searchInput.addEventListener("input", renderProducts);
    if (filterSelect) filterSelect.addEventListener("change", renderProducts);
    if (refreshBtn) {
        refreshBtn.addEventListener("click", () => {
            loadData();
            showGlobalNotification("Inventory refreshed.", "info");
        });
    }

    function escapeHtml(str) {
        if (!str) return "";
        return String(str).replace(/[&<>'"]/g, tag => ({
            '&': '&amp;',
            '<': '&lt;',
            '>': '&gt;',
            "'": '&#39;',
            '"': '&quot;'
        }[tag] || tag));
    }

    // =========================================================================
    // Supplier Management (Standalone Additive)
    // =========================================================================
    const suppliersTableBody = document.getElementById("suppliersTableBody");

    async function loadSuppliers() {
        if (!suppliersTableBody) return;
        try {
            const res = await fetch("/api/suppliers");
            if (!res.ok) {
                suppliersTableBody.innerHTML = `<tr><td colspan="3" class="text-center">Failed to load suppliers.</td></tr>`;
                return;
            }
            const suppliers = await res.json();
            if (!suppliers || suppliers.length === 0) {
                suppliersTableBody.innerHTML = `<tr><td colspan="3" class="text-center">No suppliers registered yet.</td></tr>`;
                return;
            }

            suppliersTableBody.innerHTML = suppliers.map(s => `
                <tr>
                    <td><strong>${escapeHtml(s.id)}</strong></td>
                    <td>${escapeHtml(s.name)}</td>
                    <td>${escapeHtml(s.contact)}</td>
                </tr>
            `).join("");
        } catch (err) {
            console.error("Error loading suppliers:", err);
            suppliersTableBody.innerHTML = `<tr><td colspan="3" class="text-center">Error loading suppliers.</td></tr>`;
        }
    }

    // =========================================================================
    // Initial Session Check
    // =========================================================================
    const storedUser = sessionStorage.getItem("currentUser");
    if (storedUser) {
        try {
            currentUser = JSON.parse(storedUser);
            showDashboard(currentUser);
        } catch (e) {
            sessionStorage.removeItem("currentUser");
            showLogin();
        }
    } else {
        showLogin();
    }
});
