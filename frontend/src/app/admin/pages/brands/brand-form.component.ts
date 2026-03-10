import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, Router, ActivatedRoute } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { BrandService, BrandCreateDTO } from '../../../services/brand.service';
import { Brand } from '../../../models/brand.model';

@Component({
  selector: 'app-brand-form',
  standalone: true,
  imports: [CommonModule, RouterLink, ReactiveFormsModule],
  template: `<div class="space-y-6">
  <!-- Success Toast -->
  <div *ngIf="success()" class="fixed top-4 right-4 z-50 px-4 py-3 bg-green-100 border border-green-400 text-green-700 rounded-lg shadow-lg">
    {{ success() }}
  </div>

  <!-- Error Toast -->
  <div *ngIf="error()" class="fixed top-4 right-4 z-50 px-4 py-3 bg-red-100 border border-red-400 text-red-700 rounded-lg shadow-lg">
    {{ error() }}
  </div>
  <!-- Confirmación amigable -->
  <div *ngIf="showConfirm() as confirm" class="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-40">
    <div class="bg-white rounded-lg shadow-lg p-6 max-w-md w-full relative">
      <button class="absolute top-4 right-4 text-gray-500 hover:text-gray-700 text-2xl font-bold" (click)="showConfirm.set(null)">×</button>
      <div class="text-lg font-medium mb-4">{{ confirm.message }}</div>
      <div class="flex justify-end gap-2">
        <button class="px-4 py-2 bg-gray-200 text-gray-700 rounded-lg font-medium hover:bg-gray-300 transition" (click)="showConfirm.set(null)">Cancelar</button>
        <button class="px-4 py-2 bg-red-600 text-white rounded-lg font-medium hover:bg-red-700 transition" (click)="confirm.onConfirm(); showConfirm.set(null)">Confirmar</button>
      </div>
    </div>
  </div>

  <!-- Header -->
  <div>
    <a routerLink="/admin/brands" class="text-blue-600 hover:text-blue-700 text-sm font-medium">
      ← Volver a marcas
    </a>
  </div>

  <h1 class="text-3xl font-bold text-gray-900">
    {{ isEditMode() ? 'Editar Marca' : 'Nueva Marca' }}
  </h1>

  <!-- Loading -->
  <div *ngIf="isLoading()" class="text-center py-12">
    <p class="text-gray-600">Cargando marca...</p>
  </div>

  <!-- Form -->
  <form *ngIf="!isLoading()" [formGroup]="form" (ngSubmit)="submit()" class="space-y-6">
    <!-- Form Fields -->
    <div class="bg-white border border-gray-200 rounded-lg p-6 space-y-4">
      <!-- Nombre -->
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-2">
          Nombre de la marca <span class="text-red-500">*</span>
          <span class="text-xs text-gray-500">(mínimo 3 caracteres)</span>
        </label>
        <input
          type="text"
          formControlName="nombre"
          placeholder="Ej: Dell, Apple, HP..."
          class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
        />
        <div *ngIf="form.get('nombre')?.hasError('required') && form.get('nombre')?.touched" class="text-red-600 text-sm mt-1">
          El nombre es obligatorio
        </div>
        <div *ngIf="form.get('nombre')?.hasError('minlength') && form.get('nombre')?.touched" class="text-red-600 text-sm mt-1">
          El nombre debe tener mínimo 3 caracteres
        </div>
      </div>

      <!-- Descripción -->
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-2">
          Descripción
        </label>
        <textarea
          formControlName="descripcion"
          placeholder="Descripción de la marca..."
          rows="4"
          class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
        ></textarea>
      </div>
    </div>

    <!-- Logo Section -->
    <div *ngIf="!isLoading()" class="space-y-4">
      <div class="bg-white border border-gray-200 rounded-lg p-6 space-y-4">
        <h2 class="text-lg font-semibold text-gray-900">Logo de la Marca</h2>
        <p class="text-sm text-gray-600">Solo se permite UNA imagen. Si ya existe, elimínala antes</p>

        <!-- Logo Preview -->
        <div *ngIf="logoUrl()" class="flex items-center gap-4">
          <div class="relative">
            <img
              [src]="logoUrl()"
              alt="Logo"
              class="w-32 h-32 object-cover rounded-lg border border-gray-200"
            />
          </div>
          <button
            type="button"
            (click)="deleteLogo()"
            class="px-4 py-2 bg-red-100 text-red-700 rounded-lg hover:bg-red-200 transition"
          >
            🗑️ Eliminar logo
          </button>
        </div>

        <!-- Upload Area (only if no logo) -->
        <div
          *ngIf="!logoUrl()"
          (dragover)="onDragOver($event)"
          (dragleave)="onDragLeave($event)"
          (drop)="onDrop($event)"
          [class]="'border-2 border-dashed rounded-lg p-8 text-center transition ' + (isDragging() ? 'border-blue-500 bg-blue-50' : 'border-gray-300')"
        >
          <p class="text-gray-600 mb-2">Arrastra la imagen aquí o haz clic para seleccionar</p>
          <input
            type="file"
            accept="image/*"
            (change)="onFileSelected($event)"
            class="hidden"
            #fileInput
          />
          <button
            type="button"
            (click)="fileInput.click()"
            class="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition"
          >
            📁 Seleccionar imagen
          </button>
          <p class="text-xs text-gray-500 mt-2">PNG, JPG o GIF (máx 4 MB)</p>
        </div>

        <!-- Upload Progress -->
        <div *ngIf="isUploadingImage()" class="text-center py-4">
          <p class="text-gray-600">Subiendo logo...</p>
        </div>
      </div>
    </div>

    <!-- Submit Buttons -->
    <div class="bg-white border border-gray-200 rounded-lg p-6 flex justify-between gap-4">
      <a
        routerLink="/admin/brands"
        class="px-4 py-2 bg-gray-200 text-gray-700 rounded-lg hover:bg-gray-300 transition"
      >
        Cancelar
      </a>
      <button
        type="submit"
        [disabled]="!form.valid || isSaving()"
        class="px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition disabled:opacity-50 disabled:cursor-not-allowed"
      >
        {{ isSaving() ? '⏸️ Guardando...' : (isEditMode() ? '✅ Actualizar' : '✅ Crear marca') }}
      </button>
    </div>
  </form>
</div>`,
  styles: []
})
export class BrandFormComponent implements OnInit {
  form!: FormGroup;
  isEditMode = signal(false);
  isLoading = signal(false);
  isSaving = signal(false);
  isUploadingImage = signal(false);
  isDragging = signal(false);
  error = signal<string | null>(null);
  success = signal<string | null>(null);
  logoUrl = signal<string | null>(null);
  showConfirm = signal<{ message: string, onConfirm: () => void } | null>(null);
  
