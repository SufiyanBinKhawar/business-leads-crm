# CONFLICTS.md

The single log for anything that breaks the "disjoint files, frozen design" plan.
Rules: `AGENTS.md` §7. In short:

- **Append only.** Never edit or delete another member's entry — add a follow-up line instead.
- **Write only in your own section** (Ismail → §A, Sufiyan → §B), so this file itself never causes a git conflict.
- Number entries `I-001…` (Ismail) and `S-001…` (Sufiyan). Status: `OPEN` → `AGREED` → `DONE` (or `REJECTED`).
- An entry that changes the design is valid only when **both** names are in "Agreed by".

## 1. Entry types and template

| Type | Use when |
|---|---|
| `MERGE-CONFLICT` | git reported a conflict — record files, both intents, how it was resolved |
| `CONTRACT-CHANGE` | you need a new/changed `dto` field, controller method or return type |
| `DESIGN-DEVIATION` | code must differ from D3/D4/D5 — name the diagram that must be redrawn |
| `OWNERSHIP-EXCEPTION` | you had to edit a file owned by the other member |
| `DEPENDENCY` | adding/upgrading a Maven dependency |
| `SCHEMA-CHANGE` | `db/02_schema.sql` changed — the other member must re-run the scripts |

```markdown
### I-000 · CONTRACT-CHANGE · Status: OPEN
- **Date:** 2026-11-26
- **Raised by:** Ismail
- **Files:** src/main/java/com/blms/controller/LeadController.java
- **Problem:** what blocks you, with the use case / SD step
- **Proposal:** the smallest change that solves it
- **Diagram impact:** none / D5 Fig. N + SD-NN must be redrawn
- **Agreed by:** Ismail (date), Sufiyan (date)
- **Resolution:** commit hash, what changed. Status → DONE.
```

---

## 2. Standing decisions — D5 gaps (decided 2026-11-25, apply until a signed entry changes them)

D5 is the design, but it is silent or self-inconsistent in the places below. Each decision is the
**smallest** addition that lets the D3 use case work. Items marked ✏️ add something public that is not drawn in
D5 — **redraw D5 to include them before final documentation**, and be ready to explain them in the demo.
`scripts/d5-contract.txt` marks each one with `!gap:Gn`.

