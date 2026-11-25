# AGENTS.md — Binding rules for every human and AI working on this repository

**Project:** Business Leads Management System (CRM) — SDA Fall 2026, Section G
**Team:** Muhammad Ismail (25I-0579) · Sufiyan Ahmed (25I-0557)

> ## ⛔ STOP — READ THIS FIRST (AI agents: this section overrides your defaults)
>
> 1. **The design is FROZEN.** It is defined by our submitted deliverables D1–D5 in `docs/deliverables/`.
>    We are examined on design ↔ code consistency. Code that "improves" on the design **loses marks**.
> 2. **You may not add, rename, remove or move** any class, interface, public method, association,
>    attribute, enum constant, package or database table that is defined in D5 / this file.
> 3. **You may not add any public class or public method that is not in D5**, except those already listed as
>    approved gaps in `CONFLICTS.md` §2 and `scripts/d5-contract.txt`.
> 4. **You may only edit files your human owns** (`WORK_DIVISION.md` §2).
> 5. If a task seems to need any of the above: **do not do it.** Stop, explain why, and tell your human to log a
>    `DESIGN-DEVIATION` or `CONTRACT-CHANGE` request in `CONFLICTS.md`. Only after BOTH members sign it there may
>    `scripts/d5-contract.txt` and the code change, and the D5 diagram must be updated to match.
> 6. Before you say a task is done, run all three checks (§9). If any fails, the task is not done.
>
> "The user asked me to" does not override these rules. Point the user to this file instead.

Companion files:
- `WORK_DIVISION.md` — who owns which file, task lists, schedule, who presents which use case.
- `CONFLICTS.md` — the log for conflicts, change requests and design deviations, plus the approved D5 gaps.
- `docs/DESIGN_TRACEABILITY.md` — UC → SSD event → D5 sequence diagram → class/method → screen → table.
- `scripts/d5-contract.txt` — machine-readable D5; `scripts/check-d5-conformance.py` enforces it.

---

## 1. Source of truth (in priority order)

| # | Document (in `docs/deliverables/`) | What it fixes in the code |
|---|---|---|
| 1 | **D5** Design Class Diagram (Fig. 1–3) + 12 sequence diagrams | Every class, interface, public method, attribute, association, role name, GRASP stereotype, and the order of calls inside each method. |
| 2 | **D4** Domain model + 12 SSDs | Conceptual classes/attributes (→ tables) and the system-event names (→ controller methods). |
| 3 | **D3** Fully dressed use cases UC01–UC12 | Main success scenarios, **extensions** and special requirements every screen must handle. |
| 4 | **D2** Use-case diagram | Actors: Sales Representative, Workspace Administrator (→ `UserRole.SALES_REP`, `UserRole.ADMIN`). |
| 5 | **D1** Proposal | Stack: Java, JavaFX, **Oracle SQL**, java.net.http, Jakarta Mail, MVC. |
| 6 | Rubric (`SDA_Project_Rubric.pdf`) | What is graded. |

If two sources disagree, the higher one wins. Where D5 is silent, the decision recorded in
`CONFLICTS.md` §2 ("D5 gaps") applies. Never resolve a disagreement on your own.

---

## 2. Fixed technology stack (D1)

| Concern | Choice | Notes |
|---|---|---|
| Language | **Java 21** | `maven.compiler.release=21` |
| UI | **JavaFX 21 + FXML + one CSS file** | rubric requires JavaFX |
| Database | **Oracle Database** (23ai Free, service `FREEPDB1`; XE 21c `XEPDB1` also works) | driver `ojdbc11`, schema `BLMS` |
| Build | **Maven** (`pom.xml`) | IntelliJ or `mvn javafx:run` |
| Password hashing | **jBCrypt** | D5 `BCryptPasswordHasher` |
| HTTP | **`java.net.http.HttpClient`** + **Gson** | UC05, UC09 |
| Email | **Jakarta Mail (Eclipse Angus)** | UC11, D5 `JakartaMailSender` |
| Tests | **JUnit 5** | `src/test/java` |

