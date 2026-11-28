# Demo notes: Muhammad Ismail (25I-0579)

My use cases: **UC01, UC02, UC03, UC06, UC07, UC12** (WORK_DIVISION.md §5). Added under CONFLICTS.md S-005 / I-001.
For each one: screen → D5 controller → SD steps → domain → repository → Oracle table, the D3 extensions, and likely
questions. Section 1 is the setup guide for a **live** demo (real OpenAI, Apollo and Gmail SMTP).

Code paths are `package.Class.method`. In IntelliJ press Ctrl+N for a class, Ctrl+F12 for a method.

---

## 0. Before the demo (5 minutes)

1. **Oracle is running.** Windows Services: `OracleServiceFREE` and `OracleOraDB23Home1TNSListener` both *Running*.
   If Oracle is down, the app now shows *"Unable to connect to database"* at startup (UC01 extension) instead of crashing.
2. **Fresh data.** As BLMS in SQL Developer: `99_drop.sql`, then `02_schema.sql`, then `03_seed.sql`.
3. **Keys.** `config/local.properties` has the keys you want live (section 1). Restart the app after editing it.
4. **Checks green:** `python3 scripts/check-d5-conformance.py`, `bash scripts/check-architecture.sh`, `mvn -q verify`.
5. **One story, both of us:** UC01 login → UC02 workspace → UC03 add 2 leads with *your own* addresses → UC04 CSV
   → UC08 qualify → UC05 discovery → UC09 → UC10 → UC11 (mail arrives in your inbox) → UC06 edit/archive → UC07 note → UC12 export.

Three GRASP ideas to say once at the start (same as Sufiyan):
- **Controller:** each screen's `*ViewController` calls exactly one of the six D5 `controller/*Controller` classes.
- **Pure Fabrication:** all SQL is in the four repositories; services and `BackgroundTaskRunner` keep the domain clean.
- **Information Expert:** each domain object changes its own data (`Lead.applyUpdates`, `Lead.archive`, `Lead.addInteraction`).

MVC in one sentence: *FXML + `ui/` is the View, the D5 controllers are the Controller, `domain/` is the Model, and
`ui/` never imports `persistence`, `integration`, `service` or `security` (rule R2, checked by `check-architecture.sh`).*

---

## 1. Live-API setup guide (where to get every key)

All secrets go in **`config/local.properties`** (git-ignored, never committed). It overrides
`src/main/resources/application.properties`. Blank value = offline mode for that service only (gap G12).

```properties
db.url=jdbc:oracle:thin:@//localhost:1521/FREEPDB1
db.user=blms
db.password=blms
security.credentialKey=<a long random string - DO NOT change it after saving SMTP settings>
llm.apiKey=sk-...            # OpenAI (UC09)
b2b.apiKey=...               # Apollo.io (UC05) - read 1.2 first
```

