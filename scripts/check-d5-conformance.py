#!/usr/bin/env python3
"""
Checks that the business-layer code matches the D5 design exactly (scripts/d5-contract.txt).
FAILS when:
  - a D5 class is missing, or has the wrong kind / parent / interface
  - a D5 public operation is missing
  - a class has a PUBLIC method that is not in D5, not an approved gap, and not a getter/setter of a field
  - a class exists in a design package but is not in D5 and not approved infrastructure
  - a dto type exists that D5 does not name
Run:  python3 scripts/check-d5-conformance.py
"""
import os
import re
import sys

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
SRC = os.path.join(ROOT, "src", "main", "java", "com", "blms")
CONTRACT = os.path.join(ROOT, "scripts", "d5-contract.txt")

spec, infra_pkgs, infra_classes, dtos, field_spec = {}, set(), set(), set(), {}
for raw in open(CONTRACT, encoding="utf-8"):
    line = raw.split("#", 1)[0].strip() if not raw.lstrip().startswith("#") else ""
    if not line:
        continue
    if line.startswith("@infra-package"):
        infra_pkgs |= set(line.split()[1:]); continue
    if line.startswith("@infra-class"):
        infra_classes |= set(line.split()[1:]); continue
    if line.startswith("@dto"):
        dtos |= set(line.split()[1:]); continue
    if line.startswith("=fields"):
        cls, rest = line[len("=fields"):].split(":", 1)
        drawn, _, extra = rest.partition("|")
        field_spec[cls.strip()] = set(drawn.split()) | {re.sub(r"\(.*\)", "", x) for x in extra.split()}
        continue
    m = re.match(r"([\w.]+)\s*\[([^\]]+)\]\s*:\s*(.*)", line)
    if not m:
        sys.exit(f"bad contract line: {line}")
    name, kind, rest = m.groups()
    required, allowed = set(), set()
    for item in filter(None, (x.strip() for x in rest.split(","))):
        if item.startswith("!"):
            allowed.add(item.split()[-1])
        else:
            required.add(item)
    spec[name] = (kind.strip(), required, allowed)

errors = []
found = {}
for dirpath, _, files in os.walk(SRC):
    for f in files:
        if f.endswith(".java"):
            rel = os.path.relpath(os.path.join(dirpath, f), SRC)[:-5].replace(os.sep, ".")
            found[rel] = open(os.path.join(dirpath, f), encoding="utf-8").read()

def strip_comments(src):
    src = re.sub(r"/\*.*?\*/", "", src, flags=re.S)
    return re.sub(r"//[^\n]*", "", src)

for name, (kind, required, allowed) in spec.items():
    if name not in found:
        errors.append(f"MISSING D5 class {name}"); continue
    src = strip_comments(found[name])
    simple = name.split(".")[-1]
    head = re.search(r"public\s+(abstract\s+)?(class|interface|enum)\s+" + simple + r"\b([^{]*)\{", src)
    if not head:
        errors.append(f"{name}: no public type declaration"); continue
    abstract, ktype, tail = head.groups()
    if kind == "interface" and ktype != "interface": errors.append(f"{name}: must be an interface")
    if kind == "enum" and ktype != "enum": errors.append(f"{name}: must be an enum")
    if kind == "abstract" and not abstract: errors.append(f"{name}: must be abstract")
    if kind.startswith("extends ") and not re.search(r"extends\s+" + kind.split()[1] + r"\b", tail):
        errors.append(f"{name}: must extend {kind.split()[1]}")
    if kind.startswith("implements ") and not re.search(r"implements\s+[\w\s,<>]*\b" + kind.split()[1] + r"\b", tail):
        errors.append(f"{name}: must implement {kind.split()[1]}")
    fields = {m.group(1) for m in re.finditer(r"(?:private|protected)\s+(?:final\s+)?[\w<>, \[\]]+\s+(\w+)\s*(?:=|;)", src)}
    public = set(re.findall(r"public\s+(?:static\s+)?(?:abstract\s+)?(?:final\s+)?(?:<[^>]+>\s+)?[\w<>\[\], ]+?\s+(\w+)\s*\(", src))
    if ktype == "interface":
        public |= set(re.findall(r"^\s+(?!return|throw|new)[\w<>\[\], ]+\s+(\w+)\s*\([^)]*\)\s*;", src, flags=re.M))
    public.discard(simple)
    for op in required - public:
        errors.append(f"{name}: D5 operation '{op}' is missing")
    for op in public - required - allowed:
        accessor = re.match(r"(get|set|is)([A-Z]\w*)$", op)
        if accessor:
            field = accessor.group(2)[0].lower() + accessor.group(2)[1:]
            if field in fields or ("is" + accessor.group(2)) in fields:
                continue
        errors.append(f"{name}: public method '{op}' is NOT in D5 (add it to D5 + CONFLICTS.md, or make it private)")

CONSTANT = re.compile(r"^[A-Z0-9_]+$")
for name, allowed_fields in field_spec.items():
    if name not in found:
        continue
    src = strip_comments(found[name])
    declared = {m.group(1) for m in re.finditer(
        r"^\s*(?:private|protected)\s+(?:final\s+)?[\w<>, \[\].]+\s+(\w+)\s*(?:=|;)", src, flags=re.M)}
    declared = {f for f in declared if not CONSTANT.match(f)}
    for f in sorted(declared - allowed_fields):
        errors.append(f"{name}: field '{f}' is not a D5 attribute/role (add to D5 + CONFLICTS.md, or remove it)")
    for f in sorted(allowed_fields - declared):
        errors.append(f"{name}: D5 attribute/role '{f}' is missing")

for name in found:
    top = name.split(".")[0]
    if name in spec or top in infra_pkgs or name in infra_classes:
        if top == "dto" and name.split(".")[-1] not in dtos:
            errors.append(f"{name}: dto type not named anywhere in D5")
        continue
    errors.append(f"{name}: class is NOT in D5 and not approved infrastructure (see scripts/d5-contract.txt)")

print(f"D5 conformance: {len(spec)} design classes, {len(field_spec)} attribute lists checked, {len(found)} source files scanned.")
if errors:
    print("FAILED:")
    for e in sorted(errors):
        print("  - " + e)
    sys.exit(1)
print("OK - code matches D5.")
