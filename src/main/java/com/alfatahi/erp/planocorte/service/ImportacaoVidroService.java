package com.alfatahi.erp.planocorte.service;

import com.alfatahi.erp.planocorte.dto.ResultadoImportacao;
import com.alfatahi.erp.planocorte.entity.OrigemHistorico;
import com.alfatahi.erp.planocorte.entity.TipoVidro;
import com.alfatahi.erp.planocorte.entity.Vidro;
import com.alfatahi.erp.planocorte.repository.VidroRepository;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;



@Service
public class ImportacaoVidroService {

    private static final List<String> COLUNAS_OBRIGATORIAS = List.of(
            "nome", "tipo", "espessura", "valor_m2");

    private final VidroRepository vidroRepository;
    private final VidroService vidroService;

    public ImportacaoVidroService(VidroRepository vidroRepository, VidroService vidroService) {
        this.vidroRepository = vidroRepository;
        this.vidroService = vidroService;
    }

    @Transactional
    public ResultadoImportacao importarCsv(InputStream inputStream) throws IOException, CsvException {
        List<String[]> linhas;
        try (CSVReader reader = new CSVReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            linhas = reader.readAll();
        }
        return processarLinhas(linhas);
    }

    @Transactional
    public ResultadoImportacao importarXlsx(InputStream inputStream) throws IOException {
        List<String[]> linhas = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            for (Row row : sheet) {
                int ultimaColuna = row.getLastCellNum();
                if (ultimaColuna < 0) {
                    continue;
                }
                String[] valores = new String[ultimaColuna];
                for (int c = 0; c < ultimaColuna; c++) {
                    Cell cell = row.getCell(c);
                    valores[c] = cell != null ? formatter.formatCellValue(cell).trim() : "";
                }
                linhas.add(valores);
            }
        }
        return processarLinhas(linhas);
    }

    public String modeloCsv() {
        return "nome,fabricante,tipo,espessura,cor,acabamento,valor_m2,valor_minimo,peso_m2\n"
                + "Vidro Incolor 8mm,Cebrace,COMUM,8,Incolor,Polido,180.00,60.00,20.0\n";
    }

    private ResultadoImportacao processarLinhas(List<String[]> linhas) {
        ResultadoImportacao resultado = new ResultadoImportacao();
        if (linhas.isEmpty()) {
            resultado.getErros().add("Planilha vazia.");
            return resultado;
        }

        Map<String, Integer> indice = indexarCabecalho(linhas.get(0));
        for (String coluna : COLUNAS_OBRIGATORIAS) {
            if (!indice.containsKey(coluna)) {
                resultado.getErros().add("Coluna obrigatória ausente: " + coluna);
            }
        }
        if (!resultado.getErros().isEmpty()) {
            return resultado;
        }

        for (int i = 1; i < linhas.size(); i++) {
            String[] valores = linhas.get(i);
            int numeroLinha = i + 1;
            if (linhaVazia(valores)) {
                continue;
            }
            try {
                processarLinha(valores, indice, resultado);
            } catch (Exception e) {
                resultado.getErros().add("Linha " + numeroLinha + ": " + e.getMessage());
            }
        }
        return resultado;
    }

    private void processarLinha(String[] valores, Map<String, Integer> indice, ResultadoImportacao resultado) {
        String nome = valorColuna(valores, indice, "nome");
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("nome em branco");
        }

        String tipoTexto = valorColuna(valores, indice, "tipo");
        TipoVidro tipo;
        try {
            tipo = TipoVidro.valueOf(tipoTexto.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            throw new IllegalArgumentException("tipo inválido '" + tipoTexto + "'");
        }

        BigDecimal espessura = parseDecimal(valorColuna(valores, indice, "espessura"));
        BigDecimal valorM2 = parseDecimal(valorColuna(valores, indice, "valor_m2"));
        if (espessura == null || valorM2 == null) {
            throw new IllegalArgumentException("espessura ou valor_m2 inválido");
        }
        BigDecimal valorMinimo = parseDecimal(valorColuna(valores, indice, "valor_minimo"));
        BigDecimal pesoM2 = parseDecimal(valorColuna(valores, indice, "peso_m2"));

        Optional<Vidro> existenteOpt = vidroRepository.findFirstByNomeIgnoreCase(nome.trim());
        Vidro vidro = existenteOpt.orElseGet(Vidro::new);
        boolean novo = vidro.getId() == null;
        BigDecimal valorAntigo = vidro.getValorPorM2();

        vidro.setNome(nome.trim());
        vidro.setFabricante(valorColuna(valores, indice, "fabricante"));
        vidro.setTipo(tipo);
        vidro.setEspessura(espessura);
        vidro.setCor(valorColuna(valores, indice, "cor"));
        vidro.setAcabamento(valorColuna(valores, indice, "acabamento"));
        vidro.setValorPorM2(valorM2);
        vidro.setValorMinimo(valorMinimo);
        vidro.setPesoPorM2(pesoM2);
        vidro.setAtivo(true);

        Vidro salvo = vidroRepository.save(vidro);

        if (novo || valorAntigo == null || valorAntigo.compareTo(valorM2) != 0) {
            vidroService.registrarHistorico(salvo, valorAntigo, valorM2, OrigemHistorico.IMPORTACAO);
        }

        if (novo) {
            resultado.incrementarCriados();
        } else {
            resultado.incrementarAtualizados();
        }
    }

    private Map<String, Integer> indexarCabecalho(String[] cabecalho) {
        Map<String, Integer> indice = new HashMap<>();
        for (int i = 0; i < cabecalho.length; i++) {
            indice.put(normalizarColuna(cabecalho[i]), i);
        }
        return indice;
    }

    private String normalizarColuna(String texto) {
        return texto == null ? "" : texto.trim().toLowerCase(Locale.ROOT);
    }

    private String valorColuna(String[] valores, Map<String, Integer> indice, String coluna) {
        Integer pos = indice.get(coluna);
        if (pos == null || pos >= valores.length) {
            return null;
        }
        String valor = valores[pos];
        return valor != null ? valor.trim() : null;
    }

    private BigDecimal parseDecimal(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        String limpo = texto.trim();



        if (limpo.contains(",")) {
            limpo = limpo.replace(".", "").replace(",", ".");
        }
        try {
            return new BigDecimal(limpo);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("número inválido '" + texto + "'");
        }
    }

    private boolean linhaVazia(String[] valores) {
        if (valores == null || valores.length == 0) {
            return true;
        }
        for (String v : valores) {
            if (v != null && !v.isBlank()) {
                return false;
            }
        }
        return true;
    }
}
