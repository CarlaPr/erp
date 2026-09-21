(() => {
    const $ = id => document.getElementById(id);
    const csrfHeaders = json => { const h = json ? {'Content-Type':'application/json'} : {}; const t=document.querySelector('meta[name="_csrf"]')?.content; const n=document.querySelector('meta[name="_csrf_header"]')?.content||'X-CSRF-TOKEN'; if(t) h[n]=t; return h; };
    const esc = value => String(value ?? '').replace(/[&<>'"]/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;',"'":'&#39;','"':'&quot;'}[c]));

    const normalizeSearch = value => String(value ?? '').normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLowerCase().trim();
    function toggleModal(id, show) { const modal=$(id); if(!modal)return; modal.classList.toggle('hidden',!show); modal.classList.toggle('flex',show); }
    function openClientSearchModal(){ $('clientSearchInput').value=''; filterClientOptions(); toggleModal('clientSearchModal',true); requestAnimationFrame(()=>$('clientSearchInput').focus()); }
    function closeClientSearchModal(){ toggleModal('clientSearchModal',false); }
    function clearClientSearchInput(){ $('clientSearchInput').value=''; filterClientOptions(); $('clientSearchInput').focus(); }
    function filterClientOptions(){
        const input=$('clientSearchInput');
        const results=$('clientSearchResults');
        const empty=$('clientSearchEmpty');
        const clearBtn=$('clearClientSearch');
        if(!input||!results)return;

        const q=normalizeSearch(input.value);
        let visible=0;

        results.querySelectorAll('.client-option').forEach(option=>{
            const name=option.dataset.clientName||option.querySelector('.client-option-name')?.textContent||'';
            const city=option.dataset.clientCity||option.querySelector('.client-option-city')?.textContent||'';
            const haystack=normalizeSearch(`${name} ${city}`);
            const show=!q||haystack.includes(q);

            option.classList.toggle('client-filter-hidden',!show);
            if(show)visible++;
        });

        if(empty) empty.classList.toggle('hidden',visible!==0);
        if(clearBtn){
            clearBtn.classList.toggle('hidden',!q);
            clearBtn.classList.toggle('flex',!!q);
        }
    }
    function selectClientFromModal(option){ const id=option.dataset.clientId||''; const name=option.dataset.clientName||'Cliente'; const city=option.dataset.clientCity||''; $('newClientId').value=id; $('selectedClientName').textContent=name; $('selectedClientCity').textContent=city||'Cliente selecionado'; $('clientSelectButton').classList.add('has-client'); document.querySelectorAll('#clientSearchResults .client-option').forEach(item=>item.classList.toggle('is-selected',item.dataset.clientId===id)); closeClientSearchModal(); }
    function openInlineClientModal(){ closeClientSearchModal(); toggleModal('clientInlineModal',true); requestAnimationFrame(()=>$('inlineClientName')?.focus()); }
    function closeInlineClientModal(){ toggleModal('clientInlineModal',false); }
    function setInlineClientError(id,message){
        const error=$(id);
        if(!error)return;
        error.textContent=message||'';
        error.classList.toggle('hidden',!message);
    }
    function clearInlineClientErrors(){ ['inlineDocumentError','inlineEmailError','inlinePhoneError','inlineCepError'].forEach(id=>setInlineClientError(id,'')); }
    function resetInlineClientForm(){
        $('inlineClientForm')?.reset();
        clearInlineClientErrors();
        const button=$('saveInlineClientButton');
        if(button){
            button.disabled=false;
            button.innerHTML='<i data-lucide="user-plus" class="w-4 h-4"></i><span>Salvar e selecionar</span>';
        }
        lucide.createIcons();
    }
    async function buscarCepInline(cep){
        const cleanCep=cep.replace(/\D/g,'');
        if(cleanCep.length!==8)return;
        setInlineClientError('inlineCepError','');
        try{
            const response=await fetch(`https://viacep.com.br/ws/${cleanCep}/json/`);
            if(!response.ok)throw new Error('Falha ao consultar CEP');
            const data=await response.json();
            if(data.erro){ setInlineClientError('inlineCepError','CEP não encontrado'); return; }
            $('inlineClientAddress').value=`${data.logradouro||''}, Número:     - Bairro: ${data.bairro||''} - ${data.uf||''}`;
            $('inlineClientCity').value=data.localidade||'';
            $('inlineClientAddress').focus();
        }catch(error){
            console.error(error);
            setInlineClientError('inlineCepError','Erro ao buscar CEP');
        }
    }
    async function saveInlineClient(event){
        event.preventDefault();
        const name=$('inlineClientName').value.trim();
        const type=$('inlineClientType').value;
        const documentValue=$('inlineClientDoc').value.trim();
        const email=$('inlineClientEmail').value.trim();
        const phone=$('inlineClientPhone').value.trim();
        const digits=value=>value.replace(/\D/g,'');
        clearInlineClientErrors();

        if(!name){ alert('Por favor, insira o Nome Completo / Razão Social!'); $('inlineClientName').focus(); return; }

        let valid=true;
        if(documentValue){
            const required=type==='company'?14:11;
            if(digits(documentValue).length!==required){
                setInlineClientError('inlineDocumentError',type==='company'?'CNPJ deve ter 14 números':'CPF deve ter 11 números');
                valid=false;
            }
        }
        if(email&&!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)){
            setInlineClientError('inlineEmailError','E-mail inválido');
            valid=false;
        }
        if(phone&&(digits(phone).length<10||digits(phone).length>11)){
            setInlineClientError('inlinePhoneError','Telefone inválido');
            valid=false;
        }
        if(!valid)return;

        const button=$('saveInlineClientButton');
        button.disabled=true;
        button.innerHTML='<span class="completion-spinner"></span><span>Salvando...</span>';
        try{
            const response=await fetch('/technical-visits/clients',{
                method:'POST',
                headers:csrfHeaders(true),
                body:JSON.stringify({
                    name,
                    type,
                    document:documentValue,
                    email,
                    phone,
                    address:$('inlineClientAddress').value.trim(),
                    city:$('inlineClientCity').value.trim()
                })
            });
            const savedClient=await response.json().catch(()=>({}));
            if(!response.ok)throw new Error(savedClient.error||'Erro ao guardar o cliente.');

            const option=document.createElement('button');
            option.type='button';
            option.className='client-option';
            option.dataset.clientId=savedClient.id;
            option.dataset.clientName=savedClient.name;
            option.dataset.clientCity=savedClient.city||'';
            option.onclick=()=>selectClientFromModal(option);
            option.innerHTML=`<span class="client-option-icon"><i data-lucide="user-round" class="w-5 h-5"></i></span><span class="min-w-0 flex-1 text-left"><span class="client-option-name">${esc(savedClient.name)}</span><span class="client-option-city">${esc(savedClient.city||'Cidade não informada')}</span></span><span class="client-option-check"><i data-lucide="check" class="w-4 h-4"></i></span>`;

            const results=$('clientSearchResults');
            results.insertBefore(option,$('clientSearchEmpty'));
            const count=results.querySelectorAll('.client-option').length;
            if($('clientSearchCount'))$('clientSearchCount').textContent=`${count} cliente(s) cadastrado(s)`;

            selectClientFromModal(option);
            resetInlineClientForm();
            closeInlineClientModal();
        }catch(error){
            console.error(error);
            alert(error.message||'Erro ao guardar o cliente.');
        }finally{
            if(!$('clientInlineModal').classList.contains('hidden')){
                button.disabled=false;
                button.innerHTML='<i data-lucide="user-plus" class="w-4 h-4"></i><span>Salvar e selecionar</span>';
                lucide.createIcons();
            }
        }
    }
    $('inlineClientCep')?.addEventListener('input',event=>{
        let value=event.target.value.replace(/\D/g,'').substring(0,8);
        if(value.length>5)value=value.replace(/^(\d{5})(\d{1,3})$/,'$1-$2');
        event.target.value=value;
        if(value.length===9)buscarCepInline(value);
    });
    $('clientInlineModal')?.addEventListener('click',event=>{ if(event.target===$('clientInlineModal'))closeInlineClientModal(); });
    document.addEventListener('keydown',event=>{ if(event.key==='Escape'&&!$('clientInlineModal').classList.contains('hidden'))closeInlineClientModal(); });

    async function createVisit(event) {
        event.preventDefault();
        const clientId=$('newClientId').value;
        if(!clientId){ openClientSearchModal(); return; }
        const button=event.target.querySelector('button[type="submit"], .submit-btn');
        button.disabled=true;
        try {
            const response=await fetch('/technical-visits/create', {method:'POST',headers:csrfHeaders(true),body:JSON.stringify({clientId,visitDate:$('newVisitDate').value,visitTime:$('newVisitTime').value||null,responsible:$('newVisitResponsible').value.trim(),notes:$('newVisitNotes').value,status:'AGENDADA'})});
            const data=await response.json().catch(()=>({}));
            if(!response.ok)throw new Error(data.error||'Não foi possível agendar a visita.');
            if(location.pathname==='/agenda')location.reload();
            else location.href='/technical-visits?visit='+data.id;
        } catch(error) { alert(error.message||'Erro de comunicação com o servidor.'); }
        finally { button.disabled=false; }
    }
    Object.assign(window, {openClientSearchModal, closeClientSearchModal, clearClientSearchInput, filterClientOptions, selectClientFromModal, openInlineClientModal, closeInlineClientModal, setInlineClientError, clearInlineClientErrors, resetInlineClientForm, buscarCepInline, saveInlineClient, createVisit});
    document.addEventListener('DOMContentLoaded',()=>{
        const date=new Date();
        $('newVisitDate').value=[date.getFullYear(),String(date.getMonth()+1).padStart(2,'0'),String(date.getDate()).padStart(2,'0')].join('-');
        filterClientOptions();
        $('clientSearchModal').addEventListener('click',event=>{if(event.target===$('clientSearchModal'))closeClientSearchModal();});
    });
})();
