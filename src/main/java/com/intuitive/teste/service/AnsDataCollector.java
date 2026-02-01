package com.intuitive.teste.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AnsDataCollector {

    private static final String BASE_URL = "https://dadosabertos.ans.gov.br/FTP/PDA/demonstracoes_contabeis/";
    private static final String DOWNLOAD_DIR = "downloads";

    public static void main(String[] args) {
        runCollection();
    }

    public static void runCollection() {
        System.out.println("=== Iniciando Coleta de Dados da ANS (Modo Automático) ===");

        try {
            Files.createDirectories(Paths.get(DOWNLOAD_DIR));
        } catch (IOException e) {
            System.err.println("Erro crítico ao criar diretório: " + e.getMessage());
            return;
        }

        HttpClient client = HttpClient.newHttpClient();

        List<String> periods = calculateLastQuarters(3);

        for (String period : periods) {
            String[] parts = period.split("/");
            String year = parts[0];
            String quarter = parts[1];

            String directoryUrl = BASE_URL + year + "/";

            String searchPattern = quarter + "T" + year;

            System.out.println("\n------------------------------------------------");
            System.out.println("Verificando: Trimestre " + quarter + " de " + year);
            System.out.println("Buscando termo '" + searchPattern + "' em: " + directoryUrl);

            try {
                String htmlContent = fetchHtml(client, directoryUrl);

                String zipFilename = findZipLink(htmlContent, searchPattern);

                if (zipFilename != null) {
                    System.out.println("Arquivo encontrado: " + zipFilename);
                    downloadFile(client, directoryUrl + zipFilename, zipFilename);
                } else {
                    System.out.println(
                            "AVISO: Nenhum arquivo encontrado para " + searchPattern + " (Ainda não publicado?)");
                }

            } catch (Exception e) {
                System.err.println("Não foi possível acessar a pasta do ano " + year + " (Talvez não exista ainda).");
            }
        }
        System.out.println("\n=== Processo Finalizado ===");
    }

    private static List<String> calculateLastQuarters(int amount) {
        List<String> list = new ArrayList<>();
        LocalDate date = LocalDate.now();

        int year = date.getYear();
        int month = date.getMonthValue();

        int currentQuarter = (month - 1) / 3 + 1;

        currentQuarter--;

        if (currentQuarter == 0) {
            currentQuarter = 4;
            year--;
        }

        for (int i = 0; i < amount; i++) {
            list.add(year + "/" + currentQuarter);

            currentQuarter--;
            if (currentQuarter == 0) {
                currentQuarter = 4;
                year--;
            }
        }
        return list;
    }

    private static String fetchHtml(HttpClient client, String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200)
            throw new IOException("Status Code: " + response.statusCode());
        return response.body();
    }

    private static String findZipLink(String html, String term) {
        String regex = "(?i)href=\"([^\"]*" + term + "[^\"]*\\.zip)\"";

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(html);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static void downloadFile(HttpClient client, String url, String filename)
            throws IOException, InterruptedException {
        System.out.println("Baixando: " + filename + "...");
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
        Path destination = Paths.get(DOWNLOAD_DIR, filename);

        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        Files.copy(response.body(), destination, StandardCopyOption.REPLACE_EXISTING);
        System.out.println("✔ Salvo em: " + destination.toAbsolutePath());
    }
}
