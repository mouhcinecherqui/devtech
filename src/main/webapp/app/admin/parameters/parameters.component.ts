import { Component, OnInit, inject } from '@angular/core';
import { AppParameter } from './parameters.model';
import { ParametersService } from './parameters.service';
import SharedModule from 'app/shared/shared.module';
import { FormsModule } from '@angular/forms';

// Types de paramètres utilisés pour les sections
const TICKET_STATUS = 'ticket-status';
const TICKET_TYPE = 'ticket-type';
const TICKET_PRIORITY = 'ticket-priority';

@Component({
  selector: 'app-admin-parameters',
  templateUrl: './parameters.component.html',
  styleUrls: ['./parameters.component.scss'],
  standalone: true,
  imports: [SharedModule, FormsModule],
})
export class ParametersComponent implements OnInit {
  parameters: AppParameter[] = [];
  private readonly parametersService = inject(ParametersService);

  // Pour l'ajout/édition
  showForm = false;
  editMode = false;
  form: AppParameter = { key: '', value: '', type: '', description: '' };
  formType: string = '';

  ngOnInit(): void {
    this.loadAll();
  }

  loadAll(): void {
    this.parametersService.getAll().subscribe(params => {
      this.parameters = params;
    });
  }

  // Filtrage par type
  get ticketStatusList(): AppParameter[] {
    return this.parameters.filter(p => p.type === TICKET_STATUS);
  }
  get ticketTypeList(): AppParameter[] {
    return this.parameters.filter(p => p.type === TICKET_TYPE);
  }
  get ticketPriorityList(): AppParameter[] {
    return this.parameters.filter(p => p.type === TICKET_PRIORITY);
  }
  get otherList(): AppParameter[] {
    return this.parameters.filter(p => ![TICKET_STATUS, TICKET_TYPE, TICKET_PRIORITY].includes(p.type ?? ''));
  }

  // Ajout/édition par type
  openAddForm(type: string): void {
    this.showForm = true;
    this.editMode = false;
    this.formType = type;
    this.form = { key: '', value: '', type, description: '' };
  }

  openEditForm(param: AppParameter): void {
    this.showForm = true;
    this.editMode = true;
    this.formType = param.type ?? '';
    this.form = { ...param };
  }

  save(): void {
    this.form.type = this.formType;
    if (this.editMode && this.form.id) {
      this.parametersService.update(this.form).subscribe(() => {
        this.loadAll();
        this.showForm = false;
      });
    } else {
      this.parametersService.create(this.form).subscribe(() => {
        this.loadAll();
        this.showForm = false;
      });
    }
  }

  delete(param: AppParameter): void {
    if (param.id && confirm('Supprimer ce paramètre ?')) {
      this.parametersService.delete(param.id).subscribe(() => this.loadAll());
    }
  }

  cancel(): void {
    this.showForm = false;
  }
}
