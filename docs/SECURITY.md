# Security controls

| Threat | Control | Verification |
|---|---|---|
| Unauthorized access | Stateless JWT, active-user recheck, method-security role guards, owner checks | `JwtServiceTest`, controller security tests, `EndpointAuthorizationTest` |
| DB breach | Per-document AES-256-GCM keys wrapped by an application master key | `CryptoServiceTest`, `DocumentServiceTest` |
| Forever-valid links | Expiring/revocable policies and cleanup token invalidation | `PolicyServiceTest`, `CleanupServiceTest` |
| Unlimited copying | Policy action checks and atomic print/edit quotas | `PolicyGuardTest`, request/release tests |
| Untraceable leaks | Append-only audit records for releases and rejected attempts | `AuditCoverageTest`, audit query tests |
| Silent printing | Owner approval and single-use approved requests | request approval/release tests |
| Silent modification | Pending encrypted versions and owner approval | `EditServiceTest`, `RequestEditApprovalTest` |
| Residual data after use | Per-document cleanup removes all version files and wrapped keys | `CleanupServiceTest` |

Secrets must be supplied through environment variables. Never use demo values in production.

## Known limitations

- No real printer integration.
- Application-level key management; no KMS/HSM.
- Local encrypted-file storage.
- No AI-based leak detection.
- Single-editor edit workflow.
- Continuous connectivity is required.
