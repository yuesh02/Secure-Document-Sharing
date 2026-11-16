# Progress

## Phase 1 - Foundation, data model, authentication/RBAC, audit service

- Added the Spring Boot 3 Maven project, environment-based configuration, UTC `Clock`, and complete Flyway V1 schema.
- Added JPA entities/enums/repositories for the full planned data model.
- Added BCrypt signup/login, HS256 JWT authentication with active-user revalidation, stateless security, CORS, JSON security errors, and ADMIN seeding.
- Added ADMIN account status endpoint and append-only audit service with `REQUIRES_NEW` rejected-attempt auditing.
- Added focused authentication and JWT unit tests.

Validation: `mvn clean verify` passes. Application startup requires a configured MySQL 8 instance and environment values from `.env.example`; no database was available in this workspace for a live startup check.

## Phase 2 - Document upload and encryption

- Added AES-256-GCM document encryption with per-document keys, wrapped by the configured Base64 master key.
- Added UUID-based encrypted file storage with temporary writes, atomic moves, and traversal-safe paths.
- Added PDF validation, configured multipart limits, transactional upload persistence, orphan cleanup on database failure, and owner-scoped document metadata endpoints.
- Added document deletion that removes encrypted versions, removes the wrapped key, revokes policies, marks the document deleted, and audits the operation.
- Added crypto, storage, validation, ownership, and orphan-cleanup tests.

Validation: `mvn clean verify` passes. A startup attempt reached datasource initialization but could not complete because no reachable/configured MySQL 8 instance was available in the workspace.

## Phase 3 - Access policy and token

- Added policy creation/replacement with action, print-count, edit-count, and expiry validation.
- Added 32-byte URL-safe access tokens, previous-policy revocation, token resolution, and owner-only policy reads/revocation.
- Added shared ordered `PolicyGuard` checks for action, expiry, and print quota.
- Added ZXing QR PNG generation for policy tokens.
- Added token, validation, ownership/RBAC, revocation, guard-order, expiry, and QR decoding tests.

Validation: `mvn clean verify` passes. Live startup still requires a reachable MySQL 8 instance.

## Phase 4 - Requests, approval, release, watermark, VIEW/DOWNLOAD

- Added PRINT request creation, duplicate prevention, request listings, owner approval/rejection, and requester/owner authorization.
- Added conditional single-use request release and atomic print quota consumption using the injected UTC `Clock`.
- Added AES decrypt and PDFBox watermarking entirely in memory, with footer and diagonal watermark on every page.
- Added token-based VIEW and DOWNLOAD release endpoints with policy guarding, no-store responses, and audit events.
- Added plaintext/encrypted-buffer zeroing after release and independent rejected-attempt auditing.
- Added watermark and controller-security tests; stored encrypted files are never modified during release.

Validation: `mvn clean verify` passes. Full MySQL/Testcontainers concurrency validation requires Docker and was not runnable in this workspace; application startup also requires a reachable MySQL 8 instance.

## Phase 5 - Audit queries and automatic deletion

- Made `AuditLog` immutable and exposed append-only audit repository operations without update/delete methods.
- Added owner document-audit and ADMIN filtered audit-search endpoints with newest-first pagination capped at 100.
- Added cleanup candidate detection for expired or exhausted non-revoked policies, with per-document transactional cleanup.
- Cleanup removes all encrypted versions and wrapped keys, nulls policy tokens, rejects pending requests, marks documents deleted, and writes `AUTO_DELETED` system audits.
- Cleanup failures are isolated per document and leave the document eligible for retry; repeated runs are idempotent.
- Added audit coverage, owner/admin query, RBAC, cleanup, retry, exhaustion, and non-interference tests.

Audit event coverage: `SIGNUP`, `LOGIN_SUCCESS`, `LOGIN_FAILED`, `UPLOAD`, `DOCUMENT_DELETED`, `POLICY_CREATED`, `POLICY_REVOKED`, `REQUEST_CREATED`, `REQUEST_APPROVED`, `REQUEST_REJECTED`, `PRINT_RELEASED`, `VIEW_RELEASED`, `DOWNLOAD_RELEASED`, and `AUTO_DELETED`; rejected paths use `recordRejected`.

Validation: `mvn clean verify` passes. MySQL/Testcontainers integration and live scheduler execution require Docker and a reachable MySQL 8 instance.

## Phase 6 - Edit access and versioning

- Added PRINT_SHOP edit submission with EDIT policy/quota/expiry checks, overlay validation, and one pending edit per document.
- Added `EditStrategy` and PDFBox `PdfOverlayEditStrategy`; edits are decrypted, applied, re-encrypted with a fresh IV, and stored as PENDING versions.
- Added owner version listing and no-store version content comparison endpoints.
- Extended owner approval/rejection to activate/archive versions atomically for EDIT, consume edit quota conditionally, or reject and delete pending edit files.
- Confirmed print/view/download continue resolving `VersionStatus.ACTIVE`; cleanup already removes every version file.
- Added edit strategy, pending-version, approval lifecycle, quota, watermark-at-rest, and audit coverage tests.

Validation: `mvn clean verify` passes. Live MySQL/Testcontainers concurrency validation requires Docker and a reachable MySQL 8 instance.

## Phase 7 - Admin and dashboard endpoints

- Added ADMIN user listing with role/status filters, metadata-only document listing, and aggregate statistics.
- Added administrator self-disable protection.
- Added USER owner summary and request-history endpoints.
- Added PRINT_SHOP request-history status filtering.
- Added common generic `PageResponse<T>` with a hard page-size cap of 100 for new dashboard lists; existing list services retain the same cap.
- Admin controllers expose no decrypted document-content route; content/version access remains USER-owner or PRINT_SHOP-requester protected.
- Added dashboard aggregation, self-disable, pagination, metadata-boundary, and role-guard tests.

Validation: `mvn clean verify` passes. Live database role-matrix and seeded-stat integration tests require a reachable MySQL 8/Testcontainers environment.

## Phase 8 - Hardening, API docs, deployment, and security

- Added fail-closed secret configuration, security headers/HSTS, configurable in-memory rate limiting, dev HTTPS and profile-gated Swagger UI.
- Added reflection-based endpoint authorization coverage, Dockerfile, Docker Compose, README, and threat-to-control security documentation.
- Added deployment profile configuration and retained no-store semantics on all PDF/content responses.
