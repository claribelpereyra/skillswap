package com.uade.skillswap.application.usecases.wallet;

import com.uade.skillswap.application.common.ResourceNotFoundException;
import com.uade.skillswap.application.dto.wallet.WalletView;
import com.uade.skillswap.application.ports.in.wallet.LoadWalletCommand;
import com.uade.skillswap.application.ports.out.UnitOfWork;
import com.uade.skillswap.application.ports.out.wallet.SkillCoinGateway;
import com.uade.skillswap.application.ports.out.wallet.WalletRepository;
import com.uade.skillswap.application.usecases.wallet.LoadWalletService;
import com.uade.skillswap.domain.common.BusinessRuleException;
import com.uade.skillswap.domain.common.SkillCoins;
import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.wallet.Wallet;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LoadWalletServiceTest {

    private static final UserId USER = new UserId("ana");
    private static final SkillCoins AMOUNT = new SkillCoins(50);
    private static final Instant NOW = Instant.parse("2026-11-15T10:00:00Z");

    private final WalletRepository wallets = mock(WalletRepository.class);
    private final SkillCoinGateway gateway = mock(SkillCoinGateway.class);
    private final UnitOfWork unitOfWork = Supplier::get;
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final LoadWalletService service = new LoadWalletService(wallets, gateway, unitOfWork, clock);

    @Test
    void zeroCreditLoadsAreRejected() {
        assertThatThrownBy(() -> new LoadWalletCommand(USER, SkillCoins.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be positive");
    }

    @Test
    void approvedSimulatedPaymentAddsCreditsToTheWallet() {
        Wallet wallet = Wallet.open(USER);
        when(wallets.findByOwnerId(USER)).thenReturn(Optional.of(wallet));
        when(gateway.authorizeCredit(USER, AMOUNT)).thenReturn(true);

        WalletView result = service.loadCredits(new LoadWalletCommand(USER, AMOUNT));

        assertThat(result.balance()).isEqualTo(AMOUNT.amount());
        assertThat(result.movements()).singleElement().satisfies(movement -> {
            assertThat(movement.type()).isEqualTo("TOP_UP");
            assertThat(movement.occurredAt()).isEqualTo(NOW);
        });
        verify(wallets).save(wallet);
    }

    @Test
    void declinedSimulatedPaymentDoesNotChangeTheWallet() {
        Wallet wallet = Wallet.open(USER);
        when(wallets.findByOwnerId(USER)).thenReturn(Optional.of(wallet));
        when(gateway.authorizeCredit(USER, AMOUNT)).thenReturn(false);

        assertThatThrownBy(() -> service.loadCredits(new LoadWalletCommand(USER, AMOUNT)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("payment was declined");

        assertThat(wallet.balance()).isEqualTo(SkillCoins.ZERO);
        assertThat(wallet.movements()).isEmpty();
        verify(wallets, never()).save(wallet);
    }

    @Test
    void aMissingWalletFailsBeforeCallingTheGateway() {
        when(wallets.findByOwnerId(USER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadCredits(new LoadWalletCommand(USER, AMOUNT)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Wallet not found");

        verify(gateway, never()).authorizeCredit(USER, AMOUNT);
    }

    @Test
    void gatewayAuthorizationHappensOutsideTheUnitOfWork() {
        Wallet wallet = Wallet.open(USER);
        AtomicBoolean inUnitOfWork = new AtomicBoolean();
        UnitOfWork trackingUnitOfWork = new UnitOfWork() {
            @Override
            public <T> T execute(Supplier<T> operation) {
                inUnitOfWork.set(true);
                try {
                    return operation.get();
                } finally {
                    inUnitOfWork.set(false);
                }
            }
        };
        LoadWalletService serviceWithTrackingUnitOfWork =
                new LoadWalletService(wallets, (userId, amount) -> {
                    assertThat(inUnitOfWork.get()).isFalse();
                    return true;
                }, trackingUnitOfWork, clock);
        when(wallets.findByOwnerId(USER)).thenReturn(Optional.of(wallet));

        serviceWithTrackingUnitOfWork.loadCredits(new LoadWalletCommand(USER, AMOUNT));

        assertThat(inUnitOfWork.get()).isFalse();
        verify(wallets).save(wallet);
    }
}
