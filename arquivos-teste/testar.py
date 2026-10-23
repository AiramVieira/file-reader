"""
Roda o cenario de teste completo contra a API do File Reader:

  1. cria os servicos "TESTE DARF" e "TESTE DAS" (apagando versoes anteriores com o mesmo nome);
  2. envia A como arquivo-modelo do DARF e C como arquivo-modelo do DAS;
  3. marca Competencia, CNPJ e termos de baixa clicando nos tokens pelo texto, e cadastra os termos de negacao digitados;
  4. processa A, B, C e D e compara com o gabarito.

Uso:
  python testar.py                                # API em http://localhost:8080, marcacao "natural"
  python testar.py --url http://localhost:8090
  python testar.py --marcacao alternativa         # marca CNPJ/Competencia em outra ocorrencia do texto
  python testar.py --termos compartilhados        # termos de baixa presentes nos dois tipos + termos de negacao

Atencao: outros servicos ATIVOS no banco tambem participam do reconhecimento e podem causar AMBIGUO.
"""

import argparse
import json
import sys
import urllib.error
import urllib.parse
import urllib.request
import uuid
from pathlib import Path

PASTA = Path(__file__).resolve().parent

SERVICOS = {
    "TESTE DARF": {
        "modelo": "A_DARF_modelo.pdf",
        "termos": {
            # palavras que so existem no DARF
            "distintivos": {"baixa": ["Federais", "Sicalc"], "negacao": []},
            # palavras que tambem aparecem no DAS: so o termo de negacao (digitado) separa os dois tipos
            "compartilhados": {"baixa": ["Darf", "Arrecadação", "IRPJ"], "negacao": ["PGDAS-D"]},
        },
        # (texto do token, ocorrencia) -- ocorrencia 1 = primeira vez que o texto aparece no arquivo
        "natural": {"COMPETENCIA": ("30/06/2026", 1), "CNPJ": ("11.222.333/0001-81", 1)},
        "alternativa": {"COMPETENCIA": ("30/06/2026", 1), "CNPJ": ("11.222.333/0001-81", 2)},
    },
    "TESTE DAS": {
        "modelo": "C_DAS_modelo.pdf",
        "termos": {
            "distintivos": {"baixa": ["PGDAS-D", "Simples", "Nacional"], "negacao": []},
            "compartilhados": {"baixa": ["Simples", "Nacional", "Arrecadação"], "negacao": ["Receitas Federais"]},
        },
        "natural": {"COMPETENCIA": ("Maio/2026", 1), "CNPJ": ("23.456.789/0001-95", 1)},
        "alternativa": {"COMPETENCIA": ("05/2026", 1), "CNPJ": ("23.456.789/0001-95", 2)},
    },
}

GABARITO = {
    "A_DARF_modelo.pdf": {"servico": "TESTE DARF", "cnpj": "11.222.333/0001-81",
                          "competencia": {"natural": "30/06/2026", "alternativa": "30/06/2026"}},
    "B_DARF_teste.pdf": {"servico": "TESTE DARF", "cnpj": "45.987.123/0001-04",
                         "competencia": {"natural": "31/03/2026", "alternativa": "31/03/2026"}},
    "C_DAS_modelo.pdf": {"servico": "TESTE DAS", "cnpj": "23.456.789/0001-95",
                         "competencia": {"natural": "Maio/2026", "alternativa": "05/2026"}},
    "D_DAS_teste.pdf": {"servico": "TESTE DAS", "cnpj": "38.111.222/0001-54",
                        "competencia": {"natural": "Abril/2026", "alternativa": "04/2026"}},
}


class Api:
    def __init__(self, base):
        self.base = base.rstrip("/")

    def _req(self, metodo, caminho, corpo=None, headers=None):
        req = urllib.request.Request(self.base + caminho, data=corpo, method=metodo, headers=headers or {})
        try:
            with urllib.request.urlopen(req) as resp:
                dados = resp.read()
                return json.loads(dados) if dados else None
        except urllib.error.HTTPError as e:
            raise RuntimeError(f"{metodo} {caminho} -> {e.code}: {e.read().decode('utf-8', 'replace')}") from None

    def json(self, metodo, caminho, payload=None):
        corpo = json.dumps(payload).encode("utf-8") if payload is not None else None
        return self._req(metodo, caminho, corpo, {"Content-Type": "application/json"})

    def upload(self, caminho, arquivo):
        fronteira = uuid.uuid4().hex
        corpo = (
            f"--{fronteira}\r\n"
            f'Content-Disposition: form-data; name="arquivo"; filename="{arquivo.name}"\r\n'
            f"Content-Type: application/octet-stream\r\n\r\n"
        ).encode("utf-8") + arquivo.read_bytes() + f"\r\n--{fronteira}--\r\n".encode("utf-8")
        return self._req("POST", caminho, corpo, {"Content-Type": f"multipart/form-data; boundary={fronteira}"})


