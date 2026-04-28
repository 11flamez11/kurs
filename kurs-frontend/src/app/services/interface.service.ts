import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../environments/environment';

export interface NetworkInterface {
  id: number;
  deviceId: number;
  name: string;
  macAddress: string;
  speed: number;
  status: string;
}

@Injectable({ providedIn: 'root' })
export class InterfaceService {
  constructor(private http: HttpClient) {}

  getAll(): Observable<NetworkInterface[]> {
    return this.http.get<NetworkInterface[]>(`${environment.apiUrl}/interfaces`, {});
  }

  create(iface: Partial<NetworkInterface>): Observable<NetworkInterface> {
    return this.http.post<NetworkInterface>(`${environment.apiUrl}/interfaces`, iface, {});
  }

  update(id: number, iface: Partial<NetworkInterface>): Observable<NetworkInterface> {
    return this.http.put<NetworkInterface>(`${environment.apiUrl}/interfaces/${id}`, iface, {});
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${environment.apiUrl}/interfaces/${id}`, {});
  }

  getByDevice(deviceId: number): Observable<NetworkInterface[]> {
    return this.http.get<NetworkInterface[]>(`${environment.apiUrl}/interfaces/device/${deviceId}`);
  }
}
