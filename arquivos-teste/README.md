# Arquivos de teste

Quatro PDFs para validar o fluxo principal do File Reader: mapear um documento, reconhecer documentos parecidos e extrair Competência e CNPJ.

| Arquivo | Papel | Tipo | Empresa | CNPJ | Competência |
|---|---|---|---|---|---|
| `A_DARF_modelo.pdf` | modelo do serviço DARF | DARF IRPJ (Sicalc), 2 páginas | METALURGICA VALE DO ITAJAI LTDA | 11.222.333/0001-81 | 30/06/2026 |
| `B_DARF_teste.pdf` | teste do serviço DARF | DARF IRPJ pago em atraso (com multa e juros) | ALFA COMERCIO DE PRODUTOS ALIMENTICIOS E BEBIDAS EIRELI | 45.987.123/0001-04 | 31/03/2026 |
| `C_DAS_modelo.pdf` | modelo do serviço DAS | DAS Simples Nacional, comércio (ICMS), 2 páginas | PADARIA PAO DOURADO LTDA | 23.456.789/0001-95 | Maio/2026 (pág. 1) · 05/2026 (pág. 2) |
| `D_DAS_teste.pdf` | teste do serviço DAS | DAS Simples Nacional, serviços (ISS), em atraso | TECH SOLUCOES EM SOFTWARE E CONSULTORIA LTDA | 38.111.222/0001-54 | Abril/2026 (pág. 1) · 04/2026 (pág. 2) |

Todos os CNPJs são fictícios, mas têm dígitos verificadores válidos.

## O que torna os arquivos difíceis

- **Ordem do texto no PDF diferente da ordem visual (A/B).** O DARF reproduz a ordem de extração de um DARF real do Sicalc: os valores aparecem antes dos rótulos, "CNPJ Razão Social" sai no meio da página e algumas palavras saem coladas (`PrincipalDenominação`).
- **Valores com tamanhos diferentes entre modelo e teste.** Os nomes de empresa têm de 4 a 8 palavras. B e D têm colunas de multa e juros que A e C não têm. D tem 7 linhas na tabela de tributos e C tem 6. Tudo isso desloca a posição absoluta dos tokens.
- **Mesmo valor em mais de um lugar.** O CNPJ aparece duas vezes em cada arquivo, a data de vencimento até quatro.
- **Vocabulário compartilhado entre DARF e DAS.** Os dois têm "Documento de Arrecadação", "IRPJ", "Receita Federal", "Período de Apuração", "AUTENTICAÇÃO MECÂNICA" e "Pague com o PIX". O DARF menciona "Simples Nacional" e "DAS" nas instruções; o DAS menciona "DARF" e "Sicalc" no extrato.
- **Dois estilos de layout no DAS.** A página 1 é um formulário com o rótulo acima do valor. A página 2 é um extrato no formato `Rótulo: valor`.

## Como mapear pela tela

1. Crie os serviços **DARF** e **DAS** e desative os outros serviços, porque todo serviço ativo participa do reconhecimento.
2. No DARF, envie `A_DARF_modelo.pdf` e marque:
   - Competência: `30/06/2026` (logo depois de "CNPJ Razão Social")
   - CNPJ: `11.222.333/0001-81` (a primeira ocorrência, no topo)
   - Termos para baixa: `Federais`, `Sicalc`
3. No DAS, envie `C_DAS_modelo.pdf` e marque:
   - Competência: `Maio/2026` (página 1) **ou** `05/2026` (página 2)
   - CNPJ: primeira ocorrência (página 1) **ou** segunda ocorrência (página 2, depois de "CNPJ Estabelecimento:")
   - Termos para baixa: `PGDAS-D`, `Simples`, `Nacional`
4. Processe A, B, C e D na tela **Processar documento** e compare com a tabela acima.

Para testar os termos de negação, use no lugar dos termos acima termos de baixa que existem nos dois tipos de documento:
- DARF: termos de baixa `Darf`, `Arrecadação`, `IRPJ`; termo de negação digitado `PGDAS-D`
- DAS: termos de baixa `Simples`, `Nacional`, `Arrecadação`; termo de negação digitado `Receitas Federais`

