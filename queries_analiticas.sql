-- QUERY 1: Quais as 5 operadoras com maior crescimento percentual de despesas
-- entre o primeiro e o último trimestre analisado?
-- Justificativa: Utilizamos um CTE para calcular o total por trimestre primeiro,
-- e depois comparamos explicitamente o menor e o maior trimestre encontrado no banco.
-- Operadoras sem dados em ambos os periodos sao excluidas pelo JOIN.

WITH despesas_por_trimestre AS (
    SELECT 
        d.operadora_id,
        d.trimestre,
        SUM(d.valor) as total_despesa
    FROM detalhe_despesas d
    GROUP BY d.operadora_id, d.trimestre
),
limites AS (
    SELECT 
        MIN(trimestre) as tri_inicial,
        MAX(trimestre) as tri_final
    FROM detalhe_despesas
)
SELECT 
    o.registro_ans,
    o.razao_social,
    d_ini.total_despesa as valor_inicial,
    d_fim.total_despesa as valor_final,
    ROUND(((d_fim.total_despesa - d_ini.total_despesa) / d_ini.total_despesa) * 100, 2) as crescimento_pct
FROM operadoras o
JOIN limites l ON 1=1
JOIN despesas_por_trimestre d_ini ON o.id = d_ini.operadora_id AND d_ini.trimestre = l.tri_inicial
JOIN despesas_por_trimestre d_fim ON o.id = d_fim.operadora_id AND d_fim.trimestre = l.tri_final
WHERE d_ini.total_despesa > 0
ORDER BY crescimento_pct DESC
LIMIT 5;

-- -----------------------------------------------------------------------------

-- QUERY 2: Qual a distribuição de despesas por UF? Liste os 5 estados com maiores despesas totais.
-- Justificativa: Agrupamento simples pela coluna UF da tabela de operadoras.

SELECT 
    o.uf,
    SUM(d.valor) as total_despesas
FROM detalhe_despesas d
JOIN operadoras o ON d.operadora_id = o.id
WHERE o.uf IS NOT NULL
GROUP BY o.uf
ORDER BY total_despesas DESC
LIMIT 5;

-- -----------------------------------------------------------------------------

-- QUERY 3: Quantas operadoras tiveram despesas acima da média geral em pelo menos 2 dos 3 trimestres analisados?
-- Justificativa: Primeiro calculamos a media do mercado por trimestre.
-- Depois comparamos cada operadora com essa media.
-- Por fim, filtramos aquelas que superaram a media em 2 ou mais trimestres distintos.

WITH media_mercado_por_trimestre AS (
    SELECT 
        trimestre,
        AVG(total_operadora) as media_geral
    FROM (
        SELECT trimestre, operadora_id, SUM(valor) as total_operadora
        FROM detalhe_despesas
        GROUP BY trimestre, operadora_id
    ) sub
    GROUP BY trimestre
),
despesas_operadora AS (
    SELECT 
        d.operadora_id,
        d.trimestre,
        SUM(d.valor) as total_gasto
    FROM detalhe_despesas d
    GROUP BY d.operadora_id, d.trimestre
)
SELECT 
    COUNT(*) as qtd_operadoras_acima_media
FROM (
    SELECT 
        do.operadora_id
    FROM despesas_operadora do
    JOIN media_mercado_por_trimestre mm ON do.trimestre = mm.trimestre
    WHERE do.total_gasto > mm.media_geral
    GROUP BY do.operadora_id
    HAVING COUNT(*) >= 2
) operadoras_filtradas;