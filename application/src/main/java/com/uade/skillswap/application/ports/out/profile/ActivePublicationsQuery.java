package com.uade.skillswap.application.ports.out.profile;

import com.uade.skillswap.application.common.Page;
import com.uade.skillswap.application.common.PageRequest;
import com.uade.skillswap.application.dto.publication.PublicationView;
import com.uade.skillswap.domain.common.UserId;

public interface ActivePublicationsQuery {

    Page<PublicationView> findActiveByUserId(UserId userId, PageRequest page);
}
