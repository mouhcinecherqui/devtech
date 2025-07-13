import { Component } from '@angular/core';

interface Parameter {
  key: string;
  value: string;
  description: string;
}

@Component({
  selector: 'app-admin-parameters',
  templateUrl: './parameters.component.html',
  styleUrls: ['./parameters.component.scss'],
})
export class ParametersComponent {
  parameters: Parameter[] = [
    { key: 'Site Name', value: 'DevTech', description: 'The name of the site' },
    { key: 'Support Email', value: 'support@devtech.com', description: 'Support contact email' },
    { key: 'Theme', value: 'Light', description: 'Current theme' },
  ];
}
