import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute } from '@angular/router';
import { NetworkInterface } from '../../models/interface';
import { InterfaceService } from '../../services/interface.service';
import { AuthService } from '../../services/auth.service';
import { Navbar } from '../navbar/navbar';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  selector: 'app-interfaces',
  standalone: true,
  imports: [CommonModule, RouterModule, Navbar],
  templateUrl: './interfaces.html',
  styleUrls: ['./interfaces.css'],
})
export class Interfaces implements OnInit {
  interfaces: NetworkInterface[] = [];
  isLoading = true;
  isRegistering = false;
  isAdmin = false;
  deviceId: number | null = null;
  expandedGroupKey: string | null = null;

  constructor(
    private interfaceService: InterfaceService,
    private auth: AuthService,
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
        this.interfaces = data
          .slice()
          .sort((a, b) => {
            const d = (a.deviceId ?? 0) - (b.deviceId ?? 0);
            if (d !== 0) return d;
            return a.name.localeCompare(b.name);
          });
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
      },
    });
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

  registerLocalInterfaces(): void {
    if (!this.deviceId) {
      alert('Откройте интерфейсы конкретного устройства с дашборда.');
      return;
    }
    this.isRegistering = true;
    this.interfaceService.registerLocal(this.deviceId).subscribe({
      next: () => {
        this.isRegistering = false;
        this.loadInterfaces();
      },
      error: (error: HttpErrorResponse) => {
        this.isRegistering = false;
        const backendMessage =
          typeof error.error === 'string' && error.error.trim().length > 0
            ? error.error
            : 'Не удалось автоматически добавить интерфейсы';
        alert(backendMessage);
      },
    });
  }

  get groups(): Array<{
    key: string;
    title: string;
    items: NetworkInterface[];
    upCount: number;
    downCount: number;
  }> {
    const map = new Map<number, NetworkInterface[]>();
    for (const iface of this.interfaces) {
      const key = iface.deviceId ?? 0;
      if (!map.has(key)) {
        map.set(key, []);
      }
      map.get(key)!.push(iface);
    }

    return Array.from(map.entries()).map(([device, items]) => ({
      key: `device-${device}`,
      title: device > 0 ? `Устройство #${device}` : 'Без устройства',
      items,
      upCount: items.filter((i) => i.status === 'UP').length,
      downCount: items.filter((i) => i.status === 'DOWN').length,
    }));
  }

  isGroupExpanded(key: string): boolean {
    return this.expandedGroupKey === key;
  }

  toggleGroup(key: string): void {
    this.expandedGroupKey = this.expandedGroupKey === key ? null : key;
  }
}
