package com.intuitive.teste.service; // CHANGED

import com.intuitive.teste.config.DatabaseConnection; // ADDED
import com.intuitive.teste.repository.DespesaRepository;
import com.intuitive.teste.repository.OperadoraRepository;
import com.intuitive.teste.utils.MathUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class AnsFileParser {

    private static final String DOWNLOADS_DIR = "downloads";
    private static Map<String, Integer> operadoraCache = new HashMap<>();
    private static final OperadoraRepository operadoraRepository = new OperadoraRepository();

    public static void main(String[] args) {
        long inicio = System.currentTimeMillis();
        System.out.println("Iniciando processamento de despesas...");

        try {
            // 1. Carrega Cache usando Repository
            System.out.println("Carregando cache de operadoras...");
            operadoraCache = operadoraRepository.carregarCache();
            System.out.println("Cache carregado com " + operadoraCache.size() + " operadoras.");

            // 2. Processa arquivos
            parseAndSaveBatch("2T2025.zip", "2T2025");
            parseAndSaveBatch("3T2025.zip", "3T2025");

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            DatabaseConnection.shutdown();
            long fim = System.currentTimeMillis();
            System.out.println("Processo finalizado em " + (fim - inicio) / 1000 + " segundos.");
        }
    }

    private static void parseAndSaveBatch(String arquivoZip, String trimestre) {
        Path path = Paths.get(DOWNLOADS_DIR, arquivoZip);
        if (!Files.exists(path)) {
            System.err.println("Arquivo nao encontrado: " + path.toAbsolutePath());
            return;
        }

        System.out.println("Abrindo arquivo: " + arquivoZip);

        try (Connection conn = DatabaseConnection.getConnection();
             ZipFile zip = new ZipFile(path.toFile())) {

            conn.setAutoCommit(false);

            // Abre o Repository de Despesas (Gerencia o PreparedStatement)
            try (DespesaRepository despesaRepo = new DespesaRepository(conn)) {
                
                zip.stream().forEach(entry -> {
                    if (!entry.isDirectory() && entry.getName().toUpperCase().endsWith(".CSV")) {
                        System.out.println("   Processando CSV interno: " + entry.getName());
                        try {
                            processarCSV(zip, entry, despesaRepo, trimestre);
                            conn.commit(); 
                        } catch (Exception e) {
                            System.err.println("Erro ao processar entry: " + entry.getName());
                            e.printStackTrace();
                            try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
                        }
                    }
                });
            }

        } catch (IOException | SQLException e) {
            e.printStackTrace();
        }
    }

    private static void processarCSV(ZipFile zip, ZipEntry entry, DespesaRepository despesaRepo, String trimestre)
            throws IOException, SQLException {
        
        try (InputStream is = zip.getInputStream(entry);
             InputStreamReader isr = new InputStreamReader(is, StandardCharsets.ISO_8859_1);
             BufferedReader reader = new BufferedReader(isr)) {

            CSVParser parser = CSVFormat.DEFAULT
                    .builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setDelimiter(';')
                    .setIgnoreHeaderCase(true)
                    .setTrim(true)
                    .build()
                    .parse(reader);

            for (CSVRecord record : parser) {
                String regAns = record.get("REG_ANS");
                Integer operadoraId = operadoraCache.get(regAns);

                if (operadoraId != null) {
                    String descricao = record.get("CD_CONTA_CONTABIL");

                    if (descricao.startsWith("4")) {
                        double valor = MathUtils.converterValorMonetario(record.get("VL_SALDO_FINAL"));
                        
                        // Delega ao Repository a inserção e controle de batch
                        despesaRepo.adicionarDespesa(operadoraId, trimestre, java.sql.Date.valueOf("2025-01-01"), valor);
                    }
                }
            }
            // Garante que o último lote seja enviado
            despesaRepo.flush();
            System.out.println("   Inseridos " + despesaRepo.getTotalProcessado() + " registros para " + trimestre);
        }
    }
}
