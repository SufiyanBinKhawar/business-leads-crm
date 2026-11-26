# Demo notes: Sufiyan Ahmed (25I-0557)

My use cases: **UC04, UC05, UC08, UC09, UC10, UC11** (WORK_DIVISION.md §5).
For each one: the D5 sequence diagram step by step (which class does what, and which GRASP/GoF idea it shows),
where every D3 extension is handled, and the questions an examiner is likely to ask.

Code paths are `package.Class.method`. Open the class in IntelliJ and press Ctrl+F12 to jump to the method.

---

## 0. Before the demo (5 minutes)

1. **Oracle is running.** Check that the `OracleServiceFREE` and `OracleOraDB23Home1TNSListener` services are both running.
   If the app says *"Unable to connect to database"*, start the listener from Services or as admin with `lsnrctl start`.
   The listener now binds to `localhost`, so a new Wi-Fi IP no longer breaks it. Before this was fixed, it bound to `host.docker.internal`.
2. **Fresh data.** As BLMS, run `99_drop.sql`, then `02_schema.sql`, then `03_seed.sql`.
3. **Offline mode** (gap G12): leave `b2b.apiKey` and `llm.apiKey` blank in `config/local.properties`. Discovery then returns 3
   built-in prospects and the LLM returns a template email, so Wi-Fi and API quota can't break the demo.
4. **UC11 and SMTP.** If the workspace has **no SMTP host**, emails are logged instead of sent (G12). After UC02 has saved a
   host, a real send is attempted. Choose one:
   - Show **UC11 before UC02** (the seed workspace has no host), or
   - Save real credentials in UC02, for example `smtp.gmail.com`, port `587`, your Gmail address and an
     **app password**, and send to your own second address.
5. **Known limit: draft list (CONFLICTS.md S-004).** Until Ismail signs S-004, `lead.getDrafts()` is empty after a reload,
   so the UI cannot list a lead's drafts. Show UC10 right after UC09, or open a draft by id.
6. Demo order that tells one story: UC01 → UC04 (import sample CSV) → UC08 (qualify two leads) → UC05 (discovery)
   → UC09 → UC10 → UC11 → UC12.

Three GRASP ideas run through every one of my use cases. Say them once at the start:
- **Controller:** the UI only calls one of the six `controller/*Controller` classes, never the domain.
- **Pure Fabrication:** repositories (all SQL), services and `BackgroundTaskRunner` are invented classes that
  keep the domain clean (High Cohesion, Low Coupling).
- **Information Expert:** each domain object changes its own data (`Lead.changeStatus`, `EmailDraft.revise`, …).

---

## UC04: Import Leads via CSV (SD04)

**Screen:** `csv_import.fxml`. Click Choose File, check the mapping grid, click Import. The import runs in a JavaFX `Task`
(gap G7: D3 asks for a background thread, but D5 SD04 is synchronous, so the UI owns the thread).

### SD04 walk-through
| SD step | Code | Pattern |
|---|---|---|
| 1-2 `selectCsvFile(csvFile)` → «create» `CsvLeadSource(csvFile)` | `controller.LeadIngestionController.selectCsvFile` | **Controller**; **Creator**: the controller creates the source because it has the initialising data (the file) |
| 3-4 `readHeaders()` → headers | `integration.source.CsvLeadSource.readHeaders`: strict UTF-8 decode, RFC 4180 parser | |
| 5 `requestColumnMapping(headers)` | the UI fills one combo box per system field | |
| 6-7 `confirmMappingAndImport(columnMap)` → `setColumnMap` | `LeadIngestionController.confirmMappingAndImport` | |
| 8 `importFrom(src, ws)` | `service.LeadImportService.importFrom` | **Pure Fabrication**: the import algorithm is not a Lead responsibility |
| 9-10 `fetchLeads()` → `List<LeadData>` | `CsvLeadSource.fetchLeads`: one `LeadData` per row; irregular rows come back invalid | **Polymorphism / Strategy**: the service only sees `LeadSource` |
| 11-12 loop: `existsByEmailDomain(row.email)` | `persistence.LeadRepository.existsByEmailDomain` | duplicates are by **email domain** (G11), also within the same file |
| 13-14 opt: `ws.createLead(row)` | `domain.Workspace.createLead` → `new Lead(data)` → `new Contact`, `new DiscoveredState` | **Creator**: Workspace aggregates leads, and Lead composes Contact and its state |
| 15-16 `saveAll(newLeads)` → count | `LeadRepository.saveAll`: **one transaction** for the whole list | |
| 17-18 `ImportSummary(imported, duplicates, invalid)` | e.g. sample file: *3 imported, 1 duplicate, 1 invalid* | |

