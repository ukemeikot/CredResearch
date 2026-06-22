// Stable entrypoint for the generated API contract.
// `pnpm --filter @credresearch/api-client generate` writes ./schema.ts from openapi.yaml;
// this file re-exports it so consumers import from a fixed path:
//
//   import type { paths, components } from "@credresearch/api-client";
//
// Until `generate` has been run once, ./schema.ts does not exist yet.
export * from "./schema";
