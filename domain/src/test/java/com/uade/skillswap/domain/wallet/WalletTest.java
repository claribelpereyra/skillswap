package com.uade.skillswap.domain.wallet;

import com.uade.skillswap.domain.common.BusinessRuleException;
import com.uade.skillswap.domain.common.InsufficientBalanceException;
import com.uade.skillswap.domain.common.SkillCoins;
import com.uade.skillswap.domain.common.UserId;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WalletTest {

    private static final UserId OWNER = new UserId("ana");
    private static final Instant NOW = Instant.parse("2026-11-14T10:00:00Z");
    private static final long BOOKING_ID = 42L;

    @Test
    void aNewWalletStartsEmpty() {
        Wallet wallet = Wallet.open(OWNER);

        assertThat(wallet.ownerId()).isEqualTo(OWNER);
        assertThat(wallet.balance()).isEqualTo(SkillCoins.ZERO);
        assertThat(wallet.movements()).isEmpty();
    }

    @Test
    void topUpIncreasesBalanceAndRecordsAnUnassociatedMovement() {
        Wallet wallet = Wallet.open(OWNER);

        wallet.topUp(new SkillCoins(100), NOW);

        assertThat(wallet.balance()).isEqualTo(new SkillCoins(100));
        assertThat(wallet.movements()).containsExactly(
                new CreditMovement(CreditMovementType.TOP_UP, new SkillCoins(100), NOW, null));
    }

    @Test
    void debitReducesBalanceAndIsLinkedToItsBooking() {
        Wallet wallet = Wallet.open(OWNER);
        Wallet provider = Wallet.open(new UserId("provider"));
        wallet.topUp(new SkillCoins(100), NOW);
        Instant debitedAt = NOW.plusSeconds(60);

        wallet.payBookingTo(provider, new SkillCoins(35), BOOKING_ID, debitedAt);

        assertThat(wallet.balance()).isEqualTo(new SkillCoins(65));
        assertThat(wallet.movements()).last().isEqualTo(
                new CreditMovement(CreditMovementType.DEBIT, new SkillCoins(35), debitedAt, BOOKING_ID));
        assertThat(provider.movements()).last().isEqualTo(
                new CreditMovement(CreditMovementType.CREDIT, new SkillCoins(35), debitedAt, BOOKING_ID));
    }

    @Test
    void creditsAndRefundsIncreaseBalanceAndRecordTheirBooking() {
        Wallet client = Wallet.open(OWNER);
        Wallet provider = Wallet.open(new UserId("provider"));
        client.topUp(new SkillCoins(100), NOW);
        client.payBookingTo(provider, new SkillCoins(40), BOOKING_ID, NOW.plusSeconds(60));
        provider.refundBookingTo(client, new SkillCoins(15), BOOKING_ID, NOW.plusSeconds(120));

        assertThat(client.balance()).isEqualTo(new SkillCoins(75));
        assertThat(provider.balance()).isEqualTo(new SkillCoins(25));
        assertThat(client.movements()).extracting(CreditMovement::type)
                .containsExactly(CreditMovementType.TOP_UP, CreditMovementType.DEBIT, CreditMovementType.REFUND);
        assertThat(provider.movements()).extracting(CreditMovement::type)
                .containsExactly(CreditMovementType.CREDIT, CreditMovementType.DEBIT);
        assertThat(client.movements()).extracting(CreditMovement::bookingId)
                .containsExactly(null, BOOKING_ID, BOOKING_ID);
    }

    @Test
    void restoreRebuildsBalanceFromMovements() {
        List<CreditMovement> movements = List.of(
                new CreditMovement(CreditMovementType.TOP_UP, new SkillCoins(20), NOW, null),
                new CreditMovement(CreditMovementType.DEBIT, new SkillCoins(5), NOW.plusSeconds(60), BOOKING_ID),
                new CreditMovement(CreditMovementType.CREDIT, new SkillCoins(10), NOW.plusSeconds(120), BOOKING_ID));

        Wallet restored = Wallet.restore(OWNER, movements);

        assertThat(restored.ownerId()).isEqualTo(OWNER);
        assertThat(restored.balance()).isEqualTo(new SkillCoins(25));
        assertThat(restored.movements()).containsExactlyElementsOf(movements);
    }

    @Test
    void anInsufficientDebitLeavesTheBalanceAndLedgerUnchanged() {
        Wallet wallet = Wallet.open(OWNER);
        Wallet provider = Wallet.open(new UserId("provider"));
        wallet.topUp(new SkillCoins(20), NOW);
        int movementCount = wallet.movements().size();

        assertThatThrownBy(() -> wallet.payBookingTo(provider, new SkillCoins(21), BOOKING_ID, NOW.plusSeconds(60)))
                .isExactlyInstanceOf(InsufficientBalanceException.class)
                .hasMessage("Insufficient balance: 20 < 21");

        assertThat(wallet.balance()).isEqualTo(new SkillCoins(20));
        assertThat(wallet.movements()).hasSize(movementCount);
        assertThat(provider.balance()).isEqualTo(SkillCoins.ZERO);
        assertThat(provider.movements()).isEmpty();
    }

    @Test
    void noPermitePagarLaMismaContratacionDosVeces() {
        Wallet client = Wallet.open(OWNER);
        Wallet provider = Wallet.open(new UserId("provider"));
        client.topUp(new SkillCoins(100), NOW);
        client.payBookingTo(provider, new SkillCoins(35), BOOKING_ID, NOW.plusSeconds(60));

        assertThatThrownBy(() ->
                client.payBookingTo(provider, new SkillCoins(35), BOOKING_ID, NOW.plusSeconds(120)))
                .isExactlyInstanceOf(BusinessRuleException.class)
                .hasMessage("A payment already exists for booking " + BOOKING_ID);

        assertThat(client.balance()).isEqualTo(new SkillCoins(65));
        assertThat(provider.balance()).isEqualTo(new SkillCoins(35));
        assertThat(client.movements()).hasSize(2);
        assertThat(provider.movements()).hasSize(1);
    }

    @Test
    void noPermiteReintegrarLaMismaContratacionDosVeces() {
        Wallet client = Wallet.open(OWNER);
        Wallet provider = Wallet.open(new UserId("provider"));
        client.topUp(new SkillCoins(100), NOW);
        client.payBookingTo(provider, new SkillCoins(35), BOOKING_ID, NOW.plusSeconds(60));
        provider.refundBookingTo(client, new SkillCoins(15), BOOKING_ID, NOW.plusSeconds(120));

        assertThatThrownBy(() ->
                provider.refundBookingTo(client, new SkillCoins(15), BOOKING_ID, NOW.plusSeconds(180)))
                .isExactlyInstanceOf(BusinessRuleException.class)
                .hasMessage("A refund already exists for booking " + BOOKING_ID);

        assertThat(client.balance()).isEqualTo(new SkillCoins(80));
        assertThat(provider.balance()).isEqualTo(new SkillCoins(20));
        assertThat(client.movements()).hasSize(3);
        assertThat(provider.movements()).hasSize(2);
    }

    @Test
    void saldoInsuficienteDelProveedorNoModificaLaWalletDelCliente() {
        Wallet client = Wallet.open(OWNER);
        Wallet provider = Wallet.open(new UserId("provider"));
        Wallet thirdParty = Wallet.open(new UserId("carla"));
        client.topUp(new SkillCoins(100), NOW);
        client.payBookingTo(provider, new SkillCoins(35), BOOKING_ID, NOW.plusSeconds(60));
        provider.payBookingTo(thirdParty, new SkillCoins(35), BOOKING_ID + 1, NOW.plusSeconds(120));

        assertThatThrownBy(() ->
                provider.refundBookingTo(client, new SkillCoins(35), BOOKING_ID, NOW.plusSeconds(180)))
                .isExactlyInstanceOf(InsufficientBalanceException.class)
                .hasMessage("Insufficient balance: 0 < 35");

        assertThat(client.balance()).isEqualTo(new SkillCoins(65));
        assertThat(provider.balance()).isEqualTo(SkillCoins.ZERO);
        assertThat(client.movements()).hasSize(2);
        assertThat(provider.movements()).hasSize(2);
    }

    @Test
    void overflowDelSaldoDelProveedorNoDebitaAlCliente() {
        Wallet client = Wallet.open(OWNER);
        Wallet provider = Wallet.open(new UserId("provider"));
        client.topUp(new SkillCoins(100), NOW);
        provider.topUp(new SkillCoins(Long.MAX_VALUE), NOW);

        assertThatThrownBy(() ->
                client.payBookingTo(provider, new SkillCoins(1), BOOKING_ID, NOW.plusSeconds(60)))
                .isExactlyInstanceOf(ArithmeticException.class);

        assertThat(client.balance()).isEqualTo(new SkillCoins(100));
        assertThat(provider.balance()).isEqualTo(new SkillCoins(Long.MAX_VALUE));
        assertThat(client.movements()).hasSize(1);
        assertThat(provider.movements()).hasSize(1);
    }

    @Test
    void reintegroCeroNoGeneraMovimientos() {
        Wallet client = Wallet.open(OWNER);
        Wallet provider = Wallet.open(new UserId("provider"));
        client.topUp(new SkillCoins(100), NOW);
        client.payBookingTo(provider, new SkillCoins(35), BOOKING_ID, NOW.plusSeconds(60));
        int clientMovementCount = client.movements().size();
        int providerMovementCount = provider.movements().size();

        provider.refundBookingTo(client, SkillCoins.ZERO, BOOKING_ID, NOW.plusSeconds(120));

        assertThat(client.balance()).isEqualTo(new SkillCoins(65));
        assertThat(provider.balance()).isEqualTo(new SkillCoins(35));
        assertThat(client.movements()).hasSize(clientMovementCount);
        assertThat(provider.movements()).hasSize(providerMovementCount);
    }

    @Test
    void zeroAmountMovementsAreRejectedWithoutChangingTheWallet() {
        Wallet wallet = Wallet.open(OWNER);

        assertThatThrownBy(() -> wallet.topUp(SkillCoins.ZERO, NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A credit movement amount must be positive");

        assertThat(wallet.balance()).isEqualTo(SkillCoins.ZERO);
        assertThat(wallet.movements()).isEmpty();
    }

    @Test
    void movementsCannotBeBackdated() {
        Wallet wallet = Wallet.open(OWNER);
        Wallet provider = Wallet.open(new UserId("provider"));
        wallet.topUp(new SkillCoins(20), NOW);

        assertThatThrownBy(() -> wallet.payBookingTo(provider, new SkillCoins(10), BOOKING_ID, NOW.minusSeconds(1)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("A movement cannot be dated before the previous movement");

        assertThat(wallet.balance()).isEqualTo(new SkillCoins(20));
        assertThat(wallet.movements()).hasSize(1);
        assertThat(provider.balance()).isEqualTo(SkillCoins.ZERO);
        assertThat(provider.movements()).isEmpty();
    }
}
