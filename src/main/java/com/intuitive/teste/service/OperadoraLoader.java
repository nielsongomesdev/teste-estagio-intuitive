package com.intuitive.teste.service;

import com.intuitive.teste.config.DatabaseConnection;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;

public class OperadoraLoader {

    private static final String CSV_PATH = "downloads/Relatorio_cadop.csv";

    public static void main(String[] args) {
        loadOperadorasBatch();
        DatabaseConnection.shutdown();
    }

    public static void loadOperadorasBatch() {
        System.out.println("=== Iniciando Importacao OTIMIZADA (Batch) ===");

        if (!Files.exists(Paths.get(CSV_PATH))) {
            System.err.println("[ERRO] Arquivo nao encontrado: " + CSV_PATH);
            return;
        }

        String sql = "INSERT INTO operadoras (registro_ans, cnpj, razao_social, nome_fantasia, modalidade, " +
                "logradouro, numero, complemento, bairro, cidade, uf, cep, telefone, email) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE " +
                "razao_social = VALUES(razao_social), " +
                "nome_fantasia = VALUES(nome_fantasia), " +
                "modalidade = VALUES(modalidade), " +
                "logradouro = VALUES(logradouro), " +
                "numero = VALUES(numero), " +
                "complemento = VALUES(complemento), " +
                "bairro = VALUES(bairro), " +
                "cidade = VALUES(cidade), " +
                "uf = VALUES(uf), " +
                "cep = VALUES(cep), " +
                "telefone = VALUES(telefone), " +
                "email = VALUES(email)";

        try (Connection conn = DatabaseConnection.getConnection();
                Reader reader = Files.newBufferedReader(Paths.get(CSV_PATH), StandardCharsets.UTF_8);
                CSVParser parser = CSVFormat.DEFAULT.builder()
                        .setDelimiter(';').setHeader().setSkipHeaderRecord(true).setIgnoreHeaderCase(true).setTrim(true)
                        .build().parse(reader);
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            conn.setAutoCommit(false);

            int count = 0;
            System.out.println("[INFO] Lendo e agrupando dados...");

            for (CSVRecord record : parser) {
                try {
                    stmt.setString(1, record.get("REGISTRO_OPERADORA"));
                    stmt.setString(2, record.get("CNPJ"));
                    stmt.setString(3, record.get("Razao_Social"));
                    stmt.setString(4, record.get("Nome_Fantasia"));
                    stmt.setString(5, record.get("Modalidade"));
                    stmt.setString(6, record.get("Logradouro"));
                    stmt.setString(7, record.get("Numero"));
                    stmt.setString(8, record.get("Complemento"));
                    stmt.setString(9, record.get("Bairro"));
                    stmt.setString(10, record.get("Cidade"));
                    stmt.setString(11, record.get("UF"));
                    stmt.setString(12, record.get("CEP"));

                    String ddd = record.get("DDD");
                    String tel = record.get("Telefone");
                    stmt.setString(13, (ddd != null ? "(" + ddd + ") " : "") + (tel != null ? tel : ""));

                    stmt.setString(14, record.get("Endereco_eletronico"));

                    stmt.addBatch();
                    count++;

                    if (count % 500 == 0) {
                        stmt.executeBatch();
                        conn.commit();
                        System.out.print(".");
                    }

                } catch (Exception e) {
                    
                }
            }

            stmt.executeBatch();
            conn.commit();

            System.out.println("\n[SUCESSO] " + count + " operadoras importadas via Batch!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
