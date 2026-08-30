(function () {
    'use strict';

    // Modal de atenção para a role VENDAS: alerta sobre Ordens de Serviço com prazo de
    // entrega expirando em até 7 dias (ou já expirado) que ainda não possuem nenhum
    // agendamento na Agenda Comercial. Sobrepõe qualquer tela do sistema (dashboard
    // comercial, orçamentos, agenda, etc.) até que o usuário tome ciência.

    const ENDPOINT = '/agenda/pending-schedule-alerts';
    const SNOOZE_KEY = 'pendingScheduleAlert:dismissedAt';
    const SNOOZE_MS = 15 * 60 * 1000; // não insiste de novo por 15 min após "Ciente" nesta sessão de navegação

    function alreadySnoozed() {
        try {
            const raw = sessionStorage.getItem(SNOOZE_KEY);
            if (!raw) return false;
            const dismissedAt = parseInt(raw, 10);
            if (Number.isNaN(dismissedAt)) return false;
            return (Date.now() - dismissedAt) < SNOOZE_MS;
        } catch (e) {
            return false;
        }
    }

    function markSnoozed() {
        try {
            sessionStorage.setItem(SNOOZE_KEY, String(Date.now()));
        } catch (e) { /* ignora se storage indisponível */ }
    }

    function escapeHtml(str) {
        if (str === null || str === undefined) return '';
        return String(str)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }

    function formatDate(isoDate) {
        if (!isoDate) return 'Sem prazo definido';
        const parts = isoDate.split('-');
        if (parts.length !== 3) return isoDate;
        return parts[2] + '/' + parts[1] + '/' + parts[0];
    }

    function deadlineLabel(item) {
        const dateStr = formatDate(item.deadlineDate);
        if (item.overdue) {
            const dias = Math.abs(item.daysRemaining);
            const sufixo = dias === 1 ? 'dia' : 'dias';
            return dateStr + ' &mdash; <strong>vencido h\u00e1 ' + dias + ' ' + sufixo + '</strong>';
        }
        if (item.daysRemaining === 0) {
            return dateStr + ' &mdash; <strong>vence hoje</strong>';
        }
        const dias = item.daysRemaining;
        const sufixo = dias === 1 ? 'dia' : 'dias';
        return dateStr + ' &mdash; <strong>vence em ' + dias + ' ' + sufixo + '</strong>';
    }

    function buildCard(item) {
        const badgeClass = item.overdue
            ? 'background:#fef2f2;color:#b91c1c;border:1px solid #fecaca;'
            : 'background:#fffbeb;color:#b45309;border:1px solid #fde68a;';
        const badgeText = item.overdue ? 'PRAZO VENCIDO' : 'PRAZO PR\u00d3XIMO';

        const services = (item.serviceDetails && item.serviceDetails.length)
            ? item.serviceDetails
            : [item.serviceType || 'Servi\u00e7o n\u00e3o detalhado'];

        const servicesHtml = services.map(function (s) {
            return '<li style="margin:0 0 2px;">' + escapeHtml(s) + '</li>';
        }).join('');

        return '' +
            '<div class="psa-card">' +
            '  <div style="display:flex;align-items:center;justify-content:space-between;gap:8px;flex-wrap:wrap;margin-bottom:6px;">' +
            '    <span style="font-weight:800;font-size:14.5px;color:#0f172a;">' +
            '      OS ' + escapeHtml(item.workOrderNumber || '\u2014') +
            (item.quoteNumber ? ' <span style="font-weight:500;color:#64748b;font-size:12.5px;">(Or\u00e7. ' + escapeHtml(item.quoteNumber) + ')</span>' : '') +
            '    </span>' +
            '    <span style="' + badgeClass + 'font-size:10.5px;font-weight:800;letter-spacing:.03em;padding:3px 8px;border-radius:999px;white-space:nowrap;">' + badgeText + '</span>' +
            '  </div>' +
            '  <div style="font-size:13.5px;color:#334155;margin-bottom:6px;">' +
            '    <i data-lucide="user" style="width:13px;height:13px;vertical-align:-2px;display:inline-block;"></i> ' +
            '    <strong>' + escapeHtml(item.clientName || 'Cliente n\u00e3o informado') + '</strong>' +
            '  </div>' +
            '  <ul style="font-size:13px;color:#475569;margin:0 0 8px;padding-left:18px;list-style:disc;">' + servicesHtml + '</ul>' +
            '  <div style="font-size:13px;color:#0f172a;background:#f8fafc;border:1px solid #e2e8f0;border-radius:8px;padding:6px 10px;display:inline-block;">' +
            '    <i data-lucide="calendar-clock" style="width:13px;height:13px;vertical-align:-2px;display:inline-block;"></i> ' +
            '    Prazo de entrega: ' + deadlineLabel(item) +
            '  </div>' +
            '</div>';
    }

    function buildOverlay(items) {
        const wrap = document.createElement('div');
        wrap.id = 'pendingScheduleAlertOverlay';

        const cardsHtml = items.map(buildCard).join('');
        const count = items.length;
        const plural = count === 1 ? 'ordem de servi\u00e7o' : 'ordens de servi\u00e7o';

        wrap.innerHTML =
            '<style>' +
            '#pendingScheduleAlertOverlay{position:fixed;inset:0;width:100%;height:100%;background:rgba(2,6,23,.72);' +
            'backdrop-filter:blur(3px);z-index:2147483000;display:flex;align-items:center;justify-content:center;' +
            'font-family:Inter,system-ui,sans-serif;padding:16px;}' +
            '#pendingScheduleAlertOverlay .psa-box{background:#ffffff;border-radius:18px;max-width:560px;width:100%;' +
            'max-height:88vh;display:flex;flex-direction:column;box-shadow:0 25px 60px rgba(0,0,0,.45);' +
            'animation:psaPop .22s ease-out;overflow:hidden;}' +
            '@keyframes psaPop{from{opacity:0;transform:scale(.95)}to{opacity:1;transform:scale(1)}}' +
            '#pendingScheduleAlertOverlay .psa-header{background:linear-gradient(135deg,#b91c1c,#dc2626);padding:22px 24px;' +
            'display:flex;align-items:flex-start;gap:14px;flex-shrink:0;}' +
            '#pendingScheduleAlertOverlay .psa-header-icon{background:rgba(255,255,255,.18);border-radius:12px;' +
            'width:42px;height:42px;display:flex;align-items:center;justify-content:center;flex-shrink:0;}' +
            '#pendingScheduleAlertOverlay h3{color:#fff;font-size:18px;font-weight:800;margin:0 0 4px;line-height:1.3;}' +
            '#pendingScheduleAlertOverlay .psa-subtitle{color:rgba(255,255,255,.9);font-size:13px;margin:0;line-height:1.45;}' +
            '#pendingScheduleAlertOverlay .psa-body{padding:18px 20px;overflow-y:auto;flex:1;background:#f8fafc;}' +
            '#pendingScheduleAlertOverlay .psa-card{background:#fff;border:1px solid #e2e8f0;border-radius:12px;' +
            'padding:14px 16px;margin-bottom:12px;}' +
            '#pendingScheduleAlertOverlay .psa-card:last-child{margin-bottom:0;}' +
            '#pendingScheduleAlertOverlay .psa-footer{padding:14px 20px;border-top:1px solid #e2e8f0;background:#fff;' +
            'flex-shrink:0;display:flex;gap:10px;flex-wrap:wrap;align-items:center;justify-content:space-between;}' +
            '#pendingScheduleAlertOverlay .psa-footer-text{font-size:12px;color:#64748b;}' +
            '@media (max-width:480px){' +
            '#pendingScheduleAlertOverlay{padding:0;}' +
            '#pendingScheduleAlertOverlay .psa-box{max-width:100%;max-height:100dvh;height:100dvh;border-radius:0;}' +
            '#pendingScheduleAlertOverlay .psa-footer{justify-content:stretch;}' +
            '#pendingScheduleAlertOverlay .psa-footer > div{width:100%;}' +
            '#pendingScheduleAlertOverlay button.psa-btn,#pendingScheduleAlertOverlay button.psa-btn-secondary{flex:1;}' +
            '}' +
            '#pendingScheduleAlertOverlay button.psa-btn{background:#0284c7;color:#fff;border:none;padding:10px 18px;' +
            'border-radius:10px;font-weight:700;font-size:13.5px;cursor:pointer;white-space:nowrap;}' +
            '#pendingScheduleAlertOverlay button.psa-btn:hover{background:#0369a1;}' +
            '#pendingScheduleAlertOverlay button.psa-btn-secondary{background:#fff;color:#334155;border:1px solid #cbd5e1;' +
            'padding:10px 18px;border-radius:10px;font-weight:700;font-size:13.5px;cursor:pointer;white-space:nowrap;}' +
            '#pendingScheduleAlertOverlay button.psa-btn-secondary:hover{background:#f1f5f9;}' +
            '</style>' +
            '<div class="psa-box" role="dialog" aria-modal="true" aria-labelledby="psaTitle">' +
            '  <div class="psa-header">' +
            '    <div class="psa-header-icon"><i data-lucide="alert-triangle" style="width:22px;height:22px;color:#fff;"></i></div>' +
            '    <div>' +
            '      <h3 id="psaTitle">Aten\u00e7\u00e3o: servi\u00e7os com prazo vencendo/vencidos</h3>' +
            '      <p class="psa-subtitle">Voc\u00ea tem ' + count + ' ' + plural + ' com o prazo de entrega expirando em at\u00e9 7 dias (ou j\u00e1 vencido) e que ainda n\u00e3o possuem nenhum agendamento na Agenda Comercial.</p>' +
            '    </div>' +
            '  </div>' +
            '  <div class="psa-body">' + cardsHtml + '</div>' +
            '  <div class="psa-footer">' +
            '    <div style="display:flex;gap:10px;">' +
            '      <button type="button" class="psa-btn-secondary" id="psaDismissBtn">Estou ciente</button>' +
            '      <button type="button" class="psa-btn" id="psaGoToAgendaBtn">Ir para a Agenda</button>' +
            '    </div>' +
            '  </div>' +
            '</div>';

        document.body.appendChild(wrap);

        if (window.lucide && typeof window.lucide.createIcons === 'function') {
            window.lucide.createIcons();
        }

        wrap.querySelector('#psaDismissBtn').addEventListener('click', function () {
            markSnoozed();
            wrap.remove();
        });

        wrap.querySelector('#psaGoToAgendaBtn').addEventListener('click', function () {
            markSnoozed();
            window.location.href = '/agenda';
        });

        return wrap;
    }

    function checkAndShow() {
        if (alreadySnoozed()) return;
        if (document.getElementById('pendingScheduleAlertOverlay')) return;

        fetch(ENDPOINT, {
            headers: { 'X-Requested-With': 'XMLHttpRequest' },
            credentials: 'same-origin'
        })
            .then(function (res) {
                if (!res.ok) return null;
                return res.json();
            })
            .then(function (items) {
                if (!items || !Array.isArray(items) || items.length === 0) return;
                buildOverlay(items);
            })
            .catch(function () { /* falha silenciosa: alerta não é crítico para o uso da tela */ });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', checkAndShow);
    } else {
        checkAndShow();
    }
})();
