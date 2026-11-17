package com.uade.skillswap.app.presentation;

import com.uade.skillswap.controllers.booking.BookingController;
import com.uade.skillswap.controllers.booking.BookingReceiptController;
import com.uade.skillswap.controllers.notification.NotificationController;
import com.uade.skillswap.controllers.notification.NotificationPreferencesController;
import com.uade.skillswap.controllers.profile.ProfileController;
import com.uade.skillswap.controllers.publication.PublicationController;
import com.uade.skillswap.controllers.publication.RecommendationController;
import com.uade.skillswap.controllers.publication.SearchController;
import com.uade.skillswap.controllers.publication.CatalogController;
import com.uade.skillswap.controllers.rating.RatingController;
import com.uade.skillswap.controllers.session.SessionController;
import com.uade.skillswap.controllers.wallet.WalletController;
import com.uade.skillswap.controllers.wallet.WalletQueryController;

/** Contratos disponibles para el kit; ninguna pantalla necesita repositorios ni entidades JPA. */
public record ApplicationControllers(SessionController session, CatalogController catalog, ProfileController profiles,
        PublicationController publications, SearchController search, RecommendationController recommendations,
        BookingController bookings, BookingReceiptController receipts, WalletController wallet, WalletQueryController walletQuery,
        RatingController ratings, NotificationController notifications,
        NotificationPreferencesController notificationPreferences) {
}
