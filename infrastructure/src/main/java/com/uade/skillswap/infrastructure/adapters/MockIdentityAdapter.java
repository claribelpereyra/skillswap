package com.uade.skillswap.infrastructure.adapters;

import com.uade.skillswap.application.ports.out.IdentityService;
import com.uade.skillswap.domain.common.UserId;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Simulated identity adapter for local execution and end-to-end tests without external login.
 */
public class MockIdentityAdapter implements IdentityService {

    private final AtomicReference<UserId> activeUser = new AtomicReference<>();

    public MockIdentityAdapter() {
    }

    public MockIdentityAdapter(UserId initialUser) {
        this.activeUser.set(initialUser);
    }

    public void signIn(UserId userId) {
        this.activeUser.set(Objects.requireNonNull(userId, "userId must not be null"));
    }

    public void signOut() {
        this.activeUser.set(null);
    }

    public boolean isAuthenticated() {
        return activeUser.get() != null;
    }

    @Override
    public UserId currentUserId() {
        return activeUser.get();
    }
}
