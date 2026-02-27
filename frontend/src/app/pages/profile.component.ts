import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HeaderComponent } from '../components/header.component';
import { FooterComponent } from '../components/footer.component';
import { UserService, User } from '../services/user.service';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, HeaderComponent, FooterComponent, FormsModule],
  template: `
    <app-header></app-header>
    <div class="container mx-auto py-8">
      <h1 class="text-3xl font-bold mb-6 text-blue-800 text-center">Mi Perfil</h1>
      <div *ngIf="user && user.nombre; else loading">
        <div class="bg-gradient-to-br from-blue-50 to-white rounded-xl shadow-lg p-8 max-w-lg mx-auto flex flex-col items-center">
            <!-- Toast flotante (igual que producto) -->
            <div class="fixed right-6 z-50 top-20 pointer-events-none">
              <div *ngIf="toastVisible" class="min-w-[200px] max-w-sm bg-blue-600 text-white px-4 py-3 rounded shadow-lg transition-opacity duration-300">
                {{ toastMsg }}
              </div>
            </div>
          <div class="w-24 h-24 rounded-full bg-blue-200 flex items-center justify-center mb-6">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-16 w-16 text-blue-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5.121 17.804A13.937 13.937 0 0112 15c2.5 0 4.847.657 6.879 1.804M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
            </svg>
          </div>
          <div *ngIf="!editMode">
            <div class="mb-4 text-xl font-semibold text-blue-700">{{ user.nombre }} {{ user.apellido }}</div>
            <div class="mb-2 flex items-center w-full">
              <span class="font-semibold text-slate-700 w-32">Email:</span>
              <span class="text-slate-600">{{ user.email }}</span>
            </div>
            <div class="mb-2 flex items-center w-full">
              <span class="font-semibold text-slate-700 w-32">Dirección:</span>
              <span class="text-slate-600">{{ user.direccion }}</span>
            </div>
            <div class="mb-2 flex items-center w-full">
              <span class="font-semibold text-slate-700 w-32">Teléfono:</span>
              <span class="text-slate-600">{{ user.telefono || '—' }}</span>
            </div>
            <button (click)="toggleEdit()" class="mt-6 px-6 py-2 rounded-full bg-blue-600 text-white font-semibold hover:bg-blue-700 transition">Editar información</button>
          </div>
          <form *ngIf="editMode" (ngSubmit)="saveChanges()" class="w-full flex flex-col items-center">
            <div class="mb-4 text-xl font-semibold text-blue-700">Editar perfil</div>
            <div class="mb-2 flex items-center w-full">
              <span class="font-semibold text-slate-700 w-32">Nombre:</span>
              <input [(ngModel)]="editUser.nombre" name="nombre" class="flex-1 px-3 py-2 rounded border border-blue-200 focus:border-blue-600 outline-none text-slate-700" required />
            </div>
            <div class="mb-2 flex items-center w-full">
              <span class="font-semibold text-slate-700 w-32">Apellido:</span>
              <input [(ngModel)]="editUser.apellido" name="apellido" class="flex-1 px-3 py-2 rounded border border-blue-200 focus:border-blue-600 outline-none text-slate-700" required />
            </div>
            <div class="mb-2 flex items-center w-full">
              <span class="font-semibold text-slate-700 w-32">Dirección:</span>
              <input [(ngModel)]="editUser.direccion" name="direccion" class="flex-1 px-3 py-2 rounded border border-blue-200 focus:border-blue-600 outline-none text-slate-700" required />
            </div>
            <div class="mb-2 flex items-center w-full">
              <span class="font-semibold text-slate-700 w-32">Teléfono:</span>
              <input [(ngModel)]="editUser.telefono" name="telefono" class="flex-1 px-3 py-2 rounded border border-blue-200 focus:border-blue-600 outline-none text-slate-700" />
            </div>
            <div class="flex gap-4 mt-6">
              <button type="submit" [disabled]="isSaving" class="px-6 py-2 rounded-full bg-blue-600 text-white font-semibold hover:bg-blue-700 transition">
                <span *ngIf="!isSaving">Guardar</span>
                <span *ngIf="isSaving">Enviando...</span>
              </button>
              <button type="button" (click)="toggleEdit()" [disabled]="isSaving" class="px-6 py-2 rounded-full bg-slate-300 text-slate-700 font-semibold hover:bg-slate-400 transition">Cancelar</button>
            </div>
          </form>
        </div>
      </div>
      <ng-template #loading>
        <div class="flex flex-col items-center justify-center min-h-40">
          <div class="w-12 h-12 border-4 border-blue-600 border-t-transparent rounded-full animate-spin mb-4"></div>
          <span class="text-blue-700 font-semibold text-lg">Cargando detalles...</span>
        </div>
      </ng-template>
    </div>
    <app-footer></app-footer>
  `,
  styles: []
})
export class ProfileComponent implements OnInit, OnDestroy {
    toastMsg: string | null = null;
    toastVisible = false;
    editMode = false;
    editUser: Partial<User> = {};
    isSaving = false;

    toggleEdit() {
      if (!this.editMode) {
        this.editUser = { ...this.user };
      }
      this.editMode = !this.editMode;
    }

    saveChanges() {
      const userId = this.user?.id;
      if (userId) {
        this.isSaving = true;
        const payload = {
          nombre: this.editUser.nombre,
          apellido: this.editUser.apellido,
          direccion: this.editUser.direccion,
          telefono: this.editUser.telefono
        };
        this.userService.updateUser(userId, payload).subscribe({
          next: (updated) => {
            this.user = updated;
            this.isSaving = false;
            this.editMode = false;
              this.showToast('Datos actualizados');
            // Forzar renderizado para asegurar cierre inmediato
            this.cdr.detectChanges();
          },
          error: (err) => {
            alert('Error al actualizar el perfil. Intenta nuevamente.');
            this.isSaving = false;
          }
        });
      }
    }
    showToast(msg: string) {
      this.toastMsg = msg;
      this.toastVisible = true;
      setTimeout(() => {
        this.toastVisible = false;
        setTimeout(() => this.toastMsg = null, 300);
      }, 2500);
    }
  user: User | null = null;
  private userSub: any;

  constructor(
    private userService: UserService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadUser();
  }

  ngOnDestroy(): void {
    if (this.userSub) {
      this.userSub.unsubscribe();
    }
  }

  loadUser() {
    const userId = localStorage.getItem('userId');
    console.log('[Profile] Buscando userId en LocalStorage:', userId);

    if (userId) {
      this.userSub = this.userService.getUserById(userId).subscribe({
        next: (u) => {
          console.log('[Profile] Datos recibidos con éxito:', u);
          // Asignamos el usuario y forzamos a Angular a renderizar
          this.user = u;
          this.cdr.detectChanges(); 
        },
        error: (err) => {
          console.error('[Profile] Error al obtener usuario:', err);
          this.user = null;
          this.cdr.detectChanges();
        }
      });
    } else {
      console.warn('[Profile] No se encontró userId en LocalStorage');
      this.user = null;
    }
  }
}