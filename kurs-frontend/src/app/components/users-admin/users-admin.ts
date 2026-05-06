import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Navbar } from '../navbar/navbar';
import { UserService } from '../../services/user.service';
import { AuthService } from '../../services/auth.service';
import { User } from '../../models/user';

@Component({
  selector: 'app-users-admin',
  standalone: true,
  imports: [CommonModule, Navbar],
  templateUrl: './users-admin.html',
  styleUrls: ['./users-admin.css'],
})
export class UsersAdmin implements OnInit {
  users: User[] = [];
  isLoading = true;

  constructor(
    private userService: UserService,
    private auth: AuthService,
  ) {}

  get adminCount(): number {
    return this.users.filter((u) => this.isAdminUser(u)).length;
  }

  get userOnlyCount(): number {
    return this.users.length - this.adminCount;
  }

  isAdminUser(u: User): boolean {
    return (
      u.roles?.some((r) => r.includes('ADMIN') || r.endsWith('ADMIN')) ?? false
    );
  }

  roleLabel(u: User): string {
    return this.isAdminUser(u) ? 'Администратор' : 'Пользователь';
  }

  isCurrentUser(u: User): boolean {
    const me = this.auth.getCurrentUser();
    return !!me && u.username === me.username;
  }

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    this.isLoading = true;
    this.userService.getAll().subscribe({
      next: (data) => {
        this.users = data;
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
        alert('Не удалось загрузить пользователей');
      },
    });
  }

  deleteUser(user: User): void {
    if (!user.id) return;
    if (this.isCurrentUser(user)) {
      alert('Нельзя удалить текущую учётную запись.');
      return;
    }
    if (!confirm(`Удалить пользователя ${user.username}?`)) return;
    this.userService.delete(user.id).subscribe({
      next: () => {
        this.users = this.users.filter((u) => u.id !== user.id);
      },
      error: () => alert('Не удалось удалить пользователя'),
    });
  }
}
