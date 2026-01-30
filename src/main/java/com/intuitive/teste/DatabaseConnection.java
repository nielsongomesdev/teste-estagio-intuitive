package com.intuitive.teste;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DatabaseConnection {

    private static final String URL = "jdbc:mysql://localhost:3306/intuitive_teste?allowPublicKeyRetrieval=true&useSSL=false";
    private static final String USER = "root";
    private static final String PASSWORD = "root";

    public static void saveExpense(String trimestre, BigDecimal valorTotal) {
        String sql = "INSERT INTO despesas_eventos (trimestre, valor_total) VALUES (?, ?)";

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, trimestre);
                stmt.setBigDecimal(2, valorTotal);

                stmt.executeUpdate();
                System.out.println("      [BANCO] Sucesso! Dados salvos.");

            }
        } catch (ClassNotFoundException e) {
            System.err.println("      [ERRO FATAL] Driver MySQL nao encontrado.");
            e.printStackTrace();
        } catch (SQLException e) {
            System.err.println("      [BANCO] Erro ao salvar: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void shutdown() {
        try {
            com.mysql.cj.jdbc.AbandonedConnectionCleanupThread.checkedShutdown();
        } catch (Exception e) {
        }
    }
}
