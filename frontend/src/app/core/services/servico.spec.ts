import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { Servico } from './servico';

describe('Servico', () => {
  let service: Servico;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(Servico);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
