package com.uade.skillswap.application.ports.in.rating;

import com.uade.skillswap.application.common.Page;
import com.uade.skillswap.application.common.PageRequest;
import com.uade.skillswap.application.dto.rating.RatingView;
import com.uade.skillswap.domain.common.UserId;

public interface ListRatingsUseCase {

    Page<RatingView> listReceivedRatings(UserId recipientId, PageRequest pageRequest);
}