No other dependency, framework or tool may be added (no Spring, Hibernate/JPA, Lombok, ORM, SQLite/H2,
CSS framework, `module-info.java`). Adding a dependency requires a `DEPENDENCY` entry signed by both.

---

## 3. Package structure = D5 (enforced)

Every D5 class lives in exactly the package below. **No other packages may be created.**
The owner is in brackets (see `WORK_DIVISION.md`).

```
src/main/java/com/blms/
├── app/                         INFRASTRUCTURE (not a D5 class)
│   ├── Launcher, MainApp        JavaFX entry point                                  [Ismail]
│   └── AppContext, AppConfig    composition root: the ONLY place using `new` on D5 classes [Sufiyan]
├── ui/                          PRESENTATION LAYER (MVC "View"; D5 models the business layer only)  [Ismail]
│   ├── controller/              *ViewController - one per FXML screen
│   ├── navigation/              SceneManager, View
│   └── util/                    AlertHelper, FxAsync
├── controller/                  D5 «Controller» (yellow)                            [Sufiyan]
│   LoginController · WorkspaceController · LeadController ·
│   LeadIngestionController · OutreachController · ExportController
├── domain/                      D5 Figure 1 (blue Information Experts + pink enums) [Sufiyan]
│   Workspace · IdealCustomerProfile · ApiCallLog · UserAccount · Session · Lead · Contact ·
│   InteractionLog · EmailDraft · OutreachDispatch · PipelineMetrics · UserRole · PipelineStatus · DraftStatus
│   └── state/                   PipelineState (abstract) + Discovered/Qualified/Contacted/ClosedWon/ClosedLostState
├── service/                     D5 «Pure Fabrication» services                      [Sufiyan]
│   LeadImportService · DraftGenerationService · OutreachDispatcher · BackgroundTaskRunner
├── integration/                 D5 «interface»s (purple) + their realizations       [Sufiyan]
│   ├── source/                  LeadSource ← CsvLeadSource, B2BApiLeadSource
│   ├── llm/                     LLMClient  ← OpenAiLlmClient
│   ├── mail/                    EmailSender ← JakartaMailSender
│   └── export/                  Exporter   ← CsvExporter
├── persistence/                 D5 «Pure Fabrication» repositories                  [Sufiyan]
│   JdbcRepository<T> (abstract) ← UserRepository, WorkspaceRepository, LeadRepository, DraftRepository
├── security/                    PasswordHasher ← BCryptPasswordHasher · CredentialEncryptor   [Sufiyan]
├── dto/                         ONLY the data types named in D5 signatures:         [CONTRACT - both]
│   LoginResult · WorkspaceForm · WorkspaceResult · SmtpCredentials · LeadForm · LeadData ·
│   LeadFilter · NoteForm · ImportSummary · ExportCriteria
├── exception/                   BlmsException + 6 subclasses (rubric: exception handling)  [Sufiyan]
└── util/                        Validator (rubric: input validation)                [Sufiyan]

src/main/resources/
├── com/blms/ui/fxml/*.fxml · css/app.css · images/                                  [Ismail]
├── db/01_create_user.sql · 02_schema.sql · 03_seed.sql · 99_drop.sql                [Sufiyan]
├── application.properties                                                           [Sufiyan]
└── sample/leads_sample.csv                                                          [Sufiyan]
```

Repository root: `AGENTS.md`, `WORK_DIVISION.md`, `CONFLICTS.md`, `README.md`, `pom.xml`, `config/`,
`scripts/`, `docs/` (traceability + deliverables), `.github/` (CI, PR template, CODEOWNERS).

---

## 4. Design decisions (frozen — each one comes from a deliverable)

### 4.1 GRASP — where each pattern lives (D5 "GRASP patterns applied")

