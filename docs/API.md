# Phase 2 API

| Method | Path | Auth | Purpose |
|---|---|---|---|
| POST | `/auth/signup` | Public | Create USER (active) or PRINT_SHOP (pending approval) |
| POST | `/auth/login` | Public | Issue a JWT to an active account |
| PATCH | `/admin/users/{id}/status` | ADMIN | Activate or disable an account |
| POST | `/documents` | USER | Upload and encrypt a PDF |
| GET | `/documents?page=0&size=20` | USER | List the owner's non-deleted document metadata |
| GET | `/documents/{id}` | USER | Read owned document metadata |
| DELETE | `/documents/{id}` | USER | Delete an owned document and its encrypted versions |
| POST | `/documents/{id}/policy` | USER owner | Create or replace the document access policy |
| GET | `/documents/{id}/policy` | USER owner | Read the current policy and owner-only token |
| GET | `/policies/{id}/qr` | USER owner | Download the policy token as a QR PNG |
| POST | `/policies/{id}/revoke` | USER owner | Revoke a policy and invalidate its token |
| POST | `/requests` | PRINT_SHOP | Create a pending PRINT request using a policy token |
| GET | `/requests/{id}` | USER owner / requester | Read a request |
| GET | `/requests/mine` | PRINT_SHOP | List the shop's requests |
| GET | `/owners/me/pending-requests` | USER | List pending requests for owned documents |
| POST | `/requests/{id}/approve` | USER owner | Approve a pending request |
| POST | `/requests/{id}/reject` | USER owner | Reject a pending request |
| GET | `/requests/{id}/document` | PRINT_SHOP requester | Release an approved, single-use print |
| POST | `/access/view` | PRINT_SHOP | View a token-authorized watermarked PDF |
| POST | `/access/download` | PRINT_SHOP | Download a token-authorized watermarked PDF |
| POST | `/requests/edit` | PRINT_SHOP | Submit a pending PDF overlay edit |
| GET | `/documents/{id}/versions` | USER owner | List document versions and statuses |
| GET | `/documents/{id}/versions/{versionId}/content` | USER owner | View a version for comparison |
| GET | `/documents/{id}/audit` | USER owner | Read newest-first audit events for a document |
| GET | `/admin/audit` | ADMIN | Search audit events with actor/document/action/status/time filters |
| GET | `/admin/users` | ADMIN | List users filtered by role/status |
| GET | `/admin/documents` | ADMIN | List document metadata only |
| GET | `/admin/stats` | ADMIN | Read aggregate dashboard statistics |
| GET | `/documents/summary` | USER | Read owner document summary |
| GET | `/owners/me/requests` | USER | Read owned-document request history |

```bash
curl -X POST http://localhost:8080/auth/signup -H "Content-Type: application/json" \
  -d "{\"name\":\"Alice\",\"email\":\"alice@example.com\",\"password\":\"password8\",\"role\":\"USER\"}"
curl -X POST http://localhost:8080/auth/login -H "Content-Type: application/json" \
  -d "{\"email\":\"alice@example.com\",\"password\":\"password8\"}"
curl -X PATCH http://localhost:8080/admin/users/2/status -H "Authorization: Bearer <admin-jwt>" \
  -H "Content-Type: application/json" -d "{\"status\":\"ACTIVE\"}"
curl -X POST http://localhost:8080/documents -H "Authorization: Bearer <user-jwt>" \
  -F "file=@document.pdf;type=application/pdf"
curl http://localhost:8080/documents -H "Authorization: Bearer <user-jwt>"
curl -X DELETE http://localhost:8080/documents/<document-id> -H "Authorization: Bearer <user-jwt>"
curl -X POST http://localhost:8080/documents/<document-id>/policy -H "Authorization: Bearer <user-jwt>" \
  -H "Content-Type: application/json" \
  -d "{\"allowedActions\":[\"VIEW\",\"PRINT\"],\"maxPrintCount\":5,\"expiryMinutes\":60}"
curl http://localhost:8080/documents/<document-id>/policy -H "Authorization: Bearer <user-jwt>"
curl http://localhost:8080/policies/<policy-id>/qr -H "Authorization: Bearer <user-jwt>" -o policy.png
curl -X POST http://localhost:8080/policies/<policy-id>/revoke -H "Authorization: Bearer <user-jwt>"
curl -X POST http://localhost:8080/requests -H "Authorization: Bearer <shop-jwt>" \
  -H "Content-Type: application/json" -d "{\"token\":\"<policy-token>\",\"type\":\"PRINT\"}"
curl -X POST http://localhost:8080/requests/<request-id>/approve -H "Authorization: Bearer <user-jwt>" \
  -H "Content-Type: application/json" -d "{\"note\":\"Approved\"}"
curl http://localhost:8080/requests/<request-id>/document -H "Authorization: Bearer <shop-jwt>"
curl -X POST http://localhost:8080/access/view -H "Authorization: Bearer <shop-jwt>" \
  -H "Content-Type: application/json" -d "{\"token\":\"<policy-token>\"}"
curl -X POST http://localhost:8080/requests/edit -H "Authorization: Bearer <shop-jwt>" \
  -H "Content-Type: application/json" \
  -d "{\"token\":\"<policy-token>\",\"changeNote\":\"Add label\",\"overlays\":[{\"page\":1,\"x\":72,\"y\":72,\"text\":\"Approved\",\"fontSize\":12}]}"
curl http://localhost:8080/documents/<document-id>/versions -H "Authorization: Bearer <user-jwt>"
curl http://localhost:8080/documents/<document-id>/versions/<version-id>/content \
  -H "Authorization: Bearer <user-jwt>"
curl "http://localhost:8080/admin/users?role=PRINT_SHOP&status=PENDING_APPROVAL" \
  -H "Authorization: Bearer <admin-jwt>"
curl http://localhost:8080/admin/documents -H "Authorization: Bearer <admin-jwt>"
curl http://localhost:8080/admin/stats -H "Authorization: Bearer <admin-jwt>"
curl http://localhost:8080/documents/summary -H "Authorization: Bearer <user-jwt>"
curl "http://localhost:8080/owners/me/requests?status=REJECTED" -H "Authorization: Bearer <user-jwt>"
```
