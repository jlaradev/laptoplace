import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { UserService, UserResponseDTO, UserUpdateDTO } from '../../../services/user.service';

@Component({
  selector: 'app-admin-user-detail',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  templateUrl: './user-detail.component.html',
  styles: []
})
export class UserDetailComponent implements OnInit {
  user = signal<UserResponseDTO | null>(null);
  isLoading = signal(false);
  isSaving = signal(false);
  error = signal<string | null>(null);
  saveSuccess = signal(false);
  userId = '';

  editForm: UserUpdateDTO = {
    nombre: '',
    apellido: '',
    telefono: '',
    direccion: ''
  };

  constructor(
    private route: ActivatedRoute,
    private userService: UserService
  ) {}

  ngOnInit(): void {
    this.route.params.subscribe(params => {
      this.userId = params['id'];
      this.loadUser();
    });
  }

  loadUser(): void {
    this.isLoading.set(true);
    this.error.set(null);
    this.userService.getUserById(this.userId).subscribe({
      next: (user: UserResponseDTO) => {
        this.user.set(user);
        this.editForm = {
          nombre: user.nombre,
          apellido: user.apellido,
          telefono: user.telefono || '',
          direccion: user.direccion || ''
        };
        this.isLoading.set(false);
      },
      error: (err) => {
        this.error.set('Error al cargar usuario: ' + (err?.message || 'Error desconocido'));
        this.isLoading.set(false);
      }
    });
  }

  save(): void {
    this.isSaving.set(true);
    this.saveSuccess.set(false);
    this.error.set(null);
    this.userService.updateUser(this.userId, this.editForm).subscribe({
      next: (updated: UserResponseDTO) => {
        this.user.set(updated);
        this.isSaving.set(false);
        this.saveSuccess.set(true);
        setTimeout(() => this.saveSuccess.set(false), 3000);
      },
      error: (err) => {
        this.error.set('Error al guardar: ' + (err?.message || 'Error desconocido'));
        this.isSaving.set(false);
      }
    });
  }
}
