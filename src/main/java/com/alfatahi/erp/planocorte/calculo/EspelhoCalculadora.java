package com.alfatahi.erp.planocorte.calculo;

import com.alfatahi.erp.planocorte.entity.CategoriaServico;
import com.alfatahi.erp.planocorte.entity.TipoFolha;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EspelhoCalculadora implements ServicoCalculadora {

    @Override
    public CategoriaServico getCategoria() {
        return CategoriaServico.ESPELHO;
    }

    @Override
    public ResultadoCalculoServico calcular(EntradaCalculoServico entrada) {
        boolean redondo = entrada.isRedondo();
        boolean temBisote = entrada.espessuraBisoteMm() != null && entrada.espessuraBisoteMm().signum() > 0;

        StringBuilder observacao = new StringBuilder("\n \n \n Espelho — ");
        if (redondo) {
            observacao.append("Redondo — Diâmetro: ")
                    .append(entrada.alturaVaoMm().stripTrailingZeros().toPlainString()).append("mm.");
            if (temBisote) {
                observacao.append(" \nBisôte de ").append(entrada.espessuraBisoteMm().stripTrailingZeros().toPlainString())
                        .append("mm em volta de toda a circunferência.");
            }
        } else {
            observacao.append("Tamanho total: ")
                    .append(entrada.larguraVaoMm().stripTrailingZeros().toPlainString()).append("x")
                    .append(entrada.alturaVaoMm().stripTrailingZeros().toPlainString()).append("mm.");
            if (temBisote) {
                observacao.append(" \nBisôte de ").append(entrada.espessuraBisoteMm().stripTrailingZeros().toPlainString())
                        .append("mm em volta de todas as laterais.");
            }
        }
        observacao.append("\n ");

        FolhaCalculada folha = new FolhaCalculada(TipoFolha.UNICA, entrada.larguraVaoMm(), entrada.alturaVaoMm(),
                List.of(), List.of(), observacao.toString(), temBisote ? entrada.espessuraBisoteMm() : null, redondo);
        return new ResultadoCalculoServico(List.of(folha));
    }
}
