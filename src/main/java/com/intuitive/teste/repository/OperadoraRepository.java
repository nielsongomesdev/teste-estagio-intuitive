package com.intuitive.teste.repository;

import com.intuitive.teste.config.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

public class OperadoraRepository {

    /**
     * Carrega todas as operadoras do banco para um Map em memória.
     * Chave: Registro ANS (String) -> Valor: ID do Banco (Integer)
     */
    public Map<String, Integer> carregarCache() throws SQLException {
        Map<String, Integer> cache = new HashMap<>();
        String sql = "SELECT id, registro_ans FROM operadoras";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                cache.put(rs.getString("registro_ans"), rs.getInt("id"));
            }
        }
        return cache;
    }

    /**
     * Retorna o PreparedStatement para inserção de operadoras.
     * Útil para operações em Batch externas.
     */
    public PreparedStatement criarStatementInsercao(Connection conn) throws SQLException {
        String sql = "INSERT INTO operadoras (registro_ans, cnpj, razao_social, nome_fantasia, modalidade, " +
                     "logradouro, numero, complemento, bairro, cidade, uf, cep, telefone, email) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE " +
                     "razao_social = VALUES(razao_social), nome_fantasia = VALUES(nome_fantasia)";
        return conn.prepareStatement(sql);
    }
}