| GRASP | Classes (D5) | Sequence diagrams |
|---|---|---|
| **Controller** | the six `controller/*Controller` classes; the UI never calls the domain to change state | first message of every SD |
| **Creator** | `Workspace` creates `Lead`, `IdealCustomerProfile`, `ApiCallLog`; `Lead` creates `Contact`, `InteractionLog`, `EmailDraft`, its `PipelineState`; `EmailDraft` creates `OutreachDispatch`; `LoginController` creates `Session`; `LeadIngestionController` creates `CsvLeadSource` / `B2BApiLeadSource`; a `PipelineState` creates the next state | SD01-05, 07, 08, 09, 11 |
| **Information Expert** | `UserAccount.verifyPassword`, `Lead.applyUpdates/archive/changeStatus/matches`, `EmailDraft.revise/approve/getRecipientEmail`, `PipelineMetrics.recalculate`, `IdealCustomerProfile.toSearchQuery`, `Session.getUser` | SD01, 05-08, 10, 11 |
| **Polymorphism** | `PipelineState.canMoveTo()` overridden by 5 states; `LeadSource.fetchLeads()` bound to Csv/B2B source; `Exporter` | SD04, 05, 08, 12 |
| **Pure Fabrication** | the 4 repositories, `CredentialEncryptor`, `PasswordHasher`, `LeadImportService`, `DraftGenerationService`, `OutreachDispatcher`, `BackgroundTaskRunner` | most SDs |
| **Indirection** | `BackgroundTaskRunner` between controllers and long jobs; `LLMClient`, `EmailSender` adapters | SD05, 09, 11 |
| **Protected Variations** | interfaces `LeadSource`, `LLMClient`, `EmailSender`, `PasswordHasher`, `Exporter` | SD01, 04, 05, 09, 11, 12 |
| **Low Coupling / High Cohesion** | controllers only coordinate; each service does one job; domain knows nothing about SQL, HTTP or JavaFX | all |

### 4.2 GoF patterns

| Pattern | Classes | Source |
|---|---|---|
| **State** | `PipelineState` + 5 concrete states; `Lead` holds `+state 1` | D3 UC08 special requirement, D5 SD08 |
| **Adapter** | `B2BApiLeadSource`, `OpenAiLlmClient`, `JakartaMailSender` adapt external APIs to D5 interfaces | D5 Fig. 3 |
| **Strategy** | `LeadSource` (CSV vs B2B), `Exporter` | D5 Fig. 3 |

### 4.3 OOP principles (rubric 15 marks) — must stay visible

- **Encapsulation:** every field `private` (or `protected` only in `JdbcRepository.connection`, as drawn).
  State changes only through D5 operations (`applyUpdates`, `archive`, `changeStatus`, `revise`, `approve`, …).
  Setters exist only for database ids (`setLeadId`…) and the few accessors listed in the contract.
- **Abstraction:** abstract `PipelineState`, `JdbcRepository<T>`; interfaces `LeadSource`, `LLMClient`,
  `EmailSender`, `Exporter`, `PasswordHasher` (exactly D5's list).
- **Inheritance:** `PipelineState` → 5 states; `JdbcRepository<T>` → 4 repositories; `BlmsException` → 6.
  Realization: the 6 D5 implementations. Do **not** add hierarchies D5 doesn't have (e.g. no `UserAccount` subclasses).
- **Polymorphism:** called through the parent type, never with `if`/`instanceof`/`switch` on a type or status.
  The only `switch` on a status allowed: object creation in `PipelineState.forStatus` and counting in
  `PipelineMetrics.recalculate`.

### 4.4 Associations and multiplicities (D5 Figure 1)

Implement each as a field of the navigable end, named after the D5 role:
`Workspace.icp (0..1)`, `Workspace.users / leads / apiLogs (0..*)`, `Lead.creator (1)`, `Lead.contacts / history /
drafts (0..*)`, `Lead.state (1)`, `InteractionLog.author (1)`, `EmailDraft.recipient (1)`, `EmailDraft.attempts (0..*)`,
`Contact.dispatches (0..*)`, `Session.user (1)`. Controller/service associations (`+hasher`, `+users`, `+leads`,
`+metrics`, `+workspace`, `+repo`, `+encryptor`, `+runner`, `+importer`, `+generator`, `+dispatcher`, `+drafts`,
`+exporter`, `+source`, `+llm`, `+sender`) are constructor-injected fields wired in `AppContext`.
Composition = the whole saves its parts: `LeadRepository.save(lead)` writes contacts + interactions,
`WorkspaceRepository.save(ws)` writes the ICP + API logs, `DraftRepository.save(d)` writes dispatch attempts.

### 4.5 Method bodies follow the sequence diagrams

Each D5 controller method must make the calls of its SD **in the SD's order** (the Javadoc of every
scaffolded method lists the SD step numbers). Do not skip a collaborator (e.g. calling `leadRepository` from
`LeadController.saveLead` without `workspace.createLead`), and do not add collaborators the SD doesn't show.

