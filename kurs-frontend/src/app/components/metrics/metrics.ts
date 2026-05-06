import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { Subscription, interval } from 'rxjs';
import { BaseChartDirective } from 'ng2-charts';
import { ChartConfiguration } from 'chart.js';
import { Metric } from '../../models/metric';
import { MetricService } from '../../services/metric.service';
import { AuthService } from '../../services/auth.service';
import { Navbar } from '../navbar/navbar';

@Component({
  selector: 'app-metrics',
  standalone: true,
  imports: [CommonModule, RouterModule, Navbar, BaseChartDirective],
  templateUrl: './metrics.html',
  styleUrls: ['./metrics.css'],
})
export class Metrics implements OnInit, OnDestroy {
  metrics: Metric[] = [];
  liveMetrics: Metric[] = [];
  latestMetric: Metric | null = null;

  isLoading = true;
  isAdmin = false;
  deviceId: number | null = null;

  monitoringActive = false;
  requestInFlight = false;
  collectIntervalSeconds = 2;
  private monitoringSub?: Subscription;

  trafficChartData: ChartConfiguration<'line'>['data'] = {
    labels: [],
    datasets: [
      {
        data: [],
        label: 'Входящий Mbps',
        borderColor: '#42a5f5',
        backgroundColor: 'rgba(66,165,245,0.2)',
        tension: 0.25,
      },
      {
        data: [],
        label: 'Исходящий Mbps',
        borderColor: '#ab47bc',
        backgroundColor: 'rgba(171,71,188,0.2)',
        tension: 0.25,
      },
    ],
  };

  trafficChartOptions: ChartConfiguration<'line'>['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        labels: { color: '#475569', font: { size: 13 } },
      },
    },
    scales: {
      x: {
        ticks: { color: '#64748b', maxRotation: 45 },
        grid: { color: 'rgba(148, 163, 184, 0.25)' },
      },
      y: {
        ticks: { color: '#64748b' },
        grid: { color: 'rgba(148, 163, 184, 0.25)' },
      },
    },
  };

  constructor(
    private metricService: MetricService,
    private auth: AuthService,
    private route: ActivatedRoute,
  ) {}

  ngOnInit(): void {
    this.isAdmin = this.auth.isAdmin();
    this.route.queryParams.subscribe((params) => {
      this.deviceId = params['deviceId'] ? +params['deviceId'] : null;
      this.stopMonitoring();
      this.loadMetrics();
    });
  }

  ngOnDestroy(): void {
    this.stopMonitoring();
  }

  loadMetrics(): void {
    this.isLoading = true;
    const request = this.deviceId ? this.metricService.getByDevice(this.deviceId) : this.metricService.getAll();
    request.subscribe({
      next: (data) => {
        this.metrics = data
          .slice()
          .sort((a, b) => new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime());
        this.updateLatestAndChart();
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
      },
    });
  }

  startMonitoring(): void {
    if (!this.deviceId || this.monitoringActive) return;
    this.monitoringActive = true;
    this.pollLiveMetric();
    const ms = Math.max(1, this.collectIntervalSeconds) * 1000;
    this.monitoringSub = interval(ms).subscribe(() => this.pollLiveMetric());
  }

  stopMonitoring(): void {
    this.monitoringActive = false;
    this.requestInFlight = false;
    if (this.monitoringSub) {
      this.monitoringSub.unsubscribe();
      this.monitoringSub = undefined;
    }
  }

  onIntervalChange(event: Event): void {
    const target = event.target as HTMLInputElement;
    const parsed = Number(target.value);
    this.collectIntervalSeconds = Number.isFinite(parsed) ? Math.max(1, Math.min(30, parsed)) : 1;
    if (this.monitoringActive) {
      this.stopMonitoring();
      this.startMonitoring();
    }
  }

  private pollLiveMetric(): void {
    if (!this.deviceId || this.requestInFlight) return;
    this.requestInFlight = true;
    this.metricService.collectForDevice(this.deviceId, 1, 1, false).subscribe({
      next: (data) => {
        const metric = data?.[0];
        if (metric) {
          this.liveMetrics.push(metric);
          if (this.liveMetrics.length > 120) {
            this.liveMetrics = this.liveMetrics.slice(-120);
          }
          this.updateLatestAndChart();
        }
        this.requestInFlight = false;
      },
      error: () => {
        this.requestInFlight = false;
        this.stopMonitoring();
      },
    });
  }

  private updateLatestAndChart(): void {
    const merged = [...this.metrics, ...this.liveMetrics]
      .sort((a, b) => new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime());
    this.latestMetric = merged.length ? merged[merged.length - 1] : null;
    const trafficSource = merged.slice(-40);
    this.trafficChartData = {
      labels: trafficSource.map((m) =>
        new Date(m.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' }),
      ),
      datasets: [
        { ...this.trafficChartData.datasets[0], data: trafficSource.map((m) => this.num(m.downloadMbps)) },
        { ...this.trafficChartData.datasets[1], data: trafficSource.map((m) => this.num(m.uploadMbps)) },
      ],
    };
  }

  deleteMetric(id: number): void {
    if (!confirm('Удалить метрику?')) return;
    this.metricService.delete(id).subscribe({
      next: () => {
        this.metrics = this.metrics.filter((m) => m.id !== id);
        this.updateLatestAndChart();
      },
    });
  }

  // View helpers
  num(value?: number | null): number {
    if (value === undefined || value === null || Number.isNaN(value)) return 0;
    return value;
  }

  pct(value?: number | null): string {
    if (value === undefined || value === null || value < 0) return 'n/a';
    return `${value.toFixed(value < 10 ? 1 : 0)}%`;
  }

  mbps(value?: number | null): string {
    if (value === undefined || value === null || value < 0) return 'n/a';
    return `${value.toFixed(value < 10 ? 2 : 1)} Mbps`;
  }

  ping(): string {
    const v = this.latestMetric?.pingMs;
    if (v === undefined || v === null || v < 0) return 'n/a';
    return `${v.toFixed(v < 10 ? 1 : 0)} ms`;
  }

  gauge(value?: number | null): number {
    return Math.max(0, Math.min(100, this.num(value)));
  }

  gaugeStyle(value?: number | null): Record<string, string | number> {
    return { '--value': this.gauge(value) };
  }

  get rows(): Metric[] {
    if (this.monitoringActive) return this.liveMetrics.slice(-10).reverse();
    return this.metrics.slice().reverse().slice(0, 20);
  }

  get statusText(): string {
    if (this.monitoringActive) return 'Мониторинг активен';
    if (this.latestMetric) return 'Мониторинг остановлен';
    return 'Нет данных';
  }

  get statusClass(): string {
    if (!this.monitoringActive) return 'off';
    return this.requestInFlight ? 'work' : 'on';
  }
}
