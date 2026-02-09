
## 1. Scenario & Operating Assumptions

This system represents an **internal organisational portal login**.

Key assumptions that influenced design decisions:

- Users are **pre-authorised**.
- Accounts are created by administrators.
- There is **no public self-registration**.
- Identity proofing occurs outside this system.
- Infrastructure such as SMS/email gateways for MFA are not available within the time and cost constraints of this challenge.

Because of these constraints, security focus was placed on:

- Preventing abuse of valid accounts  
- Protecting administrative capability  
- Limiting blast radius  
- Making attacks visible  
- Enforcing strong authentication hygiene  

---
## 2. Core Design Philosophy
In an internal portal context, users don't self-register. Instead, accounts are provisioned by administrators (mimicking Active Directory/LDAP), given temporary credentials, and required to set their own secure password on first login. This matches how real organizations operate: IT creates accounts with default passwords, users are forced to change them immediately, and lifecycle management happens through admin intervention.

**Key Security Implementations:**
- Pre-authorized account model (admin-provisioned users, no self-registration)
- Mandatory password change on first login (enforced via servlet filter)
- Multi-layer rate limiting (IP-based + user-based)
- Differentiated protection for admin vs. regular users (cooldown vs. lockout)
- Secure session management with industry-standard cookie flags
- Comprehensive security headers (CSP, HSTS, X-Frame-Options)
- TLS 1.2/1.3 enforcement with strong cipher suites
- Dedicated security audit logging with daily rotation for SIEM integration
- Generic error messages to prevent information disclosure
- Account lifecycle management (enable/disable/inactivate)
- Fixed domain validation (email must be @kd.com for internal portal)

---

## 3. User & Operational Journeys

Beyond static controls, security must be understood in motion.

The following journeys illustrate how authentication, enforcement,
recovery, and administration behave in realistic scenarios.

These flows demonstrate how defensive mechanisms interact while
preserving operational continuity.

---

### 1. Administrative Bootstrap

Initial trusted entry into the system.

![Admin Bootstrap](./diagrams/adminBootstrap.png)
#### Security Features
- Password masking on console input
- Confirmation required
- Complexity validation

**Design Note**

The bootstrap administrator is a break-glass identity.
It exists to provision real administrators and should not be used for daily operations.

### 2. User Creation & Provisioning

How new identities enter the system under administrative control.

![User Creation](./diagrams/userCreation.png)
#### Security Features
- Admin authentication required
- Email domain restriction
- Server-side password validation
- Users flagged for mandatory password change
- Role assignment restricted



**Real-World Parallel**

HR submits onboarding → IT provisions account → temporary credential → forced rotation.

---

### 3. First Login Experience

Mandatory hygiene enforcement for newly created users.

![First Access](./diagrams/userFirstAccess.png)

#### Security Features
- Session allowed but restricted
- Redirect to password change
- Other routes blocked

**Security Implementation (PasswordChangeRequiredFilter):**
```java
// Filter checks EVERY request
if (user.isPasswordChangeRequired()) {
    // Only these paths allowed:
    // - /change-password
    // - /logout  
    // - /login
    // - /css/** (styling)
    
    // Everything else → Redirect to /change-password
}
```
**Design Note**

This prevents temporary or intercepted credentials from being used
to access the system beyond initial setup.

---

### 4. Standard Login Flow

Normal authentication path including layered protections.

![Full Login](./diagrams/fullLogin.png)

#### Security Features
- Uniform failure response (no username enumeration)
- Parallel tracking of IP reputation and account abuse
- BCrypt password verification
- Session ID rotation after authentication
- Post-authentication policy checks (e.g., forced password change)
- Successful login resets abuse counters
- Security events logged for traceability

**Design Note**

The goal is not merely to verify a password.

The goal is to decide whether this request should be trusted right now.

Even correct credentials are evaluated in context:

- Has this IP been abusive?
- Is the account in a safe state?
- Is additional hygiene required?
- Are we protecting an operational role?

By layering checks, the system avoids treating authentication as binary.
Access is granted only when identity, behaviour, and lifecycle expectations align..


---

### 5. IP Rate Limiting Scenario

This flow activates when repeated login attemps originate from the same source.
The intention is to slow automated abuse without permanently harming availability.

![IP Rate Limit](./diagrams/ipRateLimit.png)

#### Security Features
- Login tracking per IP address
- Threshold-based temporary block
- Automatic expiry of penalties 
- Counters cleared after timeout
- Security logging for visibility

**Design Note**

Rate limiting is designed to create friction for attackers, not outages for users.

Permanent lockouts based purely on network origin can be dangerous,
especially in corporate environments where many users may share NAT,
VPN exits, or proxy infrastructure.

For this reason, the block is temporary and self-healing.

At the same time, IP reputation becomes part of the overall trust signal
evaluated during authentication.

This allows the system to respond to abuse while remaining operational.
---

### 6. Standard User Lockout

This scenario represents sustained or suspicious authentication failure
associated with a specific identity.

![User Lockout](./diagrams/userLocked.png)

#### Security Features
- Account transitions to a disabled state
- Authentication attempts are rejected even with correct credentials
- Administrative intervention required for recovery
- Status changes are recorded for audit and traceability

**Design Note**

At this stage the system assumes elevated risk.

While the root cause may simply be user error,
it may also indicate credential abuse.
Automatically restoring access would favour an attacker
who can simply wait out the restriction.

