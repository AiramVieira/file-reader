import { HttpClient } from '@angular/common/http';
import { inject, Service } from '@angular/core';
import { Observable } from 'rxjs';
import {
  AtualizarServicoRequest,
  CriarServicoRequest,
  Servico as ServicoModel,
} from '../models/api.models';

const BASE_URL = '/api/servicos';

@Service()
export class Servico {
  private readonly http = inject(HttpClient);

  listar(): Observable<ServicoModel[]> {
    return this.http.get<ServicoModel[]>(BASE_URL);
  }

  buscar(id: number): Observable<ServicoModel> {
    return this.http.get<ServicoModel>(`${BASE_URL}/${id}`);
  }

  criar(request: CriarServicoRequest): Observable<ServicoModel> {
    return this.http.post<ServicoModel>(BASE_URL, request);
  }

  atualizar(id: number, request: AtualizarServicoRequest): Observable<ServicoModel> {
    return this.http.put<ServicoModel>(`${BASE_URL}/${id}`, request);
  }

  excluir(id: number): Observable<void> {
    return this.http.delete<void>(`${BASE_URL}/${id}`);
  }
}
