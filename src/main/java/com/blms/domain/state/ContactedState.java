package com.blms.domain.state;

import com.blms.domain.PipelineStatus;

/** D5 Figure 1 - allowed: CLOSED_WON, CLOSED_LOST. (D3 UC08: CLOSED_WON is only reachable from here.) */
public class ContactedState extends PipelineState {

    @Override
    public PipelineStatus getStatus() {
        return PipelineStatus.CONTACTED;
    }

    @Override
    public boolean canMoveTo(PipelineStatus next) {
        return next == PipelineStatus.CLOSED_WON || next == PipelineStatus.CLOSED_LOST;
    }
}
