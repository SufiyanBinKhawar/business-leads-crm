## What
<!-- One line: which use case(s) / D5 sequence diagram? e.g. "UC03 saveLead per SD03" -->

## Checklist (AGENTS.md)
- [ ] I changed only files I OWN (WORK_DIVISION.md §2), or there is a signed CONFLICTS.md entry
- [ ] No D5 class/method/attribute/association added, renamed or removed (or a DESIGN-DEVIATION signed by both)
- [ ] Controller bodies follow their D5 sequence diagram step by step
- [ ] `python3 scripts/check-d5-conformance.py` passes
- [ ] `bash scripts/check-architecture.sh` passes
- [ ] `mvn -q verify` passes and the app starts against Oracle
- [ ] Rebased on latest `main`
