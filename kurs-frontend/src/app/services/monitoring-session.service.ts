import { Injectable } from '@angular/core';
import { BehaviorSubject, Subscription, interval } from 'rxjs';
import { Metric } from '../models/metric';
import { MetricService } from './metric.service';

export interface MonitoringSessionState {
  deviceId: number | null;
  monitoringActive: boolean;
  requestInFlight: boolean;
  collectIntervalSeconds: number;
  liveMetrics: Metric[];
}

@Injectable({ providedIn: 'root' })
export class MonitoringSessionService {
  private readonly maxLivePoints = 120;
  private monitoringSub?: Subscription;

  private readonly stateSubject = new BehaviorSubject<MonitoringSessionState>({
    deviceId: null,
    monitoringActive: false,
    requestInFlight: false,
    collectIntervalSeconds: 2,
    liveMetrics: [],
  });

  readonly state$ = this.stateSubject.asObservable();

  constructor(private metricService: MetricService) {}

  get snapshot(): MonitoringSessionState {
    return this.stateSubject.value;
  }

  start(deviceId: number, intervalSeconds: number): void {
    const safeInterval = Math.max(1, Math.min(30, intervalSeconds || 1));
    this.stopTimerOnly();

    this.patchState({
      deviceId,
      collectIntervalSeconds: safeInterval,
      monitoringActive: true,
      requestInFlight: false,
    });

    this.pollLiveMetric();
    this.monitoringSub = interval(safeInterval * 1000).subscribe(() => this.pollLiveMetric());
  }

  stop(): void {
    this.stopTimerOnly();
    this.patchState({
      monitoringActive: false,
      requestInFlight: false,
    });
  }

  reset(): void {
    this.stopTimerOnly();
    this.stateSubject.next({
      deviceId: null,
      monitoringActive: false,
      requestInFlight: false,
      collectIntervalSeconds: 2,
      liveMetrics: [],
    });
  }

  updateInterval(intervalSeconds: number): void {
    const state = this.snapshot;
    const safeInterval = Math.max(1, Math.min(30, intervalSeconds || 1));
    this.patchState({ collectIntervalSeconds: safeInterval });
    if (state.monitoringActive && state.deviceId) {
      this.start(state.deviceId, safeInterval);
    }
  }

  private pollLiveMetric(): void {
    const state = this.snapshot;
    if (!state.monitoringActive || !state.deviceId || state.requestInFlight) {
      return;
    }

    this.patchState({ requestInFlight: true });
    this.metricService.collectForDevice(state.deviceId, 1, 1, false).subscribe({
      next: (data) => {
        const metric = data?.[0];
        if (metric) {
          const merged = [...this.snapshot.liveMetrics, metric];
          const trimmed = merged.slice(-this.maxLivePoints);
          this.patchState({ liveMetrics: trimmed });
        }
        this.patchState({ requestInFlight: false });
      },
      error: () => {
        this.stop();
      },
    });
  }

  private stopTimerOnly(): void {
    if (this.monitoringSub) {
      this.monitoringSub.unsubscribe();
      this.monitoringSub = undefined;
    }
  }

  private patchState(patch: Partial<MonitoringSessionState>): void {
    this.stateSubject.next({
      ...this.stateSubject.value,
      ...patch,
    });
  }
}
