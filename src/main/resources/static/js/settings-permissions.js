(() => {
    document.querySelectorAll('.permission-row').forEach(row => {
        const mode = row.querySelector('[data-permission-mode]');
        const view = row.querySelector('[data-permission-view]');
        const edit = row.querySelector('[data-permission-edit]');
        const remove = row.querySelector('[data-permission-delete]');
        const update = () => {
            const inherited = mode.value === 'default';
            if (inherited) {
                view.checked = row.dataset.defaultView === 'true';
                edit.checked = row.dataset.defaultEdit === 'true';
                remove.checked = row.dataset.defaultDelete === 'true';
            } else if (!view.checked) {
                edit.checked = false;
                remove.checked = false;
            }
            view.disabled = inherited;
            edit.disabled = remove.disabled = inherited || !view.checked || row.dataset.writable !== 'true';
        };
        mode.addEventListener('change', update);
        view.addEventListener('change', update);
        row.querySelector('[data-permission-revoke]').addEventListener('click', () => {
            mode.value = 'custom';
            view.checked = edit.checked = remove.checked = false;
            update();
        });
        update();
    });
})();
