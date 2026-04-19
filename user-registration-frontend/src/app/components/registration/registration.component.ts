import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { UserService } from '../../services/user.service';
import { User } from '../../models/user.model';

@Component({
  selector: 'app-registration',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, RouterLink],
  templateUrl: './registration.component.html',
  styleUrls: ['./registration.component.css']
})
export class RegistrationComponent implements OnInit {
  step1!: FormGroup;
  step2!: FormGroup;
  step = 1;
  submitting = false;
  savedUser: User | null = null;
  apiError = '';

  dialCodes = [
    { flag: '🇺🇸', dial: '+1',   label: 'US +1' },
    { flag: '🇨🇦', dial: '+1',   label: 'CA +1' },
    { flag: '🇬🇧', dial: '+44',  label: 'GB +44' },
    { flag: '🇫🇷', dial: '+33',  label: 'FR +33' },
    { flag: '🇩🇪', dial: '+49',  label: 'DE +49' },
    { flag: '🇮🇹', dial: '+39',  label: 'IT +39' },
    { flag: '🇪🇸', dial: '+34',  label: 'ES +34' },
    { flag: '🇳🇱', dial: '+31',  label: 'NL +31' },
    { flag: '🇸🇪', dial: '+46',  label: 'SE +46' },
    { flag: '🇳🇴', dial: '+47',  label: 'NO +47' },
    { flag: '🇮🇳', dial: '+91',  label: 'IN +91' },
    { flag: '🇨🇳', dial: '+86',  label: 'CN +86' },
    { flag: '🇯🇵', dial: '+81',  label: 'JP +81' },
    { flag: '🇰🇷', dial: '+82',  label: 'KR +82' },
    { flag: '🇸🇬', dial: '+65',  label: 'SG +65' },
    { flag: '🇲🇾', dial: '+60',  label: 'MY +60' },
    { flag: '🇦🇺', dial: '+61',  label: 'AU +61' },
    { flag: '🇧🇷', dial: '+55',  label: 'BR +55' },
    { flag: '🇲🇽', dial: '+52',  label: 'MX +52' },
    { flag: '🇿🇦', dial: '+27',  label: 'ZA +27' },
    { flag: '🇦🇪', dial: '+971', label: 'AE +971' },
    { flag: '🇸🇦', dial: '+966', label: 'SA +966' },
    { flag: '🇵🇰', dial: '+92',  label: 'PK +92' },
    { flag: '🇷🇺', dial: '+7',   label: 'RU +7' },
    { flag: '🇹🇷', dial: '+90',  label: 'TR +90' },
  ];
  selectedDial = '+1';

  constructor(private fb: FormBuilder, private svc: UserService) {}

  ngOnInit(): void {
    this.step1 = this.fb.group({
      firstName:   ['', [Validators.required, Validators.minLength(2)]],
      lastName:    ['', [Validators.required, Validators.minLength(2)]],
      email:       ['', [Validators.required, Validators.email]],
      dateOfBirth: [''],
    });
    this.step2 = this.fb.group({
      phoneNumber: [''],
      address:     [''],
      city:        [''],
      country:     [''],
    });
  }

  get f1() { return this.step1.controls; }
  get f2() { return this.step2.controls; }

  invalid(form: FormGroup, field: string): boolean {
    const c = form.get(field);
    return !!(c?.invalid && (c.dirty || c.touched));
  }

  goNext(): void {
    if (this.step1.invalid) { this.step1.markAllAsTouched(); return; }
    this.step = 2;
  }

  goBack(): void { this.step = 1; }

  submit(): void {
    if (this.step1.invalid) { this.step1.markAllAsTouched(); return; }
    this.submitting = true;
    this.apiError = '';
    const phone = this.step2.value.phoneNumber
      ? `${this.selectedDial} ${this.step2.value.phoneNumber}` : '';
    const user: User = {
      firstName:   this.step1.value.firstName,
      lastName:    this.step1.value.lastName,
      email:       this.step1.value.email,
      dateOfBirth: this.step1.value.dateOfBirth || undefined,
      phone,
      address: this.step2.value.address,
      city:    this.step2.value.city,
      country: this.step2.value.country,
    };
    this.svc.registerUser(user).subscribe({
      next:  u => { this.savedUser = u; this.submitting = false; },
      error: e => { this.apiError = e.message; this.submitting = false; },
    });
  }

  reset(): void {
    this.step1.reset(); this.step2.reset();
    this.step = 1; this.savedUser = null; this.apiError = '';
    this.selectedDial = '+1';
  }
}
