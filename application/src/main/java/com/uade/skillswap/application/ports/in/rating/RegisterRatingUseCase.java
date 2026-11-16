package com.uade.skillswap.application.ports.in.rating;

import com.uade.skillswap.application.dto.rating.RatingView;
import com.uade.skillswap.application.dto.rating.RegisterRatingCommand;

public interface RegisterRatingUseCase {

    RatingView register(RegisterRatingCommand command);
}
