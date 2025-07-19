import { Pipe, PipeTransform } from '@angular/core';

@Pipe({ name: 'filterPaiements', standalone: true })
export class FilterPaiementsPipe implements PipeTransform {
  transform(paiements: any[], search: string): any[] {
    if (!search) return paiements;
    const lower = search.toLowerCase();
    return paiements.filter(
      p =>
        p.user?.toLowerCase().includes(lower) ||
        p.status?.toLowerCase().includes(lower) ||
        (p.amount + '').includes(lower) ||
        p.date?.toLowerCase().includes(lower),
    );
  }
}