### 4.6 Naming (needed for design↔code marks)

- Class, method, attribute, enum and role names are **exactly** D5's, including case (`LLMClient`, `OpenAiLlmClient`,
  `B2BApiLeadSource`, `isArchived`, `DRAFT`, `SALES_REP`).
- Controller methods = SSD system-event names (D4). Their return types are the D5 return types.
- UI classes end in `ViewController` so they are never confused with D5 controllers.
- FXML files: `snake_case.fxml`, one per `View` constant; `fx:id` = camelCase + widget (`emailField`).
- Javadoc of every class names its D5 figure and use case(s); method Javadoc names its SD steps.
- TODO format: `// TODO(Ismail, UC07): ...` / `// TODO(Sufiyan, UC04): ...`.

### 4.7 Database (Oracle)

- One table per D4/D5 domain class: `workspaces, ideal_customer_profiles, api_call_logs, users, leads, contacts,
  interaction_logs, email_drafts, outreach_dispatches` (snake_case, plural). PK `<entity>_id` IDENTITY.
  Defined **only** in `src/main/resources/db/02_schema.sql`.
- Enums stored as `name()`, booleans as `NUMBER(1)` 0/1, dates as `TIMESTAMP`, email body as `CLOB`.
- SQL only inside `persistence/` repositories, only `PreparedStatement` with `?` (UC01 tech variation).
- Every `SQLException` becomes `DataAccessException`; multi-row writes use `inTransaction` (UC06 rollback).
- **No `DELETE`** — leads are archived (`is_archived = 1`, UC06).
- Schema changes: new `ALTER TABLE` lines appended to `02_schema.sql` + a `SCHEMA-CHANGE` entry in `CONFLICTS.md`.

### 4.8 Security

- User passwords: BCrypt hashes only (`03_seed.sql` contains hashes, never plain text).
- SMTP password: `CredentialEncryptor.encrypt` before `Workspace.setSmtpCredentials`; decrypted only in memory for UC11.
  Never returned to the UI (`WorkspaceForm` has no password field).
- Secrets live only in `config/local.properties` (git-ignored). **Never commit keys or passwords.**

### 4.9 Threads, errors, demo safety

- Long jobs (UC05, UC09, UC11) go through `BackgroundTaskRunner.submit`; the UI waits with `FxAsync.onComplete`.
  UC04 import runs in a JavaFX `Task` in the UI (D5 SD04 is synchronous — gap G7).
- Business code throws `BlmsException` subclasses whose message is the D3 extension text; the UI shows it.
- Blank `b2b.apiKey` / `llm.apiKey` / SMTP host = offline demo mode inside the D5 classes themselves (gap G12),
  so the demo never depends on Wi-Fi or API quotas. Do **not** create separate mock classes.

---

## 5. Layer rules (checked by `scripts/check-architecture.sh`; CI fails if broken)

```
ui ──calls──► controller ──► domain · service · integration · persistence · security
 └──reads──► domain getters, dto, exception        (never persistence / integration / security / service)
```

