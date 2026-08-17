
(function () {
    'use strict';

    const HEARTBEAT_INTERVAL_MS = 4 * 60 * 1000;   // ping a cada 4 min
    const ACTIVITY_WINDOW_MS = 15 * 60 * 1000;      // só faz ping se houve atividade nos últimos 15 min

    const nativeFetch = window.fetch.bind(window);
    let lastActivity = Date.now();
    let overlayShown = false;

    ['mousemove', 'keydown', 'click', 'scroll', 'touchstart', 'input'].forEach(function (evt) {
        window.addEventListener(evt, function () { lastActivity = Date.now(); }, { passive: true, capture: true });
    });



    window.fetch = function (input, init) {
        init = init || {};
        const headers = new Headers(init.headers || {});
        if (!headers.has('X-Requested-With')) {
            headers.set('X-Requested-With', 'XMLHttpRequest');
        }
        init.headers = headers;

        return nativeFetch(input, init).then(function (response) {
            if (response.status === 401) {
                return response.clone().json()
                    .then(function (payload) {
                        if (payload && payload.error === 'SESSION_EXPIRED') {
                            showSessionExpiredOverlay();
                        }
                        return response;
                    })
                    .catch(function () { return response; });
            }
            return response;
        });
    };



    setInterval(function () {
        if (overlayShown) return;
        if (Date.now() - lastActivity > ACTIVITY_WINDOW_MS) return; // usuário realmente ausente, não forçamos
        fetch('/keep-alive').catch(function () {});
    }, HEARTBEAT_INTERVAL_MS);



    function buildOverlay() {
        const wrap = document.createElement('div');
        wrap.id = 'sessionGuardOverlay';
        wrap.innerHTML =
            '<style>' +
            '#sessionGuardOverlay{position:fixed;inset:0;width:100%;height:100%;background:rgba(2,6,23,.85);' +
            'backdrop-filter:blur(4px);z-index:2147483647;display:flex;align-items:center;justify-content:center;' +
            'font-family:Inter,system-ui,sans-serif;padding:16px;}' +
            '#sessionGuardOverlay .sg-box{background:#1e293b;border:1px solid #334155;padding:32px;border-radius:16px;' +
            'max-width:400px;width:100%;box-shadow:0 20px 50px rgba(0,0,0,.5);text-align:center;animation:sgPop .25s ease-out;}' +
            '@keyframes sgPop{from{opacity:0;transform:scale(.94)}to{opacity:1;transform:scale(1)}}' +
            '#sessionGuardOverlay h3{color:#fff;font-size:20px;font-weight:700;margin:14px 0 8px;}' +
            '#sessionGuardOverlay p{color:#94a3b8;font-size:13.5px;line-height:1.5;margin:0 0 20px;}' +
            '#sessionGuardOverlay input{width:100%;background:#0f172a;border:1px solid #334155;border-radius:10px;' +
            'padding:11px 14px;color:#fff;font-size:14px;margin-bottom:10px;outline:none;box-sizing:border-box;}' +
            '#sessionGuardOverlay input:focus{border-color:#06b6d4;}' +
            '#sessionGuardOverlay button{width:100%;background:#0d9488;color:#fff;border:none;padding:11px;' +
            'border-radius:10px;font-weight:700;font-size:14px;cursor:pointer;}' +
            '#sessionGuardOverlay button:disabled{opacity:.6;cursor:default;}' +
            '#sessionGuardOverlay button:hover:not(:disabled){background:#0f766e;}' +
            '#sessionGuardOverlay .sg-msg{font-size:12.5px;margin:-4px 0 12px;display:none;}' +
            '#sessionGuardOverlay .sg-error{color:#fb7185;}' +
            '#sessionGuardOverlay .sg-ok{color:#34d399;}' +
            '</style>' +
            '<div class="sg-box">' +
            '<div style="font-size:38px;line-height:1">&#9200;</div>' +
            '<h3>Sess\u00e3o expirada por inatividade</h3>' +
            '<p>Fique tranquilo: o que voc\u00ea estava preenchendo nesta tela j\u00e1 foi salvo neste navegador. ' +
            'Fa\u00e7a login novamente para continuar exatamente de onde parou.</p>' +
            '<div class="sg-msg sg-error" id="sgError">Usu\u00e1rio ou senha inv\u00e1lidos.</div>' +
            '<div class="sg-msg sg-ok" id="sgOk">Login OK! Restaurando a tela...</div>' +
            '<form id="sgLoginForm">' +
            '<input type="text" id="sgUser" placeholder="Usu\u00e1rio" autocomplete="username" required />' +
            '<input type="password" id="sgPass" placeholder="Senha" autocomplete="current-password" required />' +
            '<button type="submit" id="sgSubmitBtn">Entrar e continuar</button>' +
            '</form>' +
            '</div>';
        document.body.appendChild(wrap);

        wrap.querySelector('#sgLoginForm').addEventListener('submit', function (e) {
            e.preventDefault();
            const btn = wrap.querySelector('#sgSubmitBtn');
            const errEl = wrap.querySelector('#sgError');
            const okEl = wrap.querySelector('#sgOk');
            errEl.style.display = 'none';
            okEl.style.display = 'none';

            const user = wrap.querySelector('#sgUser').value;
            const pass = wrap.querySelector('#sgPass').value;
            btn.disabled = true;
            btn.textContent = 'Entrando...';


            nativeFetch('/login', { credentials: 'same-origin' })
                .then(function (res) { return res.text(); })
                .then(function (html) {
                    const m = html.match(/name="_csrf"\s+value="([^"]+)"/);
                    const csrfToken = m ? m[1] : null;

                    const body = new URLSearchParams();
                    body.set('username', user);
                    body.set('password', pass);
                    if (csrfToken) body.set('_csrf', csrfToken);

                    return nativeFetch('/login', {
                        method: 'POST',
                        credentials: 'same-origin',
                        headers: {
                            'Content-Type': 'application/x-www-form-urlencoded',
                            'X-Requested-With': 'XMLHttpRequest'
                        },
                        body: body.toString()
                    });
                })
                .then(function (loginRes) {
                    if (loginRes.ok) {
                        okEl.style.display = 'block';
                        window.dispatchEvent(new CustomEvent('session-guard:restored'));
                        setTimeout(function () { window.location.reload(); }, 500);
                    } else {
                        errEl.style.display = 'block';
                        btn.disabled = false;
                        btn.textContent = 'Entrar e continuar';
                    }
                })
                .catch(function () {
                    errEl.textContent = 'Falha de conex\u00e3o. Verifique sua internet e tente novamente.';
                    errEl.style.display = 'block';
                    btn.disabled = false;
                    btn.textContent = 'Entrar e continuar';
                });
        });

        return wrap;
    }

    function showSessionExpiredOverlay() {
        if (overlayShown) return;
        overlayShown = true;


        window.dispatchEvent(new CustomEvent('session-guard:expired'));
        const overlay = document.getElementById('sessionGuardOverlay') || buildOverlay();
        overlay.style.display = 'flex';
        setTimeout(function () {
            const userInput = overlay.querySelector('#sgUser');
            if (userInput) userInput.focus();
        }, 50);
    }

    window.SessionGuard = { showSessionExpiredOverlay: showSessionExpiredOverlay };
})();
