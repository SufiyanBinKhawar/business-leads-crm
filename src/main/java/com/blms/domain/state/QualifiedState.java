package com.blms.domain.state;

import com.blms.domain.PipelineStatus;

/** D5 Figure 1 - allowed: CONTACTED (UC11 sets it after dispatch), CLOSED_LOST. */
public class QualifiedState extends PipelineState {

    @Override
    public PipelineStatus getStatus() {
        return PipelineStatus.QUALIFIED;
    }

    @Override
    public boolean canMoveTo(PipelineStatus next) {
        return next == PipelineStatus.CONTACTED || next == PipelineStatus.CLOSED_LOST;
    }
}
