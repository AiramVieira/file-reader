import { Component, ElementRef, HostListener, computed, inject, signal, viewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatMenuModule, MatMenuTrigger } from '@angular/material/menu';
import { MatCheckboxModule, MatCheckboxChange } from '@angular/material/checkbox';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar } from '@angular/material/snack-bar';
import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { Observable } from 'rxjs';

import { Layout as LayoutApi } from '../../../core/services/layout';
import { LayoutDTO, MarcacaoDTO, TipoCampo, TokenDTO } from '../../../core/models/api.models';

const COR_POR_TIPO: Record<TipoCampo, string> = {
  COMPETENCIA: '#2e7d32',
  CNPJ: '#f9a825',
  TERMO_BAIXA: '#4b4279',
  TERMO_NEGACAO: '#c62828',
};

@Component({
  selector: 'app-servico-layout-mapper',
  imports: [
    CommonModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatMenuModule,
    MatCheckboxModule,
    MatChipsModule,
    MatFormFieldModule,
    MatInputModule,
    FormsModule,
  ],
  templateUrl: './servico-layout-mapper.html',
  styleUrl: './servico-layout-mapper.scss',
})
export class ServicoLayoutMapper {
  private readonly route = inject(ActivatedRoute);
  private readonly layoutApi = inject(LayoutApi);
  private readonly snackBar = inject(MatSnackBar);

  private readonly servicoId = Number(this.route.snapshot.paramMap.get('id'));

  // Menus abertos programaticamente na posicao do mouse, ao soltar o clique
  private readonly gatilhoToken = viewChild('gatilhoToken', { read: MatMenuTrigger });
  private readonly ancoraToken = viewChild('gatilhoToken', { read: ElementRef });
  private readonly gatilhoIntervalo = viewChild('gatilhoIntervalo', { read: MatMenuTrigger });
  private readonly ancoraIntervalo = viewChild('gatilhoIntervalo', { read: ElementRef });

  readonly layout = signal<LayoutDTO | null>(null);
  readonly carregando = signal(false);
  readonly enviando = signal(false);

  novoTermoNegacao = '';

  /** Selecao por arraste: intervalo de indices de token entre o mousedown e o ultimo token sob o mouse. */
  private arrastando = false;
  readonly selecaoInicio = signal<number | null>(null);
  readonly selecaoFim = signal<number | null>(null);
  readonly tokenSelecionado = signal<TokenDTO | null>(null);

  private readonly limitesSelecao = computed(() => {
    const inicio = this.selecaoInicio();
    const fim = this.selecaoFim();
    if (inicio === null || fim === null) {
      return null;
    }
    return { inicio: Math.min(inicio, fim), fim: Math.max(inicio, fim) };
  });

  /** Texto do termo que sera criado a partir da selecao (palavras unidas por "_"). */
  readonly textoSelecao = computed(() => {
    const limites = this.limitesSelecao();
    const layout = this.layout();
    if (!limites || !layout) {
      return '';
    }
    return layout.tokens
      .filter((t) => t.indice >= limites.inicio && t.indice <= limites.fim)
      .map((t) => t.texto)
      .join('_');
  });

  readonly termosBaixa = computed(() => this.marcacoesDoTipo('TERMO_BAIXA'));
  readonly termosNegacao = computed(() => this.marcacoesDoTipo('TERMO_NEGACAO'));
  readonly temNegacaoNoModelo = computed(() => this.termosNegacao().some((m) => m.presenteNoModelo));

  constructor() {
    this.carregar();
  }

  carregar(): void {
    this.carregando.set(true);
    this.layoutApi.obter(this.servicoId).subscribe({
      next: (layout) => {
        this.layout.set(layout);
        this.carregando.set(false);
      },
      error: () => {
        this.snackBar.open('Falha ao carregar o layout do servico.', 'Fechar', { duration: 4000 });
        this.carregando.set(false);
      },
    });
  }

  aoSelecionarArquivo(event: Event): void {
    const input = event.target as HTMLInputElement;
    const arquivo = input.files?.[0];
    if (!arquivo) {
      return;
    }
    this.enviando.set(true);
    this.layoutApi.uploadArquivoBase(this.servicoId, arquivo).subscribe({
      next: (layout) => {
        this.layout.set(layout);
        this.enviando.set(false);
        this.snackBar.open('Arquivo-modelo carregado. Clique nas palavras para marcar os campos.', 'Fechar', {
          duration: 5000,
        });
      },
      error: () => {
        this.enviando.set(false);
        this.snackBar.open('Falha ao enviar o arquivo-modelo.', 'Fechar', { duration: 4000 });
      },
    });
    input.value = '';
  }

  // ------------------------------------------------------------ selecao por clique/arraste

  iniciarSelecao(evento: MouseEvent, token: TokenDTO): void {
    if (evento.button !== 0) {
      return;
    }
    evento.preventDefault();
    this.arrastando = true;
    this.selecaoInicio.set(token.indice);
    this.selecaoFim.set(token.indice);
  }

  estenderSelecao(token: TokenDTO): void {
    if (this.arrastando) {
      this.selecaoFim.set(token.indice);
    }
  }

