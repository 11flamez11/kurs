import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Device } from '../models/device';
import { environment } from '../environments/environment';


@Injectable({
  providedIn: 'root',
})
export class DeviceService {

  constructor(
    private http: HttpClient,
  ) {}

  getAll(): Observable<Device[]> {
    return this.http.get<Device[]>(`${environment.apiUrl}/devices`, {
    });
  }

  getById(id: number): Observable<Device> {
    return this.http.get<Device>(`${environment.apiUrl}/devices/${id}`, {
    });
  }

  create(device: Partial<Device>): Observable<Device> {
    return this.http.post<Device>(`${environment.apiUrl}/devices`, device, {
    });
  }

  update(id: number, device: Partial<Device>): Observable<Device> {
    return this.http.put<Device>(`${environment.apiUrl}/devices/${id}`, device, {
    });
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${environment.apiUrl}/devices/${id}`, {
    });
  }
}
