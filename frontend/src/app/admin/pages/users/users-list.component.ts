import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { UserService, UserPage, UserResponseDTO } from '../../../services/user.service';
import { AdminComponentsModule } from '../../components/admin-components.module';
import { AuthService } from '../../../services/auth.service';

@Component({
  selector: 'app-admin-users-list',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule, AdminComponentsModule],
  templateUrl: './users-list.component.html',
  styles: []
})
export class UsersListComponent implements OnInit {
  users = signal<UserResponseDTO[]>([]);
  isLoading = signal(false);
  error = signal<string | null>(null);
  currentPage = signal(0);
  totalPages = signal(0);
  totalElements = signal(0);
  searchEmail = '';

  private pageSize = 20;

  constructor(private userService: UserService, private authService: AuthService) {}

  ngOnInit(): void {
    this.currentUserId = this.authService.getUserId();
    this.loadUsers();
  }

    // Estado para eliminados (modal)
    inactiveUsers = signal<UserResponseDTO[]>([]);
    inactiveLoading = signal(false);
    inactiveError = signal<string | null>(null);
    showEliminadosModal = signal(false);
    showAlert = signal<{ message: string, type: 'success' | 'error' } | null>(null);
    showConfirm = signal<{ message: string, onConfirm: () => void } | null>(null);
    currentUserId: string | null = null;

    openEliminadosModal(): void {
      this.showEliminadosModal.set(true);
      this.loadInactiveUsers();
    }

    closeEliminadosModal(): void {
      this.showEliminadosModal.set(false);
      this.loadUsers(this.currentPage());
    }

    loadInactiveUsers(page: number = 0): void {
      this.inactiveLoading.set(true);
      this.inactiveError.set(null);
      this.userService.getInactiveUsers(page, this.pageSize).subscribe({
        next: (response: UserPage) => {
          this.inactiveUsers.set(response.content);
          this.inactiveLoading.set(false);
        },
        error: (err) => {
          this.inactiveError.set('Error al cargar usuarios inactivos: ' + (err?.message || 'Error desconocido'));
          this.inactiveLoading.set(false);
        }
      });
    }

    deactivateUser(id: string): void {
      if (id === this.currentUserId) {
        this.showAlert.set({ message: 'No puedes desactivarte a ti mismo', type: 'error' });
        return;
      }
      this.showConfirm.set({
        message: '¿Seguro que quieres desactivar este usuario?',
        onConfirm: () => {
          this.userService.deactivateUser(id).subscribe({
            next: () => {
              this.loadUsers(this.currentPage());
              this.showAlert.set({ message: 'Usuario desactivado correctamente', type: 'success' });
            },
            error: () => {
              this.showAlert.set({ message: 'Error al desactivar usuario', type: 'error' });
            }
          });
        }
      });
    }

    reactivateUser(id: string): void {
      this.userService.reactivateUser(id).subscribe({
        next: () => {
          this.loadInactiveUsers();
          this.showAlert.set({ message: 'Usuario reactivado correctamente', type: 'success' });
        },
        error: () => {
          this.showAlert.set({ message: 'Error al reactivar usuario', type: 'error' });
        }
      });
    }

    changeRole(user: UserResponseDTO): void {
      if (user.id === this.currentUserId) {
        this.showAlert.set({ message: 'No puedes cambiar tu propio rol', type: 'error' });
        return;
      }
      const targetRole = user.role === 'ADMIN' ? 'USER' : 'ADMIN';
      this.showConfirm.set({
        message: `¿Cambiar rol de ${user.email} a ${targetRole}?`,
        onConfirm: () => {
          this.userService.changeUserRole(user.id, targetRole as 'USER' | 'ADMIN').subscribe({
            next: (updated) => {
              this.showAlert.set({ message: `Rol cambiado a ${updated.role}`, type: 'success' });
              this.loadUsers(this.currentPage());
            },
            error: () => {
              this.showAlert.set({ message: 'Error al cambiar rol', type: 'error' });
            }
          });
        }
      });
    }

  loadUsers(page: number = 0): void {
    this.isLoading.set(true);
    this.error.set(null);
    this.userService.getUsers(page, this.pageSize).subscribe({
      next: (response: UserPage) => {
        this.users.set(response.content);
        this.currentPage.set(response.number);
        this.totalPages.set(response.totalPages);
        this.totalElements.set(response.totalElements);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.error.set('Error al cargar usuarios: ' + (err?.message || 'Error desconocido'));
        this.isLoading.set(false);
      }
    });
  }

  searchByEmail(): void {
    if (!this.searchEmail.trim()) {
      this.loadUsers(0);
      return;
    }
    this.isLoading.set(true);
    this.error.set(null);
    this.userService.getUserByEmail(this.searchEmail.trim()).subscribe({
      next: (user: UserResponseDTO) => {
        this.users.set([user]);
        this.currentPage.set(0);
        this.totalPages.set(1);
        this.totalElements.set(1);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.error.set('Usuario no encontrado');
        this.users.set([]);
        this.isLoading.set(false);
      }
    });
  }

  reset(): void {
    this.searchEmail = '';
    this.loadUsers(0);
  }

  previousPage(): void {
    if (this.currentPage() > 0) {
      this.loadUsers(this.currentPage() - 1);
    }
  }

  nextPage(): void {
    if (this.currentPage() < this.totalPages() - 1) {
      this.loadUsers(this.currentPage() + 1);
    }
  }

  truncateId(id: string): string {
    return id.substring(0, 8) + '...';
  }
}