### D3 extensions
| Extension | Where | What the user sees |
|---|---|---|
| Unsupported file type | `CsvLeadSource.readRecords`: not `.csv`, or bytes that are not valid UTF-8 (`CharsetDecoder` with `REPORT`) | "Unsupported file - please choose a UTF-8 encoded .csv file" |
| Entire file is malformed | `CsvLeadSource.readRecords` / `fetchLeads`: empty file, unterminated quote, no data rows, no row with the header's shape, or required columns missing | "The CSV file is malformed - the import was aborted and nothing was saved". Thrown **before** `saveAll`, so the DB is untouched |
| (Main flow) duplicates and irregular rows | `LeadImportService.importFrom`: `row.isValid()` and `existsByEmailDomain` | counted in the summary, not fatal |

### Likely questions
- **Why does `CsvLeadSource` not save anything itself?** It is an adapter for *reading* only (single responsibility).
  Saving is the repository's job, deciding what to import is the service's job.
- **How do you avoid code duplication with UC05?** `LeadImportService.importFrom(LeadSource, Workspace)` is the same
  method for both. Only the `LeadSource` implementation differs (Strategy + Polymorphism, D1 "dual ingestion").
- **What if the file has 50 good rows and 2 bad ones?** The 2 are counted as invalid and the 50 are saved in one transaction.
  Only a *whole-file* problem aborts.
- **How do you handle commas inside a field?** A real RFC 4180 parser handles quoted commas, quoted line breaks and `""` for a quote.
  The sample's `"Blue Lake Foods, Ltd"` shows it.
- **Why the background thread in the UI and not in the controller?** D5 SD04 is synchronous and returns `ImportSummary`.
  We kept the signature and let the UI wrap it in a JavaFX `Task` (gap G7).
- **Column names differ between files?** The actor's mapping wins. Unmapped fields are matched by meaning
  ("Email Address" → email).

---

## UC05: Discover Leads via B2B API (SD05)

**Screen:** dashboard, "Run API Discovery" with a progress indicator. The result is read with `getLastImportSummary()` (gap G1).

### SD05 walk-through
| SD step | Code | Pattern |
|---|---|---|
| 1 `runApiDiscovery()` | `controller.LeadIngestionController.runApiDiscovery` | **Controller** |
| 2-3 `getIcp()` → icp | `domain.Workspace.getIcp`. No ICP means the workspace is not configured | D3 precondition |
| 4 «create» `B2BApiLeadSource(apiKey, icp)` | same method | **Creator** |
| 5-7 `submit(discoveryTask)` → jobId → `discoveryStarted(jobId)` | `service.BackgroundTaskRunner.submit` | **Indirection** (+ Pure Fabrication): the UI thread never blocks |
| 8-9 (worker) `runDiscovery(src)` → `importFrom(src, ws)` | `LeadIngestionController.runDiscovery` → `LeadImportService.importFrom` | the **same** service as UC04 |
| 10-12 `fetchLeads()` → `toSearchQuery()` | `integration.source.B2BApiLeadSource.fetchLeads` → `domain.IdealCustomerProfile.toSearchQuery` | **Information Expert**: the ICP builds its own query (`industry=…&keywords=…`) |
| 13-15 HTTP GET → JSON → `List<LeadData>` | `java.net.http.HttpClient`, header `X-Api-Key`, Gson parsing | **Adapter (GoF)** + **Protected Variations**: a new provider = a new `LeadSource` class |
| 16-21 loop `existsByEmailDomain` / `createLead` → `saveAll` | as in UC04 | |
| 22-25 `logApiCall("Apollo","GET /people/search",200,null)` → «create» `ApiCallLog` | `domain.Workspace.logApiCall`, then `WorkspaceRepository.save(ws)` (gap G4) | **Creator**: Workspace aggregates its API logs |
| 26 `discoveryCompleted(count, ids)` | `BackgroundTaskRunner.onComplete` + `getLastImportSummary()` (gap G1) | |

