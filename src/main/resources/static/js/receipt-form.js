(function () {
    'use strict';

    const BOOT = window.RECEIPT_BOOT || {};
    const MAX_PHOTOS = 12;
    const PHOTO_MAX_SIDE = 1600;
    const DRAFT_TTL_MS = 24 * 60 * 60 * 1000;

    const state = {
        mode: 'client',
        clientId: '',
        client: null,
        workOrder: null,
        items: [],
        photos: [],
        dirty: false,
        saving: false,
        workOrdersCache: null,
        seller: '',
        clientSignature: null
    };
    let draftTimer = null;

    const $ = id => document.getElementById(id);
    const esc = v => String(v ?? '').replace(/[&<>"']/g, ch => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[ch]));
    const brl = v => new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(Number(v) || 0);
    const round2 = n => Math.round((Number(n) + Number.EPSILON) * 100) / 100;

    function parseBRL(value) {
        if (typeof value === 'number') return Number.isFinite(value) ? value : 0;
        let t = String(value ?? '').replace(/R\$/gi, '').replace(/\s/g, '');
        if (!t) return 0;
        if (t.includes(',')) t = t.replace(/\./g, '').replace(',', '.');
        const n = Number(t);
        return Number.isFinite(n) ? n : 0;
    }

    function csrf() {
        return {
            header: document.querySelector('meta[name="_csrf_header"]').getAttribute('content'),
            token: document.querySelector('meta[name="_csrf"]').getAttribute('content')
        };
    }

    function toggleModal(id, show) {
        const m = $(id);
        if (!m) return;
        m.classList.toggle('hidden', !show);
        m.classList.toggle('flex', show);
        if (show && window.lucide) window.lucide.createIcons();
    }
    window.closeModal = id => toggleModal(id, false);

    function markDirty() {
        state.dirty = true;
        scheduleDraft();
    }


    function paymentOptions() {
        return Array.from($('receiptPaymentOptions').querySelectorAll('input[name="receiptPaymentMethod"]'));
    }

    function selectedPaymentMethods() {
        return paymentOptions().filter(option => option.checked).map(option => option.value);
    }

    function updateCreditInstallments() {
        const selected = selectedPaymentMethods().includes('Cartão de Crédito');
        $('receiptCreditInstallmentsBlock').classList.toggle('hidden', !selected);
        $('receiptCreditInstallments').disabled = !selected;
        $('receiptCreditInstallments').required = selected;
    }

    function paymentTerms() {
        return selectedPaymentMethods().map(method => {
            if (method !== 'Cartão de Crédito') return method;
            const installments = Number($('receiptCreditInstallments').value);
            return method + ' (' + installments + (installments === 1 ? ' parcela)' : ' parcelas)');
        }).join('\n');
    }

    function setPaymentTerms(value) {
        const options = $('receiptPaymentOptions');
        options.querySelectorAll('[data-imported-payment]').forEach(option => option.remove());
        paymentOptions().forEach(option => { option.checked = false; });
        $('receiptCreditInstallments').value = '1';
        String(value || '').split(/\r?\n/).map(term => term.trim()).filter(Boolean).forEach(term => {
            const credit = term.match(/^Cartão de Crédito(?:\s*\((?:em até\s+)?(\d+)(?:x|\s+parcelas?)\))?$/);
            const method = credit ? 'Cartão de Crédito' : term;
            const existing = paymentOptions().find(option => option.value === method);
            if (existing) {
                existing.checked = true;
                if (credit && credit[1]) $('receiptCreditInstallments').value = credit[1];
                return;
            }
            const label = document.createElement('label');
            label.className = 'flex items-center gap-3 cursor-pointer rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-950 px-3 py-3 text-sm text-slate-900 dark:text-white sm:col-span-2';
            label.dataset.importedPayment = 'true';
            const checkbox = document.createElement('input');
            checkbox.type = 'checkbox';
            checkbox.name = 'receiptPaymentMethod';
            checkbox.value = term;
            checkbox.checked = true;
            checkbox.className = 'w-4 h-4 shrink-0 accent-brand-600 dark:accent-cyan-500';
            const text = document.createElement('span');
            text.className = 'min-w-0 break-words';
            text.textContent = term;
            label.append(checkbox, text);
            options.appendChild(label);
        });
        updateCreditInstallments();
    }



    function setMode(mode, opts) {
        state.mode = mode;
        const active = 'bg-white dark:bg-slate-700 text-brand-600 dark:text-cyan-400 shadow-sm';
        const idle = 'text-slate-500 dark:text-slate-400 hover:text-slate-700 dark:hover:text-slate-200';
        $('modeClientBtn').className = 'min-h-[44px] rounded-lg text-sm font-bold flex items-center justify-center gap-2 transition-all ' + (mode === 'client' ? active : idle);
        $('modeWorkOrderBtn').className = 'min-h-[44px] rounded-lg text-sm font-bold flex items-center justify-center gap-2 transition-all ' + (mode === 'workorder' ? active : idle);
        $('workOrderBlock').classList.toggle('hidden', mode !== 'workorder');
        $('clientAutoHint').classList.toggle('hidden', mode !== 'workorder');
        $('clientSearchBtn').classList.toggle('hidden', mode === 'workorder');
        $('receiptClientName').classList.toggle('cursor-pointer', mode !== 'workorder');
        $('reloadFromWorkOrderBtn').classList.toggle('hidden', mode !== 'workorder' || !state.workOrder);
        $('reloadFromWorkOrderBtn').classList.toggle('inline-flex', mode === 'workorder' && !!state.workOrder);

        if (!(opts && opts.keepData)) {
            if (mode === 'client') {
                state.workOrder = null;
                $('receiptWorkOrderLabel').value = '';
                $('workOrderWarning').classList.add('hidden');
                $('reloadFromWorkOrderBtn').classList.add('hidden');
                $('reloadFromWorkOrderBtn').classList.remove('inline-flex');
            } else {
                if (!state.workOrder) setClient(null);
            }
            markDirty();
        }
        if (window.lucide) window.lucide.createIcons();
    }
    window.setMode = setMode;


    function setClient(client) {
        state.client = client || null;
        state.clientId = client ? client.id : '';
        $('receiptClientName').value = client ? client.name : '';
        const panel = $('clientDetailsPanel');
        const rows = [
            ['detailDocContainer', 'detailDoc', client && client.document],
            ['detailPhoneContainer', 'detailPhone', client && client.phone],
            ['detailEmailContainer', 'detailEmail', client && client.email],
            ['detailAddressContainer', 'detailAddress', client && client.address]
        ];
        let any = false;
        rows.forEach(([box, span, value]) => {
            $(span).textContent = value || '';
            $(box).classList.toggle('hidden', !value);
            if (value) any = true;
        });
        panel.classList.toggle('hidden', !client || !any);
    }

    window.openClientModal = function () {
        if (state.mode === 'workorder') return;
        $('searchClientInput').value = '';
        renderClientList();
        toggleModal('clientSearchModal', true);
        setTimeout(() => $('searchClientInput').focus(), 100);
    };

    window.renderClientList = function () {
        const term = $('searchClientInput').value.trim().toLowerCase();
        const list = $('clientSearchList');
        const clients = (BOOT.clients || []).filter(c =>
            !term || [c.name, c.document, c.city].some(v => String(v || '').toLowerCase().includes(term)));
        list.innerHTML = '';
        $('emptyClientSearch').classList.toggle('hidden', clients.length > 0);
        clients.forEach(c => {
            const li = document.createElement('li');
            li.className = 'p-4 hover:bg-slate-50 dark:hover:bg-slate-800/50 cursor-pointer transition-colors';
            const badges = [c.document, c.city].filter(Boolean)
                .map(b => `<span class="inline-block bg-slate-100 dark:bg-slate-800 text-slate-500 dark:text-slate-400 text-[10px] px-2 py-1 rounded">${esc(b)}</span>`).join('');
            li.innerHTML = `<div class="font-bold text-slate-900 dark:text-white text-sm">${esc(c.name)}</div><div class="flex flex-wrap gap-2 mt-1.5">${badges}</div>`;
            li.onclick = () => { setClient(c); markDirty(); closeModal('clientSearchModal'); };
            list.appendChild(li);
        });
    };


    window.openWorkOrderModal = async function () {
        $('searchWorkOrderInput').value = '';
        toggleModal('workOrderModal', true);
        const status = $('workOrderStatus');
        if (!state.workOrdersCache) {
            status.textContent = 'Carregando ordens de serviço...';
            status.classList.remove('hidden');
            $('workOrderList').innerHTML = '';
            try {
                const res = await fetch('/receipts/work-orders', { credentials: 'same-origin' });
                if (!res.ok) throw new Error();
                state.workOrdersCache = await res.json();
            } catch (e) {
                status.textContent = 'Não foi possível carregar as ordens de serviço.';
                return;
            }
        }
        renderWorkOrderList();
        setTimeout(() => $('searchWorkOrderInput').focus(), 100);
    };

    window.renderWorkOrderList = function () {
        if (!state.workOrdersCache) return;
        const term = $('searchWorkOrderInput').value.trim().toLowerCase();
        const list = $('workOrderList');
        const status = $('workOrderStatus');
        const found = state.workOrdersCache.filter(w =>
            !term || String(w.number || '').toLowerCase().includes(term) || String(w.clientName || '').toLowerCase().includes(term));
        list.innerHTML = '';
        status.textContent = 'Nenhuma ordem de serviço encontrada.';
        status.classList.toggle('hidden', found.length > 0);
        found.slice(0, 80).forEach(w => {
            const li = document.createElement('li');
            li.className = 'p-4 hover:bg-slate-50 dark:hover:bg-slate-800/50 cursor-pointer transition-colors flex items-center justify-between gap-3';
            li.innerHTML = `<div class="min-w-0">
                    <div class="font-mono font-bold text-brand-600 dark:text-cyan-400 text-sm">${esc(w.number)}</div>
                    <div class="font-semibold text-slate-900 dark:text-white text-sm break-words">${esc(w.clientName || 'Sem cliente')}</div>
                    <div class="text-xs text-slate-500">${esc(w.date)}</div>
                </div>
                <div class="shrink-0 font-mono font-bold text-emerald-600 dark:text-emerald-400 text-sm">${brl(w.total)}</div>`;
            li.onclick = () => { closeModal('workOrderModal'); selectWorkOrder(w.id, w.number, true); };
            list.appendChild(li);
        });
    };

    async function fetchWorkOrderData(id) {
        const res = await fetch('/receipts/work-order-data/' + encodeURIComponent(id), { credentials: 'same-origin' });
        if (!res.ok) throw new Error((await res.text()) || 'Não foi possível carregar a O.S.');
        return res.json();
    }

    async function selectWorkOrder(id, number, fillAll) {
        try {
            const data = await fetchWorkOrderData(id);
            state.workOrder = { id: data.id, number: data.number || number };
            $('receiptWorkOrderLabel').value = state.workOrder.number + (data.client ? ' — ' + data.client.name : '');
            setClient(data.client);
            state.items = (data.items || []).map(i => ({
                description: i.description, quantity: Number(i.quantity) || 1, unitPrice: Number(i.unitPrice) || 0
            }));
            $('receiptDiscount').value = Number(data.discount) > 0 ? brl(data.discount) : '';
            if (fillAll) {
                if (data.paymentTerms) setPaymentTerms(data.paymentTerms);
                if (data.warranty) $('receiptWarranty').value = data.warranty;
            }
            const warn = $('workOrderWarning');
            if (Number(data.existingReceipts) > 0) {
                warn.textContent = 'Atenção: já existe ' + data.existingReceipts + ' recibo(s) emitido(s) para esta O.S.';
                warn.classList.remove('hidden');
            } else {
                warn.classList.add('hidden');
            }
            setMode('workorder', { keepData: true });
            renderItems();
            markDirty();
        } catch (e) {
            alert(e.message || 'Não foi possível carregar a O.S.');
        }
    }

    window.reloadFromWorkOrder = function () {
        if (!state.workOrder) return;
        if (!confirm('Recarregar os serviços e o desconto da O.S.? As edições feitas nos serviços serão substituídas.')) return;
        selectWorkOrder(state.workOrder.id, state.workOrder.number, false);
    };


    function itemRow(item, idx) {
        const sub = round2((Number(item.quantity) || 0) * (Number(item.unitPrice) || 0));
        return `<div class="rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50/60 dark:bg-slate-900/40 p-4" data-idx="${idx}">
            <div class="flex items-start justify-between gap-3 mb-3">
                <span class="text-xs font-black uppercase tracking-wider text-slate-400">Serviço ${idx + 1}</span>
                <button type="button" data-action="remove" class="p-2 -m-1 rounded-lg text-rose-600 dark:text-rose-400 hover:bg-rose-50 dark:hover:bg-rose-500/10" aria-label="Remover serviço">
                    <i data-lucide="trash-2" class="w-4 h-4"></i>
                </button>
            </div>
            <textarea data-field="description" rows="5" placeholder="Descrição do serviço realizado..." class="w-full min-h-[160px] bg-white dark:bg-slate-950 border border-slate-300 dark:border-slate-700 rounded-xl p-4 text-sm sm:text-base leading-relaxed text-slate-900 dark:text-white outline-none focus:border-brand-500 dark:focus:border-cyan-500 resize-y">${esc(item.description)}</textarea>
            <div class="grid grid-cols-2 sm:grid-cols-3 gap-3 mt-3">
                <div>
                    <label class="block text-[11px] font-bold uppercase tracking-wider text-slate-500 mb-1">Qtd.</label>
                    <input type="number" data-field="quantity" min="0.01" step="any" inputmode="decimal" value="${esc(item.quantity)}" class="w-full bg-white dark:bg-slate-900 border border-slate-300 dark:border-slate-700 rounded-xl px-3 py-2.5 text-sm text-slate-900 dark:text-white outline-none focus:border-brand-500 dark:focus:border-cyan-500">
                </div>
                <div>
                    <label class="block text-[11px] font-bold uppercase tracking-wider text-slate-500 mb-1">Valor unit.</label>
                    <input type="text" data-field="unitPrice" inputmode="decimal" value="${esc(brl(item.unitPrice))}" class="w-full bg-white dark:bg-slate-900 border border-slate-300 dark:border-slate-700 rounded-xl px-3 py-2.5 text-sm text-slate-900 dark:text-white outline-none focus:border-brand-500 dark:focus:border-cyan-500">
                </div>
                <div class="col-span-2 sm:col-span-1 flex sm:flex-col items-center sm:items-end justify-between sm:justify-end">
                    <span class="text-[11px] font-bold uppercase tracking-wider text-slate-500">Subtotal</span>
                    <span data-role="subtotal" class="font-mono font-black text-slate-800 dark:text-slate-100">${brl(sub)}</span>
                </div>
            </div>
        </div>`;
    }

    function renderItems() {
        const box = $('itemsList');
        if (!state.items.length) {
            box.innerHTML = '<p class="text-sm text-slate-400 dark:text-slate-500 py-3">Nenhum serviço adicionado. Use o botão abaixo ou selecione uma O.S.</p>';
        } else {
            box.innerHTML = state.items.map(itemRow).join('');
        }
        updateTotals();
        if (window.lucide) window.lucide.createIcons();
    }

    window.addItem = function () {
        state.items.push({ description: '', quantity: 1, unitPrice: 0 });
        renderItems();
        markDirty();
        const areas = $('itemsList').querySelectorAll('textarea');
        if (areas.length) areas[areas.length - 1].focus();
    };

    function subtotalOf() {
        return round2(state.items.reduce((acc, i) => acc + (Number(i.quantity) || 0) * (Number(i.unitPrice) || 0), 0));
    }

    function discountValue() { return Math.max(0, round2(parseBRL($('receiptDiscount').value))); }

    function updateTotals() {
        const subtotal = subtotalOf();
        const discount = discountValue();
        const invalid = discount > subtotal;
        $('discountError').classList.toggle('hidden', !invalid);
        $('subtotalRow').classList.toggle('hidden', !(discount > 0));
        $('subtotalRow').classList.toggle('flex', discount > 0);
        $('liveSubtotal').textContent = brl(subtotal);
        $('liveTotal').textContent = brl(invalid ? subtotal : subtotal - discount);
    }

    function initItemEvents() {
        const box = $('itemsList');
        box.addEventListener('input', e => {
            const card = e.target.closest('[data-idx]');
            const field = e.target.dataset.field;
            if (!card || !field) return;
            const item = state.items[Number(card.dataset.idx)];
            if (!item) return;
            if (field === 'description') item.description = e.target.value;
            if (field === 'quantity') item.quantity = Number(e.target.value) || 0;
            if (field === 'unitPrice') item.unitPrice = parseBRL(e.target.value);
            const sub = card.querySelector('[data-role="subtotal"]');
            if (sub) sub.textContent = brl((Number(item.quantity) || 0) * (Number(item.unitPrice) || 0));
            updateTotals();
            markDirty();
        });
        box.addEventListener('focusout', e => {
            if (e.target.dataset && e.target.dataset.field === 'unitPrice') e.target.value = brl(parseBRL(e.target.value));
        });
        box.addEventListener('click', e => {
            const btn = e.target.closest('[data-action="remove"]');
            const card = e.target.closest('[data-idx]');
            if (!btn || !card) return;
            state.items.splice(Number(card.dataset.idx), 1);
            renderItems();
            markDirty();
        });
        const discount = $('receiptDiscount');
        discount.addEventListener('input', () => { updateTotals(); markDirty(); });
        discount.addEventListener('blur', () => { const v = parseBRL(discount.value); discount.value = v > 0 ? brl(v) : ''; updateTotals(); });
    }


    function renderPhotos() {
        const grid = $('photosGrid');
        $('photosEmpty').classList.toggle('hidden', state.photos.length > 0);
        grid.innerHTML = state.photos.map((p, i) => `
            <div class="relative group rounded-xl overflow-hidden border border-slate-200 dark:border-slate-700 bg-slate-100 dark:bg-slate-900 aspect-square">
                <img src="${esc(p.dataUri || p.url)}" alt="Foto ${i + 1}" class="w-full h-full object-cover">
                <button type="button" data-photo="${i}" class="absolute top-1.5 right-1.5 min-w-[36px] min-h-[36px] rounded-full bg-rose-600/90 hover:bg-rose-700 text-white flex items-center justify-center shadow" aria-label="Remover foto">
                    <i data-lucide="x" class="w-4 h-4"></i>
                </button>
            </div>`).join('');
        if (window.lucide) window.lucide.createIcons();
    }

    function photoStatus(text) {
        const el = $('photosStatus');
        el.textContent = text || '';
        el.classList.toggle('hidden', !text);
    }

    function readAsDataUrl(file) {
        return new Promise((resolve, reject) => {
            const reader = new FileReader();
            reader.onload = () => resolve(reader.result);
            reader.onerror = () => reject(new Error('Falha ao ler o arquivo.'));
            reader.readAsDataURL(file);
        });
    }

    function loadImage(src) {
        return new Promise((resolve, reject) => {
            const img = new Image();
            img.onload = () => resolve(img);
            img.onerror = () => reject(new Error('Imagem inválida.'));
            img.src = src;
        });
    }

    async function shrinkPhoto(file) {
        const img = await loadImage(await readAsDataUrl(file));
        const scale = Math.min(1, PHOTO_MAX_SIDE / Math.max(img.naturalWidth, img.naturalHeight));
        const canvas = document.createElement('canvas');
        canvas.width = Math.max(1, Math.round(img.naturalWidth * scale));
        canvas.height = Math.max(1, Math.round(img.naturalHeight * scale));
        const ctx = canvas.getContext('2d');
        ctx.fillStyle = '#ffffff';
        ctx.fillRect(0, 0, canvas.width, canvas.height);
        ctx.drawImage(img, 0, 0, canvas.width, canvas.height);
        return canvas.toDataURL('image/jpeg', 0.82);
    }

    window.onPhotosSelected = async function (input) {
        const files = Array.from(input.files || []);
        input.value = '';
        if (!files.length) return;
        const room = MAX_PHOTOS - state.photos.length;
        if (room <= 0) { photoStatus('Limite de ' + MAX_PHOTOS + ' fotos por recibo.'); return; }
        let skipped = 0;
        photoStatus('Processando fotos...');
        for (const file of files.slice(0, room)) {
            if (!file.type.startsWith('image/')) { skipped++; continue; }
            try {
                state.photos.push({ dataUri: await shrinkPhoto(file) });
            } catch (e) { skipped++; }
        }
        renderPhotos();
        markDirty();
        const notes = [];
        if (files.length > room) notes.push('Limite de ' + MAX_PHOTOS + ' fotos: algumas não foram adicionadas.');
        if (skipped) notes.push(skipped + ' arquivo(s) não puderam ser usados.');
        photoStatus(notes.join(' '));
    };

    function initPhotoEvents() {
        $('photosGrid').addEventListener('click', e => {
            const btn = e.target.closest('[data-photo]');
            if (!btn) return;
            state.photos.splice(Number(btn.dataset.photo), 1);
            renderPhotos();
            markDirty();
        });
    }


    function draftKey() { return 'receiptDraft:' + ($('receiptId').value || 'new'); }

    function collectDraft() {
        return {
            savedAt: Date.now(),
            mode: state.mode,
            profileId: $('receiptProfile').value,
            client: state.client,
            workOrder: state.workOrder,
            issueDate: $('receiptIssueDate').value,
            receivedDate: $('receiptReceivedDate').value,
            items: state.items,
            discount: $('receiptDiscount').value,
            payment: selectedPaymentMethods().join('\n'),
            creditInstallments: $('receiptCreditInstallments').value,
            warranty: $('receiptWarranty').value,
            description: $('receiptDescription').value
        };
    }

    function scheduleDraft() {
        clearTimeout(draftTimer);
        draftTimer = setTimeout(() => {
            try { localStorage.setItem(draftKey(), JSON.stringify(collectDraft())); } catch (e) { /* sem armazenamento */ }
        }, 400);
    }

    window.discardDraft = function (reload) {
        try { localStorage.removeItem(draftKey()); } catch (e) { }
        $('draftRestoredBanner').classList.add('hidden');
        $('draftRestoredBanner').classList.remove('flex');
        if (reload) window.location.reload();
    };

    function restoreDraft() {
        let draft;
        try { draft = JSON.parse(localStorage.getItem(draftKey()) || 'null'); } catch (e) { return false; }
        if (!draft) return false;
        if (Date.now() - (draft.savedAt || 0) > DRAFT_TTL_MS) { discardDraft(false); return false; }

        applyForm({
            profileId: draft.profileId, client: draft.client, workOrder: draft.workOrder,
            issueDate: draft.issueDate, receivedDate: draft.receivedDate, items: draft.items,
            discount: draft.discount, payment: draft.payment, creditInstallments: draft.creditInstallments, warranty: draft.warranty,
            description: draft.description, mode: draft.mode
        });
        $('draftRestoredBanner').classList.remove('hidden');
        $('draftRestoredBanner').classList.add('flex');
        return true;
    }

    function applyForm(v) {
        if (v.profileId) $('receiptProfile').value = v.profileId;
        $('receiptIssueDate').value = v.issueDate || '';
        $('receiptReceivedDate').value = v.receivedDate || '';
        state.workOrder = v.workOrder || null;
        $('receiptWorkOrderLabel').value = state.workOrder
            ? state.workOrder.number + (v.client ? ' — ' + v.client.name : '') : '';
        setClient(v.client || null);
        state.items = Array.isArray(v.items) ? v.items.map(i => ({
            description: i.description || '', quantity: Number(i.quantity) || 1, unitPrice: Number(i.unitPrice) || 0
        })) : [];
        $('receiptDiscount').value = v.discount || '';
        setPaymentTerms(v.payment);
        if (v.creditInstallments != null) $('receiptCreditInstallments').value = v.creditInstallments;
        $('receiptWarranty').value = v.warranty || '';
        $('receiptDescription').value = v.description || '';
        setMode(v.mode || (state.workOrder ? 'workorder' : 'client'), { keepData: true });
        renderItems();
    }


    function profileInfo() {
        const sel = $('receiptProfile');
        const opt = sel.options[sel.selectedIndex];
        if (!opt || !opt.value) return null;
        const base = '/quotes/company-image/' + opt.value;
        return {
            id: opt.value,
            companyName: opt.dataset.name || '',
            document: opt.dataset.document || '',
            address: opt.dataset.address || '',
            email: opt.dataset.email || '',
            phone: opt.dataset.phone || '',
            logoUrl: base + '/logo',
            signatureUrl: base + '/signature'
        };
    }

    function validate() {
        if (!$('receiptProfile').value) return 'Selecione a Empresa Emissora.';
        if (state.mode === 'workorder' && !state.workOrder) return 'Selecione a Ordem de Serviço.';
        if (!state.clientId) return 'Selecione o Cliente.';
        if (!$('receiptIssueDate').value) return 'Informe a Data de Emissão.';
        if (!$('receiptReceivedDate').value) return 'Informe a Data de Recebimento.';
        if (selectedPaymentMethods().includes('Cartão de Crédito')) {
            const installments = Number($('receiptCreditInstallments').value);
            if (!Number.isSafeInteger(installments) || installments < 1) return 'Informe um número inteiro de parcelas do cartão de crédito, maior ou igual a 1.';
        }
        const items = state.items.filter(i => String(i.description || '').trim());
        if (!items.length) return 'Adicione pelo menos um serviço com descrição.';
        if (items.some(i => !(Number(i.quantity) > 0))) return 'A quantidade de cada serviço deve ser maior que zero.';
        if (discountValue() > subtotalOf()) return 'O desconto não pode ser maior que o valor dos serviços.';
        return null;
    }

    function buildPayload() {
        return {
            id: $('receiptId').value || null,
            profileId: $('receiptProfile').value || null,
            clientId: state.clientId || null,
            workOrderId: state.mode === 'workorder' && state.workOrder ? state.workOrder.id : null,
            issueDate: $('receiptIssueDate').value || null,
            receivedDate: $('receiptReceivedDate').value || null,
            discount: discountValue(),
            paymentTerms: paymentTerms(),
            warranty: $('receiptWarranty').value,
            description: $('receiptDescription').value,
            items: state.items
                .filter(i => String(i.description || '').trim())
                .map(i => ({ description: i.description.trim(), quantity: Number(i.quantity), unitPrice: round2(i.unitPrice) })),
            photos: state.photos.map(p => p.id ? { id: p.id } : { dataUri: p.dataUri })
        };
    }

    async function persist() {
        if (state.saving) return null;
        const problem = validate();
        if (problem) { alert('Atenção: ' + problem); window.scrollTo({ top: 0, behavior: 'smooth' }); return null; }

        state.saving = true;
        const buttons = Array.from(document.querySelectorAll('.btn-save-receipt'));
        const originals = buttons.map(b => b.innerHTML);
        buttons.forEach(b => { b.disabled = true; b.innerHTML = '<i data-lucide="loader-2" class="w-4 h-4 animate-spin"></i> Salvando...'; });
        if (window.lucide) window.lucide.createIcons();

        try {
            const { header, token } = csrf();
            const res = await fetch('/receipts/save-ajax', {
                method: 'POST',
                credentials: 'same-origin',
                headers: { 'Content-Type': 'application/json', [header]: token },
                body: JSON.stringify(buildPayload())
            });
            if (res.status === 401) return null;
            if (!res.ok) {
                const text = await res.text();
                throw new Error(res.status === 413 ? 'As fotos ultrapassam o tamanho máximo. Remova algumas e tente novamente.' : (text || 'Status: ' + res.status));
            }
            const saved = await res.json();
            $('receiptId').value = saved.id;
            state.dirty = false;
            try { localStorage.removeItem('receiptDraft:new'); localStorage.removeItem('receiptDraft:' + saved.id); } catch (e) { }
            return {
                id: saved.id,
                number: saved.number,
                client: state.client,
                profile: profileInfo()
            };
        } catch (err) {
            alert('FALHA AO SALVAR RECIBO!\n\n' + err.message);
            return null;
        } finally {
            state.saving = false;
            buttons.forEach((b, i) => { b.disabled = false; b.innerHTML = originals[i]; });
            if (window.lucide) window.lucide.createIcons();
        }
    }

    window.saveReceipt = async function (e) {
        if (e) e.preventDefault();
        const saved = await persist();
        if (saved) window.location.href = '/receipts';
    };

    async function syncPhotosFromServer(id) {
        try {
            const res = await fetch('/receipts/view-data/' + encodeURIComponent(id), { credentials: 'same-origin' });
            if (!res.ok) return;
            const data = await res.json();
            state.photos = (data.photos || []).map(p => ({ id: p.id, url: p.url }));
            renderPhotos();
        } catch (e) {  }
    }

    async function ensureSaved() {
        const existingId = $('receiptId').value;
        if (existingId && !state.dirty) {
            return { id: existingId, number: ($('formTitle').dataset.number || ''), client: state.client, profile: profileInfo() };
        }
        const saved = await persist();
        if (!saved) return null;
        $('formTitle').textContent = 'Editar Recibo: ' + saved.number;
        $('formTitle').dataset.number = saved.number;
        history.replaceState(null, '', '/receipts/' + saved.id + '/edit');
        await syncPhotosFromServer(saved.id);
        return saved;
    }


    window.openPreview = function () {
        const problem = validate();
        if (problem) { alert('Atenção: ' + problem); window.scrollTo({ top: 0, behavior: 'smooth' }); return; }
        const profile = profileInfo();
        const items = state.items.filter(i => String(i.description || '').trim());
        const subtotal = subtotalOf();
        const discount = discountValue();
        const number = $('formTitle').dataset.number || '';
        ReceiptPreview.open({
            number: number || 'REC-----',
            issueDate: $('receiptIssueDate').value,
            receivedDate: $('receiptReceivedDate').value,
            profile,
            client: state.client,
            workOrder: state.mode === 'workorder' ? state.workOrder : null,
            seller: state.seller,
            clientSignature: state.clientSignature,
            items,
            discount,
            total: round2(subtotal - discount),
            description: $('receiptDescription').value,
            paymentTerms: paymentTerms(),
            warranty: $('receiptWarranty').value,
            photos: state.photos.map(p => ({ url: p.dataUri || p.url }))
        }, { ensureSaved });
    };


    async function loadForEdit(id) {
        const res = await fetch('/receipts/view-data/' + encodeURIComponent(id), { credentials: 'same-origin' });
        if (!res.ok) throw new Error('Não foi possível carregar este recibo.');
        const r = await res.json();
        applyForm({
            profileId: r.profileId,
            client: r.client,
            workOrder: r.workOrder ? { id: r.workOrder.id, number: r.workOrder.number } : null,
            issueDate: r.issueDate,
            receivedDate: r.receivedDate,
            items: r.items,
            discount: Number(r.discount) > 0 ? brl(r.discount) : '',
            payment: r.paymentTerms,
            warranty: r.warranty,
            description: r.description,
            mode: r.workOrder ? 'workorder' : 'client'
        });
        state.photos = (r.photos || []).map(p => ({ id: p.id, url: p.url }));
        state.seller = r.seller || '';
        state.clientSignature = r.clientSignature || null;
        renderPhotos();
        $('formTitle').textContent = 'Editar Recibo: ' + r.number;
        $('formTitle').dataset.number = r.number;
        document.title = 'Editar ' + r.number + ' - Grupo Glass';
        state.dirty = false;
    }

    async function init() {
        initItemEvents();
        initPhotoEvents();
        setMode('client', { keepData: true });
        renderItems();
        renderPhotos();

        ['receiptProfile', 'receiptIssueDate', 'receiptReceivedDate', 'receiptWarranty', 'receiptDescription']
            .forEach(id => $(id).addEventListener('input', markDirty));
        $('receiptProfile').addEventListener('change', markDirty);
        $('receiptPayment').addEventListener('change', () => { updateCreditInstallments(); markDirty(); });
        $('receiptCreditInstallments').addEventListener('input', markDirty);
        updateCreditInstallments();

        const receiptId = $('receiptId').value;
        if (receiptId) {
            try { await loadForEdit(receiptId); } catch (e) { alert(e.message); }
            restoreDraft();
            state.dirty = false;
            return;
        }

        state.seller = BOOT.currentUser || '';
        const today = BOOT.today || new Date().toISOString().slice(0, 10);
        $('receiptIssueDate').value = today;
        $('receiptReceivedDate').value = today;
        $('receiptDescription').value = BOOT.defaultDescription || '';
        const profiles = $('receiptProfile').options;
        if (profiles.length === 2) $('receiptProfile').value = profiles[1].value;

        const restored = restoreDraft();
        const pre = $('preselectedWorkOrderId').value;
        if (!restored && pre) {
            setMode('workorder', { keepData: true });
            await selectWorkOrder(pre, '', true);
        }
        state.dirty = false;
    }

    document.addEventListener('DOMContentLoaded', init);
    window.addEventListener('session-guard:expired', () => {
        try { localStorage.setItem(draftKey(), JSON.stringify(collectDraft())); } catch (e) { }
    });
})();
