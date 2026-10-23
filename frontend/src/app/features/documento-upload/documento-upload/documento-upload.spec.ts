import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { DocumentoUpload } from './documento-upload';

describe('DocumentoUpload', () => {
  let component: DocumentoUpload;
  let fixture: ComponentFixture<DocumentoUpload>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DocumentoUpload],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    fixture = TestBed.createComponent(DocumentoUpload);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