def indice_do_token(tokens, texto, ocorrencia=1):
    encontrados = [t["indice"] for t in tokens if t["texto"] == texto]
    if len(encontrados) < ocorrencia:
        raise RuntimeError(f"Token '{texto}' (ocorrencia {ocorrencia}) nao encontrado no arquivo-modelo; "
                           f"so e possivel marcar palavras que existem no proprio modelo")
    return encontrados[ocorrencia - 1]


def preparar_servicos(api, marcacao, termos):
    existentes = api.json("GET", "/api/servicos")
    for s in existentes:
        if s["nome"] in SERVICOS:
            api.json("DELETE", f"/api/servicos/{s['id']}")
    outros_ativos = [s["nome"] for s in existentes if s["ativo"] and s["nome"] not in SERVICOS]
    if outros_ativos:
        print(f"AVISO: outros servicos ativos participam do teste: {', '.join(outros_ativos)}\n")

    for nome, cfg in SERVICOS.items():
        servico = api.json("POST", "/api/servicos", {"nome": nome, "descricao": "Criado por testar.py"})
        sid = servico["id"]
        layout = api.upload(f"/api/servicos/{sid}/layout/upload", PASTA / cfg["modelo"])
        tokens = layout["tokens"]

        marcacoes = [(tipo, texto, oc) for tipo, (texto, oc) in cfg[marcacao].items()]
        marcacoes += [("TERMO_BAIXA", t, 1) for t in cfg["termos"][termos]["baixa"]]
        for tipo, texto, oc in marcacoes:
            api.json("PUT", f"/api/servicos/{sid}/layout/marcacoes",
                     {"tokenIndice": indice_do_token(tokens, texto, oc), "tipo": tipo})
        # termos de negacao sao digitados, nao marcados no modelo
        for texto in cfg["termos"][termos]["negacao"]:
            api.json("POST", f"/api/servicos/{sid}/layout/termos-negacao", {"texto": texto})

        resumo = api.json("GET", f"/api/servicos/{sid}/layout")["resumo"]
        print(f"[{nome}] modelo={cfg['modelo']} ({len(tokens)} tokens)")
        print(f"    competencia={resumo['competencia']}  cnpj={resumo['cnpj']}")
        print(f"    termos de baixa={resumo['termosBaixa']}  termos de negacao={resumo['termosNegacao']}")
    print()


def processar_todos(api, marcacao):
    falhas = 0
    linhas = []
    for arquivo, esperado in GABARITO.items():
        r = api.upload("/api/documentos/processar", PASTA / arquivo)
        servico = (r.get("servicoIdentificado") or {}).get("nome")
        comp_esperada = esperado["competencia"][marcacao]
        checks = {
            "servico": r["status"] == "MATCH" and servico == esperado["servico"],
            "competencia": r.get("competenciaExtraida") == comp_esperada,
            "cnpj": r.get("cnpjExtraido") == esperado["cnpj"],
        }
        falhas += sum(1 for ok in checks.values() if not ok)
        linhas.append((arquivo, r, servico, checks, esperado, comp_esperada))

    for arquivo, r, servico, checks, esperado, comp_esperada in linhas:
        def marca(ok):
            return "OK  " if ok else "FALHA"
        print(f"{arquivo}")
        print(f"    {marca(checks['servico'])} status={r['status']} servico={servico} (esperado {esperado['servico']})")
        print(f"    {marca(checks['competencia'])} competencia={r.get('competenciaExtraida')} "
              f"score={r.get('scoreCompetencia')} (esperado {comp_esperada})")
        print(f"    {marca(checks['cnpj'])} cnpj={r.get('cnpjExtraido')} "
              f"score={r.get('scoreCnpj')} (esperado {esperado['cnpj']})")
        if r["status"] != "MATCH":
            for d in r["diagnostico"]:
                print(f"        - {d['nomeServico']}: elegivel={d['elegivel']} "
                      f"faltando={d['termosBaixaFaltantes']} negacoes={d['termosNegacaoEncontrados']}")
    total = len(GABARITO) * 3
    print(f"\nResultado: {total - falhas}/{total} verificacoes OK")
    return falhas


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--url", default="http://localhost:8080")
    parser.add_argument("--marcacao", choices=["natural", "alternativa"], default="natural")
    parser.add_argument("--termos", choices=["distintivos", "compartilhados"], default="distintivos")
    args = parser.parse_args()

    api = Api(args.url)
    print(f"API: {args.url}  |  marcacao: {args.marcacao}  |  termos: {args.termos}\n")
    preparar_servicos(api, args.marcacao, args.termos)
    falhas = processar_todos(api, args.marcacao)
    sys.exit(1 if falhas else 0)


if __name__ == "__main__":
    main()
