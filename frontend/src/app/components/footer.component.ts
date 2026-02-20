import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-footer',
  standalone: true,
  imports: [CommonModule],
  template: `
    <footer class="border-t border-slate-200 mt-20 py-8">
      <div class="max-w-[1440px] mx-auto px-4 md:px-6 flex flex-wrap justify-between items-center gap-4 text-sm text-slate-600">
        <span>2026 LaptoPlace. Todos los derechos reservados.</span>
        <span class="text-slate-700 font-medium">Soporte 24/7 en soporte@laptoplace.com</span>
      </div>
    </footer>
  `,
  styles: []
})
export class FooterComponent {}
