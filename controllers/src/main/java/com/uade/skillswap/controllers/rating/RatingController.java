package com.uade.skillswap.controllers.rating;

import com.uade.skillswap.application.common.Page;
import com.uade.skillswap.application.dto.rating.RatingView;
import com.uade.skillswap.controllers.ControllerResult;

public interface RatingController {

    ControllerResult<RatingView> registerRating(String userId, RatingForm form);

    ControllerResult<Page<RatingView>> listReceivedRatings(String recipientId, int page, int size);
}