  brandId?: number;
  private uploadedFile: File | null = null;
  private errorTimeoutId: any;
  private successTimeoutId: any;

  constructor(
    private fb: FormBuilder,
    private brandService: BrandService,
    public router: Router,
    private route: ActivatedRoute
  ) {
    this.initForm();
  }

  ngOnInit(): void {
    this.route.params.subscribe(params => {
      if (params['id']) {
        this.brandId = parseInt(params['id']);
        this.isEditMode.set(true);
        this.loadBrand();
      } else {
        this.isLoading.set(false);
      }
    });
  }

  private initForm(): void {
    this.form = this.fb.group({
      nombre: ['', [Validators.required, Validators.minLength(3)]],
      descripcion: ['']
    });
  }

  private loadBrand(): void {
    if (!this.brandId) return;

    this.isLoading.set(true);
    this.error.set(null);

    this.brandService.getBrandById(this.brandId).subscribe({
      next: (brand: Brand) => {
        this.form.patchValue({
          nombre: brand.nombre,
          descripcion: brand.descripcion || ''
        });

        if (brand.imageUrl) {
          this.logoUrl.set(brand.imageUrl);
        }

        this.isLoading.set(false);
      },
      error: (err) => {
        this.error.set('Error al cargar marca: ' + (err.message || 'Error desconocido'));
        this.isLoading.set(false);
      }
    });
  }

