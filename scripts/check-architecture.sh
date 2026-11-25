#!/usr/bin/env bash
# Layer rules from AGENTS.md section 5. Run before every push:  bash scripts/check-architecture.sh
# CI runs it on every pull request. Exit code 1 = a rule is broken (offending lines are printed).
set -u
cd "$(dirname "$0")/.."
SRC=src/main/java/com/blms
fail=0
rule() {
  if [ -n "$2" ]; then echo "FAIL: $1"; echo "$2" | sed 's/^/    /'; fail=1; else echo "ok:   $1"; fi
}
code() { grep -vE ':[0-9]+:[[:space:]]*(\*|//|/\*)'; }   # drop comment lines from grep -n output

rule "R1  javafx is imported only in ui/ and app/MainApp.java" \
  "$(grep -rn 'import javafx' $SRC | grep -v "^$SRC/ui/" | grep -v "^$SRC/app/MainApp.java")"
rule "R2  ui/ never imports persistence, integration, security or service" \
  "$(grep -rnE 'import com\.blms\.(persistence|integration|security|service)' $SRC/ui)"
rule "R3  ui/ never calls domain mutators (read domain objects through getters only)" \
  "$(grep -rnE '\.(applyUpdates|archive|changeStatus|addInteraction|createDraft|revise|approve|recordDispatch|updateDetails|generateIcp|createLead|setSmtpCredentials|logApiCall|recalculate|next)\(' $SRC/ui | code)"
rule "R4  java.sql is used only in persistence/ (and the connection in app/AppContext)" \
  "$(grep -rn 'import java\.sql' $SRC | grep -v "^$SRC/persistence/" | grep -v "^$SRC/app/AppContext.java")"
rule "R5  SQL text appears only in persistence/" \
  "$(grep -rnE '"[^"]*(SELECT |INSERT INTO|UPDATE [a-z_]+ SET|DELETE )' $SRC | grep -v "^$SRC/persistence/" | code)"
rule "R6  no instanceof in the business layer (use polymorphism)" \
  "$(grep -rn 'instanceof' $SRC/domain $SRC/controller $SRC/service $SRC/persistence $SRC/integration | code)"
rule "R7  no hard deletes - leads are archived (UC06)" \
  "$(grep -rniE 'DELETE[[:space:]]+FROM' $SRC src/main/resources)"
rule "R8  threads are created only in service/BackgroundTaskRunner" \
  "$(grep -rnE 'new Thread\(|Executors\.|CompletableFuture\.(run|supply)Async' $SRC | grep -v "^$SRC/service/BackgroundTaskRunner.java" | code)"
rule "R9  domain/ depends only on util, exception, dto and security.PasswordHasher" \
  "$(grep -rnE 'import com\.blms\.' $SRC/domain | grep -vE 'com\.blms\.(domain|util|exception|dto)\.|com\.blms\.security\.PasswordHasher;')"
rule "R10 controller/ never imports ui or javafx" \
  "$(grep -rnE 'import (com\.blms\.ui|javafx)' $SRC/controller)"
rule "R11 no System.out / printStackTrace" \
  "$(grep -rnE 'System\.out|printStackTrace' $SRC | code)"
rule "R12 controllers/repositories/services/adapters are instantiated only in app/AppContext" \
  "$(grep -rnE 'new [A-Za-z]*(Controller|Repository|Service|Dispatcher|TaskRunner|Sender|LlmClient|Exporter|Hasher|Encryptor)\(' $SRC | grep -v "^$SRC/app/AppContext.java" | code)"

[ $fail -eq 0 ] && echo "Architecture check passed." || echo "Architecture check FAILED - see AGENTS.md section 5."
exit $fail