### D3 extensions
| Extension | Where | What the user sees |
|---|---|---|
| API rate limit exceeded (HTTP 429) | `B2BApiLeadSource.fetchLeads` **pauses** (reads `Retry-After`, at most 10 s), retries once, then throws `IntegrationException` | "API rate limit exceeded (HTTP 429) - discovery paused, please try again in a few minutes"; also logged in `api_call_logs` with status 429 |
| Network timeout | `B2BApiLeadSource.get`: `HttpTimeoutException` / `IOException` | "Network timeout - check your internet connection (the fetch was aborted)"; logged with status 0 |
| JSON null values (special requirement) | `B2BApiLeadSource.toLeadData`: every field read null-safely; a provider's hidden e-mail placeholder becomes null | the row is counted as invalid, never a crash |
| Preconditions | no ICP → `ValidationException`; blank API key → offline sample prospects (G12) | |

### Likely questions
- **Where is the Adapter pattern?** `B2BApiLeadSource` adapts the Apollo REST/JSON API to our `LeadSource` interface.
  `OpenAiLlmClient` and `JakartaMailSender` do the same for UC09 and UC11.
- **Why does `toSearchQuery()` live in the ICP?** Information Expert: the ICP owns the industry and criteria.
- **How does the UI know the job finished?** `BackgroundTaskRunner.onComplete(jobId, …)` (gap G1). The UI wraps the callback
  in `Platform.runLater`. Only `BackgroundTaskRunner` may create threads (architecture rule R8).
- **The worker thread and the UI both use the database: is that safe?** There is one JDBC Connection.
  `JdbcRepository` serialises every transaction and query on it (`synchronized (connection)`), so they can't interleave.
- **Why is a failed call still logged?** UC05 extensions say "notify the user". We also record it as an `ApiCallLog`
  so the API history shows the 429 or timeout.
- **What does offline mode prove?** The design is unchanged. The same class just skips the network when the key is blank, with
  no mock classes (G12).

---

## UC08: Update Pipeline Status (SD08), the State pattern

**Screen:** lead profile status combo box. On an invalid move the UI reverts the combo box. The dashboard cards come from
`getPipelineMetrics()` (gap G10).

### SD08 walk-through
| SD step | Code | Pattern |
|---|---|---|
| 1-3 `selectLead(leadId)` → `findById` | `controller.LeadController.selectLead` → `LeadRepository.findById` | **Controller**, **Pure Fabrication** |
| 4-7 `lead.getStatus()` → `state.getStatus()` → DISCOVERED | `domain.Lead.getStatus` delegates to `domain.state.PipelineState` | |
| 9-11 `changePipelineStatus(leadId, QUALIFIED)` → `findById` | `LeadController.changePipelineStatus` | |
| 12 `lead.changeStatus(newStatus)` | `Lead.changeStatus`: `state = state.next(newStatus)` | **Information Expert** |
| 13-16 `next(newStatus)` → `canMoveTo(newStatus)` → «create» `QualifiedState` | `PipelineState.next` asks the **current** state object; `forStatus` creates the next one | **GoF State**, **Polymorphism**: five subclasses override `canMoveTo`, with no if/else chain on status |
| 17 `state = nxt` | `Lead.changeStatus` | |
| 18 `save(lead)` | `LeadRepository.save`: `status` column = `PipelineStatus.name()` | D3: "enums mapped to SQL strings" |
| 19-21 `findAll()` → `recalculate(leads)` | `domain.PipelineMetrics.recalculate` (archived leads not counted) | **Information Expert**: metrics count themselves |
| 22 `statusUpdated(newStatus, metrics)` | returns `PipelineMetrics` | |

