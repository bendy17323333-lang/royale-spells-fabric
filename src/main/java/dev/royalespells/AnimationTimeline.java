package dev.royalespells;

/** Integrates a local presentation clock. It consumes the caller's time, so a
 * clock already held by electrical stun stays held. Resuming never catches up
 * all the skipped frames. No entity, renderer, or shared bone is mutated here. */
public final class AnimationTimeline {
    private boolean initialized;
    private double previous,local;
    public double sample(double input,double rate) {
        if(!initialized){initialized=true;previous=local=input;}
        double elapsed=input-previous;
        // Negative input can occur while an entity/model is reinitialized.
        if(elapsed>=0)local+=elapsed*Math.clamp(rate,0,1);
        previous=input;return local;
    }
}
