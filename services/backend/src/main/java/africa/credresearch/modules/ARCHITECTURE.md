# Backend — Clean Architecture inside a Modular Monolith

One Spring Boot deployable. Each **feature module** under `modules/<name>` is an independent
boundary; modules talk to each other **only through `application` service interfaces** — never by
reaching into another module's `infrastructure` or database tables (ADR-002).

Inside every module the dependency rule points **inward** (`interfaces → application → domain`,
`infrastructure → application/domain`). Inner layers never import outer layers.

```
modules/<feature>/
├── domain/                  # Enterprise rules. Pure Java, no Spring, no JPA.
│   ├── model/               #   Entities & value objects (the real domain types)
│   ├── port/                #   Outbound ports: repository & gateway INTERFACES
│   ├── service/             #   Domain services (logic spanning entities)
│   └── event/               #   Domain events
├── application/             # Use cases / orchestration. Depends only on domain.
│   ├── usecase/             #   One class per use case (input port impl)
│   ├── dto/                 #   Use-case command/result objects
│   └── mapper/              #   domain <-> application dto
├── infrastructure/          # Frameworks & drivers. Implements domain ports.
│   ├── persistence/
│   │   ├── entity/          #   JPA @Entity classes (NOT the domain model)
│   │   └── repository/      #   Spring Data repos + port adapters
│   ├── adapter/             #   External clients (worker, payments, email, storage)
│   └── config/              #   Spring @Configuration / bean wiring
└── interfaces/              # Inbound adapters (delivery layer).
    ├── rest/                #   @RestController — thin, calls use cases
    ├── dto/                 #   Request/response DTOs (HTTP shape)
    └── mapper/              #   http dto <-> application dto
```

## Rules
- `domain` and `application` must NOT import `org.springframework.*`, `jakarta.persistence.*`,
  or any other module's packages.
- Persistence: map JPA `entity` <-> `domain/model`; the domain never sees `@Entity`.
- A repository is declared as an **interface in `domain/port`** and implemented in
  `infrastructure/persistence/repository`.
- Controllers in `interfaces/rest` depend on `application/usecase` interfaces, never on `infrastructure`.
- Every query is tenant-scoped via `common/tenant` (FR-TEN-1) and guarded with `@PreAuthorize`.

Shared cross-cutting concerns live in `africa.credresearch.common` (tenant context, security, error
handling, job coordination, auditing) — depended on by modules, depending on none.

Modules: identity · org · project · template · document · review · paper · citation · ai ·
alignment · questionnaire · dataset · analysis · similarity · billing · notification ·
disclosure · admin.
