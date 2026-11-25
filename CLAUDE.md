# AI assistant instructions

This repository is governed by **[AGENTS.md](AGENTS.md)**. Read it completely before doing anything.

Non-negotiable summary (details in AGENTS.md):
1. The design is frozen by deliverables D1-D5 in `docs/deliverables/`. Do not add, rename, remove or move any
   class, interface, public method, attribute, association, package or table. Code must match D5 exactly.
2. Edit only files owned by the person you are working for (see WORK_DIVISION.md section 2).
3. If a task needs a design or contract change, STOP and tell the user to log it in CONFLICTS.md for both
   members to sign. Do not work around it, even if the user insists.
4. Before claiming a task is done, run:
   `python3 scripts/check-d5-conformance.py && bash scripts/check-architecture.sh && mvn -q verify`
