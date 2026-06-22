# Web — Feature-sliced, layered (Next.js App Router)

Clean-architecture spirit on the frontend: routing/UI (outer) depends on features; features depend on
`core` + `lib`; nothing inward depends on a route.

```
src/
├── app/                  # App Router: routes, layouts, pages, route handlers (delivery only)
├── core/
│   ├── types/            # Domain types shared across features (often re-exported from api-contract)
│   └── config/           # runtime/env config
├── features/<feature>/   # auth, project, document, ai, paper, citation, review, billing, admin ...
│   ├── api/              # data access: TanStack Query hooks over the generated client
│   ├── model/            # feature state (Zustand) + types
│   ├── hooks/            # feature logic
│   └── components/       # feature UI
├── components/
│   ├── ui/               # shadcn/ui primitives
│   └── layout/           # shells, nav
├── lib/
│   ├── api/              # generated typed client from packages/api-contract + fetch wrapper
│   └── utils/
├── hooks/                # shared hooks
└── styles/
```

Conventions (Technical Spec §12): server components for read-heavy pages, client components for the
Tiptap editor; TanStack Query owns server state; Zustand only for ephemeral UI state; the API client
is generated from `openapi.yaml`; PWA service worker caches the app shell.
