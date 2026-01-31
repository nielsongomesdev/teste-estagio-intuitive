package com.intuitive.teste;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

public class AnsFileParser {

    private static final String DOWNLOADS_DIR = "downloads";
    private static final Map<String, Integer> operadoraCache = new HashMap<>();

    public static void main(String[] args) {
        long inicio = System.currentTimeMillis();
        System.out.println("Iniciando processamento de despesas...");

        try {
            carregarCacheOperadoras();

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

    private static void carregarCacheOperadoras() throws SQLException {
        System.out.println("Carregando cache de operadoras...");
        try (Connection conn = DatabaseConnection.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT id, registro_ans FROM operadoras")) {

            while (rs.next()) {
                operadoraCache.put(rs.getString("registro_ans"), rs.getInt("id"));
            }
        }
        System.out.println("Cache carregado com " + operadoraCache.size() + " operadoras.");
    }

    private static void parseAndSaveBatch(String arquivoZip, String trimestre) {
        Path path = Paths.get(DOWNLOADS_DIR, arquivoZip);
        if (!Files.exists(path)) {
            System.err.println("Arquivo nao encontrado: " + path.toAbsolutePath());
            return;
        }

        System.out.println("Abrindo arquivo: " + arquivoZip);
        String sql = "INSERT INTO detalhe_despesas (operadora_id, trimestre, data_evento, valor) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ZipFile zip = new ZipFile(path.toFile())) {

            conn.setAutoCommit(false);

            zip.stream().forEach(entry -> {
                if (!entry.isDirectory() && entry.getName().toUpperCase().endsWith(".CSV")) {
                    System.out.println("   Processando CSV interno: " + entry.getName());
                    try {
                        processarCSV(zip, entry, stmt, trimestre);
                        conn.commit();
                    } catch (Exception e) {
                        System.err.println("Erro ao processar entry: " + entry.getName());
                        e.printStackTrace();
                        try {
                            conn.rollback();
                        } catch (SQLException ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            });

        } catch (IOException | SQLException e) {
            e.printStackTrace();
        }
    }

    private static void processarCSV(ZipFile zip, ZipEntry entry, PreparedStatement stmt, String trimestre)
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

            int count = 0;
            int batchSize = 1000;

            for (CSVRecord record : parser) {
                String regAns = record.get("REG_ANS");

                Integer operadoraId = operadoraCache.get(regAns);

                if (operadoraId != null) {
                    String descricao = record.get("CD_CONTA_CONTABIL");

                    if (descricao.startsWith("4")) {
                        double valor = Double.parseDouble(record.get("VL_SALDO_FINAL").replace(",", "."));

                        stmt.setInt(1, operadoraId);
                        stmt.setString(2, trimestre);
                        stmt.setDate(3, java.sql.Date.valueOf("2025-01-01"));
                        stmt.setDouble(4, valor);

                        stmt.addBatch();
                        count++;

                        if (count % batchSize == 0) {
                            stmt.executeBatch();
                            stmt.clearBatch();
                            if (count % 10000 == 0) {
                                System.out.println("      ... processados " + count + " registros.");
                            }
                        }
                    }
                }
            }

            stmt.executeBatch();
            System.out.println("   Inseridos " + count + " registros para " + trimestre);
        }
    }
}
