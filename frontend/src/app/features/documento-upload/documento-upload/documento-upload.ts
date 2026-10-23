import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatSnackBar } from '@angular/material/snack-bar';

import { Documento as DocumentoApi } from '../../../core/services/documento';
import { ResultadoProcessamentoDTO } from '../../../core/models/api.models';

@Component({
  selector: 'app-documento-upload',
  imports: [CommonModule, MatButtonModule, MatIconModule, MatCardModule],
  templateUrl: './documento-upload.html',
  styleUrl: './documento-upload.scss',
})
export class DocumentoUpload {
  private readonly documentoApi = inject(DocumentoApi);
  private readonly snackBar = inject(MatSnackBar);

  readonly processando = signal(false);
  readonly resultado = signal<ResultadoProcessamentoDTO | null>(null);

  aoSelecionarArquivo(event: Event): void {
    const input = event.target as HTMLInputElement;
    const arquivo = input.files?.[0];
    if (!arquivo) {
      return;
    }
    this.processando.set(true);
    this.resultado.set(null);
    this.documentoApi.processar(arquivo).subscribe({
      next: (resultado) => {
        this.resultado.set(resultado);
        this.processando.set(false);
      },
      error: () => {
        this.processando.set(false);
        this.snackBar.open('Falha ao processar o documento.', 'Fechar', { duration: 4000 });
      },
    });
    input.value = '';
  }
}
