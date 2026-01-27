import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import SharedModule from '../shared/shared.module';

@Component({
  selector: 'jhi-client-documentation',
  standalone: true,
  imports: [CommonModule, RouterModule, TranslateModule, SharedModule],
  templateUrl: './client-documentation.component.html',
  styleUrls: ['./client-documentation.component.scss'],
})
export class ClientDocumentationComponent implements OnInit {
  activeSection: string = 'overview';

  constructor(public translateService: TranslateService) {}

  ngOnInit(): void {
    // Scroll to top on init
    window.scrollTo(0, 0);
  }

  scrollToSection(sectionId: string): void {
    this.activeSection = sectionId;
    const element = document.getElementById(sectionId);
    if (element) {
      element.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  }
}
