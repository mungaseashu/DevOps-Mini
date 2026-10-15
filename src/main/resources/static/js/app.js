// CloudKicks Sneaker Retail Application JavaScript Logic

let cart = [];
let products = [];
let selectedSizes = {}; // maps productId -> selectedSize string
let pollingInterval = null;

document.addEventListener('DOMContentLoaded', () => {
    loadProducts();
    startMetricsPolling();
});

// Fetch and Render Sneaker Catalog
async function loadProducts() {
    try {
        const res = await fetch('/api/products');
        if (!res.ok) throw new Error('Failed to fetch sneaker catalog');
        products = await res.json();
        
        // Default size selection to first available size
        products.forEach(p => {
            if (p.availableSizes && p.availableSizes.length > 0 && !selectedSizes[p.id]) {
                selectedSizes[p.id] = p.availableSizes[0];
            }
        });

        renderProducts(products);
    } catch (err) {
        logConsole('Error loading sneakers: ' + err.message, 'error');
        showToast('Error connecting to backend API', 'error');
    }
}

function selectSize(productId, size) {
    selectedSizes[productId] = size;
    renderProducts(products);
}

function renderProducts(productList) {
    const grid = document.getElementById('productsGrid');
    if (!grid) return;

    grid.innerHTML = productList.map(prod => {
        const currentSelectedSize = selectedSizes[prod.id] || (prod.availableSizes ? prod.availableSizes[0] : 'Standard');
        const sizeButtons = prod.availableSizes ? prod.availableSizes.map(sz => `
            <button onclick="selectSize('${prod.id}', '${sz}')" class="px-2.5 py-1 text-[11px] font-bold rounded-lg border transition-all ${
                sz === currentSelectedSize 
                ? 'bg-emerald-500 text-slate-950 border-emerald-400 shadow-sm' 
                : 'bg-slate-900/80 text-slate-300 border-slate-700 hover:border-slate-500'
            }">
                ${sz}
            </button>
        `).join('') : '';

        return `
            <div class="product-card bg-brand-card/90 border border-brand-border rounded-2xl overflow-hidden flex flex-col justify-between">
                <div class="relative h-52 w-full overflow-hidden bg-slate-900">
                    <img src="${prod.imageUrl}" alt="${prod.name}" class="w-full h-full object-cover transition-transform duration-500 hover:scale-105" onerror="this.src='https://images.unsplash.com/photo-1552346154-21d32810aba3?auto=format&fit=crop&w=600&q=80'">
                    <div class="absolute top-3 left-3">
                        <span class="text-[10px] font-extrabold uppercase px-2.5 py-1 rounded-full bg-black/75 backdrop-blur-md text-emerald-400 border border-emerald-500/30">
                            ${prod.badge || prod.brand}
                        </span>
                    </div>
                    <div class="absolute bottom-3 right-3">
                        <span class="text-xs font-semibold px-2 py-0.5 rounded-md bg-slate-900/90 text-slate-300">
                            Pairs Left: <strong class="${prod.stock > 10 ? 'text-emerald-400' : 'text-amber-400'}">${prod.stock}</strong>
                        </span>
                    </div>
                </div>
                
                <div class="p-5 flex-1 flex flex-col justify-between">
                    <div>
                        <div class="flex items-center justify-between">
                            <span class="text-[11px] font-bold text-slate-400 uppercase tracking-wider">${prod.brand}</span>
                            <span class="text-[11px] text-slate-500">${prod.category}</span>
                        </div>
                        <h3 class="text-base font-bold text-white mt-1 leading-snug">${prod.name}</h3>
                        
                        <!-- Size Selector -->
                        <div class="mt-3">
                            <span class="text-[10px] font-semibold text-slate-400 uppercase block mb-1.5">Select Size:</span>
                            <div class="flex flex-wrap gap-1.5">
                                ${sizeButtons}
                            </div>
                        </div>
                    </div>

                    <div class="mt-4 pt-4 border-t border-brand-border/60 flex items-center justify-between">
                        <div>
                            <span class="text-[11px] text-slate-500 block">Retail Price</span>
                            <span class="text-xl font-extrabold text-white">$${prod.price.toFixed(2)}</span>
                        </div>
                        <button onclick="addToCart('${prod.id}')" class="px-4 py-2 rounded-xl bg-emerald-500 hover:bg-emerald-600 text-slate-950 font-bold text-xs shadow-md shadow-emerald-500/20 transition-all flex items-center gap-1.5 hover:scale-105">
                            <i class="fa-solid fa-cart-plus"></i> Add
                        </button>
                    </div>
                </div>
            </div>
        `;
    }).join('');
}

// Sneaker Cart Operations
function addToCart(productId) {
    const product = products.find(p => p.id === productId);
    if (!product) return;

    const size = selectedSizes[productId] || (product.availableSizes ? product.availableSizes[0] : 'US 10');
    const existing = cart.find(item => item.productId === productId && item.size === size);

    if (existing) {
        existing.quantity += 1;
    } else {
        cart.push({
            productId: product.id,
            productName: product.name,
            size: size,
            price: product.price,
            quantity: 1
        });
    }

    updateCartUI();
    showToast(`Added "${product.name} (${size})" to cart!`, 'success');
}