  @HostListener('document:mouseup', ['$event'])
  finalizarSelecao(evento: MouseEvent): void {
    if (!this.arrastando) {
      return;
    }
    this.arrastando = false;
    const limites = this.limitesSelecao();
    if (!limites) {
      return;
    }
    if (limites.inicio === limites.fim) {
      const token = this.layout()?.tokens.find((t) => t.indice === limites.inicio) ?? null;
      this.tokenSelecionado.set(token);
      this.abrirMenu('token', evento.clientX, evento.clientY);
    } else {
      this.abrirMenu('intervalo', evento.clientX, evento.clientY);
    }
  }

  /** Acessibilidade: Enter/Espaco no token (click sem mouse) abre o menu da palavra. */
  aoClicarComTeclado(evento: MouseEvent, token: TokenDTO): void {
    if (evento.detail !== 0) {
      return;
    }
    const caixa = (evento.target as HTMLElement).getBoundingClientRect();
    this.selecaoInicio.set(token.indice);
    this.selecaoFim.set(token.indice);
    this.tokenSelecionado.set(token);
    this.abrirMenu('token', caixa.left, caixa.bottom);
  }

  limparSelecao(): void {
    this.selecaoInicio.set(null);
    this.selecaoFim.set(null);
    this.tokenSelecionado.set(null);
  }

  emSelecao(token: TokenDTO): boolean {
    const limites = this.limitesSelecao();
    return !!limites && token.indice >= limites.inicio && token.indice <= limites.fim;
  }

  private abrirMenu(menu: 'token' | 'intervalo', x: number, y: number): void {
    const gatilho = menu === 'token' ? this.gatilhoToken() : this.gatilhoIntervalo();
    const ancora: ElementRef<HTMLElement> | undefined = menu === 'token' ? this.ancoraToken() : this.ancoraIntervalo();
    if (!gatilho || !ancora) {
      return;
    }
    ancora.nativeElement.style.left = `${x}px`;
    ancora.nativeElement.style.top = `${y}px`;
    gatilho.openMenu();
  }

  // ------------------------------------------------------------ marcacoes

  /** Competencia e CNPJ sao unicos: a opcao some do menu quando ja estao marcados em outra palavra. */
  podeMarcarUnico(token: TokenDTO, tipo: 'COMPETENCIA' | 'CNPJ'): boolean {
    const atual = this.layout()?.marcacoes.find((m) => m.tipo === tipo);
    return !atual || atual.tokenIndice === token.indice;
  }

  /** Marcacao do proprio token (nao conta termos de varias palavras que apenas passam por ele). */
  temMarcacaoNoToken(token: TokenDTO, tipo: TipoCampo): boolean {
    return !!this.layout()?.marcacoes.some(
      (m) => m.tipo === tipo && m.tokenIndice === token.indice && m.tokenIndiceFim === null,
    );
  }

  alternarCampo(token: TokenDTO, tipo: TipoCampo, evento: MatCheckboxChange): void {
    const operacao = evento.checked
      ? this.layoutApi.marcarCampo(this.servicoId, token.indice, tipo)
      : this.layoutApi.desmarcarCampo(this.servicoId, token.indice, tipo);
    this.aplicar(operacao, 'Falha ao atualizar a marcacao.');
  }

  marcarIntervalo(tipo: 'TERMO_BAIXA' | 'TERMO_NEGACAO'): void {
    const limites = this.limitesSelecao();
    if (!limites) {
      return;
    }
    this.aplicar(
      this.layoutApi.marcarCampo(this.servicoId, limites.inicio, tipo, limites.fim),
      'Falha ao marcar o termo.',
    );
  }

  removerMarcacao(marcacao: MarcacaoDTO): void {
    this.aplicar(this.layoutApi.removerMarcacao(this.servicoId, marcacao.id), 'Falha ao remover o termo.');
  }

  adicionarTermoNegacao(): void {
    const texto = this.novoTermoNegacao.trim();
    if (!texto) {
      return;
    }
    this.layoutApi.adicionarTermoNegacao(this.servicoId, texto).subscribe({
      next: (layout) => {
        this.layout.set(layout);
        this.novoTermoNegacao = '';
      },
      error: (erro: HttpErrorResponse) =>
        this.snackBar.open(erro.error?.mensagem ?? 'Falha ao adicionar o termo de negacao.', 'Fechar', {
          duration: 6000,
        }),
    });
  }

  estiloToken(token: TokenDTO): Record<string, string> {
    if (!token.tiposMarcados.length) {
      return {};
    }
    const sombras = token.tiposMarcados.map((tipo, indice) => `inset 0 0 0 ${2 + indice * 3}px ${COR_POR_TIPO[tipo]}`);
    return { 'box-shadow': sombras.join(', ') };
  }

  private marcacoesDoTipo(tipo: TipoCampo): MarcacaoDTO[] {
    return this.layout()?.marcacoes.filter((m) => m.tipo === tipo) ?? [];
  }

  private aplicar(operacao: Observable<LayoutDTO>, mensagemErro: string): void {
    operacao.subscribe({
      next: (layout) => this.layout.set(layout),
      error: (erro: HttpErrorResponse) =>
        this.snackBar.open(erro.error?.mensagem ?? mensagemErro, 'Fechar', { duration: 5000 }),
    });
  }
}
