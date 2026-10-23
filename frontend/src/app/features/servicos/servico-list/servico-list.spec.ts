import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { ServicoList } from './servico-list';

describe('ServicoList', () => {
  let component: ServicoList;
  let fixture: ComponentFixture<ServicoList>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ServicoList],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    fixture = TestBed.createComponent(ServicoList);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
