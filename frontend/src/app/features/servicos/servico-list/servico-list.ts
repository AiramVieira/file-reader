import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatCardModule } from '@angular/material/card';
import { MatSnackBar } from '@angular/material/snack-bar';

import { Servico as ServicoApi } from '../../../core/services/servico';
import { CriarServicoRequest, Servico } from '../../../core/models/api.models';

@Component({
  selector: 'app-servico-list',
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatSlideToggleModule,
    MatCardModule,
  ],
  templateUrl: './servico-list.html',
  styleUrl: './servico-list.scss',
})
export class ServicoList {
  private readonly servicoApi = inject(ServicoApi);
  private readonly snackBar = inject(MatSnackBar);

  readonly colunas = ['nome', 'ativo', 'acoes'];

  readonly servicos = signal<Servico[]>([]);
  readonly carregando = signal(false);

  novoServico: CriarServicoRequest = { nome: '', descricao: '' };

  constructor() {
    this.carregar();
  }

  carregar(): void {
    this.carregando.set(true);
    this.servicoApi.listar().subscribe({
      next: (lista) => {
        this.servicos.set(lista);
        this.carregando.set(false);
      },
      error: () => {
        this.snackBar.open('Falha ao carregar servicos.', 'Fechar', { duration: 4000 });
        this.carregando.set(false);
      },
    });
  }

  criar(): void {
    if (!this.novoServico.nome.trim()) {
      return;
    }
    this.servicoApi.criar(this.novoServico).subscribe({
      next: () => {
        this.novoServico = { nome: '', descricao: '' };
        this.snackBar.open('Servico criado com sucesso.', 'Fechar', { duration: 3000 });
        this.carregar();
      },
      error: () => this.snackBar.open('Falha ao criar servico.', 'Fechar', { duration: 4000 }),
    });
  }

  alternarAtivo(servico: Servico): void {
    this.servicoApi
      .atualizar(servico.id, { nome: servico.nome, descricao: servico.descricao ?? undefined, ativo: !servico.ativo })
      .subscribe({
        next: () => this.carregar(),
        error: () => this.snackBar.open('Falha ao atualizar servico.', 'Fechar', { duration: 4000 }),
      });
  }

  excluir(servico: Servico): void {
    if (!confirm(`Excluir o servico "${servico.nome}"? Isso remove o layout mapeado tambem.`)) {
      return;
    }
    this.servicoApi.excluir(servico.id).subscribe({
      next: () => this.carregar(),
      error: () => this.snackBar.open('Falha ao excluir servico.', 'Fechar', { duration: 4000 }),
    });
  }
}
