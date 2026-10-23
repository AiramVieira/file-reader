import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { Layout } from './layout';

describe('Layout', () => {
  let service: Layout;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(Layout);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
