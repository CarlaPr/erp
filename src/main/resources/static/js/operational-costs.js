(() => {
    'use strict';
    const el = id => document.getElementById(id);
    const esc = v => String(v ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
    const money = v => Number(v ?? 0).toLocaleString('pt-BR', {style:'currency',currency:'BRL'});
    const dayLabel = v => v ? v.split('-').reverse().join('/') : '';
    const panel = () => el('operationalCostPanel');
    const canEdit = () => panel()?.dataset.canEdit === 'true';
    const canDelete = () => panel()?.dataset.canDelete === 'true';
    const status = {PENDING:'Não alocado',PARTIALLY_ALLOCATED:'Parcial',ALLOCATED:'Alocado',CANCELLED:'Cancelado'};
    const kinds = {OPERATIONAL:'Operacional',DIRECT:'Custo direto',GENERAL:'Geral / estoque'};
    const methods = {EQUAL:'Igual',DISTANCE:'Por distância',PERCENTAGE:'Por percentual',MANUAL:'Valores manuais',DIRECT:'Vinculação direta'};
    let state = null, date = null, editCost = null, requestKey = null, allocationRequest = null, loadSerial = 0, lastFocus = null;
    let targets = null, payableOptions = [], reportOptions = null;
    async function api(url, body) {
        const headers = {'Accept':'application/json','X-Requested-With':'XMLHttpRequest'};
        if(body !== undefined) {
            headers['Content-Type']='application/json';
            const token=document.querySelector('meta[name="_csrf"]')?.content;
            if(token) headers[document.querySelector('meta[name="_csrf_header"]')?.content || 'X-CSRF-TOKEN']=token;
        }
        const response=await fetch('/operational-costs'+url,{method:body===undefined?'GET':'POST',headers,
            ...(body===undefined?{}:{body:JSON.stringify(body)})});
        const data=await response.json().catch(()=>null);
        if(!response.ok) throw new Error(data?.error || data?.message || 'Não foi possível concluir a operação ('+response.status+').');
        return data;
    }
    function error(e) { el('costDialogError').textContent=e.message || 'Erro de comunicação.'; }
    function dialog(title,html) {
        lastFocus=document.activeElement;
        el('costDialogTitle').textContent=title;el('costDialogError').textContent='';
        el('costDialogContent').innerHTML=html;
        el('costDialog').classList.remove('hidden');el('costDialog').classList.add('flex');
        el('costDialog').querySelector('input,select,button')?.focus();
        if(window.lucide) lucide.createIcons();
    }
    function close() {
        el('costDialog').classList.add('hidden');el('costDialog').classList.remove('flex');
        lastFocus?.focus();
    }
    async function busy(button,fn) {
        button.disabled=true;
        try { await fn(); } catch(e) { error(e); } finally { button.disabled=false; }
    }
    async function load(day) {
        if(!panel()) return;
        date=day;
        const serial=++loadSerial;
        el('costDayLabel').textContent=dayLabel(day);
        el('costNotice').textContent='Carregando custos...';
        el('costDayList').replaceChildren();el('costDaySummary').replaceChildren();
        try {
            const data=await api('/day?date='+encodeURIComponent(day));
            if(serial!==loadSerial) return;
            state=data;render();
        } catch(e) { if(serial===loadSerial) el('costNotice').textContent=e.message; }
    }
    function render() {
        el('costNotice').textContent=state.events.length ? '' : 'Não existem eventos para rateio neste dia. Você pode registrar custos gerais ou vincular diretamente a uma OS/orçamento.';
        const stats=[['Eventos',state.events.length],['Custos do dia',money(state.total)],['Alocado',money(state.allocated)],
            ['Não alocado',money(state.unallocated)],['Distância da rota',state.route.totalKm+' km'],['Média / evento',money(state.averagePerEvent)]];
        el('costDaySummary').innerHTML=stats.map(s=>'<div class="rounded-xl p-3 bg-slate-50 dark:bg-slate-900"><p class="text-xs text-slate-500">'+s[0]+'</p><p class="font-black mt-1">'+esc(s[1])+'</p></div>').join('');
        if(state.fuelPerKm!==null) el('costDaySummary').innerHTML+='<p class="text-xs text-slate-500 col-span-2">Combustível / km: '+money(state.fuelPerKm)+'</p>';
        el('costDayList').innerHTML=state.costs.length ? state.costs.map(c=> {
            const active=c.allocations.filter(a=>a.status==='ACTIVE');
            let buttons='<button type="button" class="cost-secondary" data-action="history" data-id="'+c.id+'">Histórico</button>';
            if(c.status!=='CANCELLED') {
                if(canEdit() && !active.length) buttons+='<button type="button" class="cost-secondary" data-action="edit" data-id="'+c.id+'">Editar</button>';
                if(canEdit() && c.status==='PENDING' && c.kind!=='GENERAL')
                    buttons+='<button type="button" class="cost-primary" data-action="allocate" data-id="'+c.id+'">'+(c.allocatable?'Ratear':'Vincular')+'</button>';
                if(canDelete() && active.length) buttons+='<button type="button" class="cost-secondary" data-action="reverse" data-id="'+c.id+'">Desfazer rateio</button>';
                if(canDelete()) buttons+='<button type="button" class="cost-secondary text-rose-600" data-action="cancel" data-id="'+c.id+'">Estornar custo</button>';
            }
            return '<article class="cost-card"><div class="flex flex-wrap justify-between gap-2"><div><b>'+esc(c.typeName)+'</b> <span class="text-xs text-slate-500">'+esc(kinds[c.kind])+' · '+esc(status[c.status])+'</span><p class="text-sm">'+esc(c.description)+'</p></div><strong>'+money(c.amount)+'</strong></div>'+
                '<p class="text-xs text-slate-500 my-2">Original: '+money(c.amount)+' · Alocado: '+money(c.allocated)+' · Saldo: '+money(c.balance)+' · '+(c.paymentStatus==='paid'?'Pago':'Conta a pagar')+'</p>'+
                (active.length?'<ul class="text-sm mb-3">'+active.map(a=>'<li>'+esc(a.label)+' — '+money(a.value)+'</li>').join('')+'</ul>':'')+
                '<div class="flex flex-wrap gap-2">'+buttons+'</div></article>';
        }).join(''):'<p class="text-sm text-slate-500 py-3">Nenhum custo registrado.</p>';
    }
    async function ensureTargets() { if(!targets) targets=await api('/targets');return targets; }
    const options = (list,key,label,selected) => list.map(t=>'<option value="'+esc(t[key])+'"'+(t[key]===selected?' selected':'')+'>'+esc(t[label])+'</option>').join('');
    async function add(cost=null) {
        if(!state || !canEdit()) return;
        editCost=cost;requestKey=crypto.randomUUID();
        try {
            await ensureTargets();
            const types=state.types.slice();
            if(cost && !types.some(t=>t.id===cost.typeId)) types.push({id:cost.typeId,name:cost.typeName,defaultAllocatable:cost.allocatable});
            dialog(cost?'Editar custo':'Adicionar custo — '+dayLabel(date),
                '<form id="costEditor" class="space-y-4"><div class="grid grid-cols-1 sm:grid-cols-2 gap-4">'+
                '<label><span class="cost-label">Tipo</span><select id="costType" required class="cost-field">'+options(types,'id','name',cost?.typeId)+'</select></label>'+
                '<label><span class="cost-label">Valor (R$)</span><input id="costAmount" type="number" min="0.01" step="0.01" required class="cost-field" value="'+esc(cost?.amount || '')+'"></label></div>'+
                '<label class="block"><span class="cost-label">Atribuição</span><select id="costMode" class="cost-field"><option value="shared">Operacional rateável</option><option value="linked">Operacional vinculado diretamente</option><option value="direct">Material / custo direto</option><option value="general">Geral / estoque, sem rateio</option><option value="unallocated">Operacional sem atribuição</option></select></label>'+
                '<label id="costDirectWrap" class="hidden block"><span class="cost-label">Destino</span><select id="costTarget" class="cost-field"><option value="">Selecione</option>'+options(state.events,'key','label',null)+options(targets,'key','label',null)+'</select></label>'+
                '<details><summary class="text-sm font-bold cursor-pointer">Mais informações</summary><div class="grid grid-cols-1 sm:grid-cols-2 gap-4 mt-3">'+
                '<label><span class="cost-label">Descrição</span><input id="costDescription" maxlength="255" class="cost-field" value="'+esc(cost?.description || '')+'"></label>'+
                '<label><span class="cost-label">Forma de pagamento</span><select id="costPayment" class="cost-field"><option value="">Não informado</option><option value="PIX">Pix</option><option value="DINHEIRO">Dinheiro</option><option value="DEBIT">Débito</option><option value="CREDIT">Crédito</option><option value="TRANSFER">Transferência</option><option value="BOLETO">Boleto</option></select></label>'+
                '<label><span class="cost-label">Equipe / responsável</span><input id="costTeam" maxlength="255" list="costTeams" class="cost-field" value="'+esc(cost?.team || state.route.team || '')+'"><datalist id="costTeams">'+state.events.map(e=>'<option value="'+esc(e.team || e.responsible || '')+'"></option>').join('')+'</datalist></label>'+
                '<label><span class="cost-label">Método preferido</span><select id="costPreferredMethod" class="cost-field">'+options(Object.entries(methods).filter(([k])=>k!=='DIRECT').map(([key,label])=>({key,label})),'key','label',cost?.method || 'EQUAL')+'</select></label>'+
                '<label class="sm:col-span-2"><span class="cost-label">Observações</span><textarea id="costNotes" maxlength="10000" class="cost-field">'+esc(cost?.notes || '')+'</textarea></label>'+
                (!cost?'<label class="sm:col-span-2"><span class="cost-label">Conta já lançada no financeiro</span><select id="costExisting" class="cost-field"><option value="">Criar lançamento financeiro único</option></select></label>':'')+
                '</div></details>'+
                (!cost?'<label class="flex gap-2 text-sm"><input id="costPaid" type="checkbox" checked> Já pago (registrar saída no Livro Caixa)</label>':'<p class="text-sm text-slate-500">Valor pago e histórico financeiro são preservados.</p>')+
                '<button type="submit" class="cost-primary">Salvar custo</button></form>');
            el('costPayment').value=cost?.paymentMethod || '';
            el('costMode').value=cost?cost.kind==='GENERAL'?'general':cost.kind==='DIRECT'?'direct':cost.allocatable?'shared':'unallocated':'shared';
            if(!cost && !types[0]?.defaultAllocatable) el('costMode').value='general';
            const modeChanged=()=>el('costDirectWrap').classList.toggle('hidden',!['linked','direct'].includes(el('costMode').value));
            el('costMode').onchange=modeChanged;modeChanged();
            el('costType').onchange=()=>{
                const t=types.find(t=>t.id===el('costType').value);
                el('costMode').value=t.defaultAllocatable?'shared':'general';modeChanged();
            };
            if(!cost) {
                payableOptions=await api('/payable-options?date='+encodeURIComponent(date)).catch(()=>[]);
                el('costExisting').innerHTML='<option value="">Criar lançamento financeiro único</option>'+options(payableOptions,'id','label',null);
                el('costExisting').onchange=()=>{
                    const p=payableOptions.find(p=>p.id===el('costExisting').value);
                    el('costPaid').disabled=!!p;
                    if(p) {
                        el('costAmount').value=p.amount;el('costType').value=p.typeId;el('costDescription').value=p.description;
                        el('costPaid').checked=p.paid;
                    }
                };
            }
            el('costEditor').onsubmit=event=>{
                event.preventDefault();
                busy(event.submitter,async()=>{
                    const mode=el('costMode').value;
                    const target=el('costTarget').value;
                    if(['linked','direct'].includes(mode) && !target) throw new Error('Selecione o destino.');
                    await api('/save-ajax',{id:cost?.id || null,version:cost?.version ?? null,requestKey,date,typeId:el('costType').value,
                        amount:el('costAmount').value,description:el('costDescription').value,paymentMethod:el('costPayment').value,
                        kind:mode==='direct'?'DIRECT':mode==='general'?'GENERAL':'OPERATIONAL',allocatable:mode==='shared',
                        method:mode==='shared'?el('costPreferredMethod').value:null,team:el('costTeam').value,notes:el('costNotes').value,
                        existingPayableId:el('costExisting')?.value || null,paid:el('costPaid')?.checked || false,
                        directTarget:['linked','direct'].includes(mode)?target:null});
                    close();await load(date);
                    if(el('costReportFilters')) await report();
                });
            };
        } catch(e) { el('costNotice').textContent=e.message; }
    }
    async function allocate(cost) {
        try {
            await ensureTargets();
            editCost=cost;requestKey=crypto.randomUUID();allocationRequest=null;
            if(!cost.allocatable) {
                dialog('Vincular '+cost.description+' — '+money(cost.amount),
                    '<label class="block"><span class="cost-label">Destino</span><select id="allocationDirect" class="cost-field">'+options(state.events,'key','label',null)+options(targets,'key','label',null)+'</select></label><button id="allocationConfirm" class="cost-primary mt-4" type="button">Confirmar vínculo</button>');
                el('allocationConfirm').onclick=e=>busy(e.currentTarget,async()=>{
                    await api('/'+cost.id+'/allocate',{requestKey,version:cost.version,method:'DIRECT',items:[{targetKey:el('allocationDirect').value}]});
                    close();await load(date);
                });
                return;
            }
            dialog('Rateio de '+cost.description+' — '+money(cost.amount),
                '<label class="block mb-4"><span class="cost-label">Método</span><select id="allocationMethod" class="cost-field">'+options(Object.entries(methods).filter(([k])=>k!=='DIRECT').map(([key,label])=>({key,label})),'key','label',cost.method)+'</select></label>'+
                (state.events.length?'<div class="overflow-x-auto"><table class="cost-table"><thead><tr><th>Participa</th><th>Evento</th><th class="allocation-distance">Distância (km)</th><th class="allocation-percent">Percentual (%)</th><th class="allocation-value">Valor (R$)</th></tr></thead><tbody>'+
                    state.events.map((e,i)=>'<tr data-event="'+esc(e.key)+'"><td><input aria-label="Selecionar '+esc(e.label)+'" type="checkbox" checked class="allocation-selected"></td><td>'+esc(e.label)+'<p class="text-xs text-slate-500">'+esc(e.team || e.responsible || '')+'</p></td>'+
                        '<td class="allocation-distance"><input aria-label="Distância '+esc(e.label)+'" type="number" step="0.001" min="0" class="cost-field" value="'+e.distanceKm+'"></td>'+
                        '<td class="allocation-percent"><input aria-label="Percentual '+esc(e.label)+'" type="number" step="0.0001" min="0" max="100" class="cost-field" value=""></td>'+
                        '<td class="allocation-value"><input aria-label="Valor '+esc(e.label)+'" type="number" step="0.01" min="0" class="cost-field" value=""></td></tr>').join('')+'</tbody></table></div>':
                    '<p class="text-sm text-slate-500">Não existem eventos para rateio nesta data.</p>')+
                '<p id="allocationInputsTotal" class="text-sm font-bold my-3"></p><div class="flex flex-wrap gap-2 mt-4"><button id="allocationCalculate" type="button" class="cost-secondary"'+(!state.events.length?' disabled':'')+'>Calcular</button>'+
                '<button id="allocationConfirm" type="button" class="cost-primary" disabled>Confirmar rateio</button></div><div id="allocationResult" class="mt-4"></div>');
            function invalidate() { allocationRequest=null;el('allocationConfirm').disabled=true;el('allocationResult').replaceChildren(); }
            function visibility() {
                const m=el('allocationMethod').value;
                ['distance','percent','value'].forEach((name,i)=>document.querySelectorAll('.allocation-'+name).forEach(e=>e.classList.toggle('hidden',m!==['DISTANCE','PERCENTAGE','MANUAL'][i])));
                invalidate();totals();
            }
            function totals() {
                const m=el('allocationMethod').value,rows=Array.from(document.querySelectorAll('[data-event]')).filter(r=>r.querySelector('.allocation-selected').checked);
                const total=rows.reduce((sum,r)=>sum+Number(r.querySelector(m==='DISTANCE'?'.allocation-distance input':m==='PERCENTAGE'?'.allocation-percent input':'.allocation-value input')?.value || 0),0);
                el('allocationInputsTotal').textContent=m==='DISTANCE'?'Distância selecionada: '+total.toLocaleString('pt-BR')+' km':m==='PERCENTAGE'?'Percentual total: '+total.toLocaleString('pt-BR')+'%':m==='MANUAL'?'Total informado: '+money(total):rows.length+' eventos selecionados';
            }
            el('allocationMethod').onchange=visibility;
            document.querySelectorAll('[data-event] input').forEach(input=>input.oninput=()=>{invalidate();totals();});
            visibility();
            el('allocationCalculate').onclick=e=>busy(e.currentTarget,async()=>{
                el('costDialogError').textContent='';
                const items=Array.from(document.querySelectorAll('[data-event]')).filter(r=>r.querySelector('.allocation-selected').checked)
                    .map(r=>({targetKey:r.dataset.event,distanceKm:r.querySelector('.allocation-distance input').value || null,
                        percentage:r.querySelector('.allocation-percent input').value || null,value:r.querySelector('.allocation-value input').value || null}));
                const payload={requestKey,version:cost.version,method:el('allocationMethod').value,items};
                const result=await api('/'+cost.id+'/calculate',payload);
                el('allocationResult').innerHTML='<h4 class="font-bold mb-2">Resultado</h4>'+result.map(r=>'<p class="text-sm py-1">'+esc(state.events.find(e=>e.key===r.targetKey)?.label)+' — '+r.percentage+'% — <b>'+money(r.value)+'</b></p>').join('')+
                    '<p class="font-black mt-2">Total: '+money(cost.amount)+'</p>';
                allocationRequest=payload;el('allocationConfirm').disabled=false;
            });
            el('allocationConfirm').onclick=e=>busy(e.currentTarget,async()=>{
                if(!allocationRequest) throw new Error('Calcule o rateio antes de confirmar.');
                await api('/'+cost.id+'/allocate',allocationRequest);close();await load(date);
            });
        } catch(e) { el('costNotice').textContent=e.message; }
    }
    async function action(name,id) {
        const c=state?.costs.find(c=>c.id===id);
        if(!c) return;
        if(name==='edit') return add(c);
        if(name==='allocate') return allocate(c);
        if(name==='history') {
            try {
                const history=await api('/'+id+'/history');
                dialog('Histórico — '+c.description,history.map(h=>'<article class="cost-card mb-3"><b>'+esc(h.action)+'</b> · '+esc(h.username)+' · '+esc(new Date(h.createdAt).toLocaleString('pt-BR'))+
                    '<pre class="text-xs mt-2 whitespace-pre-wrap break-words">'+esc(h.details)+'</pre></article>').join(''));
            } catch(e) { el('costNotice').textContent=e.message; }
            return;
        }
        const n=c.allocations.filter(a=>a.status==='ACTIVE').length;
        const question=name==='reverse'?'Desfazer '+money(c.allocated)+' atribuídos a '+n+' eventos? O custo original será preservado.':
            'Estornar este custo de '+money(c.amount)+' e sua conta financeira? '+(n?money(c.allocated)+' atribuídos a '+n+' eventos também serão estornados.':'');
        dialog(name==='reverse'?'Desfazer rateio':'Estornar custo','<p class="mb-4 text-sm">'+esc(question)+'</p><button id="costConfirmAction" type="button" class="cost-primary">Confirmar estorno</button>');
        el('costConfirmAction').onclick=e=>busy(e.currentTarget,async()=>{
            await api('/'+id+'/'+name,{version:c.version,confirmAllocated:true});close();await load(date);
            if(el('costReportFilters')) await report();
        });
    }
    async function route() {
        if(!state) return;
        const route=state.route,valid=new Set(state.events.map(e=>e.key));
        let ordered=route.stops.filter(s=>valid.has(s.eventKey)).map(s=>state.events.find(e=>e.key===s.eventKey));
        ordered=ordered.concat(state.events.filter(e=>!ordered.some(o=>o.key===e.key)));
        dialog('Rota — '+dayLabel(date),'<form id="routeEditor" class="space-y-4">'+
            '<div class="grid grid-cols-1 sm:grid-cols-2 gap-3"><label><span class="cost-label">Origem e retorno</span><input id="routeOrigin" required maxlength="255" class="cost-field" value="'+esc(route.origin)+'"></label>'+
            '<label><span class="cost-label">Equipe</span><input id="routeTeam" maxlength="255" class="cost-field" value="'+esc(route.team || '')+'"></label></div>'+
            '<p class="text-sm text-slate-500">Informe a distância de cada trecho até a parada. O retorno compõe a distância total da rota. No rateio, use a distância atribuída a cada evento.</p>'+
            '<div id="routeRows" class="space-y-2"></div><label class="block"><span class="cost-label">Última parada → empresa (km)</span><input id="routeReturn" type="number" min="0" step="0.001" required class="cost-field" value="'+route.returnKm+'"></label>'+
            (canEdit()?'<button type="submit" class="cost-primary">Salvar rota</button>':'<p class="text-sm text-slate-500">Rota disponível para consulta.</p>')+'</form>');
        function renderStops() {
            el('routeRows').innerHTML=ordered.map((e,i)=>'<div class="cost-card flex flex-wrap items-center gap-2" data-route-key="'+e.key+'"><label class="flex-1 text-sm"><input type="checkbox" class="route-selected" checked> <b>'+(i+1)+'. '+esc(e.label)+'</b><span class="block text-xs text-slate-500">'+esc(e.address)+'</span></label>'+
                '<label class="text-xs">km <input type="number" aria-label="Distância até '+esc(e.label)+'" min="0" step="0.001" class="cost-field route-km" style="width:95px" value="'+e.distanceKm+'"></label>'+
                '<button type="button" aria-label="Mover para cima" data-move="-1" data-index="'+i+'" class="cost-secondary">↑</button><button type="button" aria-label="Mover para baixo" data-move="1" data-index="'+i+'" class="cost-secondary">↓</button></div>').join('');
            el('routeRows').querySelectorAll('[data-move]').forEach(button=>button.onclick=()=>{
                document.querySelectorAll('[data-route-key]').forEach((row,i)=>{ordered[i]={...ordered[i],distanceKm:row.querySelector('.route-km').value};});
                const i=Number(button.dataset.index),to=i+Number(button.dataset.move);
                if(to<0 || to>=ordered.length) return;
                [ordered[i],ordered[to]]=[ordered[to],ordered[i]];renderStops();
            });
        }
        renderStops();
        if(!canEdit()) el('routeEditor').querySelectorAll('input,button').forEach(e=>e.disabled=true);
        el('routeEditor').onsubmit=e=>{
            e.preventDefault();if(!canEdit()) return;
            busy(e.submitter,async()=>{
                const stops=Array.from(document.querySelectorAll('[data-route-key]')).filter(r=>r.querySelector('.route-selected').checked)
                    .map(r=>({eventKey:r.dataset.routeKey,distanceKm:r.querySelector('.route-km').value}));
                await api('/route/'+date+'/save-ajax',{version:route.version,origin:el('routeOrigin').value,team:el('routeTeam').value,returnKm:el('routeReturn').value,stops});
                close();await load(date);
            });
        };
    }
    async function report() {
        if(!el('costReportFilters')) return;
        const params=new URLSearchParams(new FormData(el('costReportFilters')));
        for(const [k,v] of [...params]) if(!v) params.delete(k);
        el('costReportError').textContent='Carregando relatório...';
        try {
            const data=await api('/report?'+params);
            el('costReportError').textContent='';
            el('costReportTotals').textContent='Total: '+money(data.total)+' · Alocado: '+money(data.allocated)+' · Não alocado: '+money(data.unallocated);
            el('costReportDay').innerHTML=Object.entries(data.byDay).map(([d,v])=>'<tr><td>'+dayLabel(d)+'</td><td>'+money(v)+'</td></tr>').join('');
            el('costReportType').innerHTML=Object.entries(data.byType).map(([t,v])=>'<tr><td>'+esc(t)+'</td><td>'+money(v)+'</td></tr>').join('');
            el('costReportOrders').innerHTML=data.orders.map(o=>'<tr><td>'+esc(o.number)+'</td><td>'+money(o.sale)+'</td><td>'+money(o.directCost)+'</td><td>'+money(o.operationalCost)+'</td><td>'+money(o.totalCost)+'</td><td>'+money(o.profit)+'</td><td>'+o.margin+'%</td></tr>').join('');
            el('costReportList').innerHTML=data.costs.map(c=>'<tr><td>'+dayLabel(c.date)+'</td><td>'+esc(c.typeName)+'</td><td>'+esc(c.description)+'</td><td>'+esc(kinds[c.kind])+'</td><td>'+money(c.amount)+'</td><td>'+money(c.allocated)+'</td><td>'+money(c.balance)+'</td><td>'+esc(status[c.status])+'</td></tr>').join('');
            el('costExport').href='/operational-costs/export?'+params;
        } catch(e) { el('costReportError').textContent=e.message; }
    }
    async function typeAdmin() {
        try {
            const [types,categories]=await Promise.all([api('/types'),api('/categories')]);
            let selected=null;
            dialog('Tipos de custo','<form id="typeEditor" class="space-y-3"><label class="block"><span class="cost-label">Editar tipo existente</span><select id="typeExisting" class="cost-field"><option value="">Novo tipo</option>'+options(types,'id','name',null)+'</select></label>'+
                '<label class="block"><span class="cost-label">Nome</span><input id="typeName" required maxlength="150" class="cost-field"></label>'+
                '<label class="block"><span class="cost-label">Código</span><input id="typeCode" required pattern="[a-z0-9_]+" maxlength="120" class="cost-field"></label>'+
                '<label class="block"><span class="cost-label">Categoria financeira</span><select id="typeCategory" class="cost-field">'+options(categories,'id','name',null)+'</select></label>'+
                '<label class="flex gap-2"><input id="typeAllocatable" type="checkbox" checked> Rateável por padrão</label>'+
                '<label class="flex gap-2"><input id="typeActive" type="checkbox" checked> Ativo</label>'+
                '<button type="submit" class="cost-primary">Salvar tipo</button></form>');
            el('typeExisting').onchange=()=>{
                selected=types.find(t=>t.id===el('typeExisting').value);
                el('typeName').value=selected?.name || '';el('typeCode').value=selected?.code || '';
                el('typeCategory').value=categories.find(c=>c.code===selected?.category)?.id || categories[0].id;
                el('typeAllocatable').checked=selected?.defaultAllocatable ?? true;el('typeActive').checked=selected?.active ?? true;
                el('typeCode').disabled=!!selected;el('typeCategory').disabled=!!selected;
            };
            el('typeEditor').onsubmit=e=>{
                e.preventDefault();busy(e.submitter,async()=>{
                    await api('/types/save-ajax',{id:selected?.id || null,name:el('typeName').value,code:el('typeCode').value,
                        categoryId:el('typeCategory').value,defaultAllocatable:el('typeAllocatable').checked,active:el('typeActive').checked});
                    close();await load(date);
                });
            };
        } catch(e) { el('costNotice').textContent=e.message; }
    }
    async function initializeReport() {
        const today=new Date().toLocaleDateString('en-CA',{timeZone:'America/Sao_Paulo'});
        const qs=new URLSearchParams(location.search);
        el('costPageDate').value=qs.get('date') || today;
        el('costReportFrom').value=today.substring(0,7)+'-01';el('costReportTo').value=today;
        await load(el('costPageDate').value);
        el('costPageDate').onchange=()=>load(el('costPageDate').value);
        el('costReportFilters').onsubmit=e=>{e.preventDefault();report();};
        try {
            reportOptions=await api('/report-options');
            el('costReportOrder').innerHTML='<option value="">Todas as OS</option>'+options(reportOptions.orders,'id','label',null);
            el('costReportClient').innerHTML='<option value="">Todos os clientes</option>'+options(reportOptions.clients,'id','name',null);
        } catch(e) { el('costReportError').textContent=e.message; }
        await report();
    }
    document.addEventListener('click',e=>{
        const b=e.target.closest('#operationalCostPanel [data-action]');
        if(b) action(b.dataset.action,b.dataset.id);
    });
    document.addEventListener('keydown',e=>{
        const modal=el('costDialog');
        if(!modal || modal.classList.contains('hidden')) return;
        if(e.key==='Escape') close();
        if(e.key==='Tab') {
            const list=[...modal.querySelectorAll('button,input,select,textarea,summary,a[href]')].filter(e=>!e.disabled && e.offsetParent!==null);
            const first=list[0],last=list[list.length-1];
            if(e.shiftKey && document.activeElement===first){e.preventDefault();last.focus();}
            else if(!e.shiftKey && document.activeElement===last){e.preventDefault();first.focus();}
        }
    });
    window.OperationalCosts={load,add,close,route,typeAdmin,report};
    document.addEventListener('DOMContentLoaded',()=>{if(el('costPageDate')) initializeReport();});
})();

