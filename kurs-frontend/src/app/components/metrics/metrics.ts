import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { Subscription } from 'rxjs';
import { BaseChartDirective } from 'ng2-charts';
import { ChartConfiguration } from 'chart.js';
import { Metric } from '../../models/metric';
import { MetricService } from '../../services/metric.service';
import { AuthService } from '../../services/auth.service';
import { Navbar } from '../navbar/navbar';
import { MonitoringSessionService } from '../../services/monitoring-session.service';

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
  private routeSub?: Subscription;
  private sessionSub?: Subscription;

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
    private monitoringSession: MonitoringSessionService,
  ) {}

  ngOnInit(): void {
    this.isAdmin = this.auth.isAdmin();
    this.sessionSub = this.monitoringSession.state$.subscribe((state) => {
      this.monitoringActive = state.monitoringActive;
      this.requestInFlight = state.requestInFlight;
      this.collectIntervalSeconds = state.collectIntervalSeconds;
      this.liveMetrics = state.liveMetrics;
      this.updateLatestAndChart();
    });

    this.routeSub = this.route.queryParams.subscribe((params) => {
      this.deviceId = params['deviceId'] ? +params['deviceId'] : null;
      const session = this.monitoringSession.snapshot;
      if (session.monitoringActive && session.deviceId !== this.deviceId) {
        this.monitoringSession.stop();
      } else if (!session.monitoringActive && session.deviceId !== this.deviceId) {
        this.monitoringSession.reset();
      }
      this.loadMetrics();
    });
  }

  ngOnDestroy(): void {
    this.routeSub?.unsubscribe();
    this.sessionSub?.unsubscribe();
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
    if (!this.deviceId) return;
    this.monitoringSession.start(this.deviceId, this.collectIntervalSeconds);
  }

  stopMonitoring(): void {
    this.monitoringSession.stop();
  }

  onIntervalChange(event: Event): void {
    const target = event.target as HTMLInputElement;
    const parsed = Number(target.value);
    const next = Number.isFinite(parsed) ? Math.max(1, Math.min(30, parsed)) : 1;
    this.collectIntervalSeconds = next;
    this.monitoringSession.updateInterval(next);
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
