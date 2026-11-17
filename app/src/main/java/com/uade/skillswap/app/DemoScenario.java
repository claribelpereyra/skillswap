package com.uade.skillswap.app;

import com.uade.skillswap.app.presentation.ApplicationControllers;
import com.uade.skillswap.controllers.ControllerResult;
import com.uade.skillswap.controllers.profile.CreateProfileForm;
import com.uade.skillswap.controllers.publication.PublicationForm;
import java.time.Clock;
import java.time.Duration;
import java.util.*;

/** Escenario persistente e idempotente; crea datos por controllers, sin reemplazar reglas de negocio. */
final class DemoScenario {
    private DemoScenario() { }
    static void prepare(ApplicationControllers app, Clock clock) {
        require(app.session().signIn("user-mateo"));
        long category = ensureCatalogItem(app, "Tecnología", "CATEGORY", null);
        long skill = ensureCatalogItem(app, "Java", "SKILL", category);
        for (String user : List.of("user-mateo", "user-sofia")) {
            require(app.session().signIn(user));
            var profile = app.profiles().getPublicProfile(user,0,1);
            boolean created = profile.getErrorType() == ControllerResult.ErrorType.NOT_FOUND;
            if (created) {
                require(app.profiles().createProfile(user,new CreateProfileForm(user.equals("user-mateo") ? "Mateo Silva" : "Sofía Méndez",
                        "Perfil de demostración · Java y mentorías", "America/Argentina/Buenos_Aires", "PROFESSIONAL")));
                require(app.profiles().addSkill(user,skill));
                require(app.profiles().addInterest(user,"java"));
                require(app.notificationPreferences().changeChannel(user.equals("user-mateo") ? "EMAIL" : "PUSH"));
            } else require(profile);
            var wallet = app.walletQuery().getMyWallet();
            if (wallet.getErrorType() == ControllerResult.ErrorType.NOT_FOUND) require(app.wallet().loadCredits(user,500L));
            else require(wallet);
            var first = require(app.publications().listMine(user,null,0,1));
            if (first.totalItems() == 0) {
                var start = clock.instant().plus(Duration.ofDays(7));
                var draft = require(app.publications().createDraft(user,new PublicationForm("MENTORING",
                        "Mentoría Java · " + (user.equals("user-mateo") ? "Mateo" : "Sofía"),
                        "Práctica de Java y diseño de software con ejemplos.",category,"REMOTE",60,1,100L,Set.of("java","diseño"),
                        List.of(new PublicationForm.SlotForm(start,start.plusSeconds(3600)),
                                new PublicationForm.SlotForm(start.plusSeconds(7200),start.plusSeconds(10800))))));
                require(app.publications().changeStatus(user,draft.id(),"PUBLISH",null));
            }
        }
        app.session().signOut();
    }
    // El catálogo se carga por el caso de uso, igual que el resto de la demo: las migraciones son solo esquema.
    private static long ensureCatalogItem(ApplicationControllers app, String name, String kind, Long parent) {
        var existing = require(app.catalog().list(0, 100)).items().stream()
                .filter(entry -> entry.kind().equals(kind) && entry.name().equals(name)).findFirst();
        if (existing.isPresent()) return existing.get().id();
        String user = app.session().currentUserId();
        return require(parent == null ? app.catalog().addCategory(user, null, name)
                : app.catalog().addSkill(user, parent, name)).id();
    }

    private static <T> T require(ControllerResult<T> result) {
        if (!result.isSuccess()) throw new IllegalStateException("Demo setup failed: " + result.getMessage());
        return result.getData();
    }
}
