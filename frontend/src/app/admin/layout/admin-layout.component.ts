import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet } from '@angular/router';
import { AdminSidebarComponent } from './admin-sidebar.component';
import { AdminHeaderComponent } from './admin-header.component';

@Component({
  selector: 'app-admin-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, AdminSidebarComponent, AdminHeaderComponent],
  template: `
    <div class="flex min-h-screen bg-gray-50">
      <!-- Sidebar -->
      <app-admin-sidebar [collapsed]="sidebarCollapsed()"></app-admin-sidebar>

      <!-- Main Content -->
      <div class="flex-1 flex flex-col">
        <!-- Header -->
        <app-admin-header 
          (toggleSidebar)="sidebarCollapsed.set(!sidebarCollapsed())"
        ></app-admin-header>

        <!-- Page Content -->
        <main class="flex-1 overflow-auto">
          <div class="max-w-[1440px] mx-auto p-6">
            <router-outlet></router-outlet>
          </div>
        </main>
      </div>
    </div>
  `,
  styles: []
})
export class AdminLayoutComponent {
  sidebarCollapsed = signal(false);
}
