import { Component } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [RouterModule],
  templateUrl: './navbar.html',
  styleUrls: ['./navbar.css'],
})
export class Navbar {
  username = '';
  isAdmin = false;

  constructor(
    public auth: AuthService,
    private router: Router,
  ) {
    this.updateUserInfo();
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
}
