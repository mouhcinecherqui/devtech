import { Component } from '@angular/core';

@Component({
  selector: 'jhi-footer',
  templateUrl: './footer.component.html',
})
export default class FooterComponent {
  currentYear = new Date().getFullYear();
}
