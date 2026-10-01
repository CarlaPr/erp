(function () {
    'use strict';

    let busy = false;
    const buttonSelector = '.js-receipt-pdf-btn';

    function filenameFor(receipt) {
        const number = String(receipt.number || 'REC').replace(/^REC-/, 'REC ');
        const client = String(receipt.client?.name || 'Cliente').toUpperCase();
        return `${number} - ${client}`
            .replace(/[\\/:*?"<>|\u0000-\u001f]/g, '').replace(/\s+/g, ' ').trim() + '.pdf';
    }

    function keepObjectUrl(blob) {
        const url = URL.createObjectURL(blob);

        setTimeout(() => URL.revokeObjectURL(url), 5 * 60 * 1000);
        return url;
    }

    function openProgress() {
        try {
            const tab = window.open('about:blank', '_blank');
            if (tab) {
                tab.opener = null;
                tab.document.title = 'Gerando recibo - Grupo Glass';
                tab.document.body.textContent = 'Gerando o PDF do recibo…';
                tab.document.body.style.cssText = 'font:18px sans-serif;padding:40px;color:#334155';
            }
            return tab;
        } catch (_) {
            return null;
        }
    }

    function closeProgress(tab) {
        if (tab && !tab.closed) tab.close();
    }

    function download(pdf) {
        const link = document.createElement('a');
        link.href = keepObjectUrl(pdf.blob);
        link.download = pdf.filename;
        document.body.appendChild(link);
        link.click();
        link.remove();
    }

    async function requestPdf(receipt) {
        const controller = new AbortController();
        const timeout = setTimeout(() => controller.abort(), 45000);
        try {
            const response = await fetch(`/receipts/pdf/${encodeURIComponent(receipt.id)}`, {
                credentials: 'same-origin',
                cache: 'no-store',
                headers: { Accept: 'application/pdf', 'X-Requested-With': 'XMLHttpRequest' },
                signal: controller.signal
            });
            if (response.status === 401 || (response.redirected && /\/login(?:[/?]|$)/.test(response.url))) {
                const error = new Error('Sua sessão expirou. Entre novamente para gerar o PDF.');
                error.sessionExpired = true;
                throw error;
            }
            if (response.status === 403) throw new Error('Você não tem permissão para acessar este recibo.');
            if (response.status === 404) throw new Error('Recibo não encontrado. Atualize a listagem.');
            if (!response.ok) throw new Error('O servidor não conseguiu gerar o PDF. Tente novamente em instantes.');
            if (!response.headers.get('Content-Type')?.toLowerCase().startsWith('application/pdf')) {
                throw new Error('O servidor retornou uma resposta inválida. Atualize a página e tente novamente.');
            }
            const blob = await response.blob();
            if (blob.size < 5 || await blob.slice(0, 5).text() !== '%PDF-') {
                throw new Error('O arquivo recebido não é um PDF válido. Tente novamente.');
            }
            let filename = filenameFor(receipt);
            const encodedName = response.headers.get('Content-Disposition')?.match(/filename\*=UTF-8''([^;]+)/i);
            if (encodedName) {
                try { filename = decodeURIComponent(encodedName[1]); } catch (_) { }
            }
            filename = filename.replace(/[\\/:*?"<>|\u0000-\u001f]/g, '').slice(0, 200);
            if (!filename.toLowerCase().endsWith('.pdf')) filename += '.pdf';
            return { blob, filename };
        } catch (error) {
            if (error.name === 'AbortError') throw new Error('A geração demorou mais que o esperado. Tente novamente.');
            if (error instanceof TypeError) throw new Error('Não foi possível conectar ao servidor. Verifique a conexão e tente novamente.');
            throw error;
        } finally {
            clearTimeout(timeout);
        }
    }

    function supportsShare(file) {
        try {
            return typeof navigator.share === 'function' && typeof navigator.canShare === 'function'
                && navigator.canShare({ files: [file] });
        } catch (_) {
            return false;
        }
    }

    function whatsappUrl(receipt) {
        let phone = String(receipt.client?.phone || '').replace(/\D/g, '');
        if (phone.length === 10 || phone.length === 11) phone = '55' + phone;
        const params = new URLSearchParams({ text: `Olá! Segue o recibo ${receipt.number || ''}${receipt.client?.name ? ' - ' + receipt.client.name : ''}.` });
        if (phone.length >= 12 && phone.length <= 15) params.set('phone', phone);
        return `https://api.whatsapp.com/send?${params}`;
    }

    function showDeliveryPanel(pdf, receipt, shareFile) {
        document.getElementById('receiptPdfDelivery')?.remove();
        const dialog = document.createElement('dialog');
        dialog.id = 'receiptPdfDelivery';
        dialog.setAttribute('aria-label', 'Recibo em PDF pronto');
        dialog.style.cssText = 'margin:auto;padding:28px;border:1px solid #cbd5e1;border-radius:16px;max-width:460px;width:90%;color:#0f172a;background:white;box-shadow:0 20px 60px #0005';
        const heading = document.createElement('h2');
        heading.textContent = 'PDF pronto';
        heading.style.cssText = 'font-size:20px;font-weight:bold;margin-bottom:12px';
        const message = document.createElement('p');
        message.textContent = shareFile
            ? 'Toque em Compartilhar PDF e escolha o WhatsApp.'
            : 'O PDF foi baixado. Abra o WhatsApp e anexe o arquivo à conversa.';
        dialog.append(heading, message);
        const controls = document.createElement('div');
        controls.style.cssText = 'display:flex;flex-wrap:wrap;gap:12px;margin-top:20px';
        if (shareFile) {
            const share = document.createElement('button');
            share.type = 'button';
            share.textContent = 'Compartilhar PDF';
            share.onclick = async () => {
                share.disabled = true;
                try {
                    await navigator.share({ files: [shareFile], title: pdf.filename });
                    dialog.close();
                } catch (error) {
                    if (error.name !== 'AbortError') {
                        message.textContent = 'Baixe o PDF e anexe o arquivo na conversa do WhatsApp.';
                    }
                } finally {
                    share.disabled = false;
                }
            };
            controls.appendChild(share);
        }
        const save = document.createElement('a');
        save.textContent = 'Baixar PDF';
        save.href = keepObjectUrl(pdf.blob);
        save.download = pdf.filename;
        const whatsapp = document.createElement('a');
        whatsapp.textContent = 'Abrir WhatsApp';
        whatsapp.href = whatsappUrl(receipt);
        whatsapp.target = '_blank';
        whatsapp.rel = 'noopener noreferrer';
        const close = document.createElement('button');
        close.type = 'button';
        close.textContent = 'Fechar';
        close.onclick = () => dialog.close();
        controls.append(save, whatsapp, close);
        for (const control of controls.children) {
            control.style.cssText = 'padding:10px;border-radius:8px;background:#f1f5f9;cursor:pointer;font-weight:600';
        }
        dialog.appendChild(controls);
        dialog.addEventListener('close', () => dialog.remove(), { once: true });
        document.body.appendChild(dialog);
        dialog.showModal();
    }

    async function run(action, source) {
        if (busy) return;
        busy = true;
        const buttons = Array.from(document.querySelectorAll(buttonSelector));
        const originals = buttons.map(button => ({ html: button.innerHTML, disabled: button.disabled }));
        buttons.forEach(button => {
            button.disabled = true;
            button.textContent = 'Gerando PDF…';
        });
        let tab = null;
        try {
            const nativeShare = action === 'share' && typeof File === 'function'
                && supportsShare(new File(['%PDF-'], 'recibo.pdf', { type: 'application/pdf' }));
            if (action === 'print' || !nativeShare) tab = openProgress();

            const receipt = typeof source === 'function' ? await source() : source;
            if (!receipt?.id) {
                closeProgress(tab);
                tab = null;
                return;
            }

            const pdf = await requestPdf(receipt);
            if (action === 'print') {
                if (tab && !tab.closed) tab.location.replace(keepObjectUrl(pdf.blob));
                else download(pdf);
                return;
            }
            const file = typeof File === 'function' ? new File([pdf.blob], pdf.filename, { type: 'application/pdf' }) : null;
            if (file && supportsShare(file)) {
                closeProgress(tab);
                tab = null;
                if (navigator.userActivation && !navigator.userActivation.isActive) {
                    showDeliveryPanel(pdf, receipt, file);
                    return;
                }
                try {
                    await navigator.share({ files: [file], title: pdf.filename });
                } catch (error) {
                    if (error.name !== 'AbortError') showDeliveryPanel(pdf, receipt, file);
                }
            } else {
                download(pdf);
                if (tab && !tab.closed) tab.location.replace(whatsappUrl(receipt));
                showDeliveryPanel(pdf, receipt, null);
            }
        } catch (error) {
            closeProgress(tab);
            console.error('Erro ao gerar PDF do recibo:', error);
            if (error.sessionExpired && window.SessionGuard) window.SessionGuard.showSessionExpiredOverlay();
            else if (error.message) alert(error.message);
            else alert('Não foi possível gerar o PDF. Tente novamente.');
        } finally {
            buttons.forEach((button, index) => {
                button.innerHTML = originals[index].html;
                button.disabled = originals[index].disabled;
            });
            busy = false;
            if (window.lucide) window.lucide.createIcons();
        }
    }

    window.ReceiptPdf = {
        print: source => run('print', source),
        share: source => run('share', source)
    };
})();
