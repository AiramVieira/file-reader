import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';

import { ServicoLayoutMapper } from './servico-layout-mapper';

describe('ServicoLayoutMapper', () => {
  let component: ServicoLayoutMapper;
  let fixture: ComponentFixture<ServicoLayoutMapper>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ServicoLayoutMapper],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: convertToParamMap({ id: '1' }) } },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ServicoLayoutMapper);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
