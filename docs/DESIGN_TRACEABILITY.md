# Design ↔ Code Traceability (D3 → D4 → D5 → code → Oracle)

Rubric: "Consistency between design and code — 100 % mapping". This is the cheat-sheet for demo questions.
`scripts/check-d5-conformance.py` proves the class/method part automatically.

## 1. Use case → SSD event (D4) → sequence diagram (D5) → code → screen → tables

| UC | SSD system event (D4) | D5 SD | Controller method → return | Collaborators called (SD order) | Screen · handler | Oracle tables |
|---|---|---|---|---|---|---|
| UC01 | `login(username, password)` | SD01 | `LoginController.login` → `LoginResult` | `UserRepository.findByUsername` → `UserAccount.verifyPassword` → `PasswordHasher.matches` → «create» `Session` | `login.fxml` · `onLogin` | users |
| UC02 | `openWorkspaceSettings()` | SD02 1-4 | `WorkspaceController.openWorkspaceSettings` → `WorkspaceForm` | `WorkspaceRepository.findCurrent` | `workspace_settings.fxml` · `initialize` | workspaces |
| | `saveWorkspace(companyName, targetIndustry, services, smtpCredentials)` | SD02 5-15 | `WorkspaceController.saveWorkspace` → `WorkspaceResult` | `CredentialEncryptor.encrypt` → `Workspace.updateDetails` → `setSmtpCredentials` → `generateIcp` («create» ICP) → `WorkspaceRepository.save` | `onSave` | workspaces, ideal_customer_profiles |
| UC03 | `requestNewLeadForm()` | SD03 1-2 | `LeadController.requestNewLeadForm` → `LeadForm` | — | `leads.fxml` · `onAddLead` | — |
| | `saveLead(companyName, contactName, email, jobRole)` | SD03 3-13 | `LeadController.saveLead` → `Lead` | `LeadRepository.existsByEmailDomain` → `Workspace.createLead` («create» `Lead` → `Contact` → `DiscoveredState`) → `LeadRepository.save` | `lead_form.fxml` · `onSave` | leads, contacts |
| UC04 | `selectCsvFile(csvFile)` | SD04 1-5 | `LeadIngestionController.selectCsvFile` → `String[]` | «create» `CsvLeadSource` → `readHeaders` | `csv_import.fxml` · `onChooseFile` | — |
| | `confirmMappingAndImport(columnMap)` | SD04 6-18 | `LeadIngestionController.confirmMappingAndImport` → `ImportSummary` | `CsvLeadSource.setColumnMap` → `LeadImportService.importFrom` → `fetchLeads` → loop `existsByEmailDomain` / `Workspace.createLead` → `LeadRepository.saveAll` | `onImport` | leads, contacts |
| UC05 | `runApiDiscovery()` | SD05 | `LeadIngestionController.runApiDiscovery` → jobId | `Workspace.getIcp` → «create» `B2BApiLeadSource` → `BackgroundTaskRunner.submit` → (worker) `runDiscovery` → `LeadImportService.importFrom` → `ICP.toSearchQuery` → HTTP GET → `saveAll` → `Workspace.logApiCall` («create» `ApiCallLog`) | `dashboard.fxml` · `onRunApiDiscovery` | leads, contacts, api_call_logs |
| UC06 | `filterLeads(criteria)` | SD06 1-5 | `LeadController.filterLeads` → `List<Lead>` | `currentFilter = criteria` → `LeadRepository.findByFilter` | `leads.fxml` · `onApplyFilter` | leads, contacts |
| | `editLead(leadId)` | SD06 6-9 | `LeadController.editLead` → `Lead` | `LeadRepository.findById` | `onEditLead` | leads, contacts |
| | `saveLeadChanges(leadId, updates, archived)` | SD06 10-18 | `LeadController.saveLeadChanges` → `List<Lead>` | `findById` → `Lead.applyUpdates` → opt `Lead.archive` → `save` → `findByFilter(currentFilter)` | `lead_form.fxml` · `onSave` | leads, contacts |
| UC07 | `addInteractionNote(leadId)` | SD07 1-2 | `LeadController.addInteractionNote` → `NoteForm` | — | `lead_detail.fxml` · `onAddNote` | — |
| | `submitInteraction(leadId, note)` | SD07 3-12 | `LeadController.submitInteraction` → `InteractionLog` | `Session.getUser` → `findById` → `Lead.addInteraction` («create» `InteractionLog`) → `save` | `onSubmitNote` | interaction_logs |
| UC08 | `selectLead(leadId)` | SD08 1-8 | `LeadController.selectLead` → `PipelineStatus` | `findById` → `Lead.getStatus` → `PipelineState.getStatus` | `lead_detail.fxml` · `setLead` | leads |
| | `changePipelineStatus(leadId, newStatus)` | SD08 9-22 | `LeadController.changePipelineStatus` → `PipelineMetrics` | `findById` → `Lead.changeStatus` → `PipelineState.next` → `canMoveTo` → «create» next state → `save` → `findAll` → `PipelineMetrics.recalculate` | `onStatusChanged` | leads |
| UC09 | `generateDrafts(qualifiedLeadIds)` | SD09 | `OutreachController.generateDrafts` → jobId | `BackgroundTaskRunner.submit` → (worker) `runGeneration` → `LeadRepository.findAllByIds` → `DraftGenerationService.generate` → `buildPrompt` → `LLMClient.complete` → `Lead.createDraft` («create» `EmailDraft`) → `DraftRepository.save` | `leads.fxml` · `onGenerateDrafts` | email_drafts |
| UC10 | `openDraft(draftId)` | SD10 1-4 | `OutreachController.openDraft` → `EmailDraft` | `DraftRepository.findById` | `draft_review.fxml` · `setDraft` | email_drafts |
| | `approveRevisedDraft(draftId, subject, body)` | SD10 5-12 | `OutreachController.approveRevisedDraft` → `DraftStatus` | `findById` → `EmailDraft.revise` → `approve` → `save` | `onApprove` | email_drafts |
| UC11 | `sendApprovedEmails(leadIds)` | SD11 | `OutreachController.sendApprovedEmails` → jobId | `submit` → (worker) `runDispatch` → `DraftRepository.findApprovedByLeadIds` → `Workspace.getSmtpCredentials` → `OutreachDispatcher.dispatch` → loop `getRecipientEmail` / `EmailSender.send` / `recordDispatch` («create» `OutreachDispatch`) → `save` → `findAllByIds` → `Lead.changeStatus(CONTACTED)` → `saveAll` | `leads.fxml` · `onSendEmails` | outreach_dispatches, email_drafts, leads |
| UC12 | `openExportOptions()` | SD12 1-2 | `ExportController.openExportOptions` → `ExportCriteria[]` | — | `export.fxml` · `initialize` | — |
| | `requestExport(criteria)` | SD12 3-9 | `ExportController.requestExport` | `LeadRepository.findByFilter(criteria.toFilter())` → `Exporter.format` → `pendingRows = rows` | `onExport` | leads, contacts |
| | `chooseDestination(filePath)` | SD12 10-12 | `ExportController.chooseDestination` → `int` | `Exporter.write(pendingRows, filePath)` | `onExport` | — |

