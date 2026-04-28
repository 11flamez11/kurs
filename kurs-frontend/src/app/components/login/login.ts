import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './login.html',
  styleUrls: ['./login.css'],
})
export class Login {
  username = '';
  password = '';
  errorMessage = '';
  isLoading = false;

  constructor(
    private auth: AuthService,
    private router: Router,
  ) {}

  onSubmit(): void {
    if (!this.username || !this.password) {
      this.errorMessage = 'Введите логин и пароль';
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';

    this.auth.login(this.username, this.password).subscribe({
      next: (user) => {
        this.auth.me().subscribe(()=>{
          this.isLoading = false
          this.router.navigate(['/dashboard'])
        })
        console.log('Login successful:', user)
      },
      error: (error) => {
        this.isLoading = false;
        console.error('Login error:', error)
        if (error.status === 401) {
          this.errorMessage = 'Неверный логин или пароль'
        } else if (error.status === 0) {
          this.errorMessage = 'Сервер недоступен. Проверьте, запущен ли бэкенд.'
        } else {
          this.errorMessage = 'Ошибка подключения. Попробуйте позже.'
        }
      },
    });
  }
}
