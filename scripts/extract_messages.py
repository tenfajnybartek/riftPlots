"""Migrate presentation expressions found by ConfigurationAudit.java to lang.yml.

Uses source ranges from the Java compiler, never regular-expression replacement of Java syntax.
Run without --apply to produce a reviewable migration plan. Uses Python's standard library only.
"""
from pathlib import Path
import argparse
import hashlib
import json
import re
import unicodedata

ROOT = Path(__file__).resolve().parents[1]
REPORT = ROOT / 'build/reports/configuration'


def flat_yaml(path):
    result, parents = {}, []
    for line in path.read_text(encoding='utf-8').splitlines():
        match = re.match(r'^( *)([A-Za-z0-9_-]+):(?:\s+(.*))?$', line)
        if not match: continue
        indent, name, value = len(match[1]), match[2], match[3]
        while parents and parents[-1][0] >= indent: parents.pop()
        key = '.'.join([p[1] for p in parents] + [name])
        if value is None or value == '': parents.append((indent, name)); continue
        if value.startswith('"'): result[key] = json.loads(value)
        elif value.startswith("'"): result[key] = value[1:-1].replace("''", "'")
        elif not value.startswith('#'): result[key] = value
    return result


def save_yaml(path, flat):
    tree = {}
    for key, value in flat.items():
        current = tree
        parts = key.split('.')
        for part in parts[:-1]: current = current.setdefault(part, {})
        current[parts[-1]] = value
    lines = ['# riftPlots: MiniMessage, {prefix} oraz placeholdery nazwane / {arg1}, {arg2}, ...',
             '# Komendy, komunikaty gracza, walidacja, historia i diagnostyka. GUI: gui.yml.',
             '# Usunięty klucz korzysta z domyślnego szablonu dołączonego do JAR-a.', '']
    def emit(mapping, depth=0):
        for key, value in mapping.items():
            prefix = '  ' * depth + key + ':'
            if isinstance(value, dict): lines.append(prefix); emit(value, depth + 1)
            else: lines.append(prefix + ' ' + json.dumps(value, ensure_ascii=False))
            if depth == 0: lines.append('')
    emit(tree)
    path.write_text('\n'.join(lines).rstrip() + '\n', encoding='utf-8')


def human(text):
    if sql(text): return False
    if text.startswith(('jdbc:', 'https:', 'http:', 'minecraft:')): return False
    if re.search(r'yyyy|HH:mm|%[,.0-9]*[dfs]', text): return False
    if re.search(r'<(?:red|green|gold|gray|yellow|white|aqua|blue|dark_|gradient|bold|b>)', text): return True
    if re.search(r'[A-Za-z\u00c0-\u024f]{2,}\s+[A-Za-z\u00c0-\u024f<]', text): return True
    if re.search(r'[\u00c0-\u024f]', text) and not re.search(r'[\[\]\\^$]', text): return True
    return (bool(re.fullmatch(r'[A-Z][a-z]+', text)) and text not in {'Vault','WorldEdit','WorldGuard','SQLite','MySQL','Minecraft','Paper','Boolean','String','Integer'}) or text in {'Brak', 'Nigdy', 'brak', 'wszystkie', 'Unknown', 'SYSTEM', 'Player'}


def sql(text):
    return bool(re.search(r'\b(SELECT|INSERT|CREATE|ALTER|UPDATE|DELETE|VARCHAR|PRIMARY KEY|ON CONFLICT|ON DUPLICATE|DROP TABLE|ORDER BY|WHERE|GROUP BY|LIMIT|OFFSET)\b', text))


