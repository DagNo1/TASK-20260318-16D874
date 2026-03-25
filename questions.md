# Business Logic Questions Log

## 1. JWT Token Expiry Duration
- **Question:** Prompt does not specify JWT access token or refresh token expiry time.
- **Assumption:** Access token expires in 2 hours, refresh token in 7 days.
- **Solution:** Configured as environment variables so it can be adjusted without code changes.

## 2. Role Permission Scope
- **Question:** Prompt lists 4 roles (platform administrator, merchant operator, regular buyer, reviewer) but does not define which endpoints each role can access.
- **Assumption:** Admin has full access; merchants manage their own products/inventory; buyers can browse, purchase, and message; reviewers can only view reports and audit logs.
- **Solution:** Implemented Spring Security with @PreAuthorize role-based annotations per endpoint.

## 3. Cooking Timer — Server-side or Client-side?
- **Question:** Prompt requires "multiple parallel timers" and "progress saved every 30 seconds or on step changes" but does not explicitly define timer state ownership.
- **Assumption:** For this pure backend prompt, timer/session state is server-authoritative; clients may run local countdown only for UI display.
- **Solution:** Backend provides APIs to create/start/pause/resume parallel timers and persist checkpoints every 30 seconds or on step changes; client syncs with backend and restores from server state.

## 4. Report Export Format
- **Question:** Prompt mentions "template exports for completion certificates and assessment forms" but does not specify file format.
- **Assumption:** Excel (.xlsx) for data reports, PDF for certificates and assessment forms.
- **Solution:** Used Apache POI for Excel export and iText for PDF generation.

## 5. Message Retention Cleanup — Hard or Soft Delete?
- **Question:** Default message retention is 180 days but prompt does not specify whether expired messages are hard or soft deleted.
- **Assumption:** Soft delete first (deleted_at marked), then hard deleted by a separate cleanup job after a grace period.
- **Solution:** Added deleted_at field to messages table; scheduler marks expired messages daily and hard deletes after 7-day grace period.

## 6. Inventory Alert Notification Channel
- **Question:** Prompt specifies inventory alerts trigger when stock ≤ 10 but does not clarify how merchants are notified (since SMS and email are disabled).
- **Assumption:** Alerts delivered via the internal notification system only.
- **Solution:** Inventory check triggers an internal notification record for the merchant on each stock update.

## 7. Duplicate Message Folding Scope
- **Question:** Prompt says "duplicate text within the same session folded within 10 seconds" — does folding mean hiding from display only, or blocking the message from being stored?
- **Assumption:** Message is stored but flagged as a duplicate; UI layer handles folding display. Backend deduplication prevents storing identical content from same sender within 10 seconds.
- **Solution:** Added a fingerprint hash + timestamp check on message insert; returns a duplicate flag in response.

## 8. Achievement Version Rollback
- **Question:** Prompt states "version numbers increment for the same achievement and cannot be rolled back" — does this mean the API should actively reject rollback requests or just not provide the feature?
- **Assumption:** API should actively reject any request that attempts to set a version number lower than or equal to the current version.
- **Solution:** Version increment is enforced at service layer; any non-incrementing version change throws a validation exception.

## 9. Dual Approval Workflow
- **Question:** Prompt requires "dual approval for permission changes and critical operations" but does not define who the two approvers are or what the approval flow looks like.
- **Assumption:** First approval by the initiating admin, second approval required from a different admin account. Cannot self-approve.
- **Solution:** Implemented an approval_requests table with status tracking (pending/approved/rejected); operation executes only when two distinct admin approvals are recorded.

## 10. Scheduled Report Generation Time Zone
- **Question:** Prompt says reports generate daily at 2:00 AM but does not specify the time zone.
- **Assumption:** Server local time (UTC+8, China Standard Time) since the system is described as operating offline in a local environment.
- **Solution:** Cron expression set to "0 0 2 * * ?" with server timezone configured to Asia/Shanghai in application.yml.