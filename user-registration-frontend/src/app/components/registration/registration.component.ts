import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { UserService } from '../../services/user.service';
import { User } from '../../models/user.model';

@Component({
  selector: 'app-registration',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './registration.component.html'
})
export class RegistrationComponent implements OnInit {
  registrationForm!: FormGroup;
  isSubmitting = false;
  successMessage = '';
  errorMessage = '';
  registeredUser: User | null = null;

  constructor(private fb: FormBuilder, private userService: UserService) {}

  ngOnInit(): void {
    this.registrationForm = this.fb.group({
      firstName: ['', [Validators.required, Validators.minLength(2)]],
      lastName: ['', [Validators.required, Validators.minLength(2)]],
      email: ['', [Validators.required, Validators.email]],
      phone: [''],
      dateOfBirth: [''],
      address: [''],
      city: [''],
      country: ['']
    });
  }

  get f() {
    return this.registrationForm.controls;
  }

  isFieldInvalid(field: string): boolean {
    const control = this.registrationForm.get(field);
    return !!(control && control.invalid && (control.dirty || control.touched));
  }

  onSubmit(): void {
    if (this.registrationForm.invalid) {
      this.registrationForm.markAllAsTouched();
      return;
    }

    this.isSubmitting = true;
    this.errorMessage = '';
    this.successMessage = '';

    const formValue = this.registrationForm.value;
    const user: User = {
      ...formValue,
      dateOfBirth: formValue.dateOfBirth || undefined
    };

    this.userService.registerUser(user).subscribe({
      next: (savedUser) => {
        this.registeredUser = savedUser;
        this.successMessage = `User "${savedUser.firstName} ${savedUser.lastName}" registered successfully!`;
        this.registrationForm.reset();
        this.isSubmitting = false;
      },
      error: (err: Error) => {
        this.errorMessage = err.message;
        this.isSubmitting = false;
      }
    });
  }

  resetForm(): void {
    this.successMessage = '';
    this.errorMessage = '';
    this.registeredUser = null;
    this.registrationForm.reset();
  }
}
