import { Component, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ProductDetailService, ProductDetail } from '../services/product-detail.service';
import { HeaderComponent } from '../components/header.component';
import { FooterComponent } from '../components/footer.component';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-compare',
  standalone: true,
  imports: [CommonModule, FormsModule, HeaderComponent, FooterComponent],
  template: `
    <app-header></app-header>
    <main class="flex flex-col items-center min-h-screen bg-white p-8">
        <h1 class="text-3xl font-extrabold mb-10 text-blue-800 drop-shadow">Comparar productos</h1>
      <div class="flex flex-col md:flex-row gap-8 w-full max-w-4xl justify-center">
        <div class="flex-1">
          <label class="block mb-2 font-semibold text-blue-700">Laptop 1</label>
          <input type="text" [(ngModel)]="search1" (ngModelChange)="onSearch(1)" placeholder="Buscar..." class="w-full px-3 py-2 border rounded-lg mb-2" />
          <ul *ngIf="search1 && results1().length > 0" class="border rounded bg-white shadow max-h-40 overflow-auto mb-2">
            <li *ngFor="let p of results1()" (click)="selectProduct(1, p)" class="px-3 py-2 hover:bg-blue-100 cursor-pointer">{{ p.nombre }}</li>
          </ul>
          <div *ngIf="selected1()" class="p-4 border rounded bg-slate-50 mt-2 flex flex-col items-center gap-2">
            <div class="font-bold text-blue-800 text-lg mb-2">{{ selected1()?.nombre }}</div>
            <div class="w-72 h-72 flex items-center justify-center bg-white border rounded">
              <img *ngIf="getLaptopImage(selected1())" [src]="getLaptopImage(selected1())" alt="Imagen laptop" class="w-full h-full object-contain" />
              <div *ngIf="!getLaptopImage(selected1())" class="w-full h-full flex items-center justify-center text-xs text-slate-400">Imagen no disponible</div>
            </div>
          </div>
        </div>
        <div class="flex-1">
          <label class="block mb-2 font-semibold text-blue-700">Laptop 2</label>
          <input type="text" [(ngModel)]="search2" (ngModelChange)="onSearch(2)" placeholder="Buscar..." class="w-full px-3 py-2 border rounded-lg mb-2" />
          <ul *ngIf="search2 && results2().length > 0" class="border rounded bg-white shadow max-h-40 overflow-auto mb-2">
            <li *ngFor="let p of results2()" (click)="selectProduct(2, p)" class="px-3 py-2 hover:bg-blue-100 cursor-pointer">{{ p.nombre }}</li>
          </ul>
          <div *ngIf="selected2()" class="p-4 border rounded bg-slate-50 mt-2 flex flex-col items-center gap-2">
            <div class="font-bold text-blue-800 text-lg mb-2">{{ selected2()?.nombre }}</div>
            <div class="w-72 h-72 flex items-center justify-center bg-white border rounded">
              <img *ngIf="getLaptopImage(selected2())" [src]="getLaptopImage(selected2())" alt="Imagen laptop" class="w-full h-full object-contain" />
              <div *ngIf="!getLaptopImage(selected2())" class="w-full h-full flex items-center justify-center text-xs text-slate-400">Imagen no disponible</div>
            </div>
          </div>
        </div>
      </div>
        <button (click)="compare()" [disabled]="!selected1() || !selected2()" class="mt-10 px-8 py-3 bg-blue-600 text-white rounded-full font-bold text-lg shadow hover:bg-blue-700 transition disabled:opacity-50">Comparar</button>
        
      <div *ngIf="showComparison()" class="w-full max-w-6xl mt-10">
        <h2 class="text-2xl font-bold mb-6 text-blue-800 text-center">Comparación de especificaciones</h2>
        <div class="flex justify-center">
          <table class="w-full mx-auto text-lg">
            <colgroup>
              <col style="width: 20%;">
              <col style="width: 10%;">
              <col style="width: 20%;">
              <col style="width: 20%;">
              <col style="width: 10%;">
              <col style="width: 20%;">
            </colgroup>
            <tbody>
              <tr class="h-20">
                <td></td>
                <td></td>
                <td class="text-center font-bold text-blue-700 text-xl">{{ selected1()?.nombre || '---' }}</td>
                <td class="text-center font-bold text-blue-700 text-xl">{{ selected2()?.nombre || '---' }}</td>
                <td></td>
                <td></td>
              </tr>
              <tr class="h-16">
                <td class="text-blue-800 font-semibold pr-6 text-right align-middle">Precio</td>
                <td></td>
                <td class="w-32 text-center align-middle">{{ selected1()?.precio ? ('$ ' + selected1()?.precio) : '-' }}</td>
                <td class="w-32 text-center align-middle">{{ selected2()?.precio ? ('$ ' + selected2()?.precio) : '-' }}</td>
                <td></td>
                <td></td>
              </tr>
              <tr class="h-16">
                <td class="text-blue-800 font-semibold pr-6 text-right align-middle">Procesador</td>
                <td></td>
                <td class="w-32 text-center align-middle">{{ selected1()?.procesador || '-' }}</td>
                <td class="w-32 text-center align-middle">{{ selected2()?.procesador || '-' }}</td>
                <td></td>
                <td></td>
              </tr>
              <tr class="h-16">
                <td class="text-blue-800 font-semibold pr-6 text-right align-middle">RAM (GB)</td>
                <td></td>
                <td class="w-32 text-center align-middle">{{ selected1()?.ram || '-' }}</td>
                <td class="w-32 text-center align-middle">{{ selected2()?.ram || '-' }}</td>
                <td></td>
                <td></td>
              </tr>
              <tr class="h-16">
                <td class="text-blue-800 font-semibold pr-6 text-right align-middle">Almacenamiento (GB)</td>
                <td></td>
                <td class="w-32 text-center align-middle">{{ selected1()?.almacenamiento || '-' }}</td>
                <td class="w-32 text-center align-middle">{{ selected2()?.almacenamiento || '-' }}</td>
                <td></td>
                <td></td>
              </tr>
              <tr class="h-16">
                <td class="text-blue-800 font-semibold pr-6 text-right align-middle">Pantalla</td>
                <td></td>
                <td class="w-32 text-center align-middle">{{ selected1()?.pantalla || '-' }}</td>
                <td class="w-32 text-center align-middle">{{ selected2()?.pantalla || '-' }}</td>
                <td></td>
                <td></td>
              </tr>
              <tr class="h-16">
                <td class="text-blue-800 font-semibold pr-6 text-right align-middle">GPU</td>
                <td></td>
                <td class="w-32 text-center align-middle">{{ selected1()?.gpu || '-' }}</td>
                <td class="w-32 text-center align-middle">{{ selected2()?.gpu || '-' }}</td>
                <td></td>
                <td></td>
              </tr>
              <tr class="h-16">
                <td class="text-blue-800 font-semibold pr-6 text-right align-middle">Peso (g)</td>
                <td></td>
                <td class="w-32 text-center align-middle">{{ selected1()?.peso || '-' }}</td>
                <td class="w-32 text-center align-middle">{{ selected2()?.peso || '-' }}</td>
                <td></td>
                <td></td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </main>
    <app-footer></app-footer>
  `
})
export class CompareComponent {
  private productDetailService = inject(ProductDetailService);
  search1 = '';
  search2 = '';
  results1 = signal<ProductDetail[]>([]);
  results2 = signal<ProductDetail[]>([]);
  selected1 = signal<ProductDetail|null>(null);
  selected2 = signal<ProductDetail|null>(null);
  showComparison = signal(false);
  private selecting = false;
  private searchSub1: Subscription | null = null;
  private searchSub2: Subscription | null = null;