function updateCartUI() {
    const totalItems = cart.reduce((acc, item) => acc + item.quantity, 0);
    const subtotal = cart.reduce((acc, item) => acc + (item.price * item.quantity), 0);

    const countBadge = document.getElementById('cartCountBadge');
    if (countBadge) countBadge.textContent = totalItems;

    const subtotalVal = document.getElementById('cartSubtotalVal');
    const totalVal = document.getElementById('cartTotalVal');
    if (subtotalVal) subtotalVal.textContent = `$${subtotal.toFixed(2)}`;
    if (totalVal) totalVal.textContent = `$${subtotal.toFixed(2)}`;

    const itemsContainer = document.getElementById('cartItemsList');
    if (!itemsContainer) return;

    if (cart.length === 0) {
        itemsContainer.innerHTML = `
            <div class="text-center py-12 text-slate-500">
                <i class="fa-solid fa-shoe-prints text-4xl mb-3 block text-slate-600"></i>
                <p>No sneakers in cart. Select your size and click Add to Cart.</p>
            </div>
        `;
    } else {
        itemsContainer.innerHTML = cart.map(item => `
            <div class="p-3 bg-slate-900 rounded-xl border border-slate-800 flex items-center justify-between">
                <div>
                    <h4 class="text-sm font-semibold text-white leading-tight">${item.productName}</h4>
                    <span class="text-xs text-emerald-400 font-medium">Size: ${item.size}</span>
                    <span class="text-xs text-slate-400 ml-2">$${item.price.toFixed(2)} × ${item.quantity}</span>
                </div>
                <div class="flex items-center gap-2">
                    <span class="text-sm font-bold text-emerald-400">$${(item.price * item.quantity).toFixed(2)}</span>
                    <button onclick="removeFromCart('${item.productId}', '${item.size}')" class="text-slate-500 hover:text-red-400 ml-2">
                        <i class="fa-solid fa-trash-can text-xs"></i>
                    </button>
                </div>
            </div>
        `).join('');
    }
}

function removeFromCart(productId, size) {
    cart = cart.filter(item => !(item.productId === productId && item.size === size));
    updateCartUI();
}

function toggleCartModal() {
    const modal = document.getElementById('cartModal');
    if (!modal) return;
    modal.classList.toggle('hidden');
}

// Checkout Order Dispatch
async function checkoutOrder() {
    if (cart.length === 0) {
        showToast('Cart is empty!', 'warning');
        return;
    }

    const checkoutBtn = document.getElementById('checkoutBtn');
    if (checkoutBtn) checkoutBtn.disabled = true;

    try {
        const orderPayload = {
            customerName: "Sneakerhead Drop Buyer",
            customerEmail: "sneakers@cloudkicks.io",
            items: cart
        };

        const startTime = performance.now();
        const res = await fetch('/api/orders', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(orderPayload)
        });
        const elapsed = Math.round(performance.now() - startTime);

        if (!res.ok) {
            const errJson = await res.json().catch(() => ({}));
            throw new Error(errJson.error || `HTTP ${res.status}`);
        }

        const confirmedOrder = await res.json();
        logConsole(`[SNEAKER CHECKOUT SUCCESS] Order: ${confirmedOrder.orderId} | Total: $${confirmedOrder.totalAmount} | Latency: ${elapsed}ms`, 'success');
        showToast(`Sneaker Order Confirmed! ID: ${confirmedOrder.orderId}`, 'success');

        cart = [];
        updateCartUI();
        toggleCartModal();
        loadProducts(); // refresh inventory stock
        fetchMetricsSummary(); // update KPI
    } catch (err) {
        logConsole(`[CHECKOUT FAILED] ${err.message}`, 'error');
        showToast(`Checkout Failed: ${err.message}`, 'error');
    } finally {
        if (checkoutBtn) checkoutBtn.disabled = false;
    }
}

// Real-Time Telemetry & Metrics Polling
function startMetricsPolling() {
    fetchMetricsSummary();
    if (pollingInterval) clearInterval(pollingInterval);
    pollingInterval = setInterval(fetchMetricsSummary, 3000);
}

