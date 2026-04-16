package com.example.product_api.dto.FinancialDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductRankingDTO {
    private Long id;                 // ID do produto
    private String nome;             // nome do produto
    private String categoria;        // categoria
    private Long preco;              // preço de venda (centavos)
    private Long precoCusto;         // preço de custo (centavos)
    private Double margem;           // margem de lucro (%)
    private Integer vezesVendido;    // quantidade de vendas (para tipo=top)
    private Long receitaTotal;       // receita total (para tipo=top, centavos)
    private Integer estoque;         // quantidade em estoque (para tipos parados/baixa-margem)
}
