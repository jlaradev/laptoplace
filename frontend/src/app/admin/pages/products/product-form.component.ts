import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, Router, ActivatedRoute } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ProductService } from '../../../services/product.service';
import { BrandService } from '../../../services/brand.service';
import { Brand } from '../../../models/brand.model';
import { Product, ProductImage } from '../../../models/product.model';

@Component({
  selector: 'app-admin-product-form',
  standalone: true,
  imports: [CommonModule, RouterLink, ReactiveFormsModule],
  templateUrl: './product-form.component.html',
  styles: []
})
export class ProductFormComponent implements OnInit {
  form!: FormGroup;
  brands = signal<Brand[]>([]);
  isEditMode = signal(false);
  isLoading = signal(false);
  isSaving = signal(false);
  isUploadingImage = signal(false);
  isDragging = signal(false);
  error = signal<string | null>(null);
  success = signal<string | null>(null);
  showConfirm = signal<{ message: string, onConfirm: () => void } | null>(null);
  productImages = signal<ProductImage[]>([]);
  
  productId?: number;

  constructor(
    private fb: FormBuilder,
    private productService: ProductService,
    private brandService: BrandService,
    public router: Router,
    private route: ActivatedRoute
  ) {
    this.initForm();
  }

  ngOnInit(): void {
    this.route.params.subscribe(params => {
      if (params['id']) {
        this.productId = parseInt(params['id']);
        this.isEditMode.set(true);
        // En modo edit, cargar marcas PRIMERO, luego el producto
        this.loadBrandsAndProduct();
      } else {
        // En modo create, solo cargar marcas
        this.loadBrands();
      }
    });
  }

  private loadBrandsAndProduct(): void {
    this.isLoading.set(true);
    this.brandService.getAllBrands().subscribe({
      next: (brands) => {
        this.brands.set(brands);
        // Una vez cargadas las marcas, cargar el producto
        this.loadProduct();
      },
      error: (err) => {
        this.error.set('Error al cargar marcas: ' + (err.message || 'Error desconocido'));
        this.isLoading.set(false);
      }
    });
  }

  private initForm(): void {
    this.form = this.fb.group({
      nombre: ['', [Validators.required, Validators.minLength(3)]],
      brandId: ['', [Validators.required]],
      precio: [0, [Validators.required, Validators.min(0.01)]],
      stock: [0, [Validators.required, Validators.min(0)]],
      descripcion: [''],
      procesador: [''],
      ram: [''],
      almacenamiento: [''],
      pantalla: [''],
      gpu: [''],
      peso: ['']
    });
  }

  private loadBrands(): void {
    this.brandService.getAllBrands().subscribe({
      next: (brands) => {
        this.brands.set(brands);
      },
      error: (err) => {
        this.error.set('Error al cargar marcas: ' + (err.message || 'Error desconocido'));
      }
    });
  }

  private loadProduct(): void {
    if (!this.productId) return;

    this.error.set(null);

    this.productService.getProductById(this.productId).subscribe({
      next: (product) => {
        // El backend devuelve brand como objeto, y necesitamos extraer el ID
        // product.brand = { id: 5, nombre: "Dell", ... }
        // Entonces convertimos product.brand.id a string para el formulario
        const brandIdValue = (product as any).brand?.id ? (product as any).brand.id.toString() : '';
        
        this.form.patchValue({
          nombre: product.nombre,
          brandId: brandIdValue,
          precio: product.precio,
          stock: product.stock,
          descripcion: product.descripcion || '',
          procesador: product.procesador || '',
          ram: product.ram || '',
          almacenamiento: product.almacenamiento || '',
          pantalla: product.pantalla || '',
          gpu: product.gpu || '',
          peso: product.peso || ''
        });

        // Cargar imágenes del producto
        this.productService.getProductImages(this.productId!).subscribe({
          next: (images) => {
            this.productImages.set(images);
            this.isLoading.set(false);
          },
          error: (err) => {
            this.isLoading.set(false);
          }
        });
      },
      error: (err) => {
        this.error.set('Error al cargar producto: ' + (err.message || 'Error desconocido'));
        this.isLoading.set(false);
      }
    });
  }

