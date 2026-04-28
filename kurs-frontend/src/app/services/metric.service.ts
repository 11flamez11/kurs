import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Metric } from '../models/metric';
import { environment } from '../environments/environment';

@Injectable({ providedIn: 'root' })
export class MetricService {
  constructor(private http: HttpClient) {}

  getAll(): Observable<Metric[]> {
    return this.http.get<Metric[]>(`${environment.apiUrl}/metrics`, {});
  }

  create(metric: Partial<Metric>): Observable<Metric> {
    return this.http.post<Metric>(`${environment.apiUrl}/metrics`, metric, {});
  }

  update(id: number, metric: Partial<Metric>): Observable<Metric> {
    return this.http.put<Metric>(`${environment.apiUrl}/metrics/${id}`, metric, {});
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${environment.apiUrl}/metrics/${id}`, {});
  }

  getByDevice(deviceId: number): Observable<Metric[]> {
    return this.http.get<Metric[]>(`${environment.apiUrl}/metrics/device/${deviceId}`);
  }

}
