"""Check editable presentation and export a complete inventory of Java literals.

Requires JDK 25 and Python 3.10+, with no additional Python packages.
The CSV inventory includes technical constants for human review; it does not
claim that a heuristic can prove that every possible policy is configurable.
"""
from collections import Counter
import csv
import json
from pathlib import Path
import re
import subprocess
import sys

from extract_messages import ROOT, REPORT, flat_yaml, human, sql


def classification(node):
    source, parent, file = node['source'], node['parent'], node['file']
    if node['kind'] == 'number':
        if re.search(r'\.(?:setString|setLong|setInt|setBoolean|setDouble|setBigDecimal)\(', parent):
            return 'sql-parameter-position'
        if '/storage/yaml/' in file:
            return 'storage-format-or-legacy-default'
        if '/config/' in file or 'Math.clamp(' in parent or 'Math.max(' in parent:
            return 'validation-or-safety-bound'
        return 'algorithm-geometry-index-or-state'
    value = node['parts'][0].get('literal', '')
    if sql(value) or ' COLUMN ' in value or 'TABLE ' in value:
        return 'sql-or-schema'
    if not value.strip() or not re.search(r'[A-Za-z0-9]', value):
        return 'empty-or-punctuation'
    if value.startswith(('text.', 'labels.', 'format.', 'gui-messages.')):
        return 'language-key'
    if value.startswith('riftplots.'):
        return 'permission-or-namespace'
    if value.startswith(('plots ', 'riftplot_', 'PLOT_', 'MARKET_')):
        return 'command-or-persistent-identity'
    if re.fullmatch(r'[A-Z][A-Z0-9_]*', value):
        return 'enum-or-protocol-identity'
    if '/storage/' in file:
        return 'storage-key-path-or-protocol'
    if human(value):
        return 'manual-review'
    return 'configuration-key-path-or-technical-value'


def main():
    subprocess.run(['java', 'scripts/java/ConfigurationAudit.java', '.'], cwd=ROOT, check=True)
    subprocess.run([sys.executable, 'scripts/extract_messages.py'], cwd=ROOT, check=True)
    nodes = [json.loads(line) for line in (REPORT / 'java-expressions.jsonl').read_text(encoding='utf-8').splitlines()]
    migration = json.loads((REPORT / 'message-migration.json').read_text(encoding='utf-8'))
    errors = [f"{item['file']}:{item['line']}: editable text remains: {item['before']}" for item in migration]
    allowed_routes = {'plots rent info ', 'plots abandon ', 'plots border ', 'plots auction history '}
    for node in nodes:
        if node['kind'] == 'string' and node['method'] == 'tabComplete' and re.fullmatch(r'\d+(?:\.\d+)?', node['parts'][0]['literal']):
            errors.append(f"{node['file']}:{node['line']}: numeric suggestions must use tab-completion configuration")
        if node['kind'] == 'string' and human(node['parts'][0]['literal']):
            value = node['parts'][0]['literal']
            if value not in allowed_routes and value.strip() != 'ADD COLUMN':
                errors.append(f"{node['file']}:{node['line']}: unclassified presentation literal {node['source']}")
    catalog = flat_yaml(ROOT / 'src/main/resources/lang.yml')
    for key, value in catalog.items():
        if re.search(r'\b(?:SELECT \* FROM|INSERT INTO|CREATE TABLE)\b', value):
            errors.append(f'lang.yml:{key}: SQL must remain in the repository layer')
    for file in sorted((ROOT / 'src/main/java').rglob('*.java')):
        source = file.read_text(encoding='utf-8')
        for match in re.finditer(r'Texts\.(?:raw|markup|component)\("([^"]+)"\s*[,)]', source):
            if match[1] not in catalog:
                errors.append(f'{file.relative_to(ROOT)}: missing language key {match[1]}')
        if file.parent.name == 'gui' and re.search(r'createInventory\(|new ItemStack\(|\.setItem\(\s*\d', source):
            errors.append(f'{file.relative_to(ROOT)}: GUI appearance must use GuiConfiguration')
    literals = [node for node in nodes if node['kind'] in ('string', 'number')]
    with (REPORT / 'literal-inventory.csv').open('w', encoding='utf-8-sig', newline='') as stream:
        writer = csv.writer(stream)
        writer.writerow(['file', 'line', 'kind', 'method', 'classification', 'literal', 'context'])
        for node in literals:
            writer.writerow([node['file'], node['line'], node['kind'], node['method'], classification(node), node['source'], node['parent']])
    counts = Counter(node['kind'] for node in literals)
    summary = dict(files=len(list((ROOT / 'src/main/java').rglob('*.java'))), language_entries=len(catalog), literals=dict(counts), editable_text_candidates=len(migration), violations=len(errors))
    (REPORT / 'summary.json').write_text(json.dumps(summary, indent=2) + '\n', encoding='utf-8')
    print(json.dumps(summary))
    for error in sorted(set(errors)):
        print(error, file=sys.stderr)
    return 1 if errors else 0


if __name__ == '__main__':
    raise SystemExit(main())