Allowed moves (D3 UC08 + CONFLICTS §2): DISCOVERED→QUALIFIED, QUALIFIED→CONTACTED, CONTACTED→CLOSED_WON,
any open state → CLOSED_LOST. The two closed states are terminal.

### D3 extension
| Extension | Where | What the user sees |
|---|---|---|
| Invalid state transition (e.g. DISCOVERED → CLOSED_WON) | `PipelineState.next` throws `InvalidStateTransitionException` because `DiscoveredState.canMoveTo(CLOSED_WON)` is false. Thrown **before** `save`, so nothing changes | "Cannot move a lead from DISCOVERED to CLOSED_WON"; the UI reverts the combo box |

### Likely questions
- **Show me the State pattern.** The abstract `PipelineState` has 5 subclasses. `Lead` has `+state 1`. `Lead.changeStatus` has no
  `if`. Each state decides its own allowed moves in `canMoveTo`.
- **But there is a `switch` in `PipelineState.forStatus`?** That switch only *creates* objects (Creator / factory),
  for example when a lead is loaded from the database. The *rules* are polymorphic. The architecture check allows that switch and
  the one in `recalculate`, and nothing else.
- **How do you add a new stage, e.g. "Negotiation"?** Add a subclass and an enum constant, and change the `canMoveTo` of the states
  that may lead to it. `Lead` and the controllers do not change (Open/Closed, Protected Variations).
- **Why is the status a String in Oracle and not a number?** D3 "Java Enums mapped to SQL strings". A CHECK constraint
  in `02_schema.sql` allows only the five names.
- **Where does UC11 move leads to CONTACTED?** `OutreachController.runDispatch` calls the same `lead.changeStatus(CONTACTED)`,
  so even automatic changes go through the State objects.

---

## UC09: Generate Emails via LLM (SD09)

**Screen:** leads table, multi-select qualified leads, click "Generate Drafts". Afterwards read `getLastResultCount()` /
`getLastFailureCount()` (G1).

### SD09 walk-through
| SD step | Code | Pattern |
|---|---|---|
| 1-4 `generateDrafts(ids)` → `submit(generationTask)` → jobId | `controller.OutreachController.generateDrafts` | **Controller**, **Indirection** (BackgroundTaskRunner) |
| 5-7 (worker) `runGeneration(ids)` → `findAllByIds(ids)` | `OutreachController.runGeneration` → `LeadRepository.findAllByIds` | keeps only QUALIFIED leads via `lead.matches(filter)` (D3 "batch of qualified leads"; Information Expert, no status `if`) |
| 8 `generate(leads, ws)` | `service.DraftGenerationService.generate` | **Pure Fabrication** |
| 9 `buildPrompt(lead, ws)` | `DraftGenerationService.buildPrompt`: injects `{{company_name}}`, `{{services}}`, `{{contact_name}}` … | D3 step 3 |
| 10-11 `complete(prompt)` → text | `integration.llm.LLMClient.complete` → `OpenAiLlmClient` (REST POST, JSON via Gson) | **Protected Variations / Adapter**: swap OpenAI for another vendor with no other change |
| 12-14 `lead.createDraft(subject, body)` → «create» `EmailDraft(…, DRAFT)` | `domain.Lead.createDraft` | **Creator**: Lead composes its drafts. The recipient is the lead's contact |
| 15-16 loop `save(d)` | `persistence.DraftRepository.save` (body is a CLOB) | |
| 17 `draftsReady(ids, count)` | `getLastResultCount()` | |

### D3 extension
| Extension | Where | What the user sees |
|---|---|---|
| LLM API failure | `OpenAiLlmClient.complete` throws `IntegrationException` (HTTP error, timeout, empty answer). `DraftGenerationService.generate` **catches it, skips that lead and continues** | at the end: "N drafts ready, M failed" (`getLastFailureCount`, which also counts selected leads that were not QUALIFIED) |

### Likely questions
- **Why isn't the prompt built in `Lead`?** Building a prompt is a technical job, not a domain fact, so it goes in a Pure Fabrication
  service (High Cohesion). Lead only *creates* the draft (Creator).