Sem os termos de negação, os quatro arquivos dão `AMBIGUO`.

## Script automático

O `testar.py` faz todos os passos acima pela API e compara o resultado com o gabarito. Ele precisa só de Python 3, sem bibliotecas extras.

```bash
python testar.py                               # API em localhost:8080, marcação "natural" (página 1)
python testar.py --marcacao alternativa        # DAS marcado na página 2, CNPJ do DARF na 2ª ocorrência
python testar.py --termos compartilhados       # termos de baixa que existem nos dois tipos + termos de negação
python testar.py --url http://localhost:8090   # outra instância do backend
```

O script apaga e recria os serviços `TESTE DARF` e `TESTE DAS` a cada execução, e avisa se houver outros serviços ativos. O código de saída é 0 só quando todas as 12 verificações passam (status, Competência e CNPJ para cada arquivo).

Para regerar ou alterar os PDFs, use `gerador/GerarArquivos.java`. Ele usa o PDFBox, a mesma biblioteca que o backend usa para ler os arquivos:

```bash
java -cp "<pdfbox-3.0.7.jar>;<pdfbox-io-3.0.7.jar>;<fontbox-3.0.7.jar>;<commons-logging.jar>" gerador/GerarArquivos.java .
```

## Resultado atual (15/10/2026)

Execução contra o backend real, em um banco PostgreSQL vazio, depois dos ajustes de extração e de termos de negação:

| Cenário | Reconhecimento | Competência | CNPJ | Total |
|---|---|---|---|---|
| natural · distintivos | 4/4 | 4/4 | 4/4 | **12/12** |
| alternativa · distintivos | 4/4 | 4/4 | 4/4 | **12/12** |
| natural · compartilhados (com negação) | 4/4 | 4/4 | 4/4 | **12/12** |
| alternativa · compartilhados (com negação) | 4/4 | 4/4 | 4/4 | **12/12** |

Verificações extras:
- **DARF real do Sicalc** (não gerado por aqui), processado com o mapeamento feito no A: reconhecido como DARF, com Competência `30/06/2026` e CNPJ `04.289.480/0001-03` corretos, seja o CNPJ marcado no topo (score 0,75) ou no recibo (score 1,0).
- Sem os termos de negação, o cenário "compartilhados" dá `AMBIGUO`, como esperado.
- Digitar um termo de negação que existe no modelo (`Simples Nacional` no DAS) e marcar negação clicando em uma palavra retornam 400 com mensagem explicativa.

### Antes dos ajustes

| Cenário | Total |
|---|---|
| natural · distintivos | 9/12 |
| alternativa · distintivos | 11/12 |
| natural · compartilhados | 5/12 |

Os problemas encontrados foram:
1. **Âncoras vizinhas a dados variáveis.** As âncoras eram sempre as 3 palavras imediatamente antes e depois do campo. Quando um dos lados era só de dados variáveis (razão social, datas, código de barras), o score máximo ficava em 0,5 e o valor não era extraído. O CNPJ do DARF não era extraído em nenhuma posição.
2. **Termos de negação inutilizáveis.** Eles só podiam ser marcados clicando em palavras do próprio modelo, o que fazia o modelo ser rejeitado pelo seu serviço.
3. **Pontuação contava no termo**: `Nacional,` era diferente de `Nacional`.

### O que mudou

- **Âncoras:** palavras com dígitos são puladas ao escolher as âncoras, que passam a ser o texto fixo mais próximo, até 10 posições de distância.
- **Score:** um lado inteiro batendo já é suficiente (0,75); todos batendo dão 1,0.
- **Formato:** só são candidatos tokens com o mesmo formato do valor marcado (ex.: `9.9.9/9-9` para CNPJ).
- **Negação:** os termos agora são digitados, podem ser expressões e são recusados se existirem no modelo.
- **Pontuação:** a pontuação nas pontas das palavras é ignorada na comparação de termos.
