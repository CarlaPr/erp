(() => {
    const esc=v=>String(v ?? '').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
    const money=v=>Number(v ?? 0).toLocaleString('pt-BR',{style:'currency',currency:'BRL'});
    async function fetchSummary(kind,id) {
        const response=await fetch('/operational-costs/'+kind+'/'+encodeURIComponent(id),{headers:{Accept:'application/json'}});
        const data=await response.json();
        if(!response.ok) throw new Error(data.error || data.message || 'Não foi possível carregar os custos.');
        return data;
    }
    function html(data) {
        const values=[['Venda',data.sale],['Custos diretos',data.directCost],['Custos operacionais',data.operationalCost],
            ['Custo total',data.totalCost],['Resultado',data.profit]];
        return '<div class="grid grid-cols-2 sm:grid-cols-3 gap-3 mb-4">'+values.map(([name,value])=>
            '<div class="p-3 rounded-xl bg-slate-50 dark:bg-slate-900"><p class="text-xs text-slate-500">'+name+'</p><b>'+money(value)+'</b></div>').join('')+
            '<div class="p-3 rounded-xl bg-slate-50 dark:bg-slate-900"><p class="text-xs text-slate-500">Margem</p><b>'+data.margin+'%</b></div></div>'+
            (data.lines.length?'<div class="overflow-x-auto"><table class="cost-table"><thead><tr><th>Data</th><th>Tipo / gasto</th><th>Natureza</th><th>Valor</th></tr></thead><tbody>'+
                data.lines.map(line=>'<tr><td>'+line.date.split('-').reverse().join('/')+'</td><td>'+esc(line.type)+'<p>'+esc(line.description)+'</p></td><td>'+(line.kind==='DIRECT'?'Direto':'Operacional')+'</td><td>'+money(line.value)+'</td></tr>').join('')+
                '</tbody></table></div>':'<p class="text-sm text-slate-500">Nenhum custo alocado pela Agenda.</p>');
    }
    async function quote(id) {
        const modal=document.getElementById('quoteCostModal');
        const content=document.getElementById('quoteCostContent');
        content.textContent='Carregando...';modal.classList.remove('hidden');modal.classList.add('flex');
        try {
            const data=await fetchSummary('quote',id);
            document.getElementById('quoteCostTitle').textContent='Custos internos — '+data.number;
            content.innerHTML=html(data);
        } catch(e) { content.textContent=e.message; }
    }
    window.CostSummary={fetchSummary,html,quote};
})();