async function fetchMetricsSummary() {
    try {
        const start = performance.now();
        const res = await fetch('/api/metrics/summary');
        const latency = Math.round(performance.now() - start);

        if (res.ok) {
            const data = await res.json();
            
            // Latency KPI
            const latencyVal = document.getElementById('latencyVal');
            const latencySub = document.getElementById('latencySubText');
            if (latencyVal) latencyVal.textContent = `${latency} ms`;
            if (latencySub) {
                if (latency > 500) {
                    latencySub.className = "mt-1 text-[11px] text-red-400 font-bold";
                    latencySub.innerHTML = '<i class="fa-solid fa-triangle-exclamation"></i> DEGRADED (&gt;500ms ALERT)';
                } else if (latency > 250) {
                    latencySub.className = "mt-1 text-[11px] text-amber-400 font-semibold";
                    latencySub.innerHTML = '<i class="fa-solid fa-clock"></i> Moderate';
                } else {
                    latencySub.className = "mt-1 text-[11px] text-emerald-400";
                    latencySub.innerHTML = '<i class="fa-solid fa-arrow-down"></i> Optimal (&lt; 200ms)';
                }
            }

            // Orders & Drop Shoppers
            const ordersVal = document.getElementById('totalOrdersVal');
            if (ordersVal) ordersVal.textContent = data.ordersPlacedTotal;

            const shoppersVal = document.getElementById('activeShoppersVal');
            if (shoppersVal) shoppersVal.textContent = data.activeShoppers;

            const appStatusVal = document.getElementById('appStatusVal');
            if (appStatusVal) appStatusVal.textContent = data.status;
        }
    } catch (e) {
        const appStatusVal = document.getElementById('appStatusVal');
        if (appStatusVal) appStatusVal.textContent = 'UNREACHABLE';
    }
}

// Sneaker Drop Rush Simulator
async function simulateTraffic(requestCount) {
    logConsole(`[DROP SIMULATOR] Simulating burst of ${requestCount} concurrent sneaker checkout transactions...`, 'info');
    document.getElementById('consoleStatusText').textContent = 'DROP RUSH IN PROGRESS...';

    const shoeIds = ['SHOE-001', 'SHOE-002', 'SHOE-003', 'SHOE-004', 'SHOE-005'];
    let successCount = 0;
    let failCount = 0;

    const promises = [];
    for (let i = 0; i < requestCount; i++) {
        const randomShoeId = shoeIds[Math.floor(Math.random() * shoeIds.length)];
        const payload = {
            customerName: `Bot_Shopper_${i + 1}`,
            customerEmail: `bot-${i+1}@snkrs-rush.io`,
            items: [{ productId: randomShoeId, size: "US 10.5", quantity: 1 }]
        };

        const p = fetch('/api/orders', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        })
        .then(res => {
            if (res.ok) successCount++;
            else failCount++;
        })
        .catch(() => failCount++);

        promises.push(p);
    }

    await Promise.all(promises);
    document.getElementById('consoleStatusText').textContent = 'Ready';
    logConsole(`[DROP COMPLETED] Total: ${requestCount} | Confirmed: ${successCount} | Failed: ${failCount}`, successCount > 0 ? 'success' : 'error');
    showToast(`Drop burst completed: ${successCount}/${requestCount} pairs purchased`, 'info');
    fetchMetricsSummary();
    loadProducts();
}

async function injectLatency(delayMs) {
    try {
        const res = await fetch(`/api/simulate/latency?delayMs=${delayMs}`, { method: 'POST' });
        const data = await res.json();
        logConsole(`[ALERT TRIGGER] Injected ${delayMs}ms artificial delay! Prometheus SLA histogram breached.`, 'warning');
        showToast(`Drop Congestion Injected: ${delayMs}ms. Grafana Alert will fire!`, 'warning');
        fetchMetricsSummary();
    } catch (err) {
        logConsole(`Failed to set latency: ${err.message}`, 'error');
    }
}

async function resetSimulation() {
    try {
        const res = await fetch('/api/simulate/reset', { method: 'POST' });
        const data = await res.json();
        logConsole(`[SYSTEM NORMALIZED] Bottlenecks cleared. Baseline response time restored.`, 'success');
        showToast('Sneaker platform normalized.', 'success');
        fetchMetricsSummary();
    } catch (err) {
        logConsole(`Failed to reset: ${err.message}`, 'error');
    }
}

// Console & Toast Helpers
function logConsole(message, type = 'info') {
    const consoleEl = document.getElementById('trafficConsole');
    if (!consoleEl) return;

    const time = new Date().toLocaleTimeString();
    let colorClass = 'text-slate-300';
    if (type === 'success') colorClass = 'text-emerald-400 font-semibold';
    if (type === 'warning') colorClass = 'text-amber-400 font-semibold';
    if (type === 'error') colorClass = 'text-red-400 font-bold';

    const line = document.createElement('div');
    line.className = `${colorClass} text-xs`;
    line.textContent = `[${time}] ${message}`;
    consoleEl.appendChild(line);
    consoleEl.scrollTop = consoleEl.scrollHeight;
}

function showToast(message, type = 'info') {
    const container = document.getElementById('toastContainer');
    if (!container) return;

    const toast = document.createElement('div');
    let bg = 'bg-slate-800 border-slate-700 text-white';
    if (type === 'success') bg = 'bg-emerald-950 border-emerald-500/40 text-emerald-200';
    if (type === 'warning') bg = 'bg-amber-950 border-amber-500/40 text-amber-200';
    if (type === 'error') bg = 'bg-red-950 border-red-500/40 text-red-200';

    toast.className = `p-4 rounded-xl border shadow-xl text-xs flex items-center gap-2 ${bg} animate-fade-in transition-all`;
    toast.innerHTML = `<i class="fa-solid fa-circle-info"></i><span>${message}</span>`;
    container.appendChild(toast);

    setTimeout(() => {
        toast.remove();
    }, 3500);
}
