export type TipoCampo = 'COMPETENCIA' | 'CNPJ' | 'TERMO_BAIXA' | 'TERMO_NEGACAO';

export type StatusProcessamento = 'MATCH' | 'NO_MATCH' | 'AMBIGUO';

export interface Servico {
  id: number;
  nome: string;
  descricao: string | null;
  ativo: boolean;
}

export interface CriarServicoRequest {
  nome: string;
  descricao?: string;
}

export interface AtualizarServicoRequest {
  nome: string;
  descricao?: string;
  ativo: boolean;
}

export interface TokenDTO {
  indice: number;
  texto: string;
  espacoApos: string;
  pagina: number | null;
  linha: number | null;
  coluna: number | null;
  tiposMarcados: TipoCampo[];
}

export interface ResumoLayoutDTO {
  competencia: string | null;
  cnpj: string | null;
  termosBaixa: string[];
  termosNegacao: string[];
}

export interface MarcacaoDTO {
  id: number;
  tipo: TipoCampo;
  /** Nulo para termos de negacao digitados. */
  tokenIndice: number | null;
  /** Preenchido apenas em termos marcados como intervalo (varias palavras unidas por "_"). */
  tokenIndiceFim: number | null;
  texto: string;
  /** Termo de negacao que aparece no proprio arquivo-modelo. */
  presenteNoModelo: boolean;
}

export interface LayoutDTO {
  servico: Servico;
  temArquivoBase: boolean;
  nomeArquivoBase: string | null;
  tokens: TokenDTO[];
  resumo: ResumoLayoutDTO;
  marcacoes: MarcacaoDTO[];
}

export interface DiagnosticoServicoDTO {
  servicoId: number;
  nomeServico: string;
  termosBaixaFaltantes: string[];
  termosNegacaoEncontrados: string[];
  elegivel: boolean;
}

export interface ResultadoProcessamentoDTO {
  status: StatusProcessamento;
  servicoIdentificado: Servico | null;
  competenciaExtraida: string | null;
  scoreCompetencia: number | null;
  cnpjExtraido: string | null;
  scoreCnpj: number | null;
  diagnostico: DiagnosticoServicoDTO[];
}
