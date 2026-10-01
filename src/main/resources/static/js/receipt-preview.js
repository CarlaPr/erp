(function () {
    'use strict';

    let current = null;
    let options = {};

    const esc = value => String(value ?? '').replace(/[&<>"']/g, ch => (
        { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[ch]));
    const brl = value => new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(Number(value) || 0);
    const qty = value => {
        const n = Number(value);
        return Number.isFinite(n) ? String(Number(n.toFixed(2))).replace('.', ',') : '1';
    };
    const dateBr = iso => (iso && /^\d{4}-\d{2}-\d{2}/.test(iso)) ? iso.slice(0, 10).split('-').reverse().join('/') : '--/--/----';
    const nl2br = text => esc(text).replace(/\r?\n/g, '<br>');

    function splitDescription(text) {
        const d = String(text || '').trim();
        const nl = d.indexOf('\n');
        if (nl > 0) return [d.slice(0, nl).trim(), d.slice(nl + 1).trim()];
        const dash = d.indexOf(' - ');
        if (dash > 0) return [d.slice(0, dash).trim(), d.slice(dash + 3).trim()];
        return [d, ''];
    }

    function section(title, bodyHtml) {
        return `<div class="rcp-section"><div class="rcp-section-head">${esc(title)}</div><div class="rcp-section-body">${bodyHtml}</div></div>`;
    }

    function render(d) {
        const p = d.profile || {};
        const c = d.client || {};
        const items = Array.isArray(d.items) ? d.items : [];
        const discount = Number(d.discount) || 0;
        const number = d.number ? d.number.replace('REC-', '') : '----';

        const contact = [p.email, p.phone].filter(Boolean).map(esc).join(' &nbsp;|&nbsp; ');
        const clientDetails = [
            c.document ? `<span><strong>CPF/CNPJ:</strong> ${esc(c.document)}</span>` : '',
            c.phone ? `<span><strong>Telefone:</strong> ${esc(c.phone)}</span>` : '',
            c.email ? `<span><strong>E-mail:</strong> ${esc(c.email)}</span>` : '',
            c.address ? `<span class="rcp-wide"><strong>Endereço:</strong> ${esc(c.address)}</span>` : ''
        ].filter(Boolean).join('');

        const rows = items.map(i => {
            const q = Number(i.quantity) || 0;
            const u = Number(i.unitPrice) || 0;
            const [title, detail] = splitDescription(i.description);
            return `<tr>
                <td class="rcp-desc"><div class="rcp-item-title">${esc(title)}</div>${detail ? `<div class="rcp-item-detail">${esc(detail)}</div>` : ''}</td>
                <td class="rcp-center rcp-muted">un</td>
                <td class="rcp-center">${qty(q)}</td>
                <td class="rcp-right rcp-muted">${brl(u)}</td>
                <td class="rcp-right rcp-strong">${brl(q * u)}</td></tr>`;
        }).join('');

        const subtotal = items.reduce((acc, i) => acc + (Number(i.quantity) || 0) * (Number(i.unitPrice) || 0), 0);
        const total = d.total !== undefined && d.total !== null ? Number(d.total) : subtotal - discount;

        const photos = Array.isArray(d.photos) ? d.photos.filter(ph => ph && ph.url) : [];

        return `
        <div class="rcp-page">
            <div class="rcp-header">
                <div class="rcp-company">
                    ${p.logoUrl ? `<img class="rcp-logo" src="${esc(p.logoUrl)}" alt="Logo" onerror="this.style.display='none'">` : ''}
                    <div>
                        <div class="rcp-company-name">${esc(p.companyName || 'Empresa')}</div>
                        <div class="rcp-company-line">CNPJ/CPF: ${esc(p.document || '--')}</div>
                        <div class="rcp-company-line">GRUPO TAHI GLASS</div>
                        ${p.address ? `<div class="rcp-company-line">${esc(p.address)}</div>` : ''}
                        ${contact ? `<div class="rcp-company-line">${contact}</div>` : ''}
                    </div>
                </div>
                <div class="rcp-badge">
                    <div class="rcp-badge-label">RECIBO</div>
                    <div class="rcp-badge-num">N° ${esc(number)}</div>
                    <div class="rcp-badge-field"><span>Data de emissão</span><strong>${dateBr(d.issueDate)}</strong></div>
                    <div class="rcp-badge-field"><span>Data de recebimento</span><strong>${dateBr(d.receivedDate)}</strong></div>
                    ${d.workOrder && d.workOrder.number ? `<div class="rcp-badge-field"><span>Ordem de serviço</span><strong>${esc(d.workOrder.number)}</strong></div>` : ''}
                </div>
            </div>
            <div class="rcp-separator"></div>

            <div class="rcp-client">
                <div class="rcp-client-bar"></div>
                <div class="rcp-client-body">
                    <div class="rcp-label">Recibo emitido para</div>
                    <div class="rcp-client-name">${esc(c.name || 'Selecione o cliente')}</div>
                    ${clientDetails ? `<div class="rcp-client-details">${clientDetails}</div>` : ''}
                </div>
            </div>

            <div class="rcp-scroll">
                <table class="rcp-items">
                    <thead><tr><th class="rcp-th-desc">Descrição do Serviço / Produto</th><th>Unid.</th><th>Qtd.</th><th>V. Unit.</th><th>Subtotal</th></tr></thead>
                    <tbody>${rows || '<tr><td colspan="5" class="rcp-center rcp-muted">Nenhum serviço informado.</td></tr>'}</tbody>
                </table>
            </div>
            <div class="rcp-totals">
                ${discount > 0 ? `<div class="rcp-total-row rcp-sub"><span>Subtotal:</span><span>${brl(subtotal)}</span></div>
                <div class="rcp-total-row rcp-disc"><span>Desconto:</span><span>- ${brl(discount)}</span></div>` : ''}
                <div class="rcp-total-row rcp-grand"><span>Valor Total Recebido</span><span>${brl(total)}</span></div>
            </div>

            ${d.description ? section('Declaração de recebimento', `<div class="rcp-statement">${nl2br(d.description)}</div>`) : ''}
            ${d.paymentTerms ? section('Condições de Pagamento', nl2br(d.paymentTerms)) : ''}
            ${d.warranty ? section('Termos de Garantia', nl2br(d.warranty)) : ''}
            ${photos.length ? section('Registro fotográfico do serviço finalizado',
                `<div class="rcp-photos">${photos.map(ph => `<img src="${esc(ph.url)}" alt="Foto do serviço">`).join('')}</div>`) : ''}

            <div class="rcp-signatures">
                <div class="rcp-sig">
                    <div class="rcp-sig-image">${p.signatureUrl ? `<img src="${esc(p.signatureUrl)}" alt="Assinatura da empresa" onerror="this.style.display='none'">` : ''}</div>
                    <div class="rcp-sig-line">
                        <div class="rcp-sig-name">${esc(p.companyName || 'Empresa')}</div>
                        <div class="rcp-sig-label">CNPJ/CPF: ${esc(p.document || '--')}</div>
                    </div>
                </div>
                <div class="rcp-sig">
                    <div class="rcp-sig-image">${d.clientSignature ? `<img src="${esc(d.clientSignature)}" alt="Assinatura do cliente">` : ''}</div>
                    <div class="rcp-sig-line">
                        <div class="rcp-sig-name">${esc(c.name || 'Cliente')}</div>
                        <div class="rcp-sig-label">${d.clientSignature ? 'Assinado Digitalmente' : 'Ciente'}</div>
                    </div>
                </div>
            </div>
        </div>`;
    }

    function modal() { return document.getElementById('receiptPreviewModal'); }

    function open(data, opts) {
        const el = modal();
        if (!el) return;
        current = data;
        options = opts || {};
        document.getElementById('receiptPreviewBody').innerHTML = render(data);
        el.classList.remove('hidden');
        el.classList.add('block');
        document.body.style.overflow = 'hidden';
        el.scrollTop = 0;
        if (window.lucide) window.lucide.createIcons();
    }

    function close() {
        const el = modal();
        if (!el) return;
        el.classList.add('hidden');
        el.classList.remove('block');
        document.body.style.overflow = '';
    }

    function resolveReceipt() {
        return async () => (options.ensureSaved ? await options.ensureSaved() : current);
    }

    document.addEventListener('keydown', e => { if (e.key === 'Escape') close(); });

    window.ReceiptPreview = {
        open,
        close,
        print: () => window.ReceiptPdf && window.ReceiptPdf.print(resolveReceipt()),
        share: () => window.ReceiptPdf && window.ReceiptPdf.share(resolveReceipt())
    };
})();
