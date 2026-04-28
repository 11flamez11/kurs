import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule, ActivatedRoute } from '@angular/router';
import { Event } from '../../models/event';
import { EventService } from '../../services/event.service';
import { AuthService } from '../../services/auth.service';
import { FormsModule } from '@angular/forms';
import { Navbar } from '../navbar/navbar';

@Component({
  selector: 'app-events',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, Navbar],
  templateUrl: './events.html',
  styleUrls: ['./events.css'],
})
export class Events implements OnInit {
  events: Event[] = [];
  isLoading = true;
  isAdmin = false;
  showForm = false;
  isEditing = false;
  editingId: number | null = null;
  deviceId: number | null = null;
  form: Partial<Event> = {
    deviceId: undefined,
    severity: 'INFO',
    description: '',
    resolved: false,
  };

  constructor(
    private eventService: EventService,
    private auth: AuthService,
    private router: Router,
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

  openCreateForm(): void {
    this.showForm = true;
    this.isEditing = false;
    this.editingId = null;
    this.form = {
      deviceId: this.deviceId || undefined,
      severity: 'INFO',
      description: '',
      resolved: false,
    };
  }

  openEditForm(event: Event): void {
    this.showForm = true;
    this.isEditing = true;
    this.editingId = event.id;
    this.form = { ...event };
  }

  closeForm(): void {
    this.showForm = false;
    this.form = {};
  }

  submitForm(): void {
    if (!this.form.deviceId || !this.form.description) {
      alert('Заполните обязательные поля');
      return;
    }

    if (this.isEditing && this.editingId) {
      this.eventService.update(this.editingId, this.form).subscribe({
        next: () => {
          this.closeForm();
          this.loadEvents();
        },
        error: () => alert('Ошибка обновления'),
      });
    } else {
      this.eventService.create(this.form).subscribe({
        next: () => {
          this.closeForm();
          this.loadEvents();
        },
        error: () => alert('Ошибка создания'),
      });
    }
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