## 2. Domain model (D4) → D5 class → Oracle table

| D4 conceptual class | D5 class (Fig. 1) | Table | D5 associations (role, multiplicity) |
|---|---|---|---|
| Workspace | `domain.Workspace` «Information Expert» | `workspaces` | ◆ `+icp 0..1`; ◇ `+users 0..*`, `+leads 0..*`, `+apiLogs 0..*` |
| IdealCustomerProfile | `domain.IdealCustomerProfile` | `ideal_customer_profiles` | part of Workspace (`workspace_id UNIQUE`) |
| ApiCallLog | `domain.ApiCallLog` | `api_call_logs` | `workspace_id` |
| UserAccount | `domain.UserAccount` «Information Expert» | `users` | `role : UserRole` |
| — | `domain.Session` | (not persisted) | `+user 1` |
| Lead | `domain.Lead` «Information Expert» | `leads` | `+creator 1`; ◆ `+contacts`, `+history`, `+drafts` 0..*; ◆ `+state 1` |
| Contact | `domain.Contact` | `contacts` | `+dispatches 0..*` |
| InteractionLog | `domain.InteractionLog` | `interaction_logs` | `+author 1` (`author_user_id`) |
| EmailDraft | `domain.EmailDraft` «Information Expert» | `email_drafts` | `+recipient 1` (`recipient_contact_id`); ◆ `+attempts 0..*` |
| OutreachDispatch | `domain.OutreachDispatch` | `outreach_dispatches` | `draft_id`, `recipient_contact_id` |
| — | `domain.PipelineMetrics` | (computed) | «use» Lead |
| — | `domain.state.PipelineState` + 5 states | `leads.status` | Lead `+state 1` |

