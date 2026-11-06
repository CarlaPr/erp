(() => {
    const root = document.getElementById('financial-result');
    if (!root) return;
    const mode = document.getElementById('period-mode');
    const month = document.getElementById('result-month');
    const from = document.getElementById('result-from');
    const to = document.getElementById('result-to');
    // Open the filter disclosure before the browser focuses an invalid field.
    document.getElementById('result-filters').addEventListener('invalid', () => {
        document.getElementById('filter-panel').open = true;
    }, true);
    function updatePeriod() {
        const custom = mode.value === 'custom';
        month.disabled = custom;
        from.disabled = to.disabled = !custom;
        document.getElementById('month-field').hidden = custom;
        document.getElementById('from-field').hidden = !custom;
        document.getElementById('to-field').hidden = !custom;
        to.setCustomValidity(custom && from.value && to.value && from.value > to.value
            ? 'A data final deve ser posterior ou igual à data inicial.' : '');
    }
    mode.addEventListener('change', updatePeriod);
    from.addEventListener('input', updatePeriod);
    to.addEventListener('input', updatePeriod);
    updatePeriod();
    root.querySelectorAll('[data-order-toggle]').forEach(button => button.addEventListener('click', () => {
        const detail = document.getElementById(button.getAttribute('aria-controls'));
        detail.hidden = !detail.hidden;
        button.setAttribute('aria-expanded', String(!detail.hidden));
    }));
    const brl = new Intl.NumberFormat('pt-BR', { style:'currency', currency:'BRL' });
    const date = value => value ? value.split('-').reverse().join('/') : 'Não registrada';
    const path = value => root.dataset.contextPath + value.replace(/^\/+/, '');
    function cell(row, text, className, label) {
        const element = document.createElement('td');
        element.textContent = text;
        if (className) element.className = className;
        if (label) element.dataset.label = label;
        row.appendChild(element);
        return element;
    }
    root.querySelectorAll('[data-load-history]').forEach(button => button.addEventListener('click', async () => {
        const output = button.parentElement.querySelector('.fr-history');
        button.disabled = true;
        output.textContent = 'Carregando histórico…';
        try {
            const response = await fetch(root.dataset.historyBase + encodeURIComponent(button.dataset.orderId) + '/movements', {
                headers: { Accept:'application/json', 'X-Requested-With':'XMLHttpRequest' }
            });
            if (!response.ok) throw new Error('Não foi possível consultar o histórico. Verifique sua sessão e tente novamente.');
            const movements = await response.json();
            if (!Array.isArray(movements)) throw new Error('Não foi possível consultar o histórico.');
            output.replaceChildren();
            if (!movements.length) { output.textContent = 'Nenhuma movimentação efetiva registrada para esta OS.'; return; }
            const wrap = document.createElement('div');
            wrap.className = 'fr-table-wrap';
            const table = document.createElement('table');
            table.className = 'fr-table fr-responsive-table fr-history-table';
            const head = table.createTHead().insertRow();
            ['Data', 'Descrição / lançamento original', 'Categoria', 'Entrada', 'Saída', 'Forma de pagamento'].forEach(label => {
                const th = document.createElement('th'); th.textContent = label; head.appendChild(th);
            });
            const body = table.createTBody();
            let incoming = 0, outgoing = 0;
            movements.forEach(movement => {
                const row = body.insertRow();
                cell(row, date(movement.date), '', 'Data');
                const link = document.createElement('a');
                link.href = path(movement.sourceUrl); link.className = 'fr-link'; link.textContent = movement.description;
                cell(row, '', '', 'Descrição / lançamento original').appendChild(link);
                cell(row, movement.category || 'Sem categoria', '', 'Categoria');
                cell(row, brl.format(movement.incoming), 'fr-money fr-positive fr-main-figure', 'Entrada');
                cell(row, brl.format(movement.outgoing), 'fr-money fr-negative fr-main-figure', 'Saída');
                cell(row, movement.paymentMethod || 'Não informada', '', 'Forma de pagamento');
                incoming += Math.round(Number(movement.incoming) * 100); outgoing += Math.round(Number(movement.outgoing) * 100);
            });
            const foot = table.createTFoot().insertRow();
            cell(foot, 'Total do histórico completo', 'fr-card-heading').colSpan = 3;
            cell(foot, brl.format(incoming / 100), 'fr-money fr-positive', 'Total de entradas'); cell(foot, brl.format(outgoing / 100), 'fr-money fr-negative', 'Total de saídas'); cell(foot, '', 'fr-footer-spacer');
            wrap.appendChild(table); output.appendChild(wrap);
            button.textContent = 'Atualizar movimentações da OS';
        } catch (error) { output.textContent = error.message; }
        finally { button.disabled = false; }
    }));
})();
