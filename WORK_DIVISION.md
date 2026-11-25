# WORK_DIVISION.md

| | **Muhammad Ismail (25I-0579)** | **Sufiyan Ahmed (25I-0557)** |
|---|---|---|
| Layer | **Presentation — JavaFX UI** | **Business logic + Oracle persistence + integrations** |
| GitHub | `Muhammad-Ismail-24` | `SufiyanBinKhawar` (repo owner) |
| Branch prefix | `ui/` | `bl/` |

Rules: `AGENTS.md`. Problems: `CONFLICTS.md`. Design reference: D5 in `docs/deliverables/`.

---

## 1. Why merges won't conflict

We edit **disjoint sets of files**. The only thing both sides depend on is the **contract** = the public
methods of the six D5 controllers, the D5 domain getters, and the `dto` types. All of it is already in the
scaffold with D5's exact signatures, and `scripts/check-d5-conformance.py` stops anyone from changing it.
Ismail codes screens against those signatures while Sufiyan fills in the bodies. An unfinished body throws
`UnsupportedOperationException("TODO UCxx …")`, which the UI shows as an error, so the UI runs from day one.

---

## 2. File ownership (only the owner edits)

### Ismail — UI
```
src/main/java/com/blms/app/Launcher.java, MainApp.java
src/main/java/com/blms/ui/**                    view controllers, SceneManager, View, AlertHelper, FxAsync
src/main/resources/com/blms/ui/**               all FXML, app.css, images
```

### Sufiyan — business layer + database
```
pom.xml, config/**, src/main/resources/application.properties, src/main/resources/db/**, sample/**
src/main/java/com/blms/app/AppContext.java, AppConfig.java
src/main/java/com/blms/controller/**            method BODIES (signatures = contract, §3)
src/main/java/com/blms/domain/**, service/**, integration/**, persistence/**, security/**, exception/**, util/**
src/test/**
```

### Shared — change only through a signed `CONFLICTS.md` entry
```
public signatures in controller/**, domain getters, dto/**        CONTRACT (Sufiyan edits after both sign)
scripts/d5-contract.txt                                            = the D5 design; both must sign
AGENTS.md, WORK_DIVISION.md, README.md, CLAUDE.md, GEMINI.md, .cursorrules, docs/**, scripts/**, .github/**
CONFLICTS.md                                                       each member appends only in their own section
```

---

## 3. The contract Ismail codes against (D5 Figures 2-3)

| Screen (Ismail) | D5 controller (Sufiyan) | Methods → return type |
|---|---|---|
| `login.fxml` | `LoginController` | `login(u, p)` → `LoginResult` |
| `workspace_settings.fxml` | `WorkspaceController` | `openWorkspaceSettings()` → `WorkspaceForm`; `saveWorkspace(name, industry, services, SmtpCredentials)` → `WorkspaceResult` |
| `leads.fxml`, `lead_form.fxml` | `LeadController` | `requestNewLeadForm()` → `LeadForm`; `saveLead(…)` → `Lead`; `filterLeads(LeadFilter)` → `List<Lead>`; `editLead(id)` → `Lead`; `saveLeadChanges(id, LeadData, archived)` → `List<Lead>` |
| `lead_detail.fxml` | `LeadController` | `addInteractionNote(id)` → `NoteForm`; `submitInteraction(id, note)` → `InteractionLog`; `selectLead(id)` → `PipelineStatus`; `changePipelineStatus(id, status)` → `PipelineMetrics` |
| `dashboard.fxml` | `LeadController`, `LeadIngestionController` | `getPipelineMetrics()` (gap G10); `runApiDiscovery()` → jobId |
| `csv_import.fxml` | `LeadIngestionController` | `selectCsvFile(File)` → `String[]`; `confirmMappingAndImport(Map)` → `ImportSummary` |
| `leads.fxml` (batch), `draft_review.fxml` | `OutreachController` | `generateDrafts(ids)` → jobId; `openDraft(id)` → `EmailDraft`; `approveRevisedDraft(id, s, b)` → `DraftStatus`; `sendApprovedEmails(ids)` → jobId |
| `export.fxml` | `ExportController` | `openExportOptions()` → `ExportCriteria[]`; `requestExport(c)`; `chooseDestination(path)` → `int` |

Jobs: `FxAsync.onComplete(jobId, onSuccess, AlertHelper::error)`, then read `getLastImportSummary()` /
`getLastResultCount()` / `getLastFailureCount()` (gap G1).
Domain objects returned to the UI are **read through getters only** (architecture rule R3).

Already working end-to-end (reference patterns): **UC01 login** (`LoginViewController` → `LoginController` →
`UserRepository` → Oracle `USERS` → `BCryptPasswordHasher`), `ExportController.openExportOptions`,
`LeadController.requestNewLeadForm`. Demo logins after running `03_seed.sql`: `admin/admin123`, `rep/rep123`.

---

## 4. Task lists

### Both — day 0 setup (Wed 25 Nov evening)
1. Install **Oracle Database 23ai Free** (Windows installer or Docker image) and SQL Developer.
2. As SYSTEM on `FREEPDB1`: run `db/01_create_user.sql`. As BLMS: run `02_schema.sql`, then `03_seed.sql`.
3. Clone the repo, open in IntelliJ (JDK 21), run `com.blms.app.Launcher`, log in as `admin`.
4. Read `AGENTS.md` fully. If you use an AI assistant, it reads `CLAUDE.md` / `GEMINI.md` /
   `.cursorrules` / `.github/copilot-instructions.md`, which all point to `AGENTS.md`.

