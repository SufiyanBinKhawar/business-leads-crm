package com.blms.domain.state;

import com.blms.domain.PipelineStatus;

/** D5 Figure 1 - terminal state. */
public class ClosedLostState extends PipelineState {

    @Override
    public PipelineStatus getStatus() {
        return PipelineStatus.CLOSED_LOST;
    }

    @Override
    public boolean canMoveTo(PipelineStatus next) {
        return false;
    }
}
