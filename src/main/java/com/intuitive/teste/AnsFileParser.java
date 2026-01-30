package com.intuitive.teste;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class AnsFileParser {

    private static final String DOWNLOAD_DIR = "downloads";

    public static void main(String[] args) {
        try {
            processFiles();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void processFiles() throws IOException {
        System.out.println("=== Iniciando Processamento Contábil ===");

        try (Stream<Path> paths = Files.list(Paths.get(DOWNLOAD_DIR))) {
            paths
                    .filter(p -> p.toString().endsWith(".zip"))
                    .forEach(path -> {
                        System.out.println("\n📂 Arquivo: " + path.getFileName());
                        readZip(path);
                    });
        }
        System.out.println("\n=== Processamento Finalizado ===");
    }

    private static void readZip(Path zipPath) {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipPath))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().toLowerCase().endsWith(".csv")) {
                    parseCsv(zis);
                }
            }
        } catch (IOException e) {
            System.err.println("Erro ao ler zip: " + e.getMessage());
        }
    }

    private static void parseCsv(ZipInputStream zis) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(zis, StandardCharsets.UTF_8));

        CSVParser parser = CSVFormat.DEFAULT
                .builder()
                .setDelimiter(';')
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreHeaderCase(true)
                .setTrim(true)
                .build()
                .parse(reader);

        System.out.println("   Calculando Total de 'EVENTOS/SINISTROS'...");

        BigDecimal totalDespesas = BigDecimal.ZERO;
        boolean encontrou = false;

        String ALVO = "EVENTOS/ SINISTROS CONHECIDOS OU AVISADOS DE ASSISTÊNCIA A SAÚDE MEDICO HOSPITALAR";

        for (CSVRecord record : parser) {
            if (record.size() < 6)
                continue;

            String descricao = record.get(3).trim().replaceAll("\\s+", " ");

            if (descricao.equalsIgnoreCase(ALVO)) {

                String valorString = record.get(5)
                        .replace(".", "")
                        .replace(",", ".");

                try {
                    BigDecimal valor = new BigDecimal(valorString);
                    totalDespesas = totalDespesas.add(valor);
                    encontrou = true;

                    String valorFormatado = NumberFormat.getCurrencyInstance(new Locale("pt", "BR")).format(valor);
                    System.out.println("   [+] Somando: " + valorFormatado);

                } catch (NumberFormatException e) {
                    System.err.println("   [ERRO] Valor inválido encontrado: " + valorString);
                }
            }
        }

        if (encontrou) {
            String totalFormatado = NumberFormat.getCurrencyInstance(new Locale("pt", "BR")).format(totalDespesas);
            System.out.println("   ------------------------------------------");
            System.out.println("TOTAL DO TRIMESTRE: " + totalFormatado);
            System.out.println("   ------------------------------------------");
        } else {
            System.out.println("   (Conta específica não encontrada. Verifique se o nome mudou.)");
        }
    }
}