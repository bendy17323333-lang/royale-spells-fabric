"""Check observations produced by the real packaged client, not synthetic poses."""
from pathlib import Path
import json,sys,hashlib
run=Path(sys.argv[1])
a=json.loads((run/'animation-isolation.json').read_text())
c=json.loads((run/'electrical-combat.json').read_text())
report={'run':run.name,'checks':{},'jars':{p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in (run/'mods').glob('*.jar')}}
for name,neighbour in [('Z1','Z2'),('S1','S2')]:
    rows=[r for r in a if r['name']==name and r['paused']]
    assert len(rows)>=7,(name,'pause was not observed')
    times={r['animation_time'] for r in rows}
    assert len(times)==1,(name,'animation clock advanced while stunned',times)
    for r in rows:
        assert r['bones']==rows[0]['bones'],(name,'bone pose changed while paused',r['tick'])
    other=[r for r in a if r['name']==neighbour and rows[0]['tick']<=r['tick']<=rows[-1]['tick']]
    assert len({r['animation_time'] for r in other})>=5,(name,'unaffected neighbour stopped')
    assert any(r['bones']!=other[0]['bones'] for r in other[1:]),(name,'neighbour pose did not continue')
    resumed=next(r for r in a if r['name']==name and r['tick']>rows[-1]['tick'] and not r['paused'])
    delta=resumed['animation_time']-rows[-1]['animation_time']
    assert 0<delta<3,(name,'resume jumped ahead instead of continuing',delta)
    report['checks'][name]={'held_samples':len(rows),'held_time':rows[0]['animation_time'],'resume_delta':delta,'neighbour_continues':True}
for name in ['Z1','Z2']:
    rows=[r for r in a if r['name']==name and r['tick']>=260]
    assert rows and all(r['health']>0 and r['deathTime']==0 for r in rows)
    for r in rows:
        assert r['bones']['group2']==[0,0,0,0,0,0,1,1,1],('foreign wheel death pose',name,r['bones']['group2'])
report['checks']['one_death_does_not_contaminate_survivors']=True
for slot in range(3):
    rows=[r for r in c if r['slot']==slot];previous=0;shot_times=[]
    for r in rows:
        if r['shots']>previous:shot_times.append(r['server_time']);previous=r['shots']
    assert len(shot_times)>=3,('a car failed to fire repeatedly',slot)
    intervals=[b-a for a,b in zip(shot_times,shot_times[1:])]
    # A render-tick observation can arrive one server tick late, but it must not
    # hide a doubled cadence or a car which never attacks.
    assert all(45<=d<=47 for d in intervals),(slot,intervals)
    report['checks']['car_'+str(slot)]={'shots':previous,'intervals_ticks':intervals}
held=[r for r in c if r['slot']==1 and r['paused']]
assert len(held)>=9 and len({r['windup'] for r in held})==1 and held[0]['windup']>0
report['checks']['actual_windup_pause']={'samples':len(held),'windup':held[0]['windup']}
assert 'ZAPPIES_QA_COMPLETE' in (run/'console.log').read_text(errors='replace')
assert list((run/'screenshots').glob('*connected-attack-arc*'))
out=run/'verified-client-report.json';out.write_text(json.dumps(report,ensure_ascii=False,indent=2))
print(json.dumps(report['checks'],ensure_ascii=False,indent=2));print('VERIFIED',out)
