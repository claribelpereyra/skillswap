package com.uade.skillswap.application.ports.out.rating;

import com.uade.skillswap.application.common.Page;
import com.uade.skillswap.application.common.PageRequest;
import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.rating.Rating;

public interface RatingRepository {

    Rating save(Rating rating);

    boolean existsByBookingIdAndAuthorId(long bookingId, UserId authorId);

    Page<Rating> findByRecipientId(UserId recipientId, PageRequest pageRequest);
}
