package com.uade.skillswap.app;

import com.uade.skillswap.infraestructura.persistencia.ConfiguracionPersistencia;
import jakarta.persistence.EntityManagerFactory;

/**
 * Punto de entrada y "composition root": el único lugar donde se instancian
 * las implementaciones concretas y se conectan entre sí.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        ConfiguracionPersistencia persistencia = ConfiguracionPersistencia.desdeEntorno();
        persistencia.migrar();

        EntityManagerFactory emf = persistencia.crearEntityManagerFactory();
        Runtime.getRuntime().addShutdownHook(new Thread(emf::close));

        // TODO: crear repositorios y adaptadores (infraestructura), casos de uso (aplicación)
        // y controllers, y pasárselos al Mock UI Kit en lugar del MockSkillSwapController.
        System.out.println("SkillSwap: base migrada y conexión JPA lista.");
    }
}
