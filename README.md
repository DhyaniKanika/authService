# Secure Sign-On Portal

A Spring Boot based internal authentication system built as a security engineering exercise.

The project demonstrates how layered defensive controls can be embedded
directly into authentication and account lifecycle management.

---

## Overview

This system models an **internal enterprise portal**.

Key assumptions:

- Users are pre-authorised.
- Accounts are provisioned by administrators.
- There is **no public registration**.
- Identity trust is managed through lifecycle state and behaviour.

The design prioritises:

- defence against credential abuse  
- operational continuity  
- traceability of administrative actions  

---
## Build & Run

### Build
```bash
mvn clean package
```
### Run
Windows
```bash
  mvnw spring-boot:run
```
or Linux/macOs
```bash
  ./mvnw spring-boot:run
```
---

## Technology Stack

- Java 17
- BouncyCastle (for certificate generation)
- Spring Boot 3.x  
- Spring Security  
- Thymeleaf  
- H2 database  
- Maven  

---


## Directories and Files

### config
| File | Responsibility |
|------|---------------|
| `DataLoader.java` | Bootstraps the initial administrator and seed roles. |
| `SecurityConfig.java` | Defines RBAC, session policy, CSRF handling, headers, and custom filters. |

---

### controller
| File | Responsibility |
|------|---------------|
| `AdminController.java` | Admin-only operations such as creating users and lifecycle management. |
| `AuthController.java` | Login, logout, landing routing, and password change flows. |

---

### model
| File | Responsibility |
|------|---------------|
| `Role.java` | Role definition used for authorization. |
| `User.java` | Core identity object including password hash and state flags. |
| `UserStatusHistory.java` | Audit history for lifecycle transitions. |

---

### repository
| File | Responsibility |
|------|---------------|
| `RoleRepository.java` | Database access for roles. |
| `UserRepository.java` | Database access for users. |
| `UserStatusHistoryRepository.java` | Persistence for audit history. |

---

### service
| File | Responsibility |
|------|---------------|
| `AuthService.java` | Coordinates authentication and applies security decisions. |
| `LoginRateLimiter.java` | Tracks abusive behaviour and applies temporary blocks. |
| `PasswordChangeRequiredFilter.java` | Forces password update before normal access. |
| `ValidationService.java` | Server-side enforcement of email/password policies. |

---

### utils
| File | Responsibility |
|------|---------------|
| `SelfSignedCertGenerator.java` | Generates development TLS material. |
| `TlsBootstrap.java` | Handles HTTPS bootstrapping and configuration wiring. |

---

### static
| File | Responsibility |
|------|---------------|
| `styles.css` | UI styling, externalised to satisfy CSP. |
| `js/*` | Client-side validation and form wiring without inline scripts. |
| `logback-spring.xml` | Centralised log management|

---

### templates
| File | Responsibility |
|------|---------------|
| `login.html` | Authentication entry point. |
| `landing.html` | Post-login landing. |
| `account.html` | User self-service. |
| `changePassword.html` | Forced password rotation. |
| `admin.html` | Admin dashboard. |
| `createUser.html` | Admin provisioning UI. |
| `manageUsers.html` | Lifecycle management. |
| `logoutSuccess.html` | Logout confirmation. |
| `accessDenied.html` | Authorization failure page. |


