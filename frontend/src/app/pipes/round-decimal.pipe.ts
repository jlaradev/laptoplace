import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'roundDecimal',
  standalone: true
})
export class RoundDecimalPipe implements PipeTransform {
  transform(value: number | null | undefined, decimals: number = 1): string {
    if (value === null || value === undefined) {
      return '';
    }
    const factor = Math.pow(10, decimals);
    const rounded = Math.round(value * factor) / factor;
    
    // Si el redondeado es un entero, no mostrar decimales
    if (rounded % 1 === 0) {
      return rounded.toString();
    }
    
    return rounded.toFixed(decimals);
  }
}
