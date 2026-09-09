"""Adapt the workspace's verified production launcher into an isolated MineClash QA run."""
from pathlib import Path
repo=Path(__file__).resolve().parents[1]
work=repo.parent
s=(work/'launch-inferno-production.py').read_text(encoding='utf-8')
s=s.replace("work=Path(__file__).resolve().parent", "work=Path(__file__).resolve().parents[2]")
s=s.replace("repo=work/'royale-spells-iron-1.21.1'", "repo=work/'mineclash-zappies-addon'")
a=s.index("assert mode in ("); b=s.index("(run/'options.txt')",a)
s=s[:a]+'''assert mode in ('baseline','pack','fixed','combat') and re.fullmatch(r'[a-zA-Z0-9_-]*',suffix)
run=repo/('run-production-'+mode+suffix);(run/'mods').mkdir(parents=True,exist_ok=True);(run/'natives').mkdir(exist_ok=True)
mod_version=re.search(r'^mod_version=(.+)$',(repo/'gradle.properties').read_text(),re.M)[1].strip()
jar=repo/f'build/libs/mineclash-zappies-neoforge-1.21.1-{mod_version}.jar';shutil.copy2(jar,run/'mods'/jar.name)
if mode=='pack' or 'pack' in suffix:
    for p in Path('C:/Users/BliBe/curseforge/minecraft/Instances/1.21.1/mods').glob('*.jar'):
        if mode in ('fixed','combat') and p.name.startswith('royale-spells-'):continue
        shutil.copy2(p,run/'mods'/p.name)
    if mode in ('fixed','combat'):
        b=work/'royale-spells-iron-1.21.1'
        bv=re.search(r'^mod_version=(.+)$',(b/'gradle.properties').read_text(),re.M)[1].strip()
        fixed=b/f'build/libs/royale-spells-neoforge-1.21.1-{bv}.jar'
        shutil.copy2(fixed,run/'mods'/fixed.name)
else:
    for p in (repo/'libs').glob('*.jar'):shutil.copy2(p,run/'mods'/p.name)
''' + s[b:]
a=s.index("params=['-Xmx4G'");b=s.index("params+=args(",a)
s=s[:a]+"params=['-Xmx3G','-Dzappiesaddon.smoke=true']\nif mode=='combat':params+=['-Dzappiesaddon.electricalProbe=true']\n"+s[b:]
a=s.index("marker={");b=s.index("if code or marker",a)
s=s[:a]+"marker='ZAPPIES_QA_COMPLETE'\n"+s[b:]
s=s.replace('RoyaleIsolatedQA','ZappiesIsolatedQA').replace('RoyaleNeoQA','ZappiesQA')
(repo/'tools/launch-client.py').write_text(s,encoding='utf-8')
print('Prepared isolated launcher')