- **Why an `LLMClient` interface?** Protected Variations. The LLM vendor is the thing most likely to change.
- **How does one failure not stop the batch?** The try/catch is *inside* the loop in `generate`, and the failure count is reported at
  the end (D3 extension wording).
- **Is the UI frozen while the API is slow?** No. The work runs in `BackgroundTaskRunner` (D3 "must process asynchronously").

---

## UC10: Review and Revise Drafts (SD10)

**Screen:** `draft_review.fxml`, with the recipient, a subject field, a body `TextArea` and an Approve button.

### SD10 walk-through
| SD step | Code | Pattern |
|---|---|---|
| 1-4 `openDraft(draftId)` → `findById` → `draftContents(subject, body)` | `controller.OutreachController.openDraft` → `DraftRepository.findById` (loads `+recipient` Contact) | **Controller**, **Pure Fabrication** |
| 5-7 `approveRevisedDraft(id, subject, body)` → `findById` | `OutreachController.approveRevisedDraft` | |
| 8 `d.revise(subject, body)` | `domain.EmailDraft.revise` | **Information Expert**: the draft owns subject/body/status |
| 9-10 `d.approve()` → status = APPROVED, approvedAt = now() | `EmailDraft.approve` | **Information Expert** |
| 11 `save(d)` | `DraftRepository.save`: UPDATE overwrites the original text (D3 step 4) | |
| 12 `draftApproved(id, APPROVED)` | returns `DraftStatus` | |

### D3 extension
| Extension | Where | What the user sees |
|---|---|---|
| Actor clears the text completely | `EmailDraft.revise` → `Validator.requireText` throws `ValidationException` **before** `save` | "Email body is required" / "Subject is required"; the draft stays as it was |
| (extra) editing a sent email | `EmailDraft.revise` refuses when status is SENT | "This email has already been sent and can no longer be edited" |

### Likely questions
- **Why does the draft approve itself instead of the controller setting the status?** Information Expert plus encapsulation.
  There is no `setStatus`. Status changes only through `approve()` and `recordDispatch()`.