  submit(): void {
    if (!this.form.valid) return;

    // Ensure user has a token (must be admin to create/update brands)
    const token = localStorage.getItem('token');
    if (!token) {
      this.error.set('❌ Debes iniciar sesión como administrador para crear o editar marcas');
      this.errorTimeoutId = setTimeout(() => this.error.set(null), 4000);
      return;
    }

    this.isSaving.set(true);
    this.error.set(null);

    const formData = this.form.value;
    const dto: BrandCreateDTO = {
      nombre: formData.nombre,
      descripcion: formData.descripcion || undefined
    };

    if (this.isEditMode() && this.brandId) {
      this.brandService.updateBrand(this.brandId, dto).subscribe({
        next: (updated) => {
          // If user selected a new file, upload it via backend endpoint
          if (this.uploadedFile) {
            this.isUploadingImage.set(true);
            this.brandService.uploadBrandImage(this.brandId!, this.uploadedFile).subscribe({
              next: () => {
                this.isUploadingImage.set(false);
                this.isSaving.set(false);
                this.success.set('✅ Marca actualizada correctamente');
                this.successTimeoutId = setTimeout(() => this.router.navigate(['/admin/brands']), 2000);
              },
              error: (err) => {
                this.isUploadingImage.set(false);
                this.isSaving.set(false);
                this.error.set('❌ Error al subir imagen: ' + (err.error?.message || 'Error desconocido'));
                this.errorTimeoutId = setTimeout(() => this.error.set(null), 4000);
              }
            });
          } else {
            this.isSaving.set(false);
            this.success.set('✅ Marca actualizada correctamente');
            this.successTimeoutId = setTimeout(() => this.router.navigate(['/admin/brands']), 2000);
          }
        },
        error: (err) => {
          this.isSaving.set(false);
          if (err.status === 403) {
            this.error.set('❌ No tienes permisos para actualizar marcas (se requiere rol ADMIN)');
          } else {
            this.error.set('❌ Error al actualizar: ' + (err.error?.message || 'Error desconocido'));
          }
          this.errorTimeoutId = setTimeout(() => this.error.set(null), 4000);
        }
      });
    } else {
      this.brandService.createBrand(dto).subscribe({
        next: (created) => {
          // If a file was selected during creation, upload it to the backend endpoint
          if (this.uploadedFile && created && created.id) {
            this.isUploadingImage.set(true);
            this.brandService.uploadBrandImage(created.id, this.uploadedFile).subscribe({
              next: () => {
                this.isUploadingImage.set(false);
                this.isSaving.set(false);
                this.success.set('✅ Marca creada correctamente');
                this.successTimeoutId = setTimeout(() => this.router.navigate(['/admin/brands']), 2000);
              },
              error: (err) => {
                this.isUploadingImage.set(false);
                this.isSaving.set(false);
                if (err.status === 403) {
                  this.error.set('❌ No tienes permisos para subir imágenes de marca');
                } else {
                  this.error.set('❌ Error al subir imagen: ' + (err.error?.message || 'Error desconocido'));
                }
                this.errorTimeoutId = setTimeout(() => this.error.set(null), 4000);
              }
            });
          } else {
            this.isSaving.set(false);
            this.success.set('✅ Marca creada correctamente');
            this.successTimeoutId = setTimeout(() => this.router.navigate(['/admin/brands']), 2000);
          }
        },
        error: (err) => {
          this.isSaving.set(false);
          if (err.status === 403) {
            this.error.set('❌ No tienes permisos para crear marcas (se requiere rol ADMIN)');
          } else {
            this.error.set('❌ Error al crear: ' + (err.error?.message || 'Error desconocido'));
          }
          this.errorTimeoutId = setTimeout(() => this.error.set(null), 4000);
        }
      });
    }
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.isDragging.set(true);
  }

  onDragLeave(event: DragEvent): void {
    event.preventDefault();
    this.isDragging.set(false);
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.isDragging.set(false);
    
    const files = event.dataTransfer?.files;
    if (files?.length) {
      // Solo permitir 1 imagen
      if (this.logoUrl() && !this.uploadedFile) {
        this.error.set('⚠️ Ya existe un logo. Elimínalo primero antes de subir otro.');
        this.errorTimeoutId = setTimeout(() => this.error.set(null), 4000);
        return;
      }
      
      this.uploadImage(files[0]);
    }
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files?.length) {
      // Solo permitir 1 imagen
      if (this.logoUrl() && !this.uploadedFile) {
        this.error.set('⚠️ Ya existe un logo. Elimínalo primero antes de subir otro.');
        this.errorTimeoutId = setTimeout(() => this.error.set(null), 4000);
        input.value = '';
        return;
      }
      this.uploadImage(input.files[0]);
      input.value = '';
    }
  }

  private uploadImage(file: File): void {
    this.error.set(null);
    this.isUploadingImage.set(true);
    // keep the File for later upload to backend, and create a preview
    this.uploadedFile = file;
    const reader = new FileReader();
    reader.onload = (e: any) => {
      const dataUrl = e.target.result;
      this.logoUrl.set(dataUrl);
      this.isUploadingImage.set(false);
      this.success.set('✅ Logo cargado (previsualización)');
      this.successTimeoutId = setTimeout(() => this.success.set(null), 3000);
    };
    reader.onerror = () => {
      this.isUploadingImage.set(false);
      this.error.set('Error al leer imagen');
      this.errorTimeoutId = setTimeout(() => this.error.set(null), 4000);
    };
    reader.readAsDataURL(file);
  }

  deleteLogo(): void {
    this.showConfirm.set({
      message: '¿Deseas eliminar el logo actual?',
      onConfirm: () => {
        this.logoUrl.set(null);
        this.uploadedFile = null;
        this.success.set('✅ Logo eliminado');
        this.successTimeoutId = setTimeout(() => this.success.set(null), 2000);
      }
    });
  }

  ngOnDestroy(): void {
    if (this.errorTimeoutId) clearTimeout(this.errorTimeoutId);
    if (this.successTimeoutId) clearTimeout(this.successTimeoutId);
  }
}
