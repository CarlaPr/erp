(() => {
    'use strict';

    const form = document.getElementById('clientSearchForm');
    const input = document.getElementById('clientSearch');
    const status = document.getElementById('clientSearchStatus');
    const clear = document.getElementById('clearClientSearch');
    let results = document.getElementById('clientResults');
    if (!form || !input || !results) return;

    let timer;
    let activeRequest;
    let revision = 0;

    function setBusy(busy) {
        results.setAttribute('aria-busy', String(busy));
        if (busy) status.textContent = 'Buscando clientes...';
    }

    function search(page = 0, delay = 0) {
        clearTimeout(timer);
        activeRequest?.abort();
        const currentRevision = ++revision;
        clear.classList.toggle('hidden', !input.value.trim());
        setBusy(true);
        timer = setTimeout(() => loadResults(page, currentRevision), delay);
    }

    async function loadResults(page, currentRevision) {
        const controller = new AbortController();
        activeRequest = controller;
        const url = new URL(form.dataset.resultsUrl, window.location.href);
        url.searchParams.set('search', input.value.trim());
        url.searchParams.set('page', String(page));

        try {
            const response = await fetch(url, {
                signal: controller.signal,
                credentials: 'same-origin',
                headers: { 'Accept': 'text/html' }
            });
            if (currentRevision !== revision) return;
            if (response.redirected) {
                window.location.assign(response.url);
                return;
            }
            if (!response.ok) throw new Error('Falha na busca');
            const html = await response.text();
            if (currentRevision !== revision) return;

            const updated = new DOMParser().parseFromString(html, 'text/html')
                .getElementById('clientResults');
            if (!updated) throw new Error('Resposta inválida');

            results.replaceWith(updated);
            results = updated;
            document.getElementById('clientTotalCount').textContent = results.dataset.total;
            document.getElementById('clientTotalPages').textContent = results.dataset.pages;

            const listUrl = new URL(form.action);
            listUrl.searchParams.set('page', results.dataset.page);
            if (results.dataset.search) listUrl.searchParams.set('search', results.dataset.search);
            window.history.replaceState(null, '', listUrl);

            const clientForm = document.getElementById('clientForm');
            clientForm.querySelector('[name="page"]').value = results.dataset.page;
            clientForm.querySelector('[name="search"]').value = results.dataset.search;
            document.getElementById('closeClientModal').href = listUrl.href;
            document.getElementById('cancelClientModal').href = listUrl.href;
            clear.classList.toggle('hidden', !results.dataset.search);
            window.lucide?.createIcons();
            status.textContent = results.dataset.total === '0'
                ? 'Nenhum cliente encontrado.'
                : results.dataset.total + ' cliente(s) encontrado(s).';
        } catch (error) {
            if (currentRevision === revision && error.name !== 'AbortError') {
                status.textContent = 'Não foi possível buscar os clientes. Tente novamente.';
            }
        } finally {
            if (currentRevision === revision) {
                activeRequest = null;
                setBusy(false);
            }
        }
    }

    input.addEventListener('input', event => {
        if (event.isComposing) {
            clearTimeout(timer);
            activeRequest?.abort();
            revision++;
            setBusy(false);
            return;
        }
        search(0, 200);
    });
    input.addEventListener('compositionend', () => search(0, 200));
    form.addEventListener('submit', event => {
        event.preventDefault();
        search();
    });

    function isPlainClick(event) {
        return event.button === 0 && !event.ctrlKey && !event.metaKey && !event.shiftKey && !event.altKey;
    }

    clear.addEventListener('click', event => {
        if (!isPlainClick(event)) return;
        event.preventDefault();
        input.value = '';
        input.focus();
        search();
    });

    document.addEventListener('click', event => {
        const link = event.target.closest('a[href]');
        if (!link || !results.contains(link) || !isPlainClick(event)) return;
        const url = new URL(link.href);
        if (url.pathname !== new URL(form.action).pathname) return;
        event.preventDefault();
        input.value = url.searchParams.get('search') || '';
        search(Number(url.searchParams.get('page')) || 0);
    });
})();
