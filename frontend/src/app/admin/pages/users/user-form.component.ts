import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { UserService, UserCreateDTO } from '../../../services/user.service';

@Component({
  selector: 'app-admin-user-form',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  templateUrl: './user-form.component.html',
  styles: []
})
export class UserFormComponent {
  isSaving = signal(false);
  error = signal<string | null>(null);

  form: UserCreateDTO = {
    email: '',
    password: '',
    nombre: '',
    apellido: '',
    telefono: '',
    direccion: ''
  };

  constructor(
    private userService: UserService,
    private router: Router
  ) {}

  save(): void {
    if (!this.form.email || !this.form.password || !this.form.nombre || !this.form.apellido) {
      this.error.set('Email, contraseña, nombre y apellido son obligatorios');
      return;
    }
    this.isSaving.set(true);
    this.error.set(null);
    this.userService.createUser(this.form).subscribe({
      next: () => {
        this.router.navigate(['/admin/users']);
      },
      error: (err) => {
        this.error.set('Error al crear usuario: ' + (err?.error?.message || err?.message || 'Error desconocido'));
        this.isSaving.set(false);
      }
    });
  }
}
