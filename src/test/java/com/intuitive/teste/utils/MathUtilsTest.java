package com.intuitive.teste.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MathUtilsTest {

    @Test
    public void deveConverterValorComPontoEVirgula() {
        // Cenário: "1.000,50" -> 1000.50
        Double resultado = MathUtils.converterValorMonetario("1.000,50");
        Assertions.assertEquals(1000.50, resultado);
    }

    @Test
    public void deveConverterValorSimples() {
        // Cenário: "500,00" -> 500.00
        Double resultado = MathUtils.converterValorMonetario("500,00");
        Assertions.assertEquals(500.00, resultado);
    }

    @Test
    public void deveRetornarZeroSeNulo() {
        Double resultado = MathUtils.converterValorMonetario(null);
        Assertions.assertEquals(0.0, resultado);
    }

    @Test
    public void deveRetornarZeroSeVazio() {
        Double resultado = MathUtils.converterValorMonetario("");
        Assertions.assertEquals(0.0, resultado);
    }
}
