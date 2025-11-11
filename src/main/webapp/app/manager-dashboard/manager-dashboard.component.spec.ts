import { ComponentFixture, TestBed } from '@angular/core/testing';
import {} from '@angular/common/http/testing';
import {} from '@angular/router/testing';
import { TranslateModule } from '@ngx-translate/core';
import { ManagerDashboardComponent } from './manager-dashboard.component';

describe('ManagerDashboardComponent', () => {
  let component: ManagerDashboardComponent;
  let fixture: ComponentFixture<ManagerDashboardComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ManagerDashboardComponent, TranslateModule.forRoot()],
    }).compileComponents();

    fixture = TestBed.createComponent(ManagerDashboardComponent);
    component = fixture.componentInstance;

    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have default demo data', () => {
    expect(Array.isArray(component.tickets)).toBeTruthy();
    expect(component.stats).toBeDefined();
  });
});
