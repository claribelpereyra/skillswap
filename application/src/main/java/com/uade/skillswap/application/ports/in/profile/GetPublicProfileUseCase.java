package com.uade.skillswap.application.ports.in.profile;

import com.uade.skillswap.application.common.PageRequest;
import com.uade.skillswap.application.dto.profile.PublicProfileView;
import com.uade.skillswap.domain.common.UserId;

public interface GetPublicProfileUseCase {

    PublicProfileView getPublicProfile(UserId targetUserId, PageRequest publicationsPage, PageRequest ratingsPage);

    default PublicProfileView getPublicProfile(UserId targetUserId, PageRequest ratingsPage) {
        return getPublicProfile(targetUserId, PageRequest.first(), ratingsPage);
    }

    default PublicProfileView getPublicProfile(UserId targetUserId) {
        return getPublicProfile(targetUserId, PageRequest.first(), PageRequest.first());
    }
}
