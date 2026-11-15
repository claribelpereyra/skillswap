package com.uade.skillswap.domain.publication.state;

import com.uade.skillswap.domain.publication.PublicationStatus;

public class ClosedState extends PublicationStateBase {

    @Override
    public PublicationStatus getStatus() {
        return PublicationStatus.CLOSED;
    }
}