  submit(): void {
    if (!this.form.valid) return;

    this.isSaving.set(true);
    this.error.set(null);

    const formData = this.form.value;
    const payload = {
      nombre: formData.nombre,
      brandId: parseInt(formData.brandId),
      precio: parseFloat(formData.precio),
      stock: parseInt(formData.stock),
      descripcion: formData.descripcion || undefined,
      procesador: formData.procesador || undefined,
      ram: formData.ram ? parseInt(formData.ram) : undefined,
      almacenamiento: formData.almacenamiento ? parseInt(formData.almacenamiento) : undefined,
      pantalla: formData.pantalla || undefined,
      gpu: formData.gpu || undefined,
      peso: formData.peso ? parseFloat(formData.peso) : undefined
    };

    if (this.isEditMode() && this.productId) {
      this.productService.updateProduct(this.productId, payload).subscribe({
        next: () => {
          this.isSaving.set(false);
          this.success.set('✅ Producto actualizado correctamente');
          // Sincronizar imágenes después de actualizar
          if (this.productImages().length > 0) {
            this.syncImages();
          } else {
            // Esperar a que se vea la notificación antes de navegar
            setTimeout(() => this.router.navigate(['/admin/products']), 2000);
          }
        },
        error: (err) => {
          this.isSaving.set(false);
          this.error.set('❌ Error al actualizar: ' + (err.error?.message || 'Error desconocido'));
          setTimeout(() => this.error.set(null), 4000);
        }
      });
    } else {
      // Crear producto
      this.productService.createProduct(payload).subscribe({
        next: (createdProduct) => {
          this.isSaving.set(false);
          this.success.set('✅ Producto creado correctamente');
          this.productId = createdProduct.id;
          this.isEditMode.set(true);
          // Sincronizar imágenes que fueron agregadas antes de crear el producto
          if (this.productImages().length > 0) {
            this.syncImages();
          } else {
            // Esperar a que se vea la notificación antes de navegar
            setTimeout(() => this.router.navigate(['/admin/products']), 2000);
          }
        },
        error: (err) => {
          this.isSaving.set(false);
          this.error.set('❌ Error al crear: ' + (err.error?.message || 'Error desconocido'));
          setTimeout(() => this.error.set(null), 4000);
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
      // Calcular el orden inicial
      let currentMaxOrden = this.productImages().length > 0 
        ? Math.max(...this.productImages().map(img => img.orden))
        : 0;
      
      // Procesar múltiples archivos con órdenes incrementales
      Array.from(files).forEach((file, index) => {
        const orden = currentMaxOrden + index + 1;
        this.uploadImage(file, orden);
      });
    }
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files?.length) {
      // Calcular el orden inicial
      let currentMaxOrden = this.productImages().length > 0 
        ? Math.max(...this.productImages().map(img => img.orden))
        : 0;
      
      // Procesar múltiples archivos con órdenes incrementales
      Array.from(input.files).forEach((file, index) => {
        const orden = currentMaxOrden + index + 1;
        this.uploadImage(file, orden);
      });
      
      // Limpiar el input para poder seleccionar las mismas imágenes de nuevo
      input.value = '';
    }
  }

  private uploadImage(file: File, orden?: number): void {
    this.error.set(null);

    // Si no se proporciona orden, calcularla
    if (orden === undefined) {
      const maxOrden = this.productImages().length > 0 
        ? Math.max(...this.productImages().map(img => img.orden)) 
        : 0;
      orden = maxOrden + 1;
    }

    // Si tenemos productId, subir inmediatamente al backend
    if (this.productId) {
      this.isUploadingImage.set(true);
      this.productService.uploadImage(this.productId, file, orden).subscribe({
        next: (image) => {
          // Actualizar UI solo cuando el backend confirme
          this.productImages.update(images => [...images, image]);
          this.isUploadingImage.set(false);
        },
        error: (err) => {
          this.error.set('Error al subir imagen: ' + (err.error?.message || 'Error desconocido'));
          this.isUploadingImage.set(false);
        }
      });
    } else {
      // Sin productId, agregar imagen localmente
      // Crear un objeto temporal con los datos del archivo
      const tempImage: Partial<ProductImage> = {
        id: undefined,
        url: URL.createObjectURL(file),
        orden: orden,
        descripcion: '',
        file: file as any // Almacenar el archivo para subirlo después
      } as any;
      
      this.productImages.update(images => [...images, tempImage as ProductImage]);
    }
  }

  private syncImages(): void {
    if (!this.productId) {
      this.isSaving.set(false);
      this.router.navigate(['/admin/products']);
      return;
    }

    const images = this.productImages();
    const newImages = images.filter((img: any) => !img.id); // Imágenes sin ID son nuevas
    const existingImages = images.filter(img => img.id); // Imágenes con ID ya existen

    // Si no hay imágenes de ningún tipo, terminar
    if (newImages.length === 0 && existingImages.length === 0) {
      this.isSaving.set(false);
      // Esperar a que se vea la notificación
      setTimeout(() => this.router.navigate(['/admin/products']), 2000);
      return;
    }

    // Contador para uploads
    let pendingUploads = newImages.length;
    
    // Fallback: si no hay uploads nuevos, terminar inmediatamente
    if (pendingUploads === 0) {
      this.isSaving.set(false);
      // Esperar a que se vea la notificación
      setTimeout(() => this.router.navigate(['/admin/products']), 2000);
      return;
    }

    // Helper para finalizar
    const finalizeSyncImages = () => {
      this.isSaving.set(false);
      // Esperar a que se vea la notificación
      setTimeout(() => this.router.navigate(['/admin/products']), 2000);
    };

    // Actualizar órdenes de imágenes existentes (no esperar)
    existingImages.forEach((img, index) => {
      const newOrden = index + 1 + newImages.length;
      if (img.orden !== newOrden) {
        this.productService.updateImage(img.id, { orden: newOrden }).subscribe({
          next: () => {
            img.orden = newOrden;
          },
          error: (err) => {
          }
        });
      }
    });

    // Subir imágenes nuevas
    newImages.forEach((img: any, index) => {
      const newOrden = index + 1;
      this.productService.uploadImage(this.productId!, img.file, newOrden).subscribe({
        next: (uploadedImage) => {
          // Reemplazar imagen temporal con la del backend
          const imgIndex = images.findIndex(i => i === img);
          if (imgIndex >= 0) {
            images[imgIndex] = uploadedImage;
          }
          pendingUploads--;
          if (pendingUploads === 0) {
            this.productImages.set([...images]);
            finalizeSyncImages();
          }
        },
        error: (err) => {
          pendingUploads--;
          if (pendingUploads === 0) {
            finalizeSyncImages();
          }
        }
      });
    });
  }

  deleteImage(imageId: number | undefined, index: number): void {
    this.showConfirm.set({
      message: '¿Estás seguro de que deseas eliminar esta imagen?',
      onConfirm: () => {
        // proceed with deletion (closure captures imageId and index)
        const images = this.productImages();

        // Si la imagen no tiene ID, es local - solo borrarla del array
        if (!imageId) {
          this.productImages.set(images.filter((_, i) => i !== index));
          return;
        }

        // Si tiene ID, eliminar del backend primero
        this.productService.deleteImage(imageId).subscribe({
          next: () => {
            const updatedImages = images.filter(img => img.id !== imageId);
            
            // Recalcular órdenes solo para imágenes del backend
            const updateOperations: any[] = [];
            updatedImages.forEach((img, idx) => {
              if (img.id) { // Solo actualizar imágenes existentes en el backend
                const newOrden = idx + 1;
                img.orden = newOrden;
                updateOperations.push(
                  new Promise(resolve => {
                    this.productService.updateImage(img.id, { orden: newOrden }).subscribe({
                      next: () => {
                        resolve(null);
                      },
                      error: (err) => {
                        resolve(null);
                      }
                    });
                  })
                );
              }
            });
            
            // Actualizar cuando todos los reorderings completen
            if (updateOperations.length > 0) {
              Promise.all(updateOperations).then(() => {
                this.productImages.set(updatedImages);
              });
            } else {
              this.productImages.set(updatedImages);
            }
          },
          error: (err) => {
            this.error.set('Error al eliminar imagen: ' + (err.error?.message || 'Error desconocido'));
          }
        });
      }
    });

  }

  moveImageUp(index: number): void {
    if (index <= 0) return;
    
    const images = [...this.productImages()];
    // Intercambiar posiciones en el array
    [images[index - 1], images[index]] = [images[index], images[index - 1]];
    
    // Si todas son locales (sin ID), solo reordenar localmente
    const backendImages = images.filter(img => img.id);
    if (backendImages.length === 0) {
      this.productImages.set(images);
      return;
    }
    
    // Si hay imágenes del backend, actualizar órdenes
    const updateOperations: any[] = [];
    images.forEach((img, idx) => {
      if (img.id) { // Solo actualizar imágenes del backend
        const newOrden = idx + 1;
        updateOperations.push(
          new Promise(resolve => {
            this.productService.updateImage(img.id, { orden: newOrden }).subscribe({
              next: () => {
                img.orden = newOrden;
                resolve(null);
              },
              error: (err) => {
                resolve(null);
              }
            });
          })
        );
      } else {
        // Imagen local, solo actualizar orden localmente
        img.orden = idx + 1;
      }
    });
    
    // Cuando TODOS los updates completen, actualizar el array local
    Promise.all(updateOperations).then(() => {
      this.productImages.set(images);
    });
  }

  moveImageDown(index: number): void {
    if (index >= this.productImages().length - 1) return;
    
    const images = [...this.productImages()];
    // Intercambiar posiciones en el array
    [images[index], images[index + 1]] = [images[index + 1], images[index]];
    
    // Si todas son locales (sin ID), solo reordenar localmente
    const backendImages = images.filter(img => img.id);
    if (backendImages.length === 0) {
      this.productImages.set(images);
      return;
    }
    
    // Si hay imágenes del backend, actualizar órdenes
    const updateOperations: any[] = [];
    images.forEach((img, idx) => {
      if (img.id) { // Solo actualizar imágenes del backend
        const newOrden = idx + 1;
        updateOperations.push(
          new Promise(resolve => {
            this.productService.updateImage(img.id, { orden: newOrden }).subscribe({
              next: () => {
                img.orden = newOrden;
                resolve(null);
              },
              error: (err) => {
                resolve(null);
              }
            });
          })
        );
      } else {
        // Imagen local, solo actualizar orden localmente
        img.orden = idx + 1;
      }
    });
    
    // Cuando TODOS los updates completen, actualizar el array local
    Promise.all(updateOperations).then(() => {
      this.productImages.set(images);
    });
  }
}
