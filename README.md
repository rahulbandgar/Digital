# User Registration App

A full-stack User Registration application built with **Spring Boot 3** (backend) and **Angular 17** (frontend).

---

## Project Structure

```
.
├── user-registration-backend/   # Spring Boot REST API
└── user-registration-frontend/  # Angular SPA
```

---

## Backend — Spring Boot

### Tech Stack
- Java 17 + Spring Boot 3.2
- Spring Web, Spring Data JPA, H2 (in-memory), Lombok, Validation

### Run the Backend

```bash
cd user-registration-backend
./mvnw spring-boot:run
```

- API base URL: `http://localhost:8080`
- H2 Console: `http://localhost:8080/h2-console`
  - JDBC URL: `jdbc:h2:mem:userdb`
  - Username: `sa` | Password: *(empty)*

### REST API Endpoints

| Method | Endpoint           | Description           |
|--------|--------------------|-----------------------|
| POST   | `/api/users`       | Register a new user   |
| GET    | `/api/users`       | Get all users         |
| GET    | `/api/users/{id}`  | Get user by ID        |

#### Sample Request — Register User

```json
POST http://localhost:8080/api/users
Content-Type: application/json

{
  "firstName": "Jane",
  "lastName": "Doe",
  "email": "jane.doe@example.com",
  "phone": "+1-555-0100",
  "dateOfBirth": "1990-06-15",
  "address": "123 Main St",
  "city": "New York",
  "country": "USA"
}
```

---

## Frontend — Angular 17

### Tech Stack
- Angular 17 (standalone components, reactive forms, HttpClient)
- Bootstrap 5 (CDN) + Bootstrap Icons

### Run the Frontend

```bash
cd user-registration-frontend
npm install
npm start        # or: ng serve
```

- App URL: `http://localhost:4200`

### Routes

| Route       | Description                    |
|-------------|--------------------------------|
| `/register` | User Registration Form         |
| `/users`    | All Registered Users (table)   |

---

## Features

- **Registration Form** with real-time validation (required fields, email format)
- **Success message** with a direct link to `/users` after successful registration
- **Error handling** for duplicate emails, server unavailability, and validation errors
- **Users list** with avatar initials, mailto links, and formatted registration date
- **Empty state** and **loading spinner** on the users page
- **Refresh** button on users list to re-fetch data
- Responsive layout with Bootstrap 5 grid

---

## Prerequisites

| Tool        | Version  |
|-------------|----------|
| Java        | 17+      |
| Maven       | 3.8+ (or use included `./mvnw`) |
| Node.js     | 18+      |
| Angular CLI | 17+      |

Install Angular CLI globally (first time only):

```bash
npm install -g @angular/cli
```
