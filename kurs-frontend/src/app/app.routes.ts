import { Routes } from '@angular/router';
import { AuthGuard } from './guards/auth.guard';
import { Login } from './components/login/login';
import { Register } from './components/register/register';
import { Dashboard } from './components/dashboard/dashboard';
import { Metrics } from './components/metrics/metrics';
import { Interfaces } from './components/interfaces/interfaces';
import { Events } from './components/events/events';
import { UsersAdmin } from './components/users-admin/users-admin';
import { AdminGuard } from './guards/admin.guard';
import { DeviceForm } from './components/device-form/device-form';

export const routes: Routes = [
  { path: 'login', component: Login },
  { path: 'register', component: Register },
  { path: 'dashboard', component: Dashboard, canActivate: [AuthGuard] },
  { path: 'devices/:id/edit', component: DeviceForm, canActivate: [AuthGuard] },
  { path: 'metrics', component: Metrics, canActivate: [AuthGuard] },
  { path: 'interfaces', component: Interfaces, canActivate: [AuthGuard] },
  { path: 'events', component: Events, canActivate: [AuthGuard] },
  { path: 'admin/users', component: UsersAdmin, canActivate: [AuthGuard, AdminGuard] },
  { path: '', redirectTo: '/dashboard', pathMatch: 'full' },
  { path: '**', redirectTo: '/dashboard' },
];
