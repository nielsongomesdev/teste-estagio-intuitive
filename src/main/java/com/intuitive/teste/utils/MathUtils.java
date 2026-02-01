package com.intuitive.teste.utils;

import java.math.BigDecimal;

public class MathUtils {

    /**
     * Converte string monetária brasileira (ex: "1.000,50") para Double (1000.50).
     * Retorna 0.0 se o valor for inválido.
     */
    public static Double converterValorMonetario(String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            return 0.0;
        }
        try {
            // Remove pontos de milhar e troca vírgula por ponto
            String normalized = valor.replace(".", "").replace(",", ".");
            return new BigDecimal(normalized).doubleValue();
        } catch (NumberFormatException e) {
            System.err.println("[AVISO] Erro ao converter valor: " + valor);
            return 0.0;
        }
    }
}
