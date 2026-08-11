package com.alfatahi.erp.planocorte.calculo;

import com.alfatahi.erp.planocorte.entity.DimensaoAjuste;
import com.alfatahi.erp.planocorte.entity.RegraTecnica;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public final class CalculoUtil {

    private CalculoUtil() {
    }

    public static BigDecimal dividir(BigDecimal valor, int divisor) {
        return valor.divide(BigDecimal.valueOf(Math.max(divisor, 1)), 4, RoundingMode.HALF_UP);
    }

    public static BigDecimal descontar(BigDecimal valor, BigDecimal desconto) {
        if (desconto == null) {
            return valor;
        }
        BigDecimal resultado = valor.subtract(desconto);
        return resultado.signum() < 0 ? BigDecimal.ZERO : resultado;
    }

    public static BigDecimal somar(BigDecimal valor, BigDecimal adicional) {
        if (adicional == null) {
            return valor;
        }
        return valor.add(adicional);
    }

    public static BigDecimal arredondarParaCima(BigDecimal valor, BigDecimal passo) {
        if (passo == null || passo.signum() == 0) {
            return valor;
        }
        BigDecimal[] divisaoResto = valor.divideAndRemainder(passo);
        if (divisaoResto[1].signum() == 0) {
            return valor;
        }
        return divisaoResto[0].add(BigDecimal.ONE).multiply(passo);
    }

    public static int folhasOuPadrao(Integer quantidadeFolhas, int padrao) {
        return quantidadeFolhas != null && quantidadeFolhas > 0 ? quantidadeFolhas : padrao;
    }

    public static AjusteDetalhado aplicarRegras(BigDecimal larguraBrutaMm, BigDecimal alturaBrutaMm,
                                                 List<RegraTecnica> regras) {
        BigDecimal largura = larguraBrutaMm;
        BigDecimal altura = alturaBrutaMm;
        StringBuilder detalhe = new StringBuilder();

        for (RegraTecnica regra : regras) {
            String sinal = regra.getValorMm().signum() >= 0 ? "desconto de " : "acréscimo de ";
            String valorTexto = regra.getValorMm().abs().stripTrailingZeros().toPlainString() + "mm";

            if (regra.getDimensao() == DimensaoAjuste.LARGURA || regra.getDimensao() == DimensaoAjuste.LARGURA_E_ALTURA) {
                largura = largura.subtract(regra.getValorMm());
            }
            if (regra.getDimensao() == DimensaoAjuste.ALTURA || regra.getDimensao() == DimensaoAjuste.LARGURA_E_ALTURA) {
                altura = altura.subtract(regra.getValorMm());
            }

            if (detalhe.length() > 0) {
                detalhe.append("; ");
            }
            detalhe.append(regra.getDescricao()).append(" (").append(regra.getDimensao().getDescricao().toLowerCase())
                    .append("): ").append(sinal).append(valorTexto);
        }

        if (largura.signum() < 0) {
            largura = BigDecimal.ZERO;
        }
        if (altura.signum() < 0) {
            altura = BigDecimal.ZERO;
        }

        return new AjusteDetalhado(largura, altura, detalhe.toString());
    }

    public record AjusteDetalhado(BigDecimal larguraFinalMm, BigDecimal alturaFinalMm, String resumo) {
    }
}
