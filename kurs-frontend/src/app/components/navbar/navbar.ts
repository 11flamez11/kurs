import { Component, OnDestroy } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { EventService } from '../../services/event.service';
import { Subscription, interval, startWith, switchMap } from 'rxjs';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [RouterModule],
  templateUrl: './navbar.html',
  styleUrls: ['./navbar.css'],
})
export class Navbar implements OnDestroy {
  username = '';
  isAdmin = false;
  unresolvedAlerts = 0;
  private alertsSub?: Subscription;

  constructor(
    public auth: AuthService,
    private router: Router,
    private eventService: EventService,
  ) {
    this.updateUserInfo();
    this.startAlertsPolling();
  }

  updateUserInfo(): void {
    const user = this.auth.getCurrentUser();
    if (user) {
      this.username = user.username;
      this.isAdmin = this.auth.isAdmin();
    }
  }

  logout(): void {
    this.auth.logout();
    void this.router.navigate(['/login']);
  }

  private startAlertsPolling(): void {
    this.alertsSub = interval(10000)
      .pipe(
        startWith(0),
        switchMap(() => this.eventService.getAll()),
      )
      .subscribe({
        next: (events) => {
          this.unresolvedAlerts = events.filter((e) =>
            !e.resolved && ['CRITICAL', 'ERROR', 'WARNING'].includes((e.severity || '').toUpperCase()),
          ).length;
        },
        error: () => {
          this.unresolvedAlerts = 0;
        },
      });
  }

  ngOnDestroy(): void {
    this.alertsSub?.unsubscribe();
  }
}
