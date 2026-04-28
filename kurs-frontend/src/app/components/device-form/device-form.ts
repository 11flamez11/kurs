import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { DeviceService } from '../../services/device.service';
import { AuthService } from '../../services/auth.service';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { Device } from '../../models/device';

@Component({
  selector: 'app-device-form',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './device-form.html',
  styleUrls: ['./device-form.css'],
})
export class DeviceForm implements OnInit {
  device: Partial<Device> = {
    hostname: '',
    ipAddress: '',
    os: '',
    status: 'ONLINE',
  };

  isEditMode = false;
  deviceId: number | null = null;
  isLoading = false;
  errorMessage = '';

  constructor(
    private deviceService: DeviceService,
    private auth: AuthService,
    private router: Router,
    private route: ActivatedRoute,
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.isEditMode = true;
      this.deviceId = +id;
      this.loadDevice(this.deviceId);
    }
  }

  loadDevice(id: number): void {
    this.isLoading = true;
    this.deviceService.getById(id).subscribe({
      next: (data) => {
        this.device = { ...data };
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Ошибка загрузки устройства:', error);
        this.errorMessage = 'Не удалось загрузить устройство';
        this.isLoading = false;
      },
    });
  }

  onSubmit(): void {
    if (!this.device.hostname || !this.device.ipAddress) {
      this.errorMessage = 'Hostname и IP адрес обязательны';
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';

    if (this.isEditMode && this.deviceId) {
      this.deviceService.update(this.deviceId, this.device).subscribe({
        next: () => {
          this.isLoading = false;
          this.router.navigate(['/devices', this.deviceId]);
        },
        error: (error) => {
          console.error('Ошибка обновления:', error);
          this.errorMessage = 'Не удалось обновить устройство';
          this.isLoading = false;
        },
      });
    } else {
      this.deviceService.create(this.device).subscribe({
        next: (created) => {
          this.isLoading = false;
          this.router.navigate(['/devices', created.id]);
        },
        error: (error) => {
          console.error('Ошибка создания:', error);
          this.errorMessage = 'Не удалось создать устройство';
          this.isLoading = false;
        },
      });
    }
  }

  onCancel(): void {
    if (this.isEditMode && this.deviceId) {
      this.router.navigate(['/devices', this.deviceId]);
    } else {
      this.router.navigate(['/devices']);
    }
  }
}
