import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { Metric } from '../../models/metric';
import { MetricService } from '../../services/metric.service';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../services/auth.service';
import { Navbar } from '../navbar/navbar';

@Component({
  selector: 'app-metrics',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, Navbar],
  templateUrl: './metrics.html',
  styleUrls: ['./metrics.css'],
})
export class Metrics implements OnInit {
  metrics: Metric[] = [];
  isLoading = true;
  isAdmin = false;
  showForm = false;
  isEditing = false;
  editingId: number | null = null;
  deviceId: number | null = null;
  form: Partial<Metric> = {
    deviceId: undefined,
    cpuLoad: 0,
    ramUsage: 0,
    diskUsage: 0,
  };

  constructor(
    private metricService: MetricService,
    private auth: AuthService,
    private router: Router,
    private route: ActivatedRoute,
  ) {}

  ngOnInit(): void {
    this.isAdmin = this.auth.isAdmin();
    this.route.queryParams.subscribe((params) => {
      this.deviceId = params['deviceId'] ? +params['deviceId'] : null;
      this.loadMetrics();
    });
  }

  loadMetrics(): void {
    this.isLoading = true;
    const request = this.deviceId
      ? this.metricService.getByDevice(this.deviceId)
      : this.metricService.getAll();
    request.subscribe({
      next: (data) => {
        this.metrics = data;
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
      cpuLoad: 0,
      ramUsage: 0,
      diskUsage: 0,
    };
  }

  openEditForm(metric: Metric): void {
    this.showForm = true;
    this.isEditing = true;
    this.editingId = metric.id;
    this.form = { ...metric };
  }

  closeForm(): void {
    this.showForm = false;
    this.form = {};
  }

  submitForm(): void {
    if (!this.form.deviceId) {
      alert('Укажите ID устройства');
      return;
    }

    if (this.isEditing && this.editingId) {
      this.metricService.update(this.editingId, this.form).subscribe({
        next: () => {
          this.closeForm();
          this.loadMetrics();
        },
        error: () => alert('Ошибка обновления'),
      });
    } else {
      this.metricService.create(this.form).subscribe({
        next: () => {
          this.closeForm();
          this.loadMetrics();
        },
        error: () => alert('Ошибка создания'),
      });
    }
  }

  deleteMetric(id: number): void {
    if (!confirm('Удалить метрику?')) return;
    this.metricService.delete(id).subscribe({
      next: () => {
        this.metrics = this.metrics.filter((m) => m.id !== id);
      },
      error: () => alert('Не удалось удалить'),
    });
  }
}
