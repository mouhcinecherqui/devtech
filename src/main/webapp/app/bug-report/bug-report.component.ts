import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';

@Component({
  selector: 'app-bug-report',
  templateUrl: './bug-report.component.html',
  styleUrls: ['./bug-report.component.scss'],
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
})
export class BugReportComponent {
  form: FormGroup;
  submitted = false;

  constructor(private fb: FormBuilder) {
    this.form = this.fb.group({
      name: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      website: [''],
      description: ['', Validators.required],
    });
  }

  submit() {
    this.submitted = true;
    // Here you would send the data to the backend
  }
}
