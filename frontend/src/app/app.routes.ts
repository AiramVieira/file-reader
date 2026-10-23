import { Routes } from '@angular/router';
import { ServicoList } from './features/servicos/servico-list/servico-list';
import { ServicoLayoutMapper } from './features/layout-mapper/servico-layout-mapper/servico-layout-mapper';
import { DocumentoUpload } from './features/documento-upload/documento-upload/documento-upload';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'servicos' },
  { path: 'servicos', component: ServicoList },
  { path: 'servicos/:id/layout', component: ServicoLayoutMapper },
  { path: 'processar', component: DocumentoUpload },
];
