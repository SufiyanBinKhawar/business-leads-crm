# Business Leads Management System (CRM)

JavaFX desktop CRM for B2B lead discovery, pipeline tracking and LLM-assisted outreach, on Oracle.
SDA Fall 2026 · Section G · Muhammad Ismail (25I-0579) · Sufiyan Ahmed (25I-0557)

> **Humans and AI assistants: read [`AGENTS.md`](AGENTS.md) before changing anything.**
> The design is frozen by our deliverables D1–D5 ([`docs/deliverables/`](docs/deliverables)).
> Ownership and schedule: [`WORK_DIVISION.md`](WORK_DIVISION.md). Problems and D5 gaps: [`CONFLICTS.md`](CONFLICTS.md).

## Set up and run

Requirements: **JDK 21**, IntelliJ IDEA, **Oracle Database 23ai Free** (or XE 21c) and SQL Developer.

1. **Database** (once):
   - as `SYSTEM` on service `FREEPDB1` (XE: `XEPDB1`): run `src/main/resources/db/01_create_user.sql`
   - as `BLMS` (password `blms`): run `02_schema.sql`, then `03_seed.sql`
   - reset any time: `99_drop.sql`, then `02` and `03` again
2. Optional: copy `config/local.properties.example` → `config/local.properties` (git-ignored) to change the
   DB URL/password or add API keys.
3. Open the folder in IntelliJ → *Load Maven project* → run **`com.blms.app.Launcher`**
   (or `mvn javafx:run`).
4. Log in: `admin / admin123` (Workspace Administrator) or `rep / rep123` (Sales Representative).

Blank `b2b.apiKey`, `llm.apiKey` and SMTP host = offline demo mode (sample prospects, template emails,
logged instead of sent), so the demo works without Internet. See `CONFLICTS.md` gap G12.

## Checks before every push (CI runs the same)

```bash
python3 scripts/check-d5-conformance.py   # code matches D5: classes, interfaces, inheritance, public methods
bash scripts/check-architecture.sh        # layer rules R1-R12
mvn -q verify                             # compile + unit tests
```

## Architecture (MVC, 3 layers — D1)

```
UI  (ui/ + FXML)      ──►  D5 «Controller»s (controller/)  ──►  domain/ · service/ · integration/ · security/
                                                                          │
DB  (persistence/: JdbcRepository<T> → User/Workspace/Lead/DraftRepository)  ──►  Oracle (9 tables)
```

Use case → SSD → sequence diagram → method → screen → table: [`docs/DESIGN_TRACEABILITY.md`](docs/DESIGN_TRACEABILITY.md).

## Use cases (D3)

UC01 Authenticate System User · UC02 Configure Company Workspace · UC03 Manually Add New Lead ·
UC04 Import Leads via CSV · UC05 Discover Leads via B2B API · UC06 Manage Existing Leads ·
UC07 Log Manual Interaction · UC08 Update Pipeline Status · UC09 Generate Emails via LLM ·
UC10 Review and Revise Drafts · UC11 Dispatch Automated Outreach · UC12 Export Outreach Results
