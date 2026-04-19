import { Routes } from '@angular/router';
import { RegistrationComponent } from './components/registration/registration.component';
import { UsersListComponent } from './components/users-list/users-list.component';

export const routes: Routes = [
  { path: '', redirectTo: '/register', pathMatch: 'full' },
  { path: 'register', component: RegistrationComponent },
  { path: 'users', component: UsersListComponent },
  { path: '**', redirectTo: '/register' }
];
