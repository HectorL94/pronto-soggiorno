"""Checks that every translation has the same keys and format arguments as English (values/).

Usage:  python tools/check_strings.py app/src/main/res
Exit code 1 if anything is missing, extra or has different %1$s / %1$d arguments.
"""
import glob, os, re, sys
import xml.etree.ElementTree as ET

res = sys.argv[1] if len(sys.argv) > 1 else "app/src/main/res"
ARG = re.compile(r"%(\d+\$)?[sd]")


def load(folder):
    out = {}
    for f in glob.glob(os.path.join(res, folder, "strings*.xml")):
        for el in ET.parse(f).getroot():
            if el.get("translatable") == "false":
                continue
            name = el.get("name")
            if el.tag == "string":
                out[name] = [el.text or ""]
            elif el.tag == "plurals":
                out[name] = [(i.text or "") for i in el]
    return out


base = load("values")
problems = 0
for folder in ["values-it", "values-es", "values-fr", "values-ar", "values-ru", "values-uk"]:
    tr = load(folder)
    missing = sorted(set(base) - set(tr))
    extra = sorted(set(tr) - set(base) - {"app_name"})
    bad_args = []
    for k in set(base) & set(tr):
        want = sorted(set(m.group(0) for m in ARG.finditer(base[k][0])))
        for text in tr[k]:
            got = sorted(set(m.group(0) for m in ARG.finditer(text)))
            if got != want:
                bad_args.append(k)
                break
    problems += len(missing) + len(extra) + len(bad_args)
    print(f"{folder}: {len(tr)} claves | faltan {missing or '-'} | sobran {extra or '-'} | formato distinto {bad_args or '-'}")
print("TOTAL problemas:", problems, "| claves base:", len(base))
sys.exit(1 if problems else 0)