### Ismail — UI (rubric: User Interface 10)
| # | Task | UC | Needs Sufiyan's |
|---|---|---|---|
| I-1 | Login + main shell polish; admin-only "Workspace Settings" button | UC01, UC02 | — (works now) |
| I-2 | Leads table (columns from `Lead` getters + `getPrimaryContact()`), filter bar, archived toggle, multi-select | UC06 | S-2 |
| I-3 | Lead form dialog: add mode, edit/archive mode, error label, duplicate warning | UC03, UC06 | S-2 |
| I-4 | Lead profile: status combo with revert on `InvalidStateTransitionException`, timeline, note box (Submit disabled when empty), drafts list + failed attempts (G14) | UC07, UC08 | S-3 |
| I-5 | Dashboard: metric cards, "Run API Discovery" with progress indicator | UC05, UC08 | S-3, S-5 |
| I-6 | CSV import dialog: FileChooser, header→field mapping grid, JavaFX `Task` + progress bar (G7), summary | UC04 | S-4 |
| I-7 | Workspace settings form: red `field-error` highlight, success message | UC02 | S-5 |
| I-8 | Draft review dialog: recipient, subject/body editor, Approve | UC10 | S-6 |
| I-9 | Batch buttons: Generate Drafts, Send Emails, result dialogs | UC09, UC11 | S-6, S-7 |
| I-10 | Export dialog: criteria combo, empty-export warning, save FileChooser, retry on write error | UC12 | S-7 |
| I-11 | Consistent styling, no frozen UI anywhere; screenshots of every screen for the documentation | all | — |

### Sufiyan — BL + DB (rubric: BL 15, OOP 15, DB 10, Architecture 10, Patterns 10)
Ordered so Ismail is never blocked for long. Each body follows its D5 sequence diagram step by step.

| # | Task | UC / SD |
|---|---|---|
| S-1 | `LeadRepository`: `save` (lead + contacts + interactions), `findById`, `findByFilter`, `findAll`, `existsByEmailDomain`, `saveAll`, `findAllByIds` | SD03, 06, 07, 08 |
| S-2 | `LeadController.saveLead / filterLeads / editLead / saveLeadChanges` | UC03, UC06 |
| S-3 | `LeadController.addInteractionNote / submitInteraction / selectLead / changePipelineStatus / getPipelineMetrics` | UC07, UC08 |
| S-4 | `CsvLeadSource.readHeaders / fetchLeads`, `LeadIngestionController.confirmMappingAndImport` | UC04 |
| S-5 | `WorkspaceRepository.save`, `WorkspaceController` (+ `IdealCustomerProfile.toSearchQuery`), `LeadIngestionController.runApiDiscovery / runDiscovery`, `B2BApiLeadSource` real call (offline mode already works) | UC02, UC05 |
| S-6 | `DraftRepository`, `OutreachController.runGeneration / openDraft / approveRevisedDraft`, `OpenAiLlmClient` real call | UC09, UC10 |
| S-7 | `OutreachController.runDispatch`, `JakartaMailSender.send`; `CsvExporter`, `ExportController.requestExport / chooseDestination` | UC11, UC12 |
| S-8 | Unit tests for every D3 extension; keep all three checks green | all |
| S-9 | Redraw the ✏️ gaps (G1–G5, G10) into D5 for the final documentation | — |

---

## 5. Use-case ownership for the demo (individual marking)

The rubric marks each member on "use cases assigned to group member completed" (4–5 each) and says
"individual marking will be considered". Layers are split for clean merges, but **each of us must present and
explain these use cases end-to-end** (screen → D5 controller → SD steps → domain → repository → Oracle table):

| Ismail presents | Sufiyan presents |
|---|---|
| UC01 Authenticate System User | UC04 Import Leads via CSV |
| UC02 Configure Company Workspace | UC05 Discover Leads via B2B API |
| UC03 Manually Add New Lead | UC08 Update Pipeline Status (State pattern) |
| UC06 Manage Existing Leads | UC09 Generate Emails via LLM |
| UC07 Log Manual Interaction | UC10 Review and Revise Drafts |
| UC12 Export Outreach Results | UC11 Dispatch Automated Outreach |

Before the demo, walk each other through the code and the sequence diagram of every use case you present.

---

## 6. Schedule — everything today (Wed 25 Nov), demos from Mon 30 Nov

Both of us work with AI assistants in parallel. The contract (§3) is already fixed, so neither side waits
for the other: the UI is built against the signatures while the bodies are filled in.

| Phase | Sufiyan's AI (business layer + DB) | Ismail's AI (UI) | Merge checkpoint — must work on BOTH laptops |
|---|---|---|---|
| **0** | Oracle set up, scripts run | Oracle set up, clone, run | Both log in as `admin` |
| **1** | S-1, S-2, S-3 | I-1, I-2, I-3, I-4 | UC01, UC03, UC06, UC07, UC08 |
| **2** | S-4, S-5 | I-5, I-6, I-7 | UC02, UC04, UC05 |
| **3** | S-6, S-7 | I-8, I-9, I-10 | UC09, UC10, UC11, UC12 |
| **4** | S-8, S-9 | I-11 | **Full UC01–UC12 run-through**, then code freeze |
| **Before the demo** | Explain the design (§5 split) | Explain the design (§5 split) | Rehearse with `docs/DESIGN_TRACEABILITY.md` §3 |

Each phase ends with a pull request per member and a merge into `main` after CI is green.
