(() => {
    'use strict';
    const moeda = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
    document.querySelectorAll('.catalogo-vidro').forEach(painel => {
        const form = painel.closest('form');
        const campos = ['corVidro', 'tipoVidro', 'espessuraVidroMm'].map(nome => form.elements.namedItem(nome));
        const categoria = form.elements.namedItem('categoria');
        const tipoVidro = campos[1];
        const todosTipos = Array.from(tipoVidro.options).filter(opcao => opcao.value)
            .map(opcao => ({ value: opcao.value, text: opcao.text }));
        const sincronizarTipos = () => {
            const anterior = tipoVidro.value;
            const espelho = categoria && categoria.value === 'ESPELHO';
            const disponiveis = todosTipos.filter(opcao =>
                ['ESPELHO', 'ESPELHO_CEBRACE'].includes(opcao.value) === Boolean(espelho));
            tipoVidro.replaceChildren(new Option('Selecione...', ''));
            disponiveis.forEach(opcao => tipoVidro.add(new Option(opcao.text, opcao.value)));
            tipoVidro.value = disponiveis.some(opcao => opcao.value === anterior) ? anterior : '';
        };
        const status = painel.querySelector('.catalogo-vidro-status');
        const opcoes = painel.querySelector('.catalogo-vidro-opcoes');
        const select = painel.querySelector('select[name="vidroCatalogoId"]');
        let sequencia = 0;
        let controle;
        let itens = [];
        const informar = (mensagem, aviso = false) => {
            status.textContent = mensagem;
            status.classList.toggle('text-amber-700', aviso);
            status.classList.toggle('dark:text-amber-300', aviso);
        };
        const mostrarPreco = () => {
            const item = itens.find(item => String(item.id) === select.value);
            informar(item ? item.nome + ': ' + moeda.format(item.valorPorM2) + ' / m²'
                : 'Mais de um insumo compatível. Escolha o preço do catálogo a aplicar.', !item);
        };
        const consultar = async (inicial = false) => {
            const atual = ++sequencia;
            if (controle) controle.abort();
            controle = new AbortController();
            const anterior = inicial ? select.dataset.selecionado : '';
            select.replaceChildren();
            select.required = false;
            select.disabled = true;
            opcoes.hidden = true;
            itens = [];

            const parametros = new URLSearchParams();
            campos.forEach(campo => parametros.set(campo.name, campo.value));
            if (categoria && categoria.value) parametros.set('categoria', categoria.value);
            try {
                const resposta = await fetch('/cut-plans/catalogo/vidros-compativeis?' + parametros,
                    { signal: controle.signal, headers: { Accept: 'application/json' } });
                if (!resposta.ok || !resposta.headers.get('content-type')?.includes('application/json')) {
                    throw new Error('Consulta indisponível');
                }
                const recebidos = await resposta.json();
                if (atual !== sequencia) return;
                itens = recebidos;
                if (!itens.length) {
                    informar('Nenhum insumo compatível no catálogo. Você pode salvar este vão sem preço.', true);
                    return;
                }
                select.disabled = false;
                if (itens.length > 1) {
                    select.add(new Option('Selecione o insumo e preço…', ''));
                    opcoes.hidden = false;
                    select.required = true;
                }
                itens.forEach(item => select.add(new Option(item.nome + ' — ' + moeda.format(item.valorPorM2) + ' / m²', item.id)));
                if (anterior && itens.some(item => String(item.id) === anterior)) select.value = anterior;
                mostrarPreco();
            } catch (erro) {
                if (erro.name === 'AbortError' || atual !== sequencia) return;
                informar('Não foi possível consultar agora. O catálogo será verificado ao salvar.', true);
            }
        };
        select.addEventListener('change', mostrarPreco);
        campos.forEach(campo => campo.addEventListener('change', () => consultar()));
        form.addEventListener('servico-alterado', () => {
            sincronizarTipos();
            consultar();
        });
        sincronizarTipos();
        consultar(true);
    });
})();
