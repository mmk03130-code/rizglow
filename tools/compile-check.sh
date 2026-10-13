#!/bin/bash
# Local Kotlin compile check (Gradle daemon can't fork in this sandbox).
# Usage: tools/compile-check.sh
cd "$(dirname "$0")/.."
python3 - << 'PYEOF'
import re, os
refs = set()
for root, _, files in os.walk("app/src/main/java"):
    for f in files:
        if f.endswith(".kt"):
            refs.update(re.findall(r"R\.([a-z_]+)\.([a-zA-Z0-9_]+)", open(os.path.join(root, f)).read()))
bytype = {}
for t, n in refs:
    bytype.setdefault(t, set()).add(n)
out = ["package com.rsworks.rizglow", "", "object R {"]
i = 0x7f000001
for t in sorted(bytype):
    out.append(f"    object {t} {{")
    for n in sorted(bytype[t]):
        out.append(f"        const val {n}: Int = {i}")
        i += 1
    out.append("    }")
out.append("}")
os.makedirs("tools/.rstub", exist_ok=True)
open("tools/.rstub/R.kt", "w").write("\n".join(out))
PYEOF
rm -rf tools/.kout && mkdir -p tools/.kout
~/workspace/kotlinc/bin/kotlinc $(find app/src/main/java -name "*.kt") tools/.rstub/R.kt tools/.rstub/ads-stub.kt tools/.rstub/rewarded-stub.kt \
  -cp ~/workspace/android-sdk/platforms/android-36/android.jar \
  -d tools/.kout -jvm-target 17 -nowarn && echo COMPILE_OK
