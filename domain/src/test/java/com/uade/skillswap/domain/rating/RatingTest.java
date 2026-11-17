package com.uade.skillswap.domain.rating;

import com.uade.skillswap.domain.common.UserId;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RatingTest {
    private static final Instant AT = Instant.parse("2026-11-16T10:00:00Z");

    private static final UserId AUTHOR = new UserId("ana");
    private static final UserId RECIPIENT = new UserId("bruno");
    private static final Instant RATED_AT = Instant.parse("2026-11-10T15:30:00Z");

    @Test
    void aValidRatingPreservesFieldsAndTrimsComment() {
        Rating rating = Rating.create(12L, AUTHOR, RECIPIENT, 5, "  Great session!  ", RATED_AT);

        assertThat(rating.getId()).isNull();
        assertThat(rating.getBookingId()).isEqualTo(12L);
        assertThat(rating.getAuthorId()).isEqualTo(AUTHOR);
        assertThat(rating.getRecipientId()).isEqualTo(RECIPIENT);
        assertThat(rating.getScore()).isEqualTo(5);
        assertThat(rating.getComment()).isEqualTo("Great session!");
        assertThat(rating.getRatedAt()).isEqualTo(RATED_AT);
    }

    @Test
    void nullCommentDefaultsToEmptyAndDistinguishesDifferentCasedUserIds() {
        Rating rating = Rating.create(1L, AUTHOR, new UserId("ANA"), 1, null, AT);

        assertThat(rating.getComment()).isEmpty();
        assertThat(rating.getRatedAt()).isNotNull();
        assertThat(rating.getRecipientId()).isEqualTo(new UserId("ANA"));
    }

    @Test
    void invalidRatingsThrowInvalidRatingException() {
        assertThatThrownBy(() -> Rating.create(0L, AUTHOR, RECIPIENT, 5, "ok", AT))
                .isExactlyInstanceOf(InvalidRatingException.class)
                .hasMessage("The rating must belong to a valid booking");
        assertThatThrownBy(() -> Rating.create(-4L, AUTHOR, RECIPIENT, 5, "ok", AT))
                .isExactlyInstanceOf(InvalidRatingException.class)
                .hasMessage("The rating must belong to a valid booking");
        assertThatThrownBy(() -> Rating.create(1L, null, RECIPIENT, 5, "ok", AT))
                .isExactlyInstanceOf(InvalidRatingException.class)
                .hasMessage("The rating author cannot be null");
        assertThatThrownBy(() -> Rating.create(1L, AUTHOR, null, 5, "ok", AT))
                .isExactlyInstanceOf(InvalidRatingException.class)
                .hasMessage("The rating recipient cannot be null");
        assertThatThrownBy(() -> Rating.create(1L, AUTHOR, new UserId("ana"), 5, "ok", AT))
                .isExactlyInstanceOf(InvalidRatingException.class)
                .hasMessage("A user cannot rate themselves");
        assertThatThrownBy(() -> Rating.create(1L, AUTHOR, RECIPIENT, 0, "ok", AT))
                .isExactlyInstanceOf(InvalidRatingException.class)
                .hasMessage("The rating score must be between 1 and 5 (received: 0)");
        assertThatThrownBy(() -> Rating.create(1L, AUTHOR, RECIPIENT, 6, "ok", AT))
                .isExactlyInstanceOf(InvalidRatingException.class)
                .hasMessage("The rating score must be between 1 and 5 (received: 6)");
    }

    @Test
    void rebuildingARatingRestoresIdAndValidatesInvariants() {
        Rating rebuilt = Rating.rebuild(99L, 7L, AUTHOR, RECIPIENT, 4, "Good", RATED_AT);

        assertThat(rebuilt.getId()).isEqualTo(99L);
        assertThat(rebuilt.getBookingId()).isEqualTo(7L);
        assertThat(rebuilt.getScore()).isEqualTo(4);
        assertThat(rebuilt.getRatedAt()).isEqualTo(RATED_AT);

        assertThatThrownBy(() -> Rating.rebuild(null, 7L, AUTHOR, RECIPIENT, 4, "Good", RATED_AT))
                .isExactlyInstanceOf(NullPointerException.class)
                .hasMessage("A stored rating must have an id");
    }

    @Test
    void ratingEqualityMatchesBookingAuthorAndRecipient() {
        Rating r1 = Rating.create(7L, AUTHOR, RECIPIENT, 5, "Great", RATED_AT);
        Rating r2 = Rating.create(7L, AUTHOR, RECIPIENT, 3, "Ok", RATED_AT);
        Rating diffBooking = Rating.create(8L, AUTHOR, RECIPIENT, 5, "Great", RATED_AT);
        Rating diffAuthor = Rating.create(7L, new UserId("carla"), RECIPIENT, 5, "Great", RATED_AT);
        Rating diffRecipient = Rating.create(7L, AUTHOR, new UserId("carla"), 5, "Great", RATED_AT);

        assertThat(r1.getAuthorId()).isEqualTo(AUTHOR);
        assertThat(r1.getRecipientId()).isEqualTo(RECIPIENT);
        assertThat(r1).isEqualTo(r1);
        assertThat(r1).isEqualTo(r2);
        assertThat(r1.hashCode()).isEqualTo(r2.hashCode());
        assertThat(r1).isNotEqualTo(diffBooking);
        assertThat(r1).isNotEqualTo(diffAuthor);
        assertThat(r1).isNotEqualTo(diffRecipient);
        assertThat(r1.equals(null)).isFalse();
        assertThat(r1.equals("rating")).isFalse();
    }
}
