package com.intuitive.teste.service;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class AnsZipDiagnostic {

    public static void main(String[] args) {
        System.out.println("=== DIAGNOSTICO DO ARQUIVO ZIP ===");
        
        try (ZipFile zipFile = new ZipFile("downloads/2T2025.zip")) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                
                // Pega o primeiro CSV que encontrar
                if (entry.getName().toLowerCase().endsWith(".csv")) {
                    System.out.println("Arquivo encontrado dentro do ZIP: " + entry.getName());
                    printHeaders(zipFile, entry);
                    return; // Para depois do primeiro arquivo
                }
            }
            System.out.println("Nenhum CSV encontrado dentro do ZIP.");

        } catch (Exception e) {
            System.err.println("Erro ao ler ZIP: " + e.getMessage());
        }
    }

    private static void printHeaders(ZipFile zip, ZipEntry entry) {
        try (InputStream is = zip.getInputStream(entry);
             InputStreamReader isr = new InputStreamReader(is, StandardCharsets.ISO_8859_1);
             BufferedReader reader = new BufferedReader(isr)) {

            CSVParser parser = CSVFormat.DEFAULT.builder()
                    .setDelimiter(';')
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .build()
                    .parse(reader);

            System.out.println("\nCOLUNAS ENCONTRADAS:");
            System.out.println("------------------------------------------------");
            parser.getHeaderNames().forEach(System.out::println);
            System.out.println("------------------------------------------------");

        } catch (Exception e) {
            System.err.println("Erro ao ler CSV: " + e.getMessage());
        }
    }
}