## 3. Design-principle questions → where to point

| Question | Answer in code |
|---|---|
| Where is GRASP Controller? | `controller/` — six use-case controllers, D5 Fig. 2-3 |
| Information Expert? | `UserAccount.verifyPassword`, `Lead.changeStatus/applyUpdates/archive/matches`, `EmailDraft.revise/approve/getRecipientEmail`, `PipelineMetrics.recalculate`, `IdealCustomerProfile.toSearchQuery` |
| Creator? | `Workspace.createLead/generateIcp/logApiCall`, `Lead` constructor + `addInteraction/createDraft`, `EmailDraft.recordDispatch`, `LoginController` → `Session`, `PipelineState.next` |
| Polymorphism (no if/instanceof)? | `PipelineState.canMoveTo` (5 overrides), `LeadSource.fetchLeads` (CSV vs B2B), `Exporter`, `PasswordHasher`, `LLMClient`, `EmailSender` |
| Protected Variations / Indirection? | the five D5 interfaces; `BackgroundTaskRunner` |
| Pure Fabrication? | repositories, services, `CredentialEncryptor`, `BackgroundTaskRunner` |
| Encapsulation? | all fields private; changes only via D5 operations |
| Abstraction? | `PipelineState`, `JdbcRepository<T>`, five interfaces |
| Inheritance? | `PipelineState` → 5; `JdbcRepository<T>` → 4; `BlmsException` → 6 |
| GoF? | State (UC08), Adapter (3 external APIs), Strategy (`LeadSource`, `Exporter`) |
| 3 layers? | UI = `ui/` + FXML; BL = `controller`, `domain`, `service`, `integration`, `security`; DB = `persistence/` + `db/*.sql` |
| Why something not in D5? | `CONFLICTS.md` §2, gaps G1–G15 |

## 4. D3 extensions → where handled

| UC | Extension | Code |
|---|---|---|
| UC01 | Invalid credentials | `AuthenticationException` → `LoginViewController` shows it and clears password |
| UC01 | DB unavailable | `DataAccessException("Unable to connect to database")` |
| UC02 | Missing fields | `Workspace.updateDetails/setSmtpCredentials` → `ValidationException` → red `field-error` |
| UC02 | Invalid SMTP | format validation (gap G13) |
| UC03 | Duplicate | `existsByEmailDomain` → `DuplicateLeadException` |
| UC03 | Invalid email | `Contact` constructor → `Validator.requireEmail` |
| UC04 | Unsupported / malformed file | `CsvLeadSource.readHeaders` → `ValidationException` before any insert |
| UC05 | HTTP 429 / timeout | `B2BApiLeadSource` → `IntegrationException`; logged with `Workspace.logApiCall` |
| UC06 | DB timeout | `JdbcRepository.inTransaction` rollback → `DataAccessException` |
| UC07 | Empty note | Submit button disabled + `InteractionLog` validation |
| UC08 | Invalid transition | `PipelineState.next` → `InvalidStateTransitionException`; UI reverts |
| UC09 | LLM failure | `DraftGenerationService.generate` skips the lead; `getLastFailures` |
| UC10 | Empty draft | `EmailDraft.revise` → `ValidationException` |
| UC11 | SMTP auth error / invalid recipient | `OutreachDispatcher.dispatch` aborts / `recordDispatch("FAILED")` (gap G14) |
| UC12 | No records / write denied | `requestExport` → `ValidationException`; `CsvExporter.write` → `ValidationException` |
