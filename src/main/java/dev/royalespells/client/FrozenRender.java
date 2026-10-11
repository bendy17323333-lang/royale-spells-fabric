package dev.royalespells.client;

import dev.royalespells.VisualState;
import net.minecraft.world.entity.LivingEntity;
import java.util.*;

/** Per-entity poses. Renderers and their model bones are shared, so never freeze the model globally. */
public final class FrozenRender {
    private static final Map<LivingEntity,State> STATES=new WeakHashMap<>();
    private static final Deque<Scope> SCOPES=new ArrayDeque<>();
    private static final class State {
        Frame frame;final Map<Object,float[]> poses=new IdentityHashMap<>();int signature,parts;boolean wasFrozen;
    }
    private record Frame(int tick,int hurt,int death,float body,float oldBody,float head,float oldHead,float yaw,float oldYaw,float pitch,float oldPitch,float attack,float oldAttack,float delta){
        static Frame of(LivingEntity e,float delta){return new Frame(e.tickCount,e.hurtTime,e.deathTime,e.yBodyRot,e.yBodyRotO,e.yHeadRot,e.yHeadRotO,e.getYRot(),e.yRotO,e.getXRot(),e.xRotO,e.attackAnim,e.oAttackAnim,delta);}
        void apply(LivingEntity e){e.tickCount=tick;e.hurtTime=hurt;e.deathTime=death;e.yBodyRot=body;e.yBodyRotO=oldBody;e.yHeadRot=head;e.yHeadRotO=oldHead;e.setYRot(yaw);e.yRotO=oldYaw;e.setXRot(pitch);e.xRotO=oldPitch;e.attackAnim=attack;e.oAttackAnim=oldAttack;}
    }
    public static final class Scope implements AutoCloseable {
        private final LivingEntity entity;private final State state;private final Frame actual;private final boolean frozen;private int signature=1,parts;
        // Electric pause holds poses but leaves the real entity age available to
        // PauseClock. Hard ice retains its existing full-frame capture.
        private Scope(LivingEntity e,float delta){entity=e;state=STATES.computeIfAbsent(e,k->new State());actual=Frame.of(e,delta);frozen=VisualState.frozen(e)||dev.royalespells.pause.ElectricPause.active(e);if(!frozen||state.frame==null)state.frame=actual;if(VisualState.frozen(e))state.frame.apply(e);SCOPES.push(this);}
        public float delta(){return frozen?state.frame.delta:actual.delta;}
        public float yaw(float current){return frozen?state.frame.body:current;}
        @Override public void close(){SCOPES.pop();actual.apply(entity);state.signature=signature;state.parts=parts;state.wasFrozen=frozen;}
    }
    public static Scope begin(LivingEntity e,float delta){return new Scope(e,delta);}
    public static float[] pose(Object bone,float... current){
        if(SCOPES.isEmpty())return current;
        var scope=SCOPES.peek();var state=scope.state;
        if(!scope.frozen||!state.poses.containsKey(bone))state.poses.put(bone,current.clone());
        var result=scope.frozen?state.poses.get(bone):current;
        scope.signature=31*scope.signature+Arrays.hashCode(result);scope.parts++;return result;
    }
    public static int signature(LivingEntity e){var s=STATES.get(e);return s==null?0:s.signature;}
    public static int parts(LivingEntity e){var s=STATES.get(e);return s==null?0:s.parts;}
    public static boolean wasFrozen(LivingEntity e){var s=STATES.get(e);return s!=null&&s.wasFrozen;}
    private FrozenRender(){}
}