def main():
    parser = argparse.ArgumentParser(description=__doc__); parser.add_argument('--apply', action='store_true'); args = parser.parse_args()
    nodes = [json.loads(line) for line in (REPORT/'java-expressions.jsonl').read_text(encoding='utf-8').splitlines()]
    catalog = flat_yaml(ROOT/'src/main/resources/lang.yml')
    original_keys = set(catalog)
    replacements = []
    skip = {'MessageConfiguration', 'Texts', 'GuiConfiguration', 'GuiVariables', 'Presentation', 'ConfigurationFiles', 'Settings', 'PluginConfiguration', 'StorageConfiguration', 'PlotGUIListener'}
    def template(parts):
        chunks, arguments = [], []
        for part in parts:
            if 'literal' in part: chunks.append(part['literal'])
            else: arguments.append(part['expr']); chunks.append('{arg' + str(len(arguments)) + '}')
        return ''.join(chunks), arguments
    def key_for(node, text):
        kind = Path(node['file']).stem
        prefix = re.sub(r'(?<!^)(?=[A-Z])', '-', kind.removesuffix('SubCommand')).lower()
        words = re.sub(r'<[^>]*>|\{[^}]*}', ' ', text)
        slug = unicodedata.normalize('NFKD', words).encode('ascii', 'ignore').decode().lower()
        slug = '-'.join(re.findall(r'[a-z0-9]+', slug)[:7])[:65] or 'line'
        if node['method'] in ('getUsage','getDescription'): slug = 'usage' if node['method']=='getUsage' else 'description'
        key = f'text.{prefix}.{slug}'
        if key in catalog and catalog[key] != text: key += '-' + hashlib.sha1(text.encode()).hexdigest()[:7]
        catalog[key] = text
        return key
    def replacement(node, parts, renderer='raw'):
        text, arguments = template(parts)
        # Legacy section codes become editable MiniMessage tags rather than literal section signs.
        colors = {'0':'black','1':'dark_blue','2':'dark_green','3':'dark_aqua','4':'dark_red','5':'dark_purple','6':'gold','7':'gray','8':'dark_gray','9':'blue','a':'green','b':'aqua','c':'red','d':'light_purple','e':'yellow','f':'white','l':'bold','o':'italic','n':'underlined','m':'strikethrough','k':'obfuscated','r':'reset'}
        text = re.sub('§([0-9a-fklmnor])', lambda m:'<'+colors[m[1].lower()]+'>', text, flags=re.I)
        key = key_for(node, text)
        return 'pl.riftplots.config.Texts.' + renderer + '(' + json.dumps(key) + (', ' + ', '.join(arguments) if arguments else '') + ')'
    def add(node, source, priority): replacements.append(dict(node, replacement=source, priority=priority))
    for node in nodes:
        stem = Path(node['file']).stem
        if stem in skip or stem in {'PropertyTypeManager','DistrictManager'} and node['method'] in {'registerDefaults','createDefaultConfig'}: continue
        if node['kind'] == 'call':
            select, arguments = node['select'], node['arguments']
            if select.endswith('.getRaw') and len(arguments)==2 and all(a['kind']=='STRING_LITERAL' for a in arguments):
                key, fallback = arguments[0]['parts'][0]['literal'], arguments[1]['parts'][0]['literal']
                catalog.setdefault(key, fallback); add(node, select+'('+arguments[0]['source']+')', 100); continue
            if re.search(r'(?:messageConfig|messages)\.get$', select) and len(arguments)>=2 and arguments[1]['kind']=='STRING_LITERAL':
                if arguments[0]['kind']=='STRING_LITERAL':
                    catalog.setdefault(arguments[0]['parts'][0]['literal'], arguments[1]['parts'][0]['literal'])
                    add(node, select+'('+', '.join(a['source'] for a in [arguments[0],*arguments[2:]])+')',100); continue
            index = 1 if select.endswith('AdventureUtil.sendMessage') else 0
            is_component = select.endswith('Component.text')
            is_send = select.endswith('.sendMessage') or select.endswith('AdventureUtil.parse')
            if (is_component or is_send) and len(arguments)>index and arguments[index]['kind'] in ('STRING_LITERAL','PLUS') and any('literal' in p for p in arguments[index]['parts']):
                renderer = 'component' if is_component or (select.endswith('.sendMessage') and not select.endswith('AdventureUtil.sendMessage')) else 'markup'
                expression = replacement(node, arguments[index]['parts'], renderer)
                if is_component and len(arguments)==1: add(node, expression, 90)
                else: add(dict(node, start=arguments[index]['start'],end=arguments[index]['end'],source=arguments[index]['source']), expression, 90)
        elif node['kind'] in ('string','concat'):
            parts = node['parts']
            if any(sql(p.get('literal','')) for p in parts) or sql(node['parent']): continue
            if not any(human(p.get('literal','')) for p in parts): continue
            if '.performCommand(' in node['parent']: continue
            if any(marker in node['parent'] for marker in ('Pattern.compile(', '.matches(', '.replaceAll(', '.replaceFirst(')) and node['kind']=='string': continue
            add(node, replacement(node, parts), 10)
    # Outermost expressions win. Nested expressions are handled by the next compiler scan.
    chosen = []
    for node in sorted(replacements,key=lambda n:(n['file'], -n['priority'], -(n['end']-n['start']), n['start'])):
        if not any(other['file']==node['file'] and node['start']<other['end'] and other['start']<node['end'] for other in chosen): chosen.append(node)
    plan = [{'file':n['file'],'line':n['line'],'before':n['source'],'after':n['replacement']} for n in chosen]
    used = {key for node in chosen for key in re.findall(r'Texts\.\w+\("([^\"]+)"', node['replacement'])}
    catalog = {key:value for key,value in catalog.items() if key in original_keys or not key.startswith('text.') or key in used}
    (REPORT/'message-migration.json').write_text(json.dumps(plan,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    if args.apply:
        staged = {}
        for file in sorted({n['file'] for n in chosen}):
            path=ROOT/file
            with path.open(encoding='utf-8', newline='') as reader: content=reader.read()
            for node in [n for n in chosen if n['file']==file]:
                if content[node['start']:node['end']] != node['source']:
                    raise ValueError(f'Source changed since the compiler scan: {file}:{node["line"]}')
            for node in sorted([n for n in chosen if n['file']==file],key=lambda n:-n['start']): content=content[:node['start']]+node['replacement']+content[node['end']:]
            staged[path] = content
        for path,content in staged.items():
            with path.open('w',encoding='utf-8',newline='') as writer: writer.write(content)
        save_yaml(ROOT/'src/main/resources/lang.yml',catalog)
    print(json.dumps({'expressions':len(chosen),'files':len({n['file'] for n in chosen}),'catalog_entries':len(catalog),'applied':args.apply}))

if __name__ == '__main__': main()
