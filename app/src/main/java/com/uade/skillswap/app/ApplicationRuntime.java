package com.uade.skillswap.app;

import com.uade.skillswap.app.presentation.ApplicationControllers;
import jakarta.persistence.EntityManagerFactory;

/** Dueño de los recursos de una ejecución de la aplicación. */
public record ApplicationRuntime(ApplicationControllers controllers, EntityManagerFactory persistence)
        implements AutoCloseable {
    @Override
    public void close() {
        controllers.session().signOut();
        if (persistence.isOpen()) persistence.close();
    }
}
