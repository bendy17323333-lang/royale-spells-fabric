"""Encode the isolated game's timestamped framebuffer exports; never the desktop.

Audio uses the current speaker WASAPI loopback (no microphone). The opt-in game
director waits for our recording-start file, then runs unmodified combat AI.
Install the recording-only PyAudioWPatch dependency outside the release sources.
Reference: https://github.com/s0d3s/PyAudioWPatch
"""
from pathlib import Path
import sys,time,subprocess,wave,json
ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'research/recording-deps'))
sys.path.insert(0,str(ROOT/'research/recording-runtime'))
import pyaudiowpatch as pa
FFMPEG=ROOT.parent/'video-audio/vendor/imageio_ffmpeg/binaries/ffmpeg-win-x86_64-v7.1.exe'
run=Path(sys.argv[1]).resolve()
if run.parent!=ROOT or not run.name.startswith('run-production-battle'):
    raise ValueError('Expected this project\'s isolated battle world')
out=ROOT/'dist/videos';out.mkdir(parents=True,exist_ok=True)
deadline=time.monotonic()+240
while not (run/'battle-ready.txt').exists():
    if time.monotonic()>deadline:raise TimeoutError('Game never exposed the recording scene')
    time.sleep(.3)
kind=(run/'battle-ready.txt').read_text().strip()
assert kind in ('pekka','dragon')
wav=run/'battle-audio.wav'
if '--encode-only' not in sys.argv:
    with pa.PyAudio() as p,wave.open(str(wav),'wb') as w:
        info=p.get_default_wasapi_loopback();rate=int(info['defaultSampleRate']);channels=int(info['maxInputChannels'])
        w.setnchannels(channels);w.setsampwidth(2);w.setframerate(rate)
        def callback(data,count,timing,status):
            w.writeframesraw(data)
            return (None,pa.paContinue)
        audio_start=time.time()
        with p.open(format=pa.paInt16,channels=channels,rate=rate,input=True,input_device_index=info['index'],
                    frames_per_buffer=1024,stream_callback=callback):
            time.sleep(.8)
            (run/'recording-start.txt').write_text('recording')
            print('CAPTURE_STARTED',kind,flush=True)
            deadline=time.monotonic()+200
            while not (run/'battle-complete.txt').exists():
                if time.monotonic()>deadline:raise RuntimeError('Battle/capture did not finish')
                time.sleep(.15)
            time.sleep(.4)
    (run/'audio-start.json').write_text(json.dumps({'start':audio_start,'rate':rate,'channels':channels}))
    if '--capture-only' in sys.argv:
        print('AUDIO_CAPTURE_COMPLETE',run,flush=True)
        sys.exit(0)
else:
    meta=json.loads((run/'audio-start.json').read_text())
    audio_start=meta['start'];rate=meta['rate'];channels=meta['channels']
frames=json.loads((run/'recorded-frames.json').read_text())
if len(frames)<60:raise RuntimeError('Insufficient real gameplay frames')
frames.sort(key=lambda f:f['epoch_ms'])
concat=run/'battle-frames.ffconcat'
lines=['ffconcat version 1.0']
for i,f in enumerate(frames):
    lines.append("file 'video-frames/"+f['file']+"'")
    duration=(frames[i+1]['epoch_ms']-f['epoch_ms'])/1000 if i+1<len(frames) else .05
    lines.append('duration '+str(max(.001,duration)))
lines.append("file 'video-frames/"+frames[-1]['file']+"'")
concat.write_text('\n'.join(lines)+'\n')
trim_audio=frames[0]['epoch_ms']/1000-audio_start
(run/'recording-timing.json').write_text(json.dumps({'audio_trim_seconds':trim_audio,'sample_rate':rate,'channels':channels,'frames':len(frames),'duration':(frames[-1]['epoch_ms']-frames[0]['epoch_ms'])/1000},indent=2))
final=out/('01-Zappies-vs-MineClash-PEKKA.mp4' if kind=='pekka' else '02-Zappies-vs-RoyaleSpells-InfernoDragon.mp4')
# Subtitle file is optional: gameplay is always captured first, never simulated.
ass=ROOT/'docs/validation'/('battle-'+kind+'.ass')
cmd=[str(FFMPEG),'-y','-hide_banner','-safe','0','-f','concat','-i',str(concat),'-ss',str(max(0,trim_audio)),'-i',str(wav)]
if ass.exists():
    escaped=str(ass).replace('\\','/').replace(':','\\:')
    cmd+=['-vf',"ass='"+escaped+"'"]
cmd+=['-c:v','libx264','-preset','medium','-crf','19','-r','30','-pix_fmt','yuv420p','-af','volume=0.85,alimiter=limit=0.92','-c:a','aac','-b:a','192k','-shortest','-movflags','+faststart',str(final)]
subprocess.run(cmd,check=True,stdout=subprocess.DEVNULL,stderr=(run/'encoding.log').open('w'))
print('VIDEO_SAVED',final,flush=True)
