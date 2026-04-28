import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Event } from '../models/event';
import { environment } from '../environments/environment';


@Injectable({ providedIn: 'root' })
export class EventService {
  constructor(private http: HttpClient) {}

  getAll(): Observable<Event[]> {
    return this.http.get<Event[]>(`${environment.apiUrl}/events`, {});
  }

  create(event: Partial<Event>): Observable<Event> {
    return this.http.post<Event>(`${environment.apiUrl}/events`, event, {});
  }

  update(id: number, event: Partial<Event>): Observable<Event> {
    return this.http.put<Event>(`${environment.apiUrl}/events/${id}`, event, {});
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${environment.apiUrl}/events/${id}`, {});
  }
  getByDevice(deviceId: number): Observable<Event[]> {
    return this.http.get<Event[]>(`${environment.apiUrl}/events/device/${deviceId}`);
  }
}
