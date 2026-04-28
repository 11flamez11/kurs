import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule, ActivatedRoute } from '@angular/router';
import { NetworkInterface } from '../../models/interface';
import { InterfaceService } from '../../services/interface.service';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../services/auth.service';
import { Navbar } from '../navbar/navbar';

@Component({
  selector: 'app-interfaces',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, Navbar],
  templateUrl: './interfaces.html',
  styleUrls: ['./interfaces.css'],
})
export class Interfaces implements OnInit {
  interfaces: NetworkInterface[] = [];
  isLoading = true;
  isAdmin = false;
  showForm = false;
  isEditing = false;
  editingId: number | null = null;
  deviceId: number | null = null;
  form: Partial<NetworkInterface> = {
    deviceId: undefined,
    name: '',
    macAddress: '',
    speed: 1000,
    status: 'UP',
  };

  constructor(
    private interfaceService: InterfaceService,
    private auth: AuthService,
    private router: Router,
    private route: ActivatedRoute,
  ) {}

  ngOnInit(): void {
    this.isAdmin = this.auth.isAdmin();

    this.route.queryParams.subscribe((params) => {
      this.deviceId = params['deviceId'] ? +params['deviceId'] : null;
      this.loadInterfaces();
    });
  }

  loadInterfaces(): void {
    this.isLoading = true;
    const request = this.deviceId
      ? this.interfaceService.getByDevice(this.deviceId)
      : this.interfaceService.getAll();
    request.subscribe({
      next: (data) => {
        this.interfaces = data;
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
      },
    });
  }

  openCreateForm(): void {
    this.showForm = true;
    this.isEditing = false;
    this.editingId = null;
    this.form = {
      deviceId: this.deviceId || undefined,
      name: '',
      macAddress: '',
      speed: 1000,
      status: 'UP',
    };
  }

  openEditForm(iface: NetworkInterface): void {
    this.showForm = true;
    this.isEditing = true;
    this.editingId = iface.id;
    this.form = { ...iface };
  }

  closeForm(): void {
    this.showForm = false;
    this.form = {
      deviceId: undefined,
      name: '',
      macAddress: '',
      speed: 1000,
      status: 'UP',
    };
  }

  submitForm(): void {
    if (!this.form.deviceId || !this.form.name) {
      alert('Заполните обязательные поля');
      return;
    }

    if (this.isEditing && this.editingId) {
      this.interfaceService.update(this.editingId, this.form).subscribe({
        next: () => {
          this.closeForm();
          this.loadInterfaces();
        },
        error: () => alert('Ошибка обновления'),
      });
    } else {
      this.interfaceService.create(this.form).subscribe({
        next: () => {
          this.closeForm();
          this.loadInterfaces();
        },
        error: () => alert('Ошибка создания'),
      });
    }
  }

  deleteInterface(id: number): void {
    if (!confirm('Удалить интерфейс?')) return;
    this.interfaceService.delete(id).subscribe({
      next: () => {
        this.interfaces = this.interfaces.filter((i) => i.id !== id);
      },
      error: () => alert('Не удалось удалить'),
    });
  }
}
