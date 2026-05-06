import { Component, OnInit, HostListener } from '@angular/core'; // Добавьте HostListener
import { Router } from '@angular/router';
import { DeviceService } from '../../services/device.service';
import { AuthService } from '../../services/auth.service';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { Device } from '../../models/device';
import { Navbar } from '../navbar/navbar';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule, Navbar],
  templateUrl: './dashboard.html',
  styleUrls: ['./dashboard.css'],
})
export class Dashboard implements OnInit {
  devices: Device[] = [];
  username = '';
  isAdmin = false;
  isLoading = true;
  activeDevice: Device | null = null;

  constructor(
    private deviceService: DeviceService,
    private auth: AuthService,
    private router: Router,
  ) {}

  ngOnInit(): void {
    const user = this.auth.getCurrentUser();
    if (user) {
      this.username = user.username;
      this.isAdmin = user.roles.includes('ROLE_ADMIN') || user.roles.includes('ADMIN');
    }

    this.loadDevices();
  }

  loadDevices(): void {
    this.isLoading = true;
    this.deviceService.getAll().subscribe({
      next: (data) => {
        this.devices = data;
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Ошибка загрузки устройств:', error);
        this.isLoading = false;
      },
    });
  }

  deleteDevice(id: number): void {
    if (!confirm('Удалить это устройство?')) {
      return;
    }

    this.deviceService.delete(id).subscribe({
      next: () => {
        this.devices = this.devices.filter((d) => d.id !== id);
      },
      error: (error) => {
        console.error('Ошибка удаления:', error);
        alert('Не удалось удалить устройство');
      },
    });
  }

  goEdit(id: number) {
    this.router.navigate(['/devices', id, 'edit']);
  }

  toggleMenu(event: Event, device: Device): void {
    event.stopPropagation();
    this.activeDevice = this.activeDevice === device ? null : device;
  }

  @HostListener('document:click')
  onClickOutside(): void {
    this.activeDevice = null;
  }

  goToMetrics(deviceId: number): void {
    this.router.navigate(['/metrics'], { queryParams: { deviceId } });
  }

  goToEvents(deviceId: number): void {
    this.router.navigate(['/events'], { queryParams: { deviceId } });
  }

  goToInterfaces(deviceId: number): void {
    this.router.navigate(['/interfaces'], { queryParams: { deviceId } });
  }

  addCurrentDevice(): void {
    this.deviceService.registerLocal().subscribe({
      next: () => this.loadDevices(),
      error: (error) => {
        console.error('Ошибка авторегистрации устройства:', error);
        alert('Не удалось добавить текущее устройство автоматически');
      },
    });
  }
}