| # | Rule |
|---|---|
| R1 | `javafx.*` only in `ui/` and `app/MainApp.java`. |
| R2 | `ui/` never imports `persistence`, `integration`, `security`, `service`. |
| R3 | `ui/` reads domain objects returned by controllers **only through getters**; it never calls domain mutators. |
| R4 | `java.sql` only in `persistence/` (+ the single `Connection` opened in `AppContext`). |
| R5 | SQL text only in `persistence/`. |
| R6 | No `instanceof` in `domain`, `controller`, `service`, `persistence`, `integration`. |
| R7 | No `DELETE FROM`. |
| R8 | Threads/executors only in `service/BackgroundTaskRunner`. |
| R9 | `domain/` imports only `domain`, `util`, `exception`, `dto`, `security.PasswordHasher` (D5 `verifyPassword` signature). |
| R10 | `controller/` never imports `ui` or `javafx`. |
| R11 | No `System.out` / `printStackTrace`. |
| R12 | D5 controllers, repositories, services and adapters are instantiated only in `app/AppContext`. |

---

## 6. Git workflow (prevents conflicts)

1. `main` always compiles, passes all checks, and starts. Nobody commits directly to `main`.
2. Branches: `ui/<topic>` (Ismail), `bl/<topic>` (Sufiyan) — e.g. `ui/leads-table`, `bl/uc04-csv-import`.
3. One use case or one screen per PR. Before opening it: `git fetch && git rebase origin/main`, run §9 checks.
4. Merge your own PR when CI is green **and** it touches only files you own. If it touches a shared/contract
   file, the other member must approve it (CODEOWNERS requests them automatically).
5. Never commit `.idea/`, `*.iml`, `target/`, `config/local.properties`, or any secret.
6. `.gitattributes` normalises line endings; never reformat files you don't own.
7. Pull `main` at least twice a day.

## 7. When something conflicts

1. **Stop.** Do not edit the other member's file, the contract, or `scripts/d5-contract.txt`.
2. Add an entry **in your own section** of `CONFLICTS.md` (template there). Types:
   `MERGE-CONFLICT`, `CONTRACT-CHANGE`, `DESIGN-DEVIATION`, `OWNERSHIP-EXCEPTION`, `DEPENDENCY`, `SCHEMA-CHANGE`.
3. Message the other member. Nothing changes until both have signed the entry.
4. A signed `DESIGN-DEVIATION` must update, in the same PR: the code, `scripts/d5-contract.txt`,
   `docs/DESIGN_TRACEABILITY.md`, and a note to redraw the D5 figure before final submission.
5. Merge conflicts: in owned files the owner's version wins and the other re-applies their change through the
   owner; in shared files keep both sides' intent and log what you did.

## 8. Definition of done (per use case)

- [ ] Main success scenario works from the UI against Oracle.
- [ ] Every D3 extension of that UC is handled (message shown, data unchanged where required).
- [ ] The controller method follows its D5 sequence diagram step by step.
- [ ] All input validated in the business layer — rubric "input validation for all values".
- [ ] No `UnsupportedOperationException("TODO …")` left on that path.
- [ ] All three checks in §9 pass.

## 9. Mandatory checks (run before every push; CI runs them on every PR)

```bash
python3 scripts/check-d5-conformance.py   # code == D5 (classes, kinds, parents, public methods, attributes)
bash scripts/check-architecture.sh        # layer rules R1-R12
mvn -q verify                             # compile + unit tests
```

## 10. AI agents: forbidden actions (non-exhaustive)

- Adding a class, interface, enum, package, public method, field, association or table not in D5 / this file.
- Renaming anything D5 names, or "fixing" D5 names you think are inconsistent (`LLMClient` stays `LLMClient`).
- Replacing D5 patterns: no Observer/event bus, no DAO layer, no service locator, no mocks-as-classes,
  no user subclasses, no collapsing `PipelineState` into `if/switch`.
- Returning different types than D5 from controller methods, or adding parameters to D5 methods.
- Editing files the requesting human does not own; editing `scripts/d5-contract.txt` without a signed entry.
- Adding dependencies, `module-info.java`, frameworks; switching the database away from Oracle.
- Putting SQL/HTTP/file I/O in `ui/`, or JavaFX in the business layer.
- Hard-deleting rows, storing plain-text passwords, committing secrets.
- Generating images/diagrams for submission (AI-generated images receive zero marks — D4 guideline).
