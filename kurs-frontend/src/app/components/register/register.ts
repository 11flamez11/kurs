import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './register.html',
  styleUrls: ['./register.css'],
})
export class Register {
  username = ''
  password = ''
  confirmPassword = ''
  errorMessage = ''
  successMessage = ''
  isLoading = false

  constructor(private router: Router) {}

  onSubmit(): void {
    this.errorMessage = ''
    this.successMessage = ''
    if (!this.username || !this.password) {
      this.errorMessage = 'Логин и пароль обязательны';
      return;
    }

    if (this.password.length < 4) {
      this.errorMessage = 'Пароль должен содержать минимум 4 символа';
      return;
    }

    if (this.password !== this.confirmPassword) {
      this.errorMessage = 'Пароли не совпадают'
      return;
    }
    this.isLoading = true;
    const headers = { 'Content-Type': 'application/json' }
    const body = JSON.stringify({
      username: this.username,
      password: this.password,
    });

    fetch('api/users/register', {
      method: 'POST',
      headers: headers,
      body: body,
    })
      .then((response) => {
        if (response.ok) {
          this.successMessage = 'Регистрация успешна! Теперь войдите в систему.'
          setTimeout(() => {
            void this.router.navigate(['/login'])
          }, 2000);
          return Promise.resolve();
        } else {
          return response.text().then((text) => {
            throw new Error(text || 'Ошибка регистрации')
          });
        }
      })
      .catch((error) => {
        console.error('Registration error:', error)
        this.errorMessage = error.message || 'Не удалось зарегистрироваться'
      })
      .finally(() => {
        this.isLoading = false
      });
  }
}
