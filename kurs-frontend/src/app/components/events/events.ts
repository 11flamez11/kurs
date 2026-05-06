import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute } from '@angular/router';
import { Event } from '../../models/event';
import { EventService } from '../../services/event.service';
import { AuthService } from '../../services/auth.service';
import { Navbar } from '../navbar/navbar';

@Component({
  selector: 'app-events',
  standalone: true,
  imports: [CommonModule, RouterModule, Navbar],
  templateUrl: './events.html',
  styleUrls: ['./events.css'],
})
export class Events implements OnInit {
  events: Event[] = [];
  isLoading = true;
  isAdmin = false;
  deviceId: number | null = null;

  constructor(
    private eventService: EventService,
    private auth: AuthService,
    private route: ActivatedRoute,
  ) {}

  ngOnInit(): void {
    this.isAdmin = this.auth.isAdmin();
    this.route.queryParams.subscribe((params) => {
      this.deviceId = params['deviceId'] ? +params['deviceId'] : null;
      this.loadEvents();
    });
  }

  loadEvents(): void {
    this.isLoading = true;

    const request = this.deviceId
      ? this.eventService.getByDevice(this.deviceId)
      : this.eventService.getAll();

    request.subscribe({
      next: (data) => {
        this.events = data;
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
      },
    });
  }

  deleteEvent(id: number): void {
    if (!confirm('Удалить событие?')) return;
    this.eventService.delete(id).subscribe({
      next: () => {
        this.events = this.events.filter((e) => e.id !== id);
      },
      error: () => alert('Не удалось удалить'),
    });
  }

  getSeverityClass(severity: string): string {
    return severity.toLowerCase();
  }
}