- **Do newlines survive?** Yes. The body is a CLOB and Jakarta Mail sends it as UTF-8 plain text (D3 special requirement).
- **How does the user find the draft?** The lead profile lists `lead.getDrafts()`. *Known limit:* loading drafts of an existing
  lead needs `Lead.addLoadedDraft` (CONFLICTS S-004, waiting for Ismail's signature). Be ready to say: "D5 has no
  operation to attach a stored draft to a Lead. We logged it as S-004 instead of working around the frozen design."

---

## UC11: Dispatch Automated Outreach (SD11)

**Screen:** leads table, select the approved leads, click "Send Emails". Afterwards `getLastResultCount()` = sent and
`getLastFailureCount()` = failed.

### SD11 walk-through
| SD step | Code | Pattern |
|---|---|---|
| 1-4 `sendApprovedEmails(ids)` → `submit(dispatchTask)` → jobId | `controller.OutreachController.sendApprovedEmails` | **Controller**, **Indirection** (D3 "must use Java multithreading") |
| 5-7 (worker) `runDispatch(ids)` → `findApprovedByLeadIds(ids)` | `DraftRepository.findApprovedByLeadIds`: newest APPROVED draft per lead, so no double emails | |
| 8-9 `getSmtpCredentials()` → smtp | `domain.Workspace.getSmtpCredentials` returns the **cipher text**. `OutreachController.decrypted` uses `CredentialEncryptor.decrypt` in memory only (gap G2) | **Pure Fabrication** (CredentialEncryptor) |
| 10 `dispatch(drafts, smtp)` | `service.OutreachDispatcher.dispatch`: `sender.configure(smtp)` (gap G3), then loop | **Pure Fabrication** |
| 11-12 `d.getRecipientEmail()` | `EmailDraft.getRecipientEmail` navigates `+recipient` | **Information Expert** |
| 13 `send(to, subject, body)` | `integration.mail.EmailSender` → `JakartaMailSender` (STARTTLS on 587, SSL on 465) | **Adapter**, **Protected Variations** |
| 14-16 `recordDispatch("SENT")` → «create» `OutreachDispatch` → status = SENT | `EmailDraft.recordDispatch` | **Creator**: the draft composes its send attempts |
| 17 sentCount | | |
| 18 loop `save(d)` | `DraftRepository.save` writes the draft and its new `outreach_dispatches` rows | |
| 19-22 `findAllByIds` → `changeStatus(CONTACTED)` → `saveAll` | `OutreachController.runDispatch`: only leads whose draft was SENT. The **State** objects decide (QUALIFIED→CONTACTED) | **GoF State** reused |
| 23 `dispatchCompleted(sentCount)` | `getLastResultCount()` | |

### D3 extensions
| Extension | Where | What the user sees |
|---|---|---|
| SMTP Authentication Error | `JakartaMailSender.send` turns `AuthenticationFailedException` into `IntegrationException("SMTP authentication failed - check the email password in Workspace Settings")`. `OutreachDispatcher` **rethrows**, so the batch aborts | that message (D3: "alerts the user to check their email password in Workspace Settings") |
| Invalid recipient email | bad address (`InternetAddress` strict parse) or `SendFailedException` → "Invalid recipient: …". `OutreachDispatcher` records `recordDispatch("FAILED")` with the error and **continues** | draft stays **APPROVED**, lead keeps its status, and the failed attempt is shown next to the lead's timeline (gap G14) |
| (extra) server unreachable | `MessagingException` → "SMTP server unreachable - check the host and port in Workspace Settings", aborts | |
| (extra) abort mid-batch | `runDispatch` saves progress in a `finally` block: drafts already sent are stored as SENT | an email is never sent twice on retry |

### Likely questions
- **Where is the SMTP password decrypted, and is it ever shown?** Only in `OutreachController.decrypted`, in memory, just before
  `dispatch` (gap G2). It is stored AES-GCM-encrypted (`CredentialEncryptor`, UC02) and `WorkspaceForm` has no password field.
- **Why does `EmailDraft` create `OutreachDispatch` and not the dispatcher?** Creator: the draft composes its attempts and knows
  the recipient.
- **How does the dispatcher tell an auth error from a bad address?** The mail adapter turns Jakarta exceptions into our own
  `IntegrationException` with a fixed message. Only "Invalid recipient" is a per-email failure. Everything else aborts.
- **D3 says "logs an error in the lead's timeline": where?** As a FAILED `OutreachDispatch` with `errorMessage` on that draft
  (G14). The dispatcher cannot reach the Lead, so the profile screen shows the attempt next to the timeline.
- **Why not change status inside the dispatcher?** Low coupling. The dispatcher only sends. The controller coordinates the lead
  update after saving the drafts (SD11 steps 19-22).

---

## Questions that apply to all six

| Question | Short answer |
|---|---|
| Where are the 3 layers? | UI = `ui/` + FXML; BL = `controller`, `domain`, `service`, `integration`, `security`; DB = `persistence/` + `db/*.sql`. `scripts/check-architecture.sh` enforces the dependencies (R1-R12) |
| SQL injection? | Only `PreparedStatement` with `?`, and SQL only in `persistence/` (R5). Even the dynamic filter in `findByFilter` binds every value |
| What happens on a DB error mid-save? | `JdbcRepository.inTransaction` rolls back, restores auto-commit and throws `DataAccessException`. Ids given out in the failed transaction are cleared |
| Why no DELETE? | UC06: leads are archived (`is_archived = 1`). R7 forbids `DELETE FROM` |
| How do you prove code = design? | `scripts/check-d5-conformance.py` compares every class, public method and field with `scripts/d5-contract.txt` (D5) in CI. Every additional public method is an approved gap (CONFLICTS §2) |
| How was it tested? | 71 JUnit tests with fake repositories (no DB/network), at least one per D3 extension. JDBC failures use a proxy `Connection`, HTTP uses a local `HttpServer`. Plus a full UC01-UC12 run against Oracle 26ai Free |
| Exception design? | `BlmsException` and 6 subclasses. The message is always the D3 wording, so the UI just shows `getMessage()` |
