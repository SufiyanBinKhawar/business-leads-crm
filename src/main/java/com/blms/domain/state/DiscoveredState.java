package com.blms.domain.state;

import com.blms.domain.PipelineStatus;

/** D5 Figure 1 - initial state of every new lead (SD03 step 9). Allowed: QUALIFIED, CLOSED_LOST. */
public class DiscoveredState extends PipelineState {

    @Override
    public PipelineStatus getStatus() {
        return PipelineStatus.DISCOVERED;
    }

    @Override
    public boolean canMoveTo(PipelineStatus next) {
        return next == PipelineStatus.QUALIFIED || next == PipelineStatus.CLOSED_LOST;
    }
}
