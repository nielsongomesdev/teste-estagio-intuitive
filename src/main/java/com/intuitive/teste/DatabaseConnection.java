package com.intuitive.teste;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DatabaseConnection {

    private static final String URL = "jdbc:mysql://localhost:3306/intuitive_teste?allowPublicKeyRetrieval=true&useSSL=false&characterEncoding=UTF-8";
    private static final String USER = "root";
    private static final String PASSWORD = "root";

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public static void saveOperadora(String registro, String cnpj, String razao, String fantasia,
            String modalidade, String logradouro, String numero, String complemento,
            String bairro, String cidade, String uf, String cep, String telefone, String email) {

        String sql = "INSERT IGNORE INTO operadoras (registro_ans, cnpj, razao_social, nome_fantasia, modalidade, " +
                "logradouro, numero, complemento, bairro, cidade, uf, cep, telefone, email) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, registro);
            stmt.setString(2, cnpj);
            stmt.setString(3, razao);
            stmt.setString(4, fantasia);
            stmt.setString(5, modalidade);
            stmt.setString(6, logradouro);
            stmt.setString(7, numero);
            stmt.setString(8, complemento);
            stmt.setString(9, bairro);
            stmt.setString(10, cidade);
            stmt.setString(11, uf);
            stmt.setString(12, cep);
            stmt.setString(13, telefone);
            stmt.setString(14, email);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("      [ERRO OPERADORA] " + razao + ": " + e.getMessage());
        }
    }

    public static Integer getOperadoraIdByCnpj(String cnpj) {
        String sql = "SELECT id FROM operadoras WHERE cnpj = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, cnpj);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next())
                    return rs.getInt("id");
            }
        } catch (SQLException e) {
            System.err.println("      [ERRO BUSCA] CNPJ " + cnpj + ": " + e.getMessage());
        }
        return null;
    }

    public static void saveDetalheDespesa(Integer operadoraId, String trimestre, String dataEvento, BigDecimal valor) {
        String sql = "INSERT INTO detalhe_despesas (operadora_id, trimestre, data_evento, valor) VALUES (?, ?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, operadoraId);
            stmt.setString(2, trimestre);

            if (dataEvento != null)
                stmt.setDate(3, java.sql.Date.valueOf(dataEvento));
            else
                stmt.setNull(3, java.sql.Types.DATE);

            stmt.setBigDecimal(4, valor);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("      [ERRO DESPESA] ID " + operadoraId + ": " + e.getMessage());
        }
    }

    public static void saveExpense(String trimestre, BigDecimal valorTotal) {
        String sql = "INSERT INTO despesas_eventos (trimestre, valor_total) VALUES (?, ?)";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, trimestre);
            stmt.setBigDecimal(2, valorTotal);
            stmt.executeUpdate();
            System.out.println("      [BANCO] Sucesso! Total salvo.");
        } catch (SQLException e) {
            System.err.println("      [BANCO] Erro: " + e.getMessage());
        }
    }

    public static void shutdown() {
        try {
            com.mysql.cj.jdbc.AbandonedConnectionCleanupThread.checkedShutdown();
        } catch (Exception e) {
        }
    }
}
