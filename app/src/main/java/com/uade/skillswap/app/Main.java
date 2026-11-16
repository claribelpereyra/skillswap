package com.uade.skillswap.app;

import com.uade.skillswap.infrastructure.persistence.PersistenceConfiguration;
import java.time.Clock;

public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        var runtime = CompositionRoot.open(PersistenceConfiguration.fromEnvironment(), Clock.systemUTC(), System.getenv());
        Runtime.getRuntime().addShutdownHook(new Thread(runtime::close));
        System.out.println("SkillSwap: real controllers ready. Mock UI Kit has not been supplied; "
                + "connect its entry point to runtime.controllers().");
    }
}
