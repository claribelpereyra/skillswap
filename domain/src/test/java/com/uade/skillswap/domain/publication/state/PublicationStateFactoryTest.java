package com.uade.skillswap.domain.publication.state;

import com.uade.skillswap.domain.publication.PublicationStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PublicationStateFactoryTest {

    @Test
    void everyStatusIsRebuiltIntoItsState() {
        for (PublicationStatus status : PublicationStatus.values()) {
            assertThat(PublicationStateFactory.fromStatus(status).getStatus()).isEqualTo(status);
        }
    }
}