For this reason, recovery requires an administrator.

This allows identity to be validated through organisational processes
such as internal communication or managerial confirmation.

In a larger deployment, an additional safety valve could exist:
a long-duration automatic unlock (e.g., after 24 hours),
with administrators providing a faster recovery path when necessary.

However, for this implementation, priority was given to
controlled, accountable restoration of trust.

---

### 7. Administrative Cooldown Protection

This scenario activates when repeated failures occur against a privileged operator.

![Admin Cooldown](./diagrams/adminLocked.png)

#### Security Features
- Temporary restriction on authentication attempts
- Automatic recovery after cooldown window
- Additional attempts extend the restriction
- Events logged for visibility

**Design Note**

Administrative accounts are different from standard users.

If a normal user is unavailable, the organisation can continue operating.
If administrators are unavailable, recovery, onboarding, and incident response
may halt.

For this reason, permanently disabling an administrator based solely on
authentication failures introduces a serious operational risk.
An attacker could intentionally trigger lockouts to deny service.

Instead, the system applies a cooling-off period.

This slows brute force activity while ensuring administrators
regain access without requiring intervention from another operator.

Security is preserved, but availability is not sacrificed.

---

### 8. Complete Lifecycle Overview

Comprehensive view of state transitions and recovery paths.

![Lifecycle](./diagrams/finalStateDiagram.png)

---

## Defense in Depth

Authentication risk is not controlled by a single mechanism.
Instead, multiple independent safeguards operate together so that
failure or bypass of one layer does not expose the system.

The design intentionally distributes trust decisions across transport,
network behaviour, identity state, application logic, and session handling.

---

### Layer 1 – Transport Trust

Before credentials are evaluated, the communication channel must be protected.

#### Security Measures
- TLS enforced
- Legacy protocol versions disabled
- Strong cipher suites selected
- HSTS prevents downgrade and stripping attacks
- Development certificate automatically provisioned

If transport integrity fails, every higher control becomes meaningless.
This layer ensures credentials are never exposed in transit.

```properties
# Enforce modern TLS only
server.ssl.enabled-protocols=TLSv1.2,TLSv1.3

# Explicit cipher control
server.ssl.ciphers=TLS_AES_256_GCM_SHA384,\
TLS_AES_128_GCM_SHA256,\
TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384,\
TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256
```

---

### Layer 2 – Network Behaviour

Even valid-looking requests can be malicious if behaviour is abnormal.

#### Security Measures
- IP-based rate limiting
- Temporary blocks for abusive sources
- Authentication attempts tied to source address
- Automatic expiry of penalties

The goal is to slow automation without causing unnecessary
availability impact in shared enterprise network environments.

**Relevant Implementation**
- `service/LoginRateLimiter.java`
- `service/AuthService.java`

---

### Layer 3 – Identity Protection

The system evaluates whether the account itself is in a trustworthy state.

#### Security Measures
- Lockout after repeated failure
- Administrative cooldown model
- Mandatory password change flags
- Explicit lifecycle states
- Manual recovery pathways

Trust in identity is dynamic.
The system adapts based on behaviour and history,
not only credentials.

**Relevant Implementation**
- `service/AuthService.java`
- `service/PasswordChangeRequiredFilter.java`
- `repository/UserRepository.java`
- `model/User.java`

---

### Layer 4 – Application Enforcement

Input handling and responses must not help an attacker.

#### Security Measures
- Strict validation rules
- Password policy enforcement server-side
- Uniform error messages
- CSRF protection on state changes
- Password reuse prevention

Attackers should gain as little information as possible from the interface.
```java
private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@kd\\.com$";

private static final String PASSWORD_REGEX =
  "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+=-])[A-Za-z\\d!@#$%^&*()_+=-]{8,64}$";
```
---

### Layer 5 – Session Integrity

Authentication success does not end risk.

#### Security Measures
- Session ID regeneration on login
- Secure cookie attributes
- Timeout on inactivity
- Proper invalidation on logout or password change

These measures prevent fixation, replay, and session theft
from undermining otherwise strong authentication.

```properties
server.servlet.session.timeout=15m
server.servlet.session.cookie.http-only=true
server.servlet.session.cookie.secure=true
server.servlet.session.cookie.same-site=strict
```
```java
.logout(logout -> logout
    .invalidateHttpSession(true)
    .deleteCookies("JSESSIONID")
)
```

---

### Layer 6 – Browser Containment

Client execution paths are restricted to reduce injection opportunities.

#### Security Measures
- Strict Content Security Policy
- Frame restrictions
- Controlled resource origins

By minimising what the browser is allowed to execute,
entire classes of XSS-style attacks become significantly harder.


```java
.headers(headers -> headers
    .contentSecurityPolicy(csp -> csp
        .policyDirectives(
            "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self'; frame-ancestors 'self';"
        )
    )
)
```

---

### Layer 7 – Observability & Accountability

Security must be measurable.

#### Security Measures
- Dedicated security log
- Structured event recording
- Separation from application logs
- Time-based rotation

Well-structured logs allow rapid integration with monitoring,
alerting, and forensic workflows.

Visibility turns defensive controls into actionable intelligence.

**Relevant Implementation**
- `logback-spring.xml`

```java
private static final Logger SECURITY_LOG =
        LoggerFactory.getLogger("SECURITY_AUDIT");
```

---

