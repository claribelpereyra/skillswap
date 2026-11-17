package com.uade.skillswap.application.usecases.rating;

import com.uade.skillswap.application.common.ResourceNotFoundException;
import com.uade.skillswap.application.dto.rating.RatingView;
import com.uade.skillswap.application.dto.rating.RegisterRatingCommand;
import com.uade.skillswap.application.ports.in.rating.RegisterRatingUseCase;
import com.uade.skillswap.application.ports.out.BookingRepository;
import com.uade.skillswap.application.ports.out.UnitOfWork;
import com.uade.skillswap.application.ports.out.profile.ProfileRepository;
import com.uade.skillswap.application.ports.out.rating.RatingRepository;
import com.uade.skillswap.domain.booking.Booking;
import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.profile.Profile;
import com.uade.skillswap.domain.rating.InvalidRatingException;
import com.uade.skillswap.domain.rating.Rating;

import java.util.Objects;

public class RegisterRatingService implements RegisterRatingUseCase {

    private final RatingRepository ratingRepository;
    private final ProfileRepository profileRepository;
    private final BookingRepository bookings;
    private final UnitOfWork unitOfWork;

    public RegisterRatingService(RatingRepository ratingRepository,
                                 ProfileRepository profileRepository,
                                 BookingRepository bookings,
                                 UnitOfWork unitOfWork) {
        this.ratingRepository = Objects.requireNonNull(ratingRepository, "ratingRepository must not be null");
        this.profileRepository = Objects.requireNonNull(profileRepository, "profileRepository must not be null");
        this.bookings = Objects.requireNonNull(bookings, "bookings must not be null");
        this.unitOfWork = Objects.requireNonNull(unitOfWork, "unitOfWork must not be null");
    }

    @Override
    public RatingView register(RegisterRatingCommand command) {
        if (command == null) {
            throw new InvalidRatingException("The rating command cannot be null");
        }
        UserId authorId = command.authorId();
        if (authorId == null) {
            throw new InvalidRatingException("The authorId cannot be null");
        }

        return unitOfWork.execute(() -> {
            Booking booking = bookings.findById(command.bookingId())
                    .orElseThrow(() -> new ResourceNotFoundException("Booking", command.bookingId()));
            UserId counterparty = booking.ratingRecipientFor(authorId);

            if (command.recipientId() == null) {
                throw new InvalidRatingException("The rating recipient cannot be null");
            }
            if (!counterparty.equals(command.recipientId())) {
                throw new InvalidRatingException(
                        "The recipient does not match the counterparty of booking " + command.bookingId());
            }
            if (ratingRepository.existsByBookingIdAndAuthorId(command.bookingId(), authorId)) {
                throw new InvalidRatingException(
                        "Booking " + command.bookingId() + " has already been rated by user " + authorId.value());
            }

            Profile recipientProfile = profileRepository.findByUserId(counterparty)
                    .orElseThrow(() -> new ResourceNotFoundException("Profile", counterparty.value()));

            Rating rating = Rating.create(
                    command.bookingId(),
                    authorId,
                    counterparty,
                    command.score(),
                    command.comment()
            );
            Rating saved = ratingRepository.save(rating);
            recipientProfile.registerRating(saved);
            profileRepository.save(recipientProfile);
            return RatingView.from(saved);
        });
    }
}
