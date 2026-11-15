package com.uade.skillswap.application.usecases.wallet;

import com.uade.skillswap.application.common.AccessDeniedException;
import com.uade.skillswap.application.common.ResourceNotFoundException;
import com.uade.skillswap.application.dto.wallet.GetWalletCommand;
import com.uade.skillswap.application.dto.wallet.WalletView;
import com.uade.skillswap.application.ports.out.wallet.WalletRepository;
import com.uade.skillswap.domain.common.SkillCoins;
import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.wallet.Wallet;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GetWalletServiceTest {

    private static final UserId ANA = new UserId("ana");
    private static final UserId BRUNO = new UserId("bruno");

    private final WalletRepository wallets = mock(WalletRepository.class);
    private final GetWalletService service = new GetWalletService(wallets);

    @Test
    void queryingOwnWalletReturnsSimpleBalanceAndMovementDtos() {
        Wallet wallet = Wallet.open(ANA);
        wallet.topUp(new SkillCoins(75), Instant.parse("2026-11-14T10:00:00Z"));
        when(wallets.findByOwnerId(ANA)).thenReturn(Optional.of(wallet));

        WalletView view = service.getWallet(new GetWalletCommand(ANA, ANA));

        assertThat(view.ownerId()).isEqualTo("ana");
        assertThat(view.balance()).isEqualTo(75L);
        assertThat(view.movements()).singleElement().satisfies(movement -> {
            assertThat(movement.type()).isEqualTo("TOP_UP");
            assertThat(movement.amount()).isEqualTo(75L);
            assertThat(movement.bookingId()).isNull();
        });
    }

    @Test
    void aUserCannotQueryAnotherUsersWallet() {
        assertThatThrownBy(() -> service.getWallet(new GetWalletCommand(ANA, BRUNO)))
                .isInstanceOf(AccessDeniedException.class);

        verify(wallets, never()).findByOwnerId(BRUNO);
    }

    @Test
    void queryingAMissingWalletFailsExplicitly() {
        when(wallets.findByOwnerId(ANA)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getWallet(new GetWalletCommand(ANA, ANA)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Wallet not found");
    }
}
