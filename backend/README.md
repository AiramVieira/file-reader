# File Reader

Sistema para **reconhecer documentos por layout e extrair dados deles** sem escrever regras de código para cada formato.

O usuário "ensina" o sistema uma única vez por tipo de documento: envia um arquivo de exemplo (o *arquivo-modelo*), clica nas palavras que identificam aquele documento e nas palavras que contêm os dados de interesse. A partir daí, qualquer novo arquivo enviado é comparado com os layouts cadastrados; o sistema descobre a qual tipo ele pertence e extrai automaticamente a **Competência** e o **CNPJ**.

O projeto é dividido em dois repositórios com este mesmo README:

| Parte | Stack | Função |
|---|---|---|
| **frontend** | Angular 22, Angular Material, Vitest | Telas de cadastro de serviços, mapeamento de layout e processamento de documentos |
| **backend** | Java 21, Spring Boot 4.1, Spring Data JPA, Flyway, PostgreSQL, PDFBox, Apache POI | API REST, leitura dos arquivos, identificação do layout e extração dos campos |

---

## Sumário

- [Conceitos](#conceitos)
- [Fluxo de uso](#fluxo-de-uso)
- [Como funciona por dentro](#como-funciona-por-dentro)
- [API REST](#api-rest)
- [Modelo de dados](#modelo-de-dados)
- [Como executar](#como-executar)
- [Configuração](#configuração)
- [Limitações conhecidas](#limitações-conhecidas)

---

## Conceitos

| Termo | Significado |
|---|---|
| **Serviço** | Um tipo de documento (ex.: "Guia IRPJ", "Boleto Banco X"). Cada serviço tem no máximo um arquivo-modelo e um conjunto de marcações. Pode ser ativado/desativado; só serviços ativos participam do reconhecimento. |
| **Arquivo-modelo** | Arquivo de exemplo de um serviço. É sobre ele que o usuário faz as marcações. |
| **Token** | Unidade de texto do documento. Todo arquivo, qualquer que seja o formato, é convertido em uma sequência ordenada de tokens, separados por espaço ou quebra de linha (na prática, uma "palavra"). |
| **Competência** / **CNPJ** | Os dois campos extraídos. Cada serviço pode ter **um** token marcado como Competência e **um** como CNPJ. |
| **Termo para baixa** | Palavra que **precisa** aparecer no documento para ele ser reconhecido como daquele serviço. Pode haver vários. |
| **Termo de negação** | Palavra que, se aparecer no documento, faz aquele serviço ser descartado (a busca continua nos demais serviços). Serve para diferenciar layouts parecidos. Pode haver vários. |
| **Termo de várias palavras** | Termo para baixa ou de negação criado selecionando palavras seguidas. As palavras são unidas por `_` (ex.: `Pagamento_de_boleto`) e precisam aparecer **juntas e em sequência** no documento. Já termos de uma palavra só podem estar em qualquer lugar. |
| **Âncoras** | Até 3 palavras de texto fixo antes e 3 depois da Competência/CNPJ no arquivo-modelo, a no máximo 10 posições de distância. Palavras com dígitos (datas, valores, códigos) são puladas porque mudam de um documento para outro. São salvas no momento da marcação e usadas para localizar o mesmo campo em documentos novos. |

## Fluxo de uso

### 1. Cadastrar um serviço (tela **Serviços**, `/servicos`)
Informe nome e descrição opcional. Na mesma tela é possível ativar/desativar ou excluir serviços.

### 2. Mapear o layout (tela **Mapear layout**, `/servicos/:id/layout`)
1. Envie o arquivo-modelo do serviço.
2. O conteúdo aparece como texto, com cada palavra clicável.
3. **Clique em uma palavra** e marque uma ou mais opções:
   - **Competência** e **CNPJ**: um de cada por serviço. Depois de marcados, essas opções somem do menu das outras palavras. Para trocar, desmarque na palavra atual.
   - **Termo para baixa** (vários)
   - **Termo de negação** (vários)
4. **Clique, segure e arraste** para selecionar várias palavras seguidas, em ordem de leitura. Arrastar de uma palavra para outra na linha de baixo seleciona o resto da primeira linha mais o início da segunda. Ao soltar, escolha **Termo para baixa** ou **Termo de negação**: as palavras viram um único termo unido por `_`.
5. Termos de negação que não estão no modelo podem ser digitados no card **Resumo do layout** (ex.: no serviço DAS, `Receitas Federais` rejeita os DARFs). Várias palavras digitadas também viram um termo unido por `_`.
6. O card **Resumo do layout** mostra o que está marcado e permite remover cada termo pelo X. Cada tipo de marcação tem uma cor própria no texto.

> **Atenção:** um termo de negação marcado no próprio arquivo-modelo faz o modelo, e os documentos iguais a ele, deixarem de ser reconhecidos por aquele serviço. O resumo mostra esses termos com um ícone de alerta.

Cada alteração é salva imediatamente no backend; não há botão "salvar".

**Onde marcar os campos:** qualquer ocorrência do valor funciona, desde que pelo menos um dos lados dela tenha texto fixo por perto (um rótulo como "CNPJ:", "Período de Apuração" etc.). Evite ocorrências cercadas só por números, como as que ficam no meio de um código de barras.

### 3. Processar um documento (tela **Processar documento**, `/processar`)
Envie um arquivo qualquer. O resultado pode ser:

| Status | Significado | O que a tela mostra |
|---|---|---|
| `MATCH` | Exatamente um serviço é compatível | Nome do serviço, Competência e CNPJ extraídos, com o grau de confiança de cada um |
| `NO_MATCH` | Nenhum serviço é compatível | Para cada serviço avaliado, quais termos de baixa faltaram e quais termos de negação foram encontrados |
| `AMBIGUO` | Mais de um serviço é compatível | Lista dos serviços empatados, para o usuário refinar os termos |

Todo processamento é registrado na tabela `execucao_processamento` (arquivo, status, valores extraídos, scores e diagnóstico em JSON).

## Como funciona por dentro

O processamento tem três etapas: **leitura do arquivo**, **identificação do serviço** e **extração dos campos**.

### Leitura e tokenização

O tipo do arquivo é detectado pela extensão; se o nome não tiver extensão conhecida, pelos primeiros bytes (`%PDF` → PDF, `PK..` → XLSX, caso contrário → texto).

| Formato | Como é lido |
|---|---|
| **PDF** | Texto extraído página a página com PDFBox (`PDFTextStripper`). Cada token guarda o número da página. |
| **TXT** | Texto puro em UTF-8, separado por espaços/quebras de linha. |
| **XML** | Tratado como texto puro (não é interpretado como árvore). |
| **CSV** | Texto puro, com `,` e `;` transformados em tokens separados. |
| **XLSX** | Apenas a **primeira planilha**, célula a célula, na ordem das linhas. Cada token guarda linha e coluna. |

O espaço que segue cada token (espaço, tab, quebra de linha) é preservado para reconstruir a visualização na tela de mapeamento.

### Identificação do serviço

Para cada serviço **ativo**, o backend verifica o documento inteiro:

- **todos** os termos para baixa devem estar presentes, em qualquer posição;
- **nenhum** termo de negação pode estar presente;
- termos de várias palavras (`Pagamento_de_boleto`) precisam aparecer com as palavras em sequência; termos de uma palavra podem estar em qualquer lugar;
- serviços sem nenhum termo para baixa nunca são elegíveis.

A comparação ignora maiúsculas/minúsculas, acentos e pontuação nas pontas de cada palavra (`Nacional,` = `Nacional`). Não há pontuação parcial: o serviço é elegível ou não.

### Extração de Competência e CNPJ

Só acontece quando o status é `MATCH`. Para cada campo marcado no serviço:

1. **Formato.** Só são candidatos os tokens com o mesmo formato do valor marcado no modelo. Sequências de letras viram `a`, sequências de dígitos viram `9`: `12.345.678/0001-90` → `9.9.9/9-9`, `Maio/2026` → `a/9`, `30/06/2026` → `9/9/9`. Um CNPJ marcado nunca é confundido com uma data ou um valor em reais.
2. **Âncoras.** Para cada candidato, o sistema confere se as âncoras aparecem nas mesmas posições relativas. Em cada lado, a âncora mais próxima pesa 1, a segunda ½ e a terceira ⅓.
3. **Score** = média entre (a) a proporção de peso encontrada nos dois lados juntos e (b) a proporção no melhor lado. Assim, um lado inteiro batendo já dá 0,75, mesmo que o outro lado seja um dado variável (ex.: a razão social logo depois do CNPJ). Todas as âncoras batendo dão 1,0.
4. O candidato com maior score é o valor extraído, **desde que o score seja ≥ 0,7** (configurável).
5. Se houver empate entre valores diferentes, nenhum valor é retornado. O mesmo valor repetido em dois lugares com o mesmo contexto não conta como empate.

Exemplo, com o DARF do Sicalc: no modelo, o texto extraído do PDF é `de Receitas Federais 11.222.333/0001-81 METALURGICA VALE DO ITAJAI LTDA`. As âncoras são `de Receitas Federais` antes e `METALURGICA VALE DO` depois. Em outro DARF, a razão social muda, mas `de Receitas Federais` continua igual, e o token com formato de CNPJ nessa posição é extraído com score 0,75.

## API REST

Base: `http://localhost:8080`. Erros de negócio retornam `{ "mensagem": "..." }` com status 400 ou 404.

### Serviços
| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/api/servicos` | Lista todos os serviços |
| `GET` | `/api/servicos/{id}` | Busca um serviço |
| `POST` | `/api/servicos` | Cria (`{ nome, descricao? }`) |
| `PUT` | `/api/servicos/{id}` | Atualiza (`{ nome, descricao?, ativo }`) |
| `DELETE` | `/api/servicos/{id}` | Exclui o serviço, seu arquivo-modelo e marcações |

### Layout
| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/api/servicos/{id}/layout` | Tokens do arquivo-modelo, marcações e resumo |
| `POST` | `/api/servicos/{id}/layout/upload` | Envia/troca o arquivo-modelo (`multipart/form-data`, campo `arquivo`). **Trocar apaga todas as marcações.** |
| `PUT` | `/api/servicos/{id}/layout/marcacoes` | Marca um token (`{ tokenIndice, tipo }`) ou um intervalo de tokens como um único termo (`{ tokenIndice, tokenIndiceFim, tipo }`) |
| `DELETE` | `/api/servicos/{id}/layout/marcacoes?tokenIndice=&tipo=` | Remove a marcação de um único token |
| `DELETE` | `/api/servicos/{id}/layout/marcacoes/{marcacaoId}` | Remove qualquer marcação pelo id (token, intervalo ou termo digitado) |
| `POST` | `/api/servicos/{id}/layout/termos-negacao` | Adiciona um termo de negação digitado (`{ texto }`); espaços viram `_` |
| `DELETE` | `/api/servicos/{id}/layout/termos-negacao?texto=` | Remove um termo de negação digitado |

`tipo` aceita `COMPETENCIA`, `CNPJ`, `TERMO_BAIXA` ou `TERMO_NEGACAO`. Intervalos só são aceitos para `TERMO_BAIXA` e `TERMO_NEGACAO`. A resposta do layout traz a lista `marcacoes`, com id, tipo, intervalo, texto e `presenteNoModelo` (termo de negação que aparece no próprio arquivo-modelo).

### Processamento
| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/api/documentos/processar` | Processa um documento (`multipart/form-data`, campo `arquivo`) e devolve status, serviço identificado, valores extraídos, scores e diagnóstico |

## Modelo de dados

O schema é criado pelo Flyway (`backend/src/main/resources/db/migration`).

```
servico ──1:1── documento_base ──1:N── token
   │
   ├──1:N── campo_layout ──1:N── ancora_token
   │
   └──1:N── execucao_processamento   (servico_id fica NULL se o serviço for excluído)
```

- Um índice único garante no banco que cada serviço tenha no máximo uma Competência e um CNPJ.
- Excluir um serviço apaga em cascata o arquivo-modelo, os tokens, as marcações e as âncoras.

## Como executar

### Pré-requisitos
- Java 21
- Node.js compatível com Angular 22 e npm 11
- PostgreSQL (local ou via Docker)

### Banco de dados
```bash
docker run --name filereader-db -e POSTGRES_DB=filereader -e POSTGRES_PASSWORD=postgres -p 5432:5432 -d postgres:16
```
As tabelas são criadas automaticamente pelo Flyway na primeira subida do backend.

### Backend
```bash
cd backend
./mvnw spring-boot:run        # Windows: mvnw.cmd spring-boot:run
```
A API sobe em `http://localhost:8080`.

Testes:
```bash
./mvnw test
```
O teste `BackendApplicationTests` sobe o contexto completo e exige o PostgreSQL disponível; os testes de parsers e de matching não dependem do banco.

### Frontend
```bash
cd frontend
npm install
npm start
```
Acesse `http://localhost:4200`. O dev server repassa as chamadas `/api` para `http://localhost:8080` (arquivo `proxy.conf.json`).

Outros comandos:
```bash
npm run build   # build de produção em dist/
npm test        # testes unitários com Vitest
```

## Publicar para a equipe (nginx + Cloudflare Tunnel)

A pasta `infra/`, na raiz do projeto (fora dos repositórios do frontend e do backend), publica o sistema em uma URL pública temporária:

```
equipe ──https──▶ Cloudflare ──túnel──▶ cloudflared ──▶ nginx :8088 ──┬─▶ build do frontend (arquivos estáticos)
                                                                      └─▶ /api ──▶ backend :8080
```

Pré-requisitos: backend rodando em `localhost:8080` e o `cloudflared` instalado. O nginx é baixado automaticamente, em versão portátil, para `infra/nginx/bin` na primeira execução.

```powershell
# Inicia: gera o build do frontend, sobe o nginx e abre o túnel. Mostra a URL no final.
powershell -ExecutionPolicy Bypass -File infra\iniciar-tunel.ps1

# Reaproveitando o build atual do frontend
powershell -ExecutionPolicy Bypass -File infra\iniciar-tunel.ps1 -PularBuild

# Encerra o túnel e o nginx (não mexe no backend)
powershell -ExecutionPolicy Bypass -File infra\parar-tunel.ps1
```

- O túnel é do tipo rápido (`*.trycloudflare.com`), sem conta na Cloudflare. **A URL muda a cada execução** e fica salva em `infra/.estado/url.txt`.
- **Não há proteção de acesso.** Qualquer pessoa com o link vê os serviços e documentos e pode criar ou excluir serviços. Encerre o túnel quando a equipe terminar os testes.
- Depois de alterar o frontend, rode o script de novo sem `-PularBuild` para a equipe receber a versão nova. Alterações no backend exigem reiniciar o backend.
- Frontend e API ficam na mesma origem atrás do nginx. Por isso o nginx remove o cabeçalho `Origin` antes de repassar para o backend; sem isso, o CORS do Spring (que só libera `localhost:4200`) recusaria POST, PUT e DELETE com 403.
- Logs: `infra/nginx/logs/` (nginx) e `infra/.estado/cloudflared.log` (túnel).

## Configuração

Parâmetros em `backend/src/main/resources/application.yml`:

| Variável de ambiente / propriedade | Padrão | Descrição |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/filereader` | URL do banco |
| `SPRING_DATASOURCE_USERNAME` | `postgres` | Usuário do banco |
| `SPRING_DATASOURCE_PASSWORD` | `postgres` | Senha do banco |
| `SERVER_PORT` | `8080` | Porta da API |
| `FILEREADER_UPLOAD_DIR` | `./uploads` | Pasta onde os arquivos enviados são gravados |
| `filereader.anchor.tokens-before` | `3` | Quantidade de âncoras antes do campo |
| `filereader.anchor.tokens-after` | `3` | Quantidade de âncoras depois do campo |
| `filereader.anchor.max-distance` | `10` | Distância máxima, em tokens, para procurar âncoras de texto fixo |
| `filereader.anchor.match-threshold` | `0.7` | Score mínimo para aceitar um valor extraído |
| `spring.servlet.multipart.max-file-size` | `25MB` | Tamanho máximo do arquivo |

O CORS do backend libera apenas `http://localhost:4200`.

---

## Limitações conhecidas

### Formatos de arquivo
- **PDFs escaneados (imagem) não funcionam**: não há OCR. Se o PDF não tiver camada de texto, o resultado é uma lista vazia de tokens.
- **A ordem do texto em PDFs depende do PDFBox.** Em layouts com colunas, tabelas ou caixas posicionadas, a ordem extraída pode não seguir a ordem visual e variar entre documentos parecidos, o que quebra as âncoras.
- **`.xls` (Excel antigo) não é suportado**, embora a extensão seja aceita: ela é encaminhada ao leitor de XLSX e a leitura falha.
- **XLSX: só a primeira planilha é lida.** Células com fórmula retornam o texto da fórmula (ex.: `SUM(A1:A3)`), não o valor calculado.
- **TXT, CSV e XML são lidos sempre como UTF-8.** Arquivos em ISO-8859-1/Windows-1252 (comuns em sistemas legados brasileiros) terão acentos corrompidos, e termos acentuados podem deixar de ser encontrados.
- **XML não é interpretado.** Em `<cnpj>12345678000190</cnpj>` a tag e o valor formam um único token, então não dá para marcar só o valor.
- **CSV:** toda vírgula vira separador, inclusive dentro de aspas e em números decimais (`1,50` vira `1`, `,`, `50`).
- **DOCX, ODT, imagens e outros formatos não são suportados.** Arquivos sem extensão conhecida e que não são PDF nem ZIP são lidos como texto.
- Tamanho máximo de **25 MB** por arquivo.

### Identificação do serviço
- **A posição do termo no documento não é considerada.** Termos de várias palavras exigem só que as palavras estejam em sequência, em qualquer lugar do documento.
- **Termo de negação marcado no modelo rejeita o próprio modelo** e os documentos iguais a ele. O sistema avisa, mas não impede.
- **Palavras muito comuns como termo** ("de", "total") fazem quase todo documento passar no filtro e aumentam os casos `AMBIGUO`.
- **Não há ranking entre serviços.** Se dois serviços passam no filtro, o resultado é `AMBIGUO`, mesmo que um seja claramente mais parecido. A saída é usar termos de negação.
- Serviço sem termos para baixa nunca é reconhecido.

### Extração de Competência e CNPJ
- **Só extrai um token por campo.** Valores com espaço, como `Junho de 2026` ou `12.345.678 / 0001-90`, são retornados pela metade.
- **Só existem dois campos fixos** (Competência e CNPJ). Não é possível cadastrar outros campos (valor, vencimento, razão social etc.) sem alterar o código.
- **Precisa de texto fixo em pelo menos um dos lados**, na mesma posição relativa. Se os dois lados do campo mudarem entre documentos, ou se uma palavra sem dígitos for inserida entre o rótulo e o valor, a extração falha.
- **O formato precisa ser idêntico ao do modelo.** Se o modelo tem `05/2026` e outro documento traz `5/2026`, o formato bate (`9/9`). Mas se um documento trouxer o CNPJ sem pontuação e o modelo com pontuação, ele não é encontrado. O dígito verificador do CNPJ não é conferido.
- **Empate na extração não é informado à tela.** O backend detecta a ambiguidade, mas a resposta só traz o valor vazio; a tela mostra "não foi possível extrair", igual ao caso em que nada foi encontrado.
- Campos marcados muito perto do início ou do fim do arquivo têm menos âncoras e, portanto, uma extração menos confiável.

### Mapeamento de layout
- **Trocar o arquivo-modelo apaga todas as marcações**, inclusive os termos de negação, sem pedir confirmação.
- **Um único arquivo-modelo por serviço.** Não dá para treinar com vários exemplos para tolerar variações de layout.
- **A visualização é só texto.** PDFs e planilhas aparecem como sequência de palavras, sem a aparência original do documento.
- Arquivos grandes geram um botão por palavra na tela, o que pode deixar a página lenta.

### Interface
- **Não é possível editar nome e descrição** de um serviço depois de criado (a API permite, a tela não).
- **Mensagens de erro são genéricas** ("Falha ao processar o documento."); a mensagem detalhada que o backend devolve não é exibida.
- **Não existe tela de histórico.** As execuções são gravadas no banco, mas não há endpoint nem tela para consultá-las.
- Um documento por vez; não há envio em lote.
- O seletor de arquivo não filtra extensões.

### Operação e segurança
- **Sem autenticação nem controle de acesso.** Qualquer pessoa com acesso à API pode criar, alterar ou excluir serviços.
- **Os arquivos enviados ficam no disco para sempre.** Cada upload (arquivo-modelo ou documento processado) é gravado em `uploads/` com um prefixo UUID, inclusive quando o processamento falha ou o arquivo-modelo é substituído. Não há limpeza automática nem armazenamento externo (S3 etc.).
- **Processamento síncrono**, dentro da própria requisição HTTP.
- Não há configuração de produção. O acesso externo existente é o túnel rápido de `infra/`, que depende desta máquina ligada, com backend, nginx e cloudflared rodando.
- Log em nível `DEBUG` para o pacote da aplicação por padrão.
