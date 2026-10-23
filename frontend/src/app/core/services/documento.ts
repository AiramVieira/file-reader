import { HttpClient } from '@angular/common/http';
import { inject, Service } from '@angular/core';
import { Observable } from 'rxjs';
import { ResultadoProcessamentoDTO } from '../models/api.models';

@Service()
export class Documento {
  private readonly http = inject(HttpClient);

  processar(arquivo: File): Observable<ResultadoProcessamentoDTO> {
    const formData = new FormData();
    formData.append('arquivo', arquivo);
    return this.http.post<ResultadoProcessamentoDTO>('/api/documentos/processar', formData);
  }
}
