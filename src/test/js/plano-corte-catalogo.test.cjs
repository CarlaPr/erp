const { test } = require('node:test');
const assert = require('node:assert/strict');
const vm = require('node:vm');
const fs = require('node:fs');
const codigo = fs.readFileSync('src/main/resources/static/js/plano-corte-catalogo.js', 'utf8');
const aguardar = () => new Promise(resolve => setImmediate(resolve));

function iniciar(selecionado = '') {
    const pendentes = [];
    const campo = (name, value) => ({
        name, value, ouvintes: {},
        addEventListener(tipo, fn) { this.ouvintes[tipo] = fn; },
        mudar(value) { this.value = value; this.ouvintes.change(); }
    });
    const campos = {
        corVidro: campo('corVidro', 'FUME'),
        tipoVidro: campo('tipoVidro', 'COMUM'),
        espessuraVidroMm: campo('espessuraVidroMm', '4')
    };
    const select = Object.assign(campo('vidroCatalogoId', ''), {
        dataset: { selecionado }, options: [],
        replaceChildren() { this.options = []; this.value = ''; },
        add(option) { this.options.push(option); if (this.options.length === 1) this.value = option.value; }
    });
    const status = { textContent: '', classList: { toggle() {} } };
    const opcoes = { hidden: true };
    const form = { elements: { namedItem: name => campos[name] } };
    const painel = {
        closest: () => form,
        querySelector: seletor => seletor.includes('-status') ? status : seletor.includes('-opcoes') ? opcoes : select
    };
    vm.runInNewContext(codigo, {
        document: { querySelectorAll: () => [painel] }, Intl, URLSearchParams, AbortController,
        Option: function (text, value) { this.text = text; this.value = String(value); },
        fetch: (url, config) => new Promise(resolve => pendentes.push({
            url, signal: config.signal,
            responder: lista => resolve({ ok: true, headers: { get: () => 'application/json' }, json: async () => lista })
        }))
    });
    return { campos, select, status, opcoes, pendentes };
}

test('consulta as três características e aplica a única correspondência', async () => {
    const h = iniciar();
    assert.match(h.pendentes[0].url, /corVidro=FUME&tipoVidro=COMUM&espessuraVidroMm=4/);
    h.pendentes[0].responder([{ id: 42, nome: 'Fumê comum', valorPorM2: 120.50 }]);
    await aguardar();
    assert.equal(h.select.value, '42');
    assert.equal(h.select.disabled, false);
    assert.match(h.status.textContent, /120,50/);
});

test('permite ausência de preço com aviso e sem campo obrigatório', async () => {
    const h = iniciar();
    h.pendentes[0].responder([]);
    await aguardar();
    assert.equal(h.select.required, false);
    assert.equal(h.select.disabled, true);
    assert.match(h.status.textContent, /pode salvar este vão sem preço/);
});

test('duplicidade exige escolha e restaura insumo usado na edição', async () => {
    const h = iniciar('2');
    h.pendentes[0].responder([
        { id: 1, nome: 'Fornecedor A', valorPorM2: 100 },
        { id: 2, nome: 'Fornecedor B', valorPorM2: 120 }
    ]);
    await aguardar();
    assert.equal(h.opcoes.hidden, false);
    assert.equal(h.select.required, true);
    assert.equal(h.select.value, '2');
    h.select.mudar('1');
    assert.match(h.status.textContent, /Fornecedor A.*100,00/);
});

test('resposta antiga não sobrescreve uma seleção mais recente', async () => {
    const h = iniciar();
    h.campos.espessuraVidroMm.mudar('8');
    assert.equal(h.pendentes[0].signal.aborted, true);
    h.pendentes[1].responder([{ id: 8, nome: '8 mm', valorPorM2: 180 }]);
    await aguardar();
    h.pendentes[0].responder([{ id: 4, nome: '4 mm', valorPorM2: 100 }]);
    await aguardar();
    assert.equal(h.select.value, '8');
    assert.match(h.status.textContent, /180,00/);
    h.campos.corVidro.mudar('');
    assert.equal(h.select.disabled, true);
    assert.equal(h.select.value, '');
});
