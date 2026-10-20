(function (root) {
    'use strict';

    const formatter = new Intl.NumberFormat('pt-BR', {
        style: 'currency',
        currency: 'BRL',
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
    });

    function parse(value) {
        if (typeof value === 'number') return Number.isFinite(value) ? value : 0;
        if (value == null) return 0;

        let text = String(value).replace(/R\$/gi, '').replace(/\s/g, '');
        if (!text) return 0;

        if (text.includes(',')) {
            text = text.replace(/\./g, '').replace(',', '.');
        } else if (/^-?\d{1,3}(?:\.\d{3})+$/.test(text)) {
            // Na entrada brasileira, 3.600 representa três mil e seiscentos reais.
            text = text.replace(/\./g, '');
        }

        const amount = Number(text);
        return Number.isFinite(amount) ? amount : 0;
    }

    function format(value) {
        return formatter.format(parse(value));
    }

    function sanitizeInput(input) {
        const text = input.value.replace(/R\$/gi, '').replace(/\s/g, '').replace(/[^0-9.,]/g, '');
        if (input.value !== text) input.value = text;
    }

    function prepareInput(input) {
        if (!input || !input.value.trim()) return;
        input.value = parse(input.value).toLocaleString('pt-BR', {
            useGrouping: false,
            minimumFractionDigits: 2,
            maximumFractionDigits: 2
        });
        input.select();
    }

    function formatInput(input) {
        if (!input || !input.value.trim()) return;
        input.value = format(input.value);
    }

    function bindForm(form, input) {
        if (!form || !input) return;
        form.addEventListener('formdata', event => {
            // Converte somente o dado enviado; o campo continua exibindo BRL.
            event.formData.set('totalAmount', input.value.trim() ? parse(input.value).toFixed(2) : '');
        });
    }

    root.BRLCurrency = { parse, format, sanitizeInput, prepareInput, formatInput, bindForm };
})(typeof window !== 'undefined' ? window : globalThis);
