"""Encode genuine game frames; mix the original samples resolved by the game.

The mix follows event timestamps, selected random samples, pitch, distance and
loop-stop times. This is explicitly event-based audio rendering, not WASAPI
capture. It cannot include unrelated desktop messages or microphone input.
"""
from pathlib import Path
import json,sys,subprocess,wave
import numpy as np
ROOT=Path(__file__).resolve().parents[1]
FFMPEG=ROOT.parent/'video-audio/vendor/imageio_ffmpeg/binaries/ffmpeg-win-x86_64-v7.1.exe'
run=Path(sys.argv[1]).resolve()
assert run.parent==ROOT and run.name.startswith('run-production-battle')
frames=sorted(json.loads((run/'recorded-frames.json').read_text()),key=lambda f:f['epoch_ms'])
sounds=json.loads((run/'recorded-sounds.json').read_text())
assert len(frames)>=60 and len(sounds)>=5,'Actual frame and sound events are required'
kind=json.loads((run/'battle-evidence.json').read_text())['opponent']
start=frames[0]['epoch_ms'];duration=(frames[-1]['epoch_ms']-start)/1000+.08;rate=48000
mix=np.zeros((int((duration+.5)*rate),2),np.float32);cache={};audible=0
for event in sounds:
    file=event['file']
    if file not in cache:
        raw=subprocess.run([str(FFMPEG),'-v','error','-i',str(run/'audio-samples'/file),'-f','f32le','-ac','2','-ar',str(rate),'-'],capture_output=True,check=True).stdout
        cache[file]=np.frombuffer(raw,np.float32).reshape(-1,2)
    source=cache[file];pitch=max(.5,min(2,event['pitch']));indices=np.arange(0,len(source)-1,pitch)
    data=np.column_stack([np.interp(indices,np.arange(len(source)),source[:,i]) for i in range(2)])
    if event['loop']:
        length=int(max(0,event.get('end_ms',start+duration*1000)-event['epoch_ms'])/1000*rate)
        if length==0:continue
        data=np.tile(data,(int(np.ceil(length/len(data))),1))[:length]
        fade=min(int(.03*rate),len(data));data[-fade:]*=np.linspace(1,0,fade)[:,None]
    offset=int((event['epoch_ms']-start)/1000*rate)
    if offset<0:data=data[-offset:];offset=0
    length=min(len(data),len(mix)-offset)
    if length<=0:continue
    gain=min(1,event['volume'])*event['attenuation']*.85
    if gain>0:audible+=1
    mix[offset:offset+length]+=data[:length]*gain
peak=float(np.max(np.abs(mix)));assert peak>1e-5,'Unexpected silent event mix'
# A gentle peak ceiling only; do not normalize quiet original cues into shouts.
if peak>.88:mix*=.88/peak
wav=run/'battle-game-event-audio.wav'
with wave.open(str(wav),'wb') as w:
    w.setnchannels(2);w.setsampwidth(2);w.setframerate(rate);w.writeframes((np.clip(mix,-1,1)*32767).astype(np.int16).tobytes())
concat=run/'battle-frames.ffconcat';lines=['ffconcat version 1.0']
for i,f in enumerate(frames):
    lines.append("file 'video-frames/"+f['file']+"'")
    lines.append('duration '+str(max(.001,(frames[i+1]['epoch_ms']-f['epoch_ms'])/1000) if i+1<len(frames) else .05))
lines.append("file 'video-frames/"+frames[-1]['file']+"'");concat.write_text('\n'.join(lines)+'\n')
out=ROOT/'dist/videos';out.mkdir(parents=True,exist_ok=True)
final=out/('01-Zappies-vs-MineClash-PEKKA.mp4' if kind=='pekka' else '02-Zappies-vs-RoyaleSpells-InfernoDragon.mp4')
ass=str(ROOT/'docs/validation'/('battle-'+kind+'.ass')).replace('\\','/').replace(':','\\:')
with (run/'encoding.log').open('w') as log:
    subprocess.run([str(FFMPEG),'-y','-hide_banner','-safe','0','-f','concat','-i',str(concat),'-i',str(wav),'-vf',"ass='"+ass+"'",
        '-c:v','libx264','-preset','medium','-crf','18','-r','30','-pix_fmt','yuv420p','-c:a','aac','-b:a','192k','-shortest','-movflags','+faststart',str(final)],check=True,stdout=log,stderr=log)
report={'file':final.name,'frame_count':len(frames),'duration_seconds':duration,'sound_events':len(sounds),'audible_events':audible,
        'mix_peak_before_ceiling':peak,'audio_method':'actual game sound event samples, pitch, distance, timing, loop stops; no desktop or microphone audio'}
(run/'video-report.json').write_text(json.dumps(report,ensure_ascii=False,indent=2));print(json.dumps(report,ensure_ascii=False,indent=2))
