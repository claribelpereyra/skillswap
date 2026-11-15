package com.uade.skillswap.app;

import com.uade.skillswap.infrastructure.persistence.EntityManagerContext;
import com.uade.skillswap.infrastructure.persistence.PersistenceConfiguration;

public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        PersistenceConfiguration persistence = PersistenceConfiguration.fromEnvironment();
        persistence.migrate();

        EntityManagerContext.init(persistence.createEntityManagerFactory());
        Runtime.getRuntime().addShutdownHook(new Thread(EntityManagerContext::close));

        // TODO: crear repositorios y adaptadores (infraestructura), casos de uso (aplicación)
        // y controllers, y pasárselos al Mock UI Kit en lugar del MockSkillSwapController.
        System.out.println("SkillSwap: database migrated and JPA connection ready.");
    }
}
