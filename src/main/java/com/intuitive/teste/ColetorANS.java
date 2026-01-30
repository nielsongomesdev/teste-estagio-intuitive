package com.intuitive.teste;

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
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ColetorANS {

    private static final String BASE_URL = "https://dadosabertos.ans.gov.br/FTP/PDA/demonstracoes_contabeis/";
    private static final String DIR_DOWNLOADS = "downloads";

    public static void main(String[] args) {
        executarColeta();
    }

    public static void executarColeta() {
        System.out.println("=== Iniciando Coleta de Dados da ANS (Modo Automático) ===");

        try {
            Files.createDirectories(Paths.get(DIR_DOWNLOADS));
        } catch (IOException e) {
            System.err.println("Erro crítico ao criar pasta: " + e.getMessage());
            return;
        }

        HttpClient client = HttpClient.newHttpClient();

        List<String> periodos = calcularUltimosTrimestres(3);

        for (String periodo : periodos) {
            String[] partes = periodo.split("/");
            String ano = partes[0];
            String trimestre = partes[1];

            String urlDiretorio = BASE_URL + ano + "/";

            String padraoBusca = trimestre + "T" + ano;

            System.out.println("\n------------------------------------------------");
            System.out.println("Verificando: Trimestre " + trimestre + " de " + ano);
            System.out.println("Buscando termo '" + padraoBusca + "' em: " + urlDiretorio);

            try {
                String htmlConteudo = acessarUrl(client, urlDiretorio);

                String nomeArquivoZip = encontrarLinkZip(htmlConteudo, padraoBusca);

                if (nomeArquivoZip != null) {
                    System.out.println("Arquivo encontrado: " + nomeArquivoZip);
                    Path caminhoZip = baixarArquivo(client, urlDiretorio + nomeArquivoZip, nomeArquivoZip);
                    extrairZip(caminhoZip);
                } else {
                    System.out.println(
                            "AVISO: Nenhum arquivo encontrado para " + padraoBusca + " (Ainda não publicado?)");
                }

            } catch (Exception e) {
                System.err.println(
                        "Não foi possível acessar a pasta do ano " + ano + " (Provavelmente ainda não existe).");
            }
        }
        System.out.println("\n=== Processo Finalizado ===");
    }

    private static List<String> calcularUltimosTrimestres(int quantidade) {
        List<String> lista = new ArrayList<>();
        LocalDate data = LocalDate.now();

        int ano = data.getYear();
        int mes = data.getMonthValue();

        int trimestreAtual = (mes - 1) / 3 + 1;

        trimestreAtual--;

        if (trimestreAtual == 0) {
            trimestreAtual = 4;
            ano--;
        }

        for (int i = 0; i < quantidade; i++) {
            lista.add(ano + "/" + trimestreAtual);

            trimestreAtual--;
            if (trimestreAtual == 0) {
                trimestreAtual = 4;
                ano--;
            }
        }
        return lista;
    }

    private static String acessarUrl(HttpClient client, String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200)
            throw new IOException("Status Code: " + response.statusCode());
        return response.body();
    }

    private static String encontrarLinkZip(String html, String termo) {
        String regex = "(?i)href=\"([^\"]*" + termo + "[^\"]*\\.zip)\"";

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(html);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static Path baixarArquivo(HttpClient client, String url, String nomeArquivo)
            throws IOException, InterruptedException {
        System.out.println("Baixando: " + nomeArquivo + "...");
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
        Path destino = Paths.get(DIR_DOWNLOADS, nomeArquivo);

        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        Files.copy(response.body(), destino, StandardCopyOption.REPLACE_EXISTING);
        System.out.println("✔ Salvo em: " + destino.toAbsolutePath());
        return destino;
    }

    private static void extrairZip(Path caminhoZip) {
        System.out.println("Extraindo arquivo ZIP: " + caminhoZip.getFileName());
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(caminhoZip))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path novoArquivo = Paths.get(DIR_DOWNLOADS, entry.getName());

                if (!novoArquivo.normalize().startsWith(Paths.get(DIR_DOWNLOADS).toAbsolutePath().normalize())) {
                    continue;
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(novoArquivo);
                } else {
                    Files.createDirectories(novoArquivo.getParent());
                    Files.copy(zis, novoArquivo, StandardCopyOption.REPLACE_EXISTING);
                    System.out.println(" -> Extraído: " + entry.getName());
                }
                zis.closeEntry();
            }
        } catch (IOException e) {
            System.err.println("Erro ao extrair ZIP: " + e.getMessage());
        }
    }
}
