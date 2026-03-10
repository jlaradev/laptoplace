import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { UserResponseDTO } from '../../services/user.service';

@Component({
  selector: 'app-users-eliminados-modal',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-40">
      <div class="bg-white rounded-lg shadow-lg w-full max-w-3xl p-6 relative">
        <button (click)="close()" class="absolute top-4 right-4 text-gray-500 hover:text-gray-700 text-2xl font-bold">×</button>
        <h2 class="text-2xl font-bold mb-4">Usuarios Eliminados</h2>
        <ng-container *ngIf="users && users.length > 0; else empty">
          <table class="w-full mb-4">
            <thead>
              <tr>
                <th>ID</th>
                <th>Nombre</th>
                <th>Email</th>
                <th>Registro</th>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let user of users">
                <td class="font-mono text-sm">{{ user.id.substring(0,8) }}...</td>
                <td>{{ user.nombre }} {{ user.apellido }}</td>
                <td>{{ user.email }}</td>
                <td>{{ user.createdAt | date:'dd/MM/yyyy' }}</td>
                <td>
                  <button (click)="reactivate.emit(user.id)" class="px-3 py-1 bg-green-100 text-green-700 rounded hover:bg-green-200 transition">♻️ Reactivar</button>
                </td>
              </tr>
            </tbody>
          </table>
        </ng-container>
        <ng-template #empty>
          <div class="text-center text-gray-600 py-8">No hay usuarios eliminados</div>
        </ng-template>
      </div>
    </div>
  `,
  styles: []
})
export class UsersEliminadosModalComponent {
  @Input() users: UserResponseDTO[] = [];
  @Output() closeModal = new EventEmitter<void>();
  @Output() reactivate = new EventEmitter<string>();

  close() {
    this.closeModal.emit();
  }
}