  onSearch(n: 1 | 2) {
    if (this.selecting) return;
    const term = n === 1 ? this.search1 : this.search2;
    if (!term) {
      n === 1 ? this.results1.set([]) : this.results2.set([]);
      return;
    }
    // Cancelar búsqueda anterior si existe
    if (n === 1 && this.searchSub1) { this.searchSub1.unsubscribe(); }
    if (n === 2 && this.searchSub2) { this.searchSub2.unsubscribe(); }
    const sub = this.productDetailService['http'].get<any>(`${this.productDetailService['apiUrl']}/search?nombre=${term}`)
      .subscribe((res: any) => {
        const ids = res.content.map((p: any) => p.id);
        const detailCalls = ids.map((id: number) => this.productDetailService.getProductDetail(id));
        Promise.all(detailCalls.map((obs: any) => obs.toPromise())).then((details: ProductDetail[]) => {
          if (n === 1) this.results1.set(details);
          else this.results2.set(details);
        });
      });
    if (n === 1) this.searchSub1 = sub;
    else this.searchSub2 = sub;
  }

  selectProduct(n: 1 | 2, p: ProductDetail) {
    this.selecting = true;
    if (n === 1) {
      this.selected1.set(p);
      this.search1 = '';
      this.results1.set([]);
    } else {
      this.selected2.set(p);
      this.search2 = '';
      this.results2.set([]);
    }
    this.showComparison.set(false);
    setTimeout(() => { this.selecting = false; }, 300);
  }

  compare() {
    if (this.selected1() && this.selected2()) {
      this.showComparison.set(true);
    }
  }

  getLaptopImage(laptop: ProductDetail | null): string | null {
    if (laptop && laptop.imagenes && laptop.imagenes.length > 0) {
      const img = laptop.imagenes[0];
      if (typeof img === 'string') return img;
      if (img && typeof img === 'object' && img.url) return img.url;
    }
    return null;
  }
}