| ID | Where D5 is silent | Decision in code | ✏️ |
|---|---|---|---|
| **G1** | SD05/09/11 return a `jobId`; the SSD replies `discoveryCompleted`, `draftsReady(…, count)`, `dispatchCompleted(sentCount)` have no carrier. | `BackgroundTaskRunner.onComplete(jobId, onSuccess, onError)`; results read from `LeadIngestionController.getLastImportSummary()`, `OutreachController.getLastResultCount()/getLastFailureCount()`, `DraftGenerationService.getLastFailures()` (UC09 extension "report failures at the end"). | ✏️ |
| **G2** | SD11 passes `ws.getSmtpCredentials()` straight to `dispatch`, but the password is stored encrypted (SD02). Nobody decrypts it. | `Workspace.getSmtpCredentials()` returns the cipher text; `OutreachController` holds `+encryptor CredentialEncryptor` and decrypts before `dispatch`. | ✏️ (association) |
| **G3** | `JakartaMailSender` has `- smtp` but `EmailSender.send(to, subject, body)` has no way to receive it; `dispatch(drafts, smtp)` gets it. | `EmailSender.configure(smtp)` called by `OutreachDispatcher.dispatch` before the loop. | ✏️ |
| **G4** | SD05 creates an `ApiCallLog` via `ws.logApiCall` but never saves it. | `LeadIngestionController` also holds `WorkspaceRepository` and calls `save(ws)` after logging. | ✏️ (association) |
| **G5** | `Lead` has `+creator 1` but `Workspace.createLead(data)` has no user, and `LeadIngestionController`/`LeadImportService` have no session. | `LeadData` carries `creator`; `LeadImportService` gets the current `Session` (via G6) and stamps `row.withCreator(user)`. | ✏️ (association) |
| **G6** | `LeadController` has `- session : Session`, but the Session only exists after UC01. | Constructor receives `Supplier<Session>` (`loginController::getSession`); same for `LeadImportService`. Private — no public API change. | – |
| **G7** | D3 UC04 requires a background thread; SD04 is synchronous and returns `ImportSummary`. | Signature kept. The UI runs `confirmMappingAndImport` inside a JavaFX `Task`. | – |
| **G8** | Persistence and in-between-calls state the diagrams don't draw. | Allowed: id setters; FK fields `EmailDraft.leadId`, `OutreachDispatch.recipient` (SD11 passes it to the constructor); load helpers `Lead.addLoadedContact/addLoadedInteraction`, `Workspace.setLoadedIcp`, `PipelineState.forStatus`; `Lead.getPrimaryContact` (navigates `+contacts`); `LeadIngestionController.csvSource` (the `src` kept from SD04 step 2 to step 7) and `.b2bApiKey` (the `apiKey` of SD05 step 4). | – |
| **G9** | D1 says "multi-profile workspaces"; D5 has `WorkspaceRepository.findCurrent()` with no parameter. | One active workspace per installation (lowest `workspace_id`). Say so in the demo if asked about D1. | – |
| **G10** | The dashboard must show counts before any status change; D5 only returns `PipelineMetrics` from `changePipelineStatus`. | `LeadController.getPipelineMetrics()` = `metrics.recalculate(leads.findAll())`. | ✏️ |
| **G11** | D3 UC03 step 4 / D5 `existsByEmailDomain`: duplicates are by **email domain**. | Implemented exactly so — a second contact from the same company is reported as a duplicate. Same rule inside one CSV file. | – |
| **G12** | Demo must not depend on API keys, quotas or Wi-Fi. | Offline mode **inside** the D5 classes: blank `b2b.apiKey` → `B2BApiLeadSource` returns sample prospects; blank `llm.apiKey` → `OpenAiLlmClient` returns a template; no SMTP host → `JakartaMailSender` logs instead of sending. No extra mock classes. | – |
| **G13** | D3 UC02 extension "background SMTP test connection" is not in SD02 / `WorkspaceController`. | Host/port/username format is validated; a wrong password surfaces in UC11's "SMTP Authentication Error" extension. | – |
| **G14** | D3 UC11 extension "logs an error in the lead's timeline" — the dispatcher cannot reach the Lead. | Recorded as `OutreachDispatch("FAILED")` with `errorMessage` on that draft; the lead profile screen shows the failed attempt next to the timeline. Draft stays `APPROVED`. | – |
| **G15** | No logout use case in D3/D5. | UI "Logout" only returns to the login screen; the next login replaces the Session. | – |

Other fixed decisions (from the deliverables, not gaps):
- **Oracle** database (D1). SQLite/H2 are not allowed.
- `UserRole` = `ADMIN`, `SALES_REP`; `DraftStatus` = `DRAFT`, `APPROVED`, `SENT`; `deliveryStatus` = `"SENT"` / `"FAILED"` (D5).
- Pipeline transitions (D3 UC08 + D5 SD08): `DISCOVERED→QUALIFIED`, `QUALIFIED→CONTACTED`, `CONTACTED→CLOSED_WON`,
  any open state → `CLOSED_LOST`; closed states are terminal. UC11 moves dispatched leads `QUALIFIED→CONTACTED`.
- Only `QUALIFIED` leads get drafts (D3 UC09 "selects a batch of qualified leads").

---

## §A — Ismail's entries

<!-- Ismail: append below this line, newest at the bottom. -->

---

## §B — Sufiyan's entries

<!-- Sufiyan: append below this line, newest at the bottom. -->

### S-001 · SCHEMA-CHANGE · Status: DONE
- **Date:** 2026-11-25
- **Raised by:** Sufiyan (scaffold)
- **Files:** src/main/resources/db/01_create_user.sql, 02_schema.sql, 03_seed.sql, 99_drop.sql
- **Problem:** initial Oracle schema.
- **Resolution:** 9 tables, one per D4/D5 domain class. Both members run 01 (as SYSTEM), then 02 and 03 (as BLMS).

### S-002 · DESIGN-DEVIATION · Status: DONE
- **Date:** 2026-11-25
- **Raised by:** Sufiyan (scaffold)
- **Problem:** the first scaffold was written before D5 was available (DAO layer, event bus, mock classes, SQLite).
- **Resolution:** replaced completely to match D5 class-for-class on Oracle; the remaining differences are the
  gaps G1–G15 above. No pre-D5 class remains (`check-d5-conformance.py` enforces this).
