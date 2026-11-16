package com.uade.skillswap.infrastructure.adapters;

import com.uade.skillswap.domain.common.UserId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MockIdentityAdapterTest {

    @Test
    void tracksSignInAndSignOutTransitionsCleanly() {
        MockIdentityAdapter adapter = new MockIdentityAdapter();
        assertThat(adapter.isAuthenticated()).isFalse();
        assertThat(adapter.currentUserId()).isNull();

        UserId ana = new UserId("ana");
        adapter.signIn(ana);
        assertThat(adapter.isAuthenticated()).isTrue();
        assertThat(adapter.currentUserId()).isEqualTo(ana);

        adapter.signOut();
        assertThat(adapter.isAuthenticated()).isFalse();
        assertThat(adapter.currentUserId()).isNull();

        MockIdentityAdapter preSignedIn = new MockIdentityAdapter(ana);
        assertThat(preSignedIn.isAuthenticated()).isTrue();
        assertThat(preSignedIn.currentUserId()).isEqualTo(ana);

        assertThatThrownBy(() -> adapter.signIn(null)).isInstanceOf(NullPointerException.class);
    }
}