### 1.1 OpenAI: UC09 Generate Emails via LLM. **Works live as the code is now.**
| Step | What to do |
|---|---|
| Account | Sign up at **platform.openai.com** (now redirects to developers.openai.com). |
| Billing | *Settings → Billing*: add a card and buy the minimum credit (USD 5). Without credit, calls fail and the app shows "LLM API failure (HTTP 429)". |
| Key | *API keys → Create new secret key* → copy `sk-...` once → paste into `llm.apiKey`. |
| Model | The code uses `gpt-4o-mini` on `POST /v1/chat/completions`. OpenAI lists it as **deprecated, shutdown 14 Jan 2027**, so it works for our demo. The replacement they name is `gpt-6-luna` (Sufiyan's file `OpenAiLlmClient.MODEL` if it is ever needed). |
| Cost | A batch of 10 drafts costs a fraction of a cent. |
| Test | Qualify a lead (UC08), select it in Leads, click **Generate Drafts** → "1 draft(s) generated" → open the lead → the draft is a real personalised email, not the fixed template. |

### 1.2 Apollo.io: UC05 Discover Leads via B2B API. **Fixed (CONFLICTS.md I-002 / I-003); not yet tested against the real Apollo.**
| Step | What to do |
|---|---|
| Account | Sign up at **apollo.io** with a **work/university email** (Apollo says free API access needs a work-email account). |
| Key | *Settings → Integrations → API Keys → Create new key*. Tick **People API Search** AND **Bulk People Enrichment** (or "Set as master key"). Paste into `b2b.apiKey`. A key missing an endpoint gives "rejected the API key (HTTP 403)". |
| How it works | `B2BApiLeadSource.fetchLeads`: (1) `POST /api/v1/mixed_people/api_search?q_keywords=<Target Industry>&per_page=10&page=1` (0 credits, no emails), (2) `POST /api/v1/people/bulk_match` with those 10 ids → emails, last names, company industry/phone. Still ONE `fetchLeads()` for the caller (Adapter). |
| Credits | Search is free; enrichment costs 1 credit per person whose email is revealed, so at most **10 credits per discovery run**. Check your credit balance in Apollo before the demo. People Apollo can't match come back without an email and are counted as "skipped (missing contact data)". |
| Target Industry | It's sent as free-text keywords, so `Fintech`, `SaaS`, `Healthcare` all work. |
| Rehearse | Run discovery once the day before: expect "N new lead(s) saved, M already in the CRM, K skipped". If it says 0 saved and all skipped, your plan has no email credits left. |
| Caution | These are **real people**. Do not send UC11 emails to them; use your own addresses for UC11. |

### 1.3 Gmail SMTP: UC11 Dispatch Automated Outreach. **Works live as the code is now.**
SMTP is **not** in `local.properties`. It is entered in the app in **Workspace Settings (UC02, admin only)** and
stored AES-GCM encrypted in `workspaces.smtp_credential_encrypted`.

| Step | What to do |
|---|---|
| 2-Step Verification | myaccount.google.com → *Security* → turn on **2-Step Verification** (required for app passwords). |
| App password | myaccount.google.com/apppasswords → name it "BLMS" → copy the **16-character password** (spaces don't matter). Your normal Gmail password will NOT work ("SMTP authentication failed"). |
| Workspace Settings | Host **`smtp.gmail.com`**, Port **`587`** (STARTTLS; `465` = SSL also works), Username = **your full Gmail address**, Password = the app password. Click Save → "Workspace Configured Successfully". |
| Re-enter rule | The password must be typed again on **every** save (it is never sent back to the UI, rule 4.8). |
| Recipients | **Only send to addresses you own.** Add 2 leads in UC03 with your second Gmail / university address, qualify them, generate, approve, send. The `.example` addresses in the sample CSV don't exist: Gmail accepts the message and then bounces it to your inbox. Don't send to them. |
| Test | Leads → select the lead → **Send Emails** → "1 email(s) sent - those leads are now CONTACTED" → check the inbox (and Spam). |
| Other providers | Outlook/Office 365: `smtp.office365.com` 587 (many accounts have SMTP AUTH disabled). Brevo/Mailtrap also work with their SMTP host + key. |

> **Warning:** once a real SMTP host is saved, offline mail mode is off, so every Send really sends.
> **Warning:** changing `security.credentialKey` later makes the saved password unreadable. The app then says
> "The saved SMTP password cannot be read - re-enter it in Workspace Settings".

### 1.4 Fallback during the demo
If Wi-Fi or a quota fails mid-demo: blank the key in `local.properties` and restart. That service switches to offline mode
(G12) **inside the same D5 class** (no mock classes). Error messages you may see, and what they mean:
`LLM API failure (HTTP 401)` wrong OpenAI key · `(HTTP 429)` no credit/rate limit · `SMTP authentication failed` wrong
app password · `SMTP server unreachable` host/port or network · `Network timeout - check your internet connection` (UC05).

---

## UC01: Authenticate System User (SD01)

**Screen:** `login.fxml` → `ui.controller.LoginViewController.onLogin`. The call runs in a JavaFX `Service`, so the UI
never freezes and the button is disabled while it runs.

| SD step | Code | Pattern |
|---|---|---|
| 1 `login(username, password)` | `controller.LoginController.login` | **Controller** |
| 2-3 `findByUsername(u)` → `UserAccount` | `persistence.UserRepository.findByUsername`: `PreparedStatement` with `?` (UC01 tech variation: no SQL injection) | **Pure Fabrication** |
| 4-5 `verifyPassword(raw, hasher)` → `matches` | `domain.UserAccount.verifyPassword` → `security.BCryptPasswordHasher.matches` | **Information Expert** (the account checks its own password); **Protected Variations** (`PasswordHasher` interface) |
| 6 «create» `Session(user)` | `LoginController` creates `domain.Session` | **Creator** |
| 7 `LoginResult(username, role)` | UI → `SceneManager.switchTo(MAIN_SHELL)` → `MainShellViewController.setLoginResult` | only ADMIN sees "Workspace Settings" (D2 actors) |

| D3 extension | Where | User sees |
|---|---|---|
| Wrong username/password | `LoginController.login` → `AuthenticationException` | red label "Incorrect username or password", password cleared |
| Database unreachable | `AppContext` / `JdbcRepository` → `DataAccessException` | alert "Unable to connect to database" (startup: `MainApp.init/start`) |

Questions: *Where are passwords stored?* `users.password_hash`, BCrypt only (seed has hashes, never plain text).
*Why Session?* D5 `Session.user 1`; LeadController reads the current user from it (gap G6, `Supplier<Session>`). *Logout?* Not a D3 UC (G15): it returns to login.

## UC02: Configure Company Workspace (SD02)

**Screen:** `workspace_settings.fxml` → `WorkspaceSettingsViewController` (`initialize` = open, `onSave` = save).

| SD step | Code | Pattern |
|---|---|---|
| 1-4 `openWorkspaceSettings()` → `WorkspaceForm` | `WorkspaceController.openWorkspaceSettings` → `WorkspaceRepository.findCurrent` (no password in the form) | Controller |
| 5 `saveWorkspace(name, industry, services, smtp)` | `WorkspaceController.saveWorkspace`: all fields validated first, nothing changes on error | |
| 6-7 `encrypt(password)` | `security.CredentialEncryptor.encrypt`: AES-GCM, key = SHA-256 of `security.credentialKey` | Pure Fabrication |
| 8-9 `updateDetails`, `setSmtpCredentials(host, port, user, cipher)` | `domain.Workspace` | Information Expert |
| 10-12 `generateIcp()` → «create» `IdealCustomerProfile` | `Workspace.generateIcp` | **Creator** (Workspace composes its ICP, 0..1) |
| 13-15 `repo.save(ws)` → `WorkspaceResult` | `WorkspaceRepository.save` writes `workspaces` + `ideal_customer_profiles` | composition: the whole saves its parts |

| D3 extension | User sees |
|---|---|
| Missing required fields | every empty field turns red (`field-error`), one message lists them all, nothing saved |
| Non-numeric / out-of-range port | port field red, "SMTP Port must be a valid number" / "between 1 and 65535" |
| Bad host format | "Invalid SMTP host - check the server name (e.g. smtp.gmail.com) and port" |
| Background SMTP test (G13) | not in SD02. A wrong password shows up in UC11 as "SMTP authentication failed" |

Questions: *Why is the ICP important?* UC05 builds its search query from it (`IdealCustomerProfile.toSearchQuery`).
*Multi-profile workspaces (D1)?* One active workspace per installation (G9).

## UC03: Manually Add New Lead (SD03)

**Screen:** `leads.fxml` "Add Lead" → `lead_form.fxml` dialog, `LeadFormViewController.openForAdd` / `onSave` (save in a `Task`).

| SD step | Code | Pattern |
|---|---|---|
| 1-2 `requestNewLeadForm()` → `LeadForm` | `LeadController.requestNewLeadForm` | Controller |
| 3 `saveLead(company, contact, email, role)` | `LeadController.saveLead` | |
| 4-5 `existsByEmailDomain(email)` | `LeadRepository.existsByEmailDomain` (G11: duplicates by email domain) | Pure Fabrication |
| 6-10 `ws.createLead(data)` → «create» `Lead` → «create» `Contact` → «create» `DiscoveredState` | `domain.Workspace.createLead`, `Lead(LeadData)` | **Creator** chain; State pattern starts at DISCOVERED |
| 11-13 `leads.save(lead)` | `LeadRepository.save` (lead + contacts in one transaction) | |

| D3 extension | User sees |
|---|---|
| Duplicate (same email domain) | email red, message, then "Open the existing lead's profile?" → opens it |
| Invalid email / missing field | `Validator` → `ValidationException` → red field + error label; nothing saved |

Question: *Why validate in the business layer and not only the UI?* Rubric "input validation for all values". CSV
and API imports bypass the form, so `Validator` in `util/` protects every path.

## UC06: Manage Existing Leads (SD06)

**Screen:** `leads.fxml` → `LeadsViewController`: filter bar (search, industry, stage, *Show archived*), multi-select
table, double-click = profile; "Edit" → `lead_form.fxml` in edit mode with an **Archive** checkbox.

| SD step | Code | Pattern |
|---|---|---|
| 1-5 `filterLeads(LeadFilter)` → `List<Lead>` | `LeadController.filterLeads` stores `currentFilter`, `LeadRepository.findByFilter` (runs in a `Service`, spinner) | |
| 6-9 `editLead(id)` → `Lead` | `LeadRepository.findById` | |
| 10-13 `saveLeadChanges(id, LeadData, archived)` → `applyUpdates` | `domain.Lead.applyUpdates` | Information Expert |
| 14 opt `archive()` | `Lead.archive` → `is_archived = 1` | **soft delete**: no `DELETE` anywhere (rule R7) |
| 15-18 `save(lead)` → `findByFilter(currentFilter)` | the table refreshes with the same filter | |

| D3 extension | User sees |
|---|---|
| Database error while saving | `JdbcRepository.inTransaction` rolls back → "Unable to save to the database - the change was rolled back"; data unchanged |
| No match | table placeholder "No leads found" |

Question: *How do you restore an archived lead?* Tick "Show archived", edit, untick Archive. *Why archive instead of delete?*
D3 UC06 + history (interactions, drafts) must stay for reporting/export.

## UC07: Log Manual Interaction (SD07)

**Screen:** `lead_detail.fxml` → `LeadDetailViewController` (timeline list, note box, `onAddNote`, `onSubmitNote`).

| SD step | Code | Pattern |
|---|---|---|
| 1-2 `addInteractionNote(id)` → `NoteForm` | `LeadController.addInteractionNote` | Controller |
| 3-5 `submitInteraction(id, note)` → `session.getUser()` | `LeadController.submitInteraction` | Session gives the **author** (`InteractionLog.author 1`) |
| 6-10 `findById` → `lead.addInteraction(note, user)` → «create» `InteractionLog` | `domain.Lead.addInteraction` | **Creator** (Lead records its history) |
| 11-12 `leads.save(lead)` → log | `LeadRepository.save` inserts new `interaction_logs` rows | |

| D3 extension | Where |
|---|---|
| Submit disabled until text is entered | `submitNoteButton.disableProperty()` bound to "note is blank" (UI), plus the `InteractionLog` constructor rejects blank text via `Validator.requireText` (BL) |

The same screen shows failed UC11 send attempts next to the timeline (G14).

## UC12: Export Outreach Results (SD12)

**Screen:** `export.fxml` → `ExportViewController`.

| SD step | Code | Pattern |
|---|---|---|
| 1-2 `openExportOptions()` → `ExportCriteria[]` | "All Leads" + "Only <STATUS> leads" | |
| 3-9 `requestExport(criteria)` → `findByFilter(criteria.toFilter())` → `Exporter.format` | `ExportController.requestExport` keeps `pendingRows` | **Strategy / Protected Variations**: `Exporter` interface, `CsvExporter` today |
| 10-12 `chooseDestination(path)` → `int` | `CsvExporter.write` | Pure Fabrication (file I/O out of the domain) |

| D3 extension | User sees |
|---|---|
| No records match | warning "No records match ... - the export is empty"; no file dialog, no file |
| Write denied (read-only folder) | "Cannot write there - choose another folder" → the save dialog opens again (retry loop) |

Question: *Why two calls (requestExport, then chooseDestination)?* That is D4's SSD: the system prepares the data, then
the actor picks the destination. Adding PDF/Excel later = a new `Exporter` class, no controller change.

---

## 2. UI-wide points the examiner may ask (rubric: User Interface 10)

- **No frozen UI:** login, lead save, filter and CSV import run in JavaFX `Task`/`Service`. UC05/UC09/UC11 run on
  `BackgroundTaskRunner` and come back on the FX thread only via `ui.util.FxAsync.onComplete` → `Platform.runLater`.
  Batch buttons are disabled while a job runs, so a double click can't send emails twice.
- **One navigation class:** `ui.navigation.SceneManager` (+ `View` enum). Controllers never load FXML or create Stages.
- **One message class:** `ui.util.AlertHelper`. Every `BlmsException` message is the D3 extension text.
- **One stylesheet:** `css/app.css` (`field-error`, `row-archived`, `metric-card`, sidebar).
