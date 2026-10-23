import { HttpClient } from '@angular/common/http';
import { inject, Service } from '@angular/core';
import { Observable } from 'rxjs';
import { LayoutDTO, TipoCampo } from '../models/api.models';

const baseUrl = (servicoId: number) => `/api/servicos/${servicoId}/layout`;

@Service()
export class Layout {
  private readonly http = inject(HttpClient);

  obter(servicoId: number): Observable<LayoutDTO> {
    return this.http.get<LayoutDTO>(baseUrl(servicoId));
  }

  uploadArquivoBase(servicoId: number, arquivo: File): Observable<LayoutDTO> {
    const formData = new FormData();
    formData.append('arquivo', arquivo);
    return this.http.post<LayoutDTO>(`${baseUrl(servicoId)}/upload`, formData);
  }

  /** Com tokenIndiceFim, marca o intervalo como um unico termo (palavras unidas por "_"). */
  marcarCampo(servicoId: number, tokenIndice: number, tipo: TipoCampo, tokenIndiceFim?: number): Observable<LayoutDTO> {
    return this.http.put<LayoutDTO>(`${baseUrl(servicoId)}/marcacoes`, { tokenIndice, tokenIndiceFim, tipo });
  }

  removerMarcacao(servicoId: number, marcacaoId: number): Observable<LayoutDTO> {
    return this.http.delete<LayoutDTO>(`${baseUrl(servicoId)}/marcacoes/${marcacaoId}`);
  }

  desmarcarCampo(servicoId: number, tokenIndice: number, tipo: TipoCampo): Observable<LayoutDTO> {
    return this.http.delete<LayoutDTO>(`${baseUrl(servicoId)}/marcacoes`, {
      params: { tokenIndice, tipo },
    });
  }

  adicionarTermoNegacao(servicoId: number, texto: string): Observable<LayoutDTO> {
    return this.http.post<LayoutDTO>(`${baseUrl(servicoId)}/termos-negacao`, { texto });
  }

  removerTermoNegacao(servicoId: number, texto: string): Observable<LayoutDTO> {
    return this.http.delete<LayoutDTO>(`${baseUrl(servicoId)}/termos-negacao`, { params: { texto } });
  }
}
