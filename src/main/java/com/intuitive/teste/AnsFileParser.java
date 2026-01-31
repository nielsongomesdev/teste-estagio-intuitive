package com.intuitive.teste;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class AnsFileParser {

    private static final String DOWNLOAD_DIR = "downloads/";

    private static final Map<String, Integer> OPERADORA_CACHE = new HashMap<>();

    public static void main(String[] args) {
        try {
            carregarCacheOperadoras();

            parseAndSaveBatch("2T2025.zip", "2T2025");

        } finally {
            DatabaseConnection.shutdown();
        }
    }

    private static void carregarCacheOperadoras() {
        System.out.println("[CACHE] Carregando operadoras na memoria...");
        String sql = "SELECT id, registro_ans FROM operadoras";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String reg = rs.getString("registro_ans");
                if (reg != null) {
                    OPERADORA_CACHE.put(String.valueOf(Long.parseLong(reg)), id);
                    OPERADORA_CACHE.put(reg.trim(), id);
                }
            }
            System.out.println("[CACHE] " + OPERADORA_CACHE.size() + " operadoras mapeadas.");

        } catch (SQLException e) {
            System.err.println("[ERRO CACHE] " + e.getMessage());
        }
    }

    public static void parseAndSaveBatch(String fileName, String trimestre) {
        Path path = Paths.get(DOWNLOAD_DIR + fileName);
        if (!Files.exists(path)) {
            System.out.println("[AVISO] Arquivo nao encontrado: " + fileName);
            return;
        }

        System.out.println("\n>>> Iniciando Processamento OTIMIZADO (Batch) de: " + fileName);

        String sql = "INSERT INTO detalhe_despesas (operadora_id, trimestre, data_evento, valor) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
                ZipFile zipFile = new ZipFile(path.toFile());
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            conn.setAutoCommit(false);

            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.getName().toLowerCase().endsWith(".csv")) {
                    processarCSV(zipFile, entry, stmt, trimestre);
                    conn.commit();
                }
            }

        } catch (Exception e) {
            System.err.println("[ERRO GERAL] " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void processarCSV(ZipFile zip, ZipEntry entry, PreparedStatement stmt, String trimestre)
            throws IOException, SQLException {
        try (InputStream is = zip.getInputStream(entry);
                InputStreamReader isr = new InputStreamReader(is, StandardCharsets.ISO_8859_1);
                BufferedReader reader = new BufferedReader(isr)) {

            CSVParser parser = CSVFormat.DEFAULT.builder()
                    .setDelimiter(';').setHeader().setSkipHeaderRecord(true).setIgnoreHeaderCase(true).setTrim(true)
                    .build().parse(reader);

            int salvos = 0;
            int ignorados = 0;
            int countDebug = 0;

            System.out.println("[INFO] Lendo CSV: " + entry.getName());

            for (CSVRecord record : parser) {
                if (countDebug < 5) {
                    System.out.println("   [DEBUG LINHA] Descricao: " + record.get("DESCRICAO") + " | Reg: "
                            + record.get("REG_ANS"));
                    countDebug++;
                }

                String descricao = record.get("DESCRICAO").toUpperCase();
                if (!descricao.contains("EVENTOS") && !descricao.contains("DESPESA")) {
                    continue;
                }

                try {
                    String regAnsRaw = record.get("REG_ANS");
                    String regAnsKey = String.valueOf(Long.parseLong(regAnsRaw));

                    Integer operadoraId = OPERADORA_CACHE.get(regAnsKey);

                    if (operadoraId == null)
                        operadoraId = OPERADORA_CACHE.get(regAnsRaw);

                    if (operadoraId != null) {
                        String valorStr = record.get("VL_SALDO_FINAL").replace(".", "").replace(",", ".");
                        BigDecimal valor = new BigDecimal(valorStr);

                        stmt.setInt(1, operadoraId);
                        stmt.setString(2, trimestre);
                        stmt.setDate(3, java.sql.Date.valueOf(record.get("DATA")));
                        stmt.setBigDecimal(4, valor);

                        stmt.addBatch();
                        salvos++;

                        if (salvos % 2000 == 0) {
                            stmt.executeBatch();
                            stmt.getConnection().commit();
                            System.out.print(".");
                        }
                    } else {
                        ignorados++;
                    }

                } catch (Exception e) {
                }
            }
            stmt.executeBatch();
            System.out
                    .println("\n[FIM] " + salvos + " despesas salvas. (" + ignorados + " operadoras nao encontradas).");
        }
    }
}
