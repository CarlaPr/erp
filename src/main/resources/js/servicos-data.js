const SERVICOS_DEFAULT = [
    {bloco:"01 — Box de Banheiro", norma:"NBR 14207", itens:[
            {nome:"Box Padrão", sub:"Frontal / Canto – Correr", espessuras:"8 mm, 10 mm", descL:"Vão – 10 mm (5 mm cada lateral)", descA:"Vão – 15 mm (7 sup. + 8 inf.)", furos:"Puxador ø12–15mm; dobradiça (se abrir) ø12mm — confirmar posição com fabricante", obs:"Lapidação reta obrigatória em todas as bordas."},
            {nome:"Box Até o Teto", sub:"Piso ao teto", espessuras:"8 mm, 10 mm", descL:"Vão – 10 mm (5mm/lateral)", descA:"Vão – 20 mm (10 sup. + 10 inf.)", furos:"Furo trilho superior (espigão) ø12mm centrado no topo de cada folha", obs:"Medir altura real após reboco. Largura máx. por folha: 1000mm (8mm) / 1200mm (10mm)."},
            {nome:"Box Flex / Flex Open", sub:"Articulado 2 folhas", espessuras:"8 mm", descL:"Vão – 12 mm; (Vão–12)÷2 = cada folha", descA:"Vão – 0 mm (altura máx. 1900mm)", furos:"Fechadura recorte ø30–35mm; puxador ø12mm conforme kit", obs:"Abertura até 90% do vão. Sistemas: Ideia Glass Flex, Marine Vidros Flex."},
            {nome:"Box Elegance", sub:"Roldana aparente", espessuras:"8 mm, 10 mm", descL:"Vão – 10 mm; sobrepasse 20–25mm", descA:"Vão – 80 mm (roldana + trilho inf.)", furos:"Roldana ø12–14mm (2/folha); puxador ø12mm a 100mm da borda", obs:"Roldana cromada/inox aparente."},
            {nome:"Box Camarão", sub:"Sanfonado 4 folhas", espessuras:"6 mm, 8 mm", descL:"Vão – 10 mm ÷ nº folhas", descA:"Vão – 15 mm", furos:"Dobradiça articulação ø10–12mm (2/folha interm.); pino trilho inf. ø8mm", obs:"Largura mín. por folha: 150mm."},
            {nome:"Box Abrir / Pivotante", sub:"1–2 portas + fixo", espessuras:"8 mm, 10 mm", descL:"Móvel: Vão – 8mm (4/lado)", descA:"Vão – 10 mm", furos:"Dobradiça ø12mm (2/porta, 150mm topo/rodapé); fechadura ø22–25mm; puxador ø12mm", obs:"Folha máx. 700mm de largura em 8mm."}
        ]},
    {bloco:"02 — Fechamento de Sacada", norma:"NBR 16259", itens:[
            {nome:"Cortina de Vidro", sub:"Sem roldana sup.", espessuras:"8 mm (≤4º andar), 10 mm", descL:"5 mm/lateral (folga escova)", descA:"Vão – 20 mm", furos:"Sem furos — lapidação reta obrigatória para escova adesiva", obs:"Largura máx. painel: 1200mm."},
            {nome:"Envidraçamento de Sacada", sub:"Deslizante com roldana", espessuras:"10 mm padrão, 8 mm (≤4º andar)", descL:"Vão÷nº folhas – 5mm; sobrepasse 25mm", descA:"Vão – 80 mm", furos:"Roldana ø12–14mm (2/folha, 50mm topo / 60mm lateral)", obs:"Análise de vento por andar obrigatória."},
            {nome:"Varanda Europeia", sub:"Deslizante/articulada", espessuras:"8 mm, 10 mm", descL:"Vão÷módulos – 8mm", descA:"Vão – 30 mm", furos:"Dobradiça topo ø12mm/folha; puxador lateral ø10mm", obs:"Painéis se dobram e empilham na extremidade."},
            {nome:"Sistema Versatik Sacada", sub:"Tec-Vidro VT3/VT6", espessuras:"8 mm, 10 mm", descL:"Ver bloco Versatik", descA:"Vão – 85 mm", furos:"Ver bloco Versatik", obs:"Cálculo detalhado no bloco Versatik."},
            {nome:"Sacada Fixa", sub:"Painel estrutural sem abertura", espessuras:"8 mm, 10 mm, Laminado 6+6", descL:"Vão – 10 mm (encaixe perfil U)", descA:"Vão – 20 mm", furos:"Clamps/cantoneiras ø12–14mm conforme projeto", obs:"Silicone estrutural na montagem."}
        ]},
    {bloco:"03 — Janelas", norma:"", itens:[
            {nome:"Janela de Correr", sub:"2+ folhas", espessuras:"8 mm, 10 mm", descL:"Vão÷folhas – 10mm; sobrepasse 15–20mm", descA:"Vão – 60 mm", furos:"Roldana ø10–12mm no rodapé; puxador ø12mm (1/3 da altura)", obs:"Abertura até 50% do vão (2 folhas)."},
            {nome:"Janela Pivotante", sub:"Eixo vertical central", espessuras:"6, 8, 10 mm", descL:"Vão – 8 mm (4/lado)", descA:"Vão – 8 mm (4/lado)", furos:"Pivo superior/inferior ø12mm a 50mm da borda; puxador lateral ø12mm", obs:"Eixo no centro da folha."},
            {nome:"Janela Basculante", sub:"Eixo horizontal", espessuras:"6 mm, 8 mm", descL:"Vão – 10 mm", descA:"Vão – 10 mm", furos:"Dobradiça sup. ø10mm a 50mm da lateral; corrente/braço ø8mm", obs:"Abertura ~45°."},
            {nome:"Janela Maxiar", sub:"Dobradiças no topo", espessuras:"6 mm, 8 mm", descL:"Vão – 10 mm", descA:"Vão – 10 mm", furos:"Dobradiça topo ø10mm a 60mm da lateral; fechamento inf. ø8–10mm", obs:"Abre de baixo para fora."},
            {nome:"Janela Seteira / Fixa", sub:"Sem abertura", espessuras:"4 mm comum, 6/8mm temp.", descL:"Vão – 6 mm", descA:"Vão – 6 mm", furos:"Sem furos (encaixilhado); com spider ø14–20mm", obs:"Temperado obrigatório se fixação via clamps."},
            {nome:"Janela de Abrir (Francesa)", sub:"1–2 folhas", espessuras:"8 mm, 10 mm", descL:"Vão – 10 mm (5/lado)", descA:"Vão – 10 mm", furos:"Dobradiça ø12mm a 150mm topo/rodapé; espagnolette ø20–22mm; puxador ø12mm", obs:"Largura máx./folha: 700mm."}
        ]},
    {bloco:"04 — Portas de Vidro", norma:"", itens:[
            {nome:"Porta de Correr", sub:"2 folhas, trilho sup.", espessuras:"8 mm, 10 mm", descL:"Vão÷2 + 25mm (sobrepasse 50mm)", descA:"Vão – 80 mm", furos:"Roldana ø12–14mm (2/folha); puxador H ø12mm a 1000mm do piso; fechadura ø22–25mm", obs:"Largura máx./folha: 1200mm (10mm)/1000mm (8mm)."},
            {nome:"Porta de Abrir", sub:"Convencional c/ batente", espessuras:"8 mm, 10 mm", descL:"Vão – 10 mm (5/lado)", descA:"Vão – 10 mm", furos:"Dobradiça ø12–14mm (3/folha, 200/900/1700mm); fechadura 22×65mm; puxador ø12mm; olho mágico ø14mm", obs:"Largura máx.: 900mm. Com mola hidráulica: 10mm recomendado."},
            {nome:"Porta Pivotante", sub:"Eixo vertical offset/central", espessuras:"10 mm, 12 mm", descL:"Vão – 10 mm (5/lado)", descA:"Vão – 15 mm", furos:"Pivot sup/inf ø16–20mm (Dorma/Biloba); puxador/fechadura ø12–22mm — posição do pivot é crítica", obs:"Largura até 1800mm (12mm)."},
            {nome:"Porta Corre Atrás da Parede", sub:"Wall-slot embutida", espessuras:"8 mm, 10 mm", descL:"Vão – 6 mm (3/lado)", descA:"Vão – 80 mm", furos:"Roldana ø12mm (2, 40mm do topo); puxador embutido recorte ø30mm", obs:"Trilho embutido na alvenaria."},
            {nome:"Porta Sanfonada", sub:"Múltiplas folhas dobráveis", espessuras:"6 mm, 8 mm", descL:"Vão÷folhas – 10mm", descA:"Vão – 30 mm", furos:"Dobradiça articulação ø10–12mm; pino trilho inf. ø8mm", obs:"Largura mín./folha: 200mm."},
            {nome:"Porta Vision / Blindex", sub:"Roldana aparente premium", espessuras:"8 mm, 10 mm", descL:"Vão÷folhas + sobrepasse 25–30mm", descA:"Vão – 100 mm", furos:"Roldana ø14mm (2/folha); puxador ø12mm par", obs:"Marca referência: Blindex, Ideia Glass Vision."}
        ]},
    {bloco:"05 — Kit Versatik (Tec-Vidro)", norma:"", itens:[
            {nome:"VT3 – Versatik Truck", sub:"1 fixa + 2 móveis", espessuras:"8 mm, 10 mm", descL:"Sem fechadura: (Vão+120)÷3 · Com fechadura: Vão÷3 +120mm no vidro nº3", descA:"Vão – 85 mm", furos:"Roldana ø10mm (2, 40–50mm topo); fechadura ø22–25mm; puxador H ø12mm passante", obs:"Vidros >40kg: solicitar furações extras p/ roldanas."},
            {nome:"VT6 – Versatik Truck", sub:"2 fixas + 4 móveis", espessuras:"8 mm, 10 mm", descL:"Sem fechadura: (Vão+120)÷6 · Com fechadura: Vão÷6 +120mm na(s) porta(s)", descA:"Vão – 85 mm", furos:"Mesmos padrões do VT3, duplicados", obs:"Até 160kg por peça. Peças fixas: só lapidação."},
            {nome:"Versatik Division", sub:"Divisórias", espessuras:"8 mm, 10 mm", descL:"Temp.: Vão–10 +15(transpasse) –100(fechadura) ÷folhas · Lam.: Vão–38 +15 –50 ÷folhas", descA:"Vão – 85 mm", furos:"Roldana ø10mm/folha; fechadura 3530 ø22mm (temperado); perfil embutido sem furo (laminado)", obs:"Até 160kg/peça."},
            {nome:"Versatik Engenharia", sub:"Portas leves até 60kg", espessuras:"6 mm, 8 mm", descL:"(Vão+50)÷2 (sobrepasse 50mm)", descA:"Vão – 85 mm", furos:"Roldana simples ø10mm (2/folha); puxador ø12mm", obs:"Máx. 60kg/peça."}
        ]},
    {bloco:"06 — Guarda-Corpo", norma:"NBR 14718", itens:[
            {nome:"GC Perfil U de Base", sub:"Embutido no piso", espessuras:"10, 12mm, Lam. 6+6/8+8", descL:"Painel: vão – 10mm (5/lateral)", descA:"—", furos:"Sem furos (encaixe no U)", obs:"Altura mín.: 1100mm (geral), 1300mm (>5m altura)."},
            {nome:"GC com Clamps", sub:"Fixação por garras", espessuras:"10 mm, 12 mm", descL:"Painel: vão – 10mm; espaçamento 3–5mm", descA:"—", furos:"Sem furo (pressão) ou ø14–20mm (spider-clamp)", obs:"Altura mín.: 1100mm."},
            {nome:"GC Frameless", sub:"Sem ferragem aparente", espessuras:"12, 15mm, Lam. 8+8", descL:"Projeto específico", descA:"—", furos:"ø20–25mm conforme projeto estrutural", obs:"Calculista estrutural obrigatório."},
            {nome:"GC Corrimão Embutido", sub:"Top rail + vidro", espessuras:"8 mm, 10 mm", descL:"Painel: vão – 10mm", descA:"—", furos:"Sem furos (corrimão encaixa no topo)", obs:"Altura: 900–1100mm. Usado em escadas/mezaninos."}
        ]},
    {bloco:"07 — Abrigo de Pia", norma:"", itens:[
            {nome:"Porta de Correr (Pia)", sub:"2 folhas sob a pia", espessuras:"6 mm, 8 mm", descL:"Vão÷2 + 20mm", descA:"Vão – 30 mm", furos:"Roldana ø10mm (2/folha); puxador ø12mm opcional", obs:"Sistema Versatik ou roldana simples."},
            {nome:"Porta de Abrir (Pia)", sub:"Fixas + dobradiça", espessuras:"6 mm, 8 mm", descL:"Vão – 10 mm", descA:"Vão – 10 mm", furos:"Dobradiça ø10–12mm (2/folha); puxador ø10mm; fechadura magnética conforme modelo", obs:"Largura máx.: 600mm."},
            {nome:"Painel Fixo (Pia)", sub:"Sem abertura", espessuras:"4 mm, 6 mm", descL:"Vão – 6 mm", descA:"—", furos:"Sem furos (perfil U ou silicone)", obs:"Vidro comum 4mm se encaixilhado."}
        ]},
    {bloco:"08 — Coberturas", norma:"NBR 7199", itens:[
            {nome:"Cobertura Fixa de Vidro", sub:"Plana ou inclinada", espessuras:"Lam. 6+6, 8+8, Temp.Lam.10+10", descL:"Painel: vão – 20mm; inclinação mín. 3%", descA:"—", furos:"Spider ø18–22mm ou sem furo (encaixilhado)", obs:"Laminado é OBRIGATÓRIO em coberturas (ABNT)."},
            {nome:"Cobertura Retrátil", sub:"Deslizante/dobrável", espessuras:"Lam.6+6 ou Policarb. 6/8mm", descL:"Conforme projeto; folga mín. 5mm/painel", descA:"—", furos:"Trilho/roldana ø10–12mm; acionamento motor conforme fabricante", obs:"Manual ou motorizada."},
            {nome:"Cobertura Policarbonato", sub:"Alveolar ou compacto", espessuras:"Alv. 6/8mm, Compacto 4–8mm", descL:"Placa: vão + 100mm/lado", descA:"—", furos:"Fixação treliça ø8–10mm; espaçamento máx. 500mm; não apertar (dilatação)", obs:"Não requer têmpera."},
            {nome:"Cobertura Habitat (Cebrace)", sub:"Controle solar", espessuras:"Lam. 3+3 a 6+6, Temp. 4/6/8mm", descL:"Conforme aplicação", descA:"—", furos:"Antes da têmpera (versão temperada)", obs:"Bloqueia 99% UV, reduz 70% calor."},
            {nome:"Cobertura Curva / Marquise", sub:"Vidro curvo/estrutural", espessuras:"Laminado curvo, Temp.curvo 6–10mm", descL:"Conforme raio de curvatura", descA:"—", furos:"Definidos antes da têmpera e curvagem (não reversível)", obs:"Prazo e custo elevados."}
        ]},
    {bloco:"09 — Outros Serviços", norma:"", itens:[
            {nome:"Espelho Plano", sub:"Lapidado / Bisotê", espessuras:"3 mm, 4 mm", descL:"Medida exata do nicho", descA:"—", furos:"Fixação parede ø8–10mm c/bucha; tomada embutida ø65–70mm", obs:"Vidro plano, não temperado."},
            {nome:"Tampo de Mesa / Prateleira", sub:"Temperado ou comum", espessuras:"6, 8, 10, 12 mm", descL:"Medida exata da base", descA:"—", furos:"Suporte/ponteira ø14–20mm — antes da têmpera", obs:"Acabamento de canto definido antes do pedido."},
            {nome:"Divisória de Vidro", sub:"Escritório/Cozinha", espessuras:"8 mm, 10 mm", descL:"Painel fixo: vão – 10mm; porta ver bloco 04", descA:"—", furos:"Suporte parede ø12–14mm; porta ver bloco 04", obs:"Serigrafado: aprovar arte antes da têmpera."},
            {nome:"Vitrine Comercial", sub:"Loja / Fachada", espessuras:"6, 8, 10 mm", descL:"Painel: vão – 10mm; folga perfil 5mm", descA:"—", furos:"Puxador ø12–22mm; fechadura ø22–25mm", obs:"Sistema storefront com perfil de alumínio."},
            {nome:"Vidro Comum (Float)", sub:"Encaixilhado", espessuras:"3, 4, 5, 6 mm", descL:"Vão – 4 mm (2/lado)", descA:"—", furos:"Pode furar/cortar depois (não temperado) — melhor prever antes", obs:"Não usar como porta, box ou guarda-corpo."},
            {nome:"Vidro Jateado / Acidado", sub:"Privacidade translúcida", espessuras:"4–6mm comum, 8–10mm temp.", descL:"Conforme aplicação", descA:"—", furos:"Antes da têmpera se temperado", obs:"Jateamento pode ser após corte se vidro comum."},
            {nome:"Vidro Serigrafado", sub:"Arte + temperado", espessuras:"6, 8, 10 mm", descL:"Conforme aplicação", descA:"—", furos:"Antes da têmpera — arte aprovada antes (não altera depois)", obs:"Tinta cerâmica fundida na têmpera."},
            {nome:"Bloco de Vidro", sub:"Tijolo de vidro", espessuras:"19×19cm / 20×20cm", descL:"Junta de argamassa 10–15mm", descA:"—", furos:"Sem furos", obs:"Não suporta carga estrutural."},
            {nome:"Aquário de Vidro", sub:"Residencial/comercial", espessuras:"6mm(≤300L), 8–10mm, 12mm+(>800L)", descL:"Conforme projeto", descA:"—", furos:"Filtragem ø25–50mm (vidro comum) — NÃO usar temperado", obs:"Colagem com silicone neutro atóxico."},
            {nome:"Película de Vidro", sub:"Solar/privacidade", espessuras:"50–350 mícrons", descL:"Não se aplica", descA:"—", furos:"Não requer furos", obs:"Não substitui vidro temperado em segurança estrutural."},
            {nome:"Corrimão / Escada com Vidro", sub:"Lateral de escada", espessuras:"Lam.6+6, 10mm, 12mm", descL:"Conforme projeto estrutural", descA:"—", furos:"Clamp/spider ø14–20mm — antes da têmpera", obs:"Calculista estrutural obrigatório."}
        ]}
];

function carregarServicos(){
    const salvo = localStorage.getItem('vc_servicos_config');
    return salvo ? JSON.parse(salvo) : JSON.parse(JSON.stringify(SERVICOS_DEFAULT));
}
function salvarServicos(dados){
    localStorage.setItem('vc_servicos_config', JSON.stringify(dados));
}