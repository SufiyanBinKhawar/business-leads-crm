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

### I-001 · OWNERSHIP-EXCEPTION · Status: DONE
- **Date:** 2026-11-28
- **Raised by:** Ismail
- **Files:** none (sign-off record only)
- **Problem:** S-004, S-005 and S-006 record my agreement as "verbal ... GitHub confirmation to follow".
- **Proposal:** this entry is that written confirmation.
- **Diagram impact:** S-004 only (Fig. 1 / G8, already noted by Sufiyan).
- **Agreed by:** Ismail (2026-11-28) confirms S-004 (CONTRACT-CHANGE `addLoadedDraft` / `addLoadedAttempt`),
  S-005 (`docs/DEMO_NOTES_SUFIYAN.md`) and S-006 (UI completion, PR #9).
- **Resolution:** I add `docs/DEMO_NOTES_ISMAIL.md` under the permission given in S-005.

### I-002 · DESIGN-DEVIATION · Status: OPEN (for Sufiyan: owner of `integration/source/**`, `domain/**`)
- **Date:** 2026-11-28
- **Raised by:** Ismail
- **Files:** src/main/java/com/blms/integration/source/B2BApiLeadSource.java,
  src/main/java/com/blms/domain/IdealCustomerProfile.java (`toSearchQuery`)
- **Problem:** we want to present UC05 against the real Apollo API. Apollo's docs (docs.apollo.io, People API
  Search, read 2026-11-28) say the call that works for an API key is **`POST /api/v1/mixed_people/api_search`**
  with header `x-api-key`, query parameters `q_keywords`, `organization_industries[]` (lowercase Apollo industry
  names), `person_titles[]`, `page` and `per_page`. Our code sends **GET `/mixed_people/search`** with `industry=` and
  `keywords=`, which Apollo doesn't recognise. Worse, the docs state that search "doesn't return email addresses
  or phone numbers" (the last name is obfuscated as well). So even a successful call gives rows with no email. They are all
  counted `invalid`, and live UC05 always imports **0 leads**. Offline mode (G12) is unaffected.
- **Proposal (smallest change, no public API change):**
  1. `B2BApiLeadSource`: default endpoint → `https://api.apollo.io/api/v1/mixed_people/api_search`, private `get(...)`
     → POST with an empty body (`BodyPublishers.noBody()`), keep the `x-api-key` header.
  2. `IdealCustomerProfile.toSearchQuery()` returns
     `q_keywords=<services>&organization_industries[]=<industry lowercased>&per_page=10&page=1`, all URL-encoded.
  3. To get emails: after the search, one private call to Apollo **bulk people enrichment**
     (`POST /api/v1/people/bulk_match`, up to 10 `id`s, 1 credit per email revealed), mapping `email`,
     `last_name` and `organization.phone`. Then the D3 "missing fields become null" rule still applies.
  4. Update `B2BApiLeadSourceTest` to the new request shape.
- **Diagram impact:** D5 SD05 steps 13-14 say "HTTP GET /people/search(query)". Redraw as "HTTP POST
  /mixed_people/api_search(query)" plus an enrichment self-call inside `B2BApiLeadSource` (Adapter, still one
  `fetchLeads()` from the caller's point of view). No class, method or association changes.
- **Agreed by:** Ismail (2026-11-28)
- **Resolution:** waiting for Sufiyan. If not signed before code freeze, UC05 is presented in offline mode (G12),
  and the other live services (OpenAI UC09, Gmail SMTP UC11) are unaffected.
- **Follow-up (2026-11-28, Ismail):** Sufiyan approved I-002 and I-003 and told Ismail so; recorded by Ismail, GitHub
  confirmation on the PR to follow (same practice as S-004..S-006). **Agreed by:** Ismail (2026-11-28),
  Sufiyan (2026-11-28 - approval given to Ismail, recorded by Ismail; GitHub confirmation to follow). Status → DONE.
  Implemented on `ui/final-polish`:
  - `B2BApiLeadSource`: POST `/api/v1/mixed_people/api_search`; then private POST `/api/v1/people/bulk_match`
    (batches of 10, `reveal_personal_emails=false&reveal_phone_number=false`). An unmatched (`null`) entry keeps the
    search data and has no email, so it is reported invalid. People who already have an email are not enriched.
    403 message names the two endpoints a scoped key needs.
  - `IdealCustomerProfile.toSearchQuery()` → `q_keywords=<target industry>&per_page=10&page=1`. **Change from
    the proposal:** no `organization_industries[]`, because it only accepts Apollo's exact lowercase industry names
    and a typed industry like "Fintech" would return nothing. per_page 10 = at most 10 credits per run.
  - `LeadIngestionController.OPERATION` (ApiCallLog text) → `POST /mixed_people/api_search + /people/bulk_match`.
  - Tests: `B2BApiLeadSourceTest` rewritten against a local fake Apollo (search, enrichment, batching, 429, 401/403,
    garbage, network); `WorkspaceControllerTest`, `LeadIngestionControllerTest` expected strings updated.
  - `docs/DESIGN_TRACEABILITY.md` UC05 row updated. **Redraw D5 SD05 steps 13-14** (POST + enrichment inside the
    adapter). No class, public method, attribute or association changed, so `scripts/d5-contract.txt` is unchanged.
  - Sufiyan: `docs/DEMO_NOTES_SUFIYAN.md` lines 92 and 94 still say "GET /people/search". Please update your file.

### I-003 · OWNERSHIP-EXCEPTION · Status: DONE
- **Date:** 2026-11-28
- **Raised by:** Ismail
- **Files (Sufiyan's):** src/main/java/com/blms/integration/source/B2BApiLeadSource.java,
  src/main/java/com/blms/domain/IdealCustomerProfile.java, src/main/java/com/blms/controller/LeadIngestionController.java
  (one constant), src/test/java/com/blms/integration/source/B2BApiLeadSourceTest.java,
  src/test/java/com/blms/controller/WorkspaceControllerTest.java, src/test/java/com/blms/controller/LeadIngestionControllerTest.java;
  shared: docs/DESIGN_TRACEABILITY.md (UC05 row).
- **Problem:** I-002 must be in before code freeze for a live-API demo, and Sufiyan asked Ismail to apply it.
- **Proposal:** Ismail edits exactly those files for I-002 on `ui/final-polish`; Sufiyan reviews the PR.
- **Diagram impact:** as I-002 (SD05).
- **Agreed by:** Ismail (2026-11-28), Sufiyan (2026-11-28 - approval given to Ismail, recorded by Ismail; GitHub confirmation to follow)
- **Resolution:** same branch and PR as I-002.

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

### S-003 · SCHEMA-CHANGE · Status: DONE (verified - no change needed)
- **Date:** 2026-11-25
- **Raised by:** Sufiyan
- **Files:** src/main/resources/db/01_create_user.sql, 02_schema.sql, 03_seed.sql (unchanged)
- **Problem:** the scripts had never run against a real Oracle database.
- **Resolution:** ran on Oracle AI Database 26ai Free 23.26.4 for Windows (the renamed 23ai Free; service still
  `FREEPDB1`): 01 as SYSTEM (user created, grants OK), 02 as BLMS (9 tables + 5 indexes), 03 as BLMS (1 workspace,
  2 users, commit). No error, so `02_schema.sql` is unchanged and nobody needs to re-run anything.
  `AppContext` boots against it; `admin/admin123` (ADMIN) and `rep/rep123` (SALES_REP) log in, a wrong password
  gives "Incorrect username or password"; `mvn javafx:run` starts the UI.
  Note for setup: run 01 connected to the PDB (`//localhost:1521/FREEPDB1`), not the CDB root (ORA-65096).

### S-004 · CONTRACT-CHANGE · Status: DONE
- **Date:** 2026-11-25
- **Raised by:** Sufiyan
- **Files:** src/main/java/com/blms/domain/Lead.java, src/main/java/com/blms/domain/EmailDraft.java,
  src/main/java/com/blms/persistence/LeadRepository.java, scripts/d5-contract.txt
- **Problem:** `LeadRepository.findById` (SD06 steps 7-8) should return the lead with its `+drafts` (UC10 step 1 "clicks
  on a lead with a 'Draft Ready' status"; I-4 `draftsList <- lead.getDrafts()`), and each draft with its `+attempts`
  (gap G14: the lead profile shows failed send attempts). But `Lead.drafts` and `EmailDraft.attempts` are private
  lists whose only adders create NEW objects (`Lead.createDraft`, `EmailDraft.recordDispatch`). A draft or attempt
  read from Oracle cannot be attached, so `lead.getDrafts()` is always empty after a reload.
- **Proposal:** two load helpers in the same style as the approved G8 ones (`addLoadedContact`,
  `addLoadedInteraction`): `public void Lead.addLoadedDraft(EmailDraft d)` and
  `public void EmailDraft.addLoadedAttempt(OutreachDispatch a)`; contract lines `!load addLoadedDraft` /
  `!load addLoadedAttempt`; `LeadRepository.findById` then loads drafts (with `+recipient`) and their attempts.
- **Diagram impact:** D5 Fig. 1 - add the two operations to Lead and EmailDraft (or list them under gap G8).
- **Agreed by:** Sufiyan (2026-11-25), Ismail (2026-11-26 - verbal approval given in person, recorded by Sufiyan; GitHub confirmation to follow)
- **Resolution:** until both sign, `findById` loads creator, contacts and history only; the drafts list stays empty.

### S-005 · OWNERSHIP-EXCEPTION · Status: DONE
- **Date:** 2026-11-26
- **Raised by:** Sufiyan
- **Files:** docs/DEMO_NOTES_SUFIYAN.md (new file)
- **Problem:** `docs/**` is shared (WORK_DIVISION.md §2). I need a personal demo script for my use cases
  (UC04, UC05, UC08, UC09, UC10, UC11).
- **Proposal:** add the new file only; no existing doc is changed. Ismail may add `docs/DEMO_NOTES_ISMAIL.md` the same way.
- **Diagram impact:** none.
- **Agreed by:** Sufiyan (2026-11-26), Ismail (2026-11-26 - verbal approval given in person, recorded by Sufiyan; GitHub confirmation to follow)
- **Resolution:** PR from `bl/demo-notes`, merged in PR #7.
- **Follow-up (2026-11-26, Sufiyan):** implemented on `bl/s004-draft-loading`: `Lead.addLoadedDraft`,
  `EmailDraft.addLoadedAttempt`, contract lines `!load addLoadedDraft` / `!load addLoadedAttempt`, and
  `LeadRepository.findById` now loads drafts (with `+recipient`) and their attempts. **Redraw D5 Fig. 1** (or list both
  under G8). Merged in PR #8.

### S-006 · OWNERSHIP-EXCEPTION · Status: DONE
- **Date:** 2026-11-26
- **Raised by:** Sufiyan (at Ismail's request)
- **Files:** src/main/java/com/blms/ui/**, src/main/resources/com/blms/ui/** (Ismail's files)
- **Problem:** Ismail asked Sufiyan to finish the remaining UI tasks (I-4 lead profile, I-6 CSV mapping grid, I-8 draft
  review, I-9 Generate/Send buttons, placeholder clean-up) so the whole UC01-UC12 flow works for the demo.
- **Proposal:** Sufiyan edits those UI files on `bl/ui-completion`, building on Ismail's `ui/phase1-leads` commit. Only
  existing D5 controller methods are called; no contract change. Ismail reviews the PR.
- **Diagram impact:** none.
- **Agreed by:** Sufiyan (2026-11-26), Ismail (2026-11-26 - verbal approval given in person, recorded by Sufiyan; GitHub confirmation to follow)
- **Resolution:** merged in PR #9 (Ismail approved in person).
