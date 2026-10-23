import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { Documento } from './documento';

describe('Documento', () => {
  let service: Documento;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(Documento);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
