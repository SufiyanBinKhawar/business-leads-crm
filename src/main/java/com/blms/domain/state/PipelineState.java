package com.blms.domain.state;

import com.blms.domain.PipelineStatus;
import com.blms.exception.InvalidStateTransitionException;

/**
 * D5 Figure 1 - abstract PipelineState (GoF State, GRASP Polymorphism). Lead "is in" exactly one state (+state 1).
 * Subclasses override canMoveTo(); there are no if/else chains on status anywhere else (SD08).
 */
public abstract class PipelineState {

    public abstract PipelineStatus getStatus();

    public abstract boolean canMoveTo(PipelineStatus next);

    /**
     * SD08 steps 13-16: ask canMoveTo(target); if allowed, create and return the next state object.
     * The switch below is object CREATION only (Creator) - transition rules live in the subclasses.
     */
    public PipelineState next(PipelineStatus target) {
        if (!canMoveTo(target)) {
            throw new InvalidStateTransitionException("Cannot move a lead from " + getStatus() + " to " + target);
        }
        return forStatus(target);
    }

    /** Rebuilds the state object for a status read from the database (used by Lead when loaded). */
    public static PipelineState forStatus(PipelineStatus status) {
        return switch (status) {
            case DISCOVERED -> new DiscoveredState();
            case QUALIFIED -> new QualifiedState();
            case CONTACTED -> new ContactedState();
            case CLOSED_WON -> new ClosedWonState();
            case CLOSED_LOST -> new ClosedLostState();
        };
    }
}
