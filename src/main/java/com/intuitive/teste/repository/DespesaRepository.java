package com.intuitive.teste.repository;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DespesaRepository implements AutoCloseable {

    private final Connection connection;
    private final PreparedStatement stmt;
    private int count = 0;
    private static final int BATCH_SIZE = 1000;

    public DespesaRepository(Connection conn) throws SQLException {
        this.connection = conn;
        String sql = "INSERT INTO detalhe_despesas (operadora_id, trimestre, data_evento, valor) VALUES (?, ?, ?, ?)";
        this.stmt = this.connection.prepareStatement(sql);
    }

    public void adicionarDespesa(int operadoraId, String trimestre, Date dataEvento, double valor) throws SQLException {
        stmt.setInt(1, operadoraId);
        stmt.setString(2, trimestre);
        stmt.setDate(3, dataEvento);
        stmt.setDouble(4, valor);
        stmt.addBatch();
        
        count++;
        if (count % BATCH_SIZE == 0) {
            flush();
            if (count % 10000 == 0) {
                System.out.println("      ... processados " + count + " registros.");
            }
        }
    }

    public void flush() throws SQLException {
        stmt.executeBatch();
        stmt.clearBatch();
    }
    
    public int getTotalProcessado() {
        return count;
    }

    @Override
    public void close() throws SQLException {
        if (stmt != null) stmt.close();
    }
}
