(() => {
    document.querySelectorAll('.permission-row').forEach(row => {
        const mode = row.querySelector('[data-permission-mode]');
        const view = row.querySelector('[data-permission-view]');
        const edit = row.querySelector('[data-permission-edit]');
        const remove = row.querySelector('[data-permission-delete]');
        const agendaBlocks = row.querySelector('[data-permission-agenda-blocks]');
        const update = () => {
            const inherited = mode.value === 'default';
            if (inherited) {
                view.checked = row.dataset.defaultView === 'true';
                edit.checked = row.dataset.defaultEdit === 'true';
                remove.checked = row.dataset.defaultDelete === 'true';
                if (agendaBlocks) agendaBlocks.checked = false;
            } else if (!view.checked) {
                edit.checked = false;
                remove.checked = false;
                if (agendaBlocks) agendaBlocks.checked = false;
            }
            view.disabled = inherited;
            edit.disabled = remove.disabled = inherited || !view.checked || row.dataset.writable !== 'true';
            if (agendaBlocks) agendaBlocks.disabled = inherited || row.dataset.agendaPage !== 'true' || !view.checked;
        };
        mode.addEventListener('change', update);
        view.addEventListener('change', update);
        row.querySelector('[data-permission-revoke]').addEventListener('click', () => {
            mode.value = 'custom';
            view.checked = edit.checked = remove.checked = false;
            if (agendaBlocks) agendaBlocks.checked = false;
            update();
        });
        update();
    });
})();
