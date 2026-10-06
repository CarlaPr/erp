(() => {
    const colors = ['#0284c7', '#10b981', '#8b5cf6', '#f59e0b', '#e11d48', '#06b6d4', '#d946ef', '#84cc16', '#f97316', '#6366f1'];
    const currency = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
    const percent = new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 2, minimumFractionDigits: 2 });
    const integer = new Intl.NumberFormat('pt-BR');

    const readPreference = (key, allowed, fallback) => {
        try {
            const value = localStorage.getItem(`payable-category-kpi.${key}`);
            return allowed.includes(value) ? value : fallback;
        } catch (_) { return fallback; }
    };

    const savePreference = (key, value) => {
        try { localStorage.setItem(`payable-category-kpi.${key}`, value); } catch (_) { }
    };

    document.querySelectorAll('[data-payable-category-kpi]').forEach(root => {
        if (root.dataset.initialized) return;
        root.dataset.initialized = 'true';

        const rows = Array.from(root.querySelectorAll('[data-category-row]')).map((row, index) => ({
            name: row.dataset.category,
            group: row.closest('[data-category-group]').dataset.categoryGroup,
            count: Number(row.dataset.count) || 0,
            total: Number(row.dataset.total) || 0,
            paid: Number(row.dataset.paid) || 0,
            color: row.dataset.unconfigured === 'true' ? '#64748b' : colors[index % colors.length],
            dot: row.querySelector('[data-category-dot]')
        }));

        if (!rows.length) return;

        rows.forEach(row => { row.dot.style.backgroundColor = row.color; });

        const metricSelect = root.querySelector('[data-chart-metric]');
        const groupSelect = root.querySelector('[data-chart-group]');
        const groups = root.querySelectorAll('[data-category-group]');
        const buttons = root.querySelectorAll('[data-chart-view]');
        const pie = root.querySelector('[data-chart-pie]');
        const bars = root.querySelector('[data-chart-bars]');
        const empty = root.querySelector('[data-chart-empty]');
        const caption = root.querySelector('[data-chart-caption]');

        const tooltip = document.createElement('div');
        tooltip.className = 'pc-kpi-tooltip';
        tooltip.setAttribute('role', 'tooltip');
        tooltip.hidden = true;
        const tooltipHeader = document.createElement('div');
        tooltipHeader.className = 'pc-kpi-tooltip-header';
        const tooltipDot = document.createElement('span');
        tooltipDot.className = 'pc-kpi-tooltip-dot';
        const tooltipName = document.createElement('strong');
        tooltipHeader.append(tooltipDot, tooltipName);
        const tooltipBody = document.createElement('div');
        tooltipBody.className = 'pc-kpi-tooltip-body';
        const valueLine = document.createElement('div');
        const valueLabel = document.createElement('span');
        const tooltipValue = document.createElement('strong');
        valueLine.append(valueLabel, tooltipValue);
        const shareLine = document.createElement('div');
        const shareLabel = document.createElement('span');
        shareLabel.textContent = 'Participação:';
        const tooltipShare = document.createElement('strong');
        shareLine.append(shareLabel, tooltipShare);
        tooltipBody.append(valueLine, shareLine);
        tooltip.append(tooltipHeader, tooltipBody);
        document.body.appendChild(tooltip);

        let view = readPreference('view', ['pie', 'bar'], 'pie');
        metricSelect.value = readPreference('metric', ['paid', 'total', 'count'], 'paid');
        groupSelect.value = readPreference('group', ['category', 'subcategory'], 'subcategory');

        root.querySelector('[data-chart-controls]').hidden = false;
        root.querySelector('[data-chart-visual]').hidden = false;

        let slices = [];
        let currentHoveredIndex = -1;
        const formatValue = value => metricSelect.value === 'count'
            ? `${integer.format(value)} ${value === 1 ? 'conta' : 'contas'}`
            : currency.format(value);

        const drawPie = (hoveredIndex = -1) => {
            const stops = slices.map((s, idx) => {
                const color = (hoveredIndex !== -1 && hoveredIndex !== idx) ? s.color + '66' : s.color;
                return `${color} ${s.startPercent}% ${s.endPercent}%`;
            });
            pie.style.background = stops.length ? `conic-gradient(from 0deg at 50% 50%, ${stops.join(', ')})` : '';
        };

        const handlePieLeave = () => {
            if (currentHoveredIndex !== -1) {
                currentHoveredIndex = -1;
                drawPie(-1);
            }
            tooltip.hidden = true;
            pie.style.cursor = 'default';
        };

        pie.addEventListener('mousemove', (e) => {
            if (pie.hidden || !slices.length) {
                handlePieLeave();
                return;
            }

            const rect = pie.getBoundingClientRect();
            if (rect.width <= 0 || rect.height <= 0) {
                handlePieLeave();
                return;
            }

            const x = e.clientX - rect.left - rect.width / 2;
            const y = e.clientY - rect.top - rect.height / 2;


            const distanceSquared = (x / (rect.width / 2)) ** 2 + (y / (rect.height / 2)) ** 2;
            if (distanceSquared > 1 || distanceSquared === 0) {
                handlePieLeave();
                return;
            }


            const fullTurn = 2 * Math.PI;
            const angle = (Math.atan2(x, -y) + fullTurn) % fullTurn;
            const percentAngle = angle / fullTurn * 100;

            const hoveredIndex = slices.findIndex(s => percentAngle >= s.startPercent && percentAngle < s.endPercent);

            if (hoveredIndex !== -1) {
                if (currentHoveredIndex !== hoveredIndex) {
                    currentHoveredIndex = hoveredIndex;
                    drawPie(hoveredIndex);
                    const slice = slices[hoveredIndex];
                    tooltipDot.style.backgroundColor = slice.color;

                    tooltipName.textContent = slice.row.name;
                    valueLabel.textContent = metricSelect.value === 'count' ? 'Quantidade:' : 'Valor:';
                    tooltipValue.textContent = formatValue(slice.value);
                    tooltipShare.textContent = `${percent.format(slice.share)}%`;
                }


                tooltip.hidden = false;
                const tooltipRect = tooltip.getBoundingClientRect();
                const margin = 8;
                let left = e.clientX + 15;
                let top = e.clientY + 15;

                if (left + tooltipRect.width > window.innerWidth) {
                    left = e.clientX - tooltipRect.width - 15;
                }
                if (top + tooltipRect.height > window.innerHeight) {
                    top = e.clientY - tooltipRect.height - 15;
                }

                tooltip.style.left = `${Math.max(margin, Math.min(left, window.innerWidth - tooltipRect.width - margin))}px`;
                tooltip.style.top = `${Math.max(margin, Math.min(top, window.innerHeight - tooltipRect.height - margin))}px`;
                pie.style.cursor = 'pointer';
            } else {
                handlePieLeave();
            }
        });

        pie.addEventListener('mouseleave', handlePieLeave);
        window.addEventListener('resize', handlePieLeave);
        window.addEventListener('scroll', handlePieLeave, true);
        window.addEventListener('blur', handlePieLeave);
        root.closest('details')?.addEventListener('toggle', handlePieLeave);
        document.addEventListener('keydown', event => {
            if (event.key === 'Escape') handlePieLeave();
        });

        const render = () => {
            handlePieLeave();
            slices = [];
            pie.removeAttribute('title');
            pie.setAttribute('aria-label', 'Nenhum valor disponível para esta medida.');
            drawPie();
            const metric = metricSelect.value;
            groups.forEach(group => { group.hidden = group.dataset.categoryGroup !== groupSelect.value; });
            root.querySelector('[data-group-heading]').textContent = groupSelect.value === 'category' ? 'Categoria' : 'Categoria / subcategoria';

            const sortedRows = rows.filter(row => row.group === groupSelect.value).sort((a, b) => b[metric] - a[metric]);
            const total = sortedRows.reduce((sum, row) => sum + row[metric], 0);

            buttons.forEach(button => button.setAttribute('aria-pressed', String(button.dataset.chartView === view)));
            pie.hidden = view !== 'pie' || total <= 0;
            bars.hidden = view !== 'bar' || total <= 0;
            empty.hidden = total > 0;

            empty.textContent = metric === 'paid'
                ? 'Nenhum valor pago nesta seleção. Compare por valor das contas ou quantidade de contas.'
                : 'Nenhum valor disponível para esta medida.';
            caption.textContent = `${metricSelect.selectedOptions[0].textContent}: ${formatValue(total)}. Percentuais sobre o total da seleção.`;
            bars.replaceChildren();

            if (total <= 0) return;

            let accumulatedValue = 0;
            const descriptions = [];
            const positiveRows = sortedRows.filter(row => row[metric] > 0);

            sortedRows.forEach(row => {
                const share = row[metric] / total * 100;
                const shareLabel = `${percent.format(share)}%`;
                const detail = `${formatValue(row[metric])} · ${shareLabel}`;
                descriptions.push(`${row.name}: ${detail}`);

                if (row[metric] > 0) {
                    const startPercent = accumulatedValue / total * 100;
                    accumulatedValue += row[metric];
                    slices.push({
                        row: row,
                        startPercent: startPercent,

                        endPercent: slices.length === positiveRows.length - 1 ? 100 : accumulatedValue / total * 100,
                        share: share,
                        value: row[metric],
                        color: row.color
                    });
                }


                const bar = document.createElement('div');
                const label = document.createElement('div');
                label.className = 'pc-kpi-bar-label';
                const name = document.createElement('strong');
                name.textContent = row.name;
                const valueEl = document.createElement('span');
                valueEl.textContent = detail;
                label.append(name, valueEl);
                const track = document.createElement('div');
                track.className = 'pc-kpi-bar-track';
                track.setAttribute('aria-hidden', 'true');
                const fill = document.createElement('div');
                fill.className = 'pc-kpi-bar-fill';
                fill.style.width = `${share}%`;
                fill.style.backgroundColor = row.color;
                track.append(fill);
                bar.append(label, track);
                bars.append(bar);
            });

            drawPie(-1);
            pie.setAttribute('aria-label', descriptions.join('; '));
        };

        buttons.forEach(button => button.addEventListener('click', () => {
            view = button.dataset.chartView;
            savePreference('view', view);
            render();
        }));
        metricSelect.addEventListener('change', () => {
            savePreference('metric', metricSelect.value);
            render();
        });
        groupSelect.addEventListener('change', () => {
            savePreference('group', groupSelect.value);
            render();
        });
        render();
    });
})();
