(() => {
    const permissions = window.pagePermissions;
    if (!permissions) return;
    const allowed = { edit: permissions.edit, delete: permissions.delete,
        'visit-plan': permissions.visitPlan, 'visit-field': permissions.visitField,
        'visit-delete': permissions.visitDelete };
    const denied = element => element?.closest('[data-permission]')?.dataset.permission
        .split(/\s+/).some(action => allowed[action] === false);
    document.addEventListener('click', event => {
        if (denied(event.target)) {
            event.preventDefault();
            event.stopImmediatePropagation();
        }
    }, true);
    document.addEventListener('submit', event => {
        if (denied(event.target)) {
            event.preventDefault();
            event.stopImmediatePropagation();
        }
    }, true);
    const hasVisitActions = permissions.page === 'agenda' && (permissions.visitPlan || permissions.visitDelete);
    if (!permissions.manager && permissions.writable && !permissions.edit && !permissions.delete && !hasVisitActions) {
        const main = document.querySelector('main');
        if (main) {
            const notice = document.createElement('p');
            notice.className = 'mb-6 p-4 rounded-xl bg-indigo-50 dark:bg-indigo-500/10 text-indigo-700 dark:text-indigo-300 text-sm';
            notice.setAttribute('role', 'status');
            notice.textContent = 'Acesso somente para visualização. Alterações e exclusões não estão liberadas para seu usuário.';
            main.insertBefore(notice, main.firstChild);
        }
    }
})();
