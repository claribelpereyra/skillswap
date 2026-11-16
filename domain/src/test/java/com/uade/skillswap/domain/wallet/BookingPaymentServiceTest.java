package com.uade.skillswap.domain.wallet;

import com.uade.skillswap.domain.common.BusinessRuleException;
import com.uade.skillswap.domain.common.InsufficientBalanceException;
import com.uade.skillswap.domain.common.SkillCoins;
import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.pricing.FixedPromotion;
import com.uade.skillswap.domain.pricing.PercentagePromotion;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BookingPaymentServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-14T10:00:00Z");
    private static final long BOOKING_ID = 42L;
    private static final RefundStrategy REFUND_STRATEGY =
            new TieredRefundStrategy(new RefundPolicy(Duration.ofDays(2), Duration.ofDays(1)));
    private static final BookingPaymentService SERVICE = new BookingPaymentService(REFUND_STRATEGY);

    @Test
    void paymentDebitsClientAndCreditsProviderOnce() {
        Wallet client = wallet("client", 100);
        Wallet provider = wallet("provider", 20);
        Instant paidAt = NOW.plusSeconds(60);

        SERVICE.pay(BOOKING_ID, client, provider, new SkillCoins(35), paidAt);

        assertThat(client.balance()).isEqualTo(new SkillCoins(65));
        assertThat(provider.balance()).isEqualTo(new SkillCoins(55));
        assertThat(client.movements()).last().isEqualTo(
                new CreditMovement(CreditMovementType.DEBIT, new SkillCoins(35), paidAt, BOOKING_ID));
        assertThat(provider.movements()).last().isEqualTo(
                new CreditMovement(CreditMovementType.CREDIT, new SkillCoins(35), paidAt, BOOKING_ID));

        assertThatThrownBy(() -> SERVICE.pay(BOOKING_ID, client, provider, new SkillCoins(35), paidAt))
                .isExactlyInstanceOf(BusinessRuleException.class)
                .hasMessage("A payment already exists for booking " + BOOKING_ID);

        assertThat(client.balance()).isEqualTo(new SkillCoins(65));
        assertThat(provider.balance()).isEqualTo(new SkillCoins(55));
        assertThat(client.movements()).hasSize(2);
        assertThat(provider.movements()).hasSize(2);
    }

    @Test
    void paymentAppliesAccumulatedPromotionsToTheChargedAmount() {
        BookingPaymentService service = new BookingPaymentService(
                REFUND_STRATEGY,
                price -> new FixedPromotion(new PercentagePromotion(price, 10), new SkillCoins(5)));
        Wallet client = wallet("client", 100);
        Wallet provider = wallet("provider", 0);

        service.pay(BOOKING_ID, client, provider, new SkillCoins(100), NOW.plusSeconds(60));

        assertThat(client.balance()).isEqualTo(new SkillCoins(15));
        assertThat(provider.balance()).isEqualTo(new SkillCoins(85));
        assertThat(client.movements()).last().extracting(CreditMovement::amount)
                .isEqualTo(new SkillCoins(85));
        assertThat(provider.movements()).last().extracting(CreditMovement::amount)
                .isEqualTo(new SkillCoins(85));
    }

    @Test
    void aPromotionThatMakesThePaymentFreeIsRejectedWithoutChangingWallets() {
        BookingPaymentService service = new BookingPaymentService(
                REFUND_STRATEGY,
                price -> new PercentagePromotion(price, 100));
        Wallet client = wallet("client", 100);
        Wallet provider = wallet("provider", 0);

        assertThatThrownBy(() ->
                service.pay(BOOKING_ID, client, provider, new SkillCoins(100), NOW.plusSeconds(60)))
                .isExactlyInstanceOf(BusinessRuleException.class)
                .hasMessage("The booking payment must remain positive after promotions");

        assertThat(client.balance()).isEqualTo(new SkillCoins(100));
        assertThat(provider.balance()).isEqualTo(SkillCoins.ZERO);
        assertThat(client.movements()).hasSize(1);
        assertThat(provider.movements()).isEmpty();
    }

    @Test
    void failedPaymentDoesNotChangeEitherWallet() {
        Wallet client = wallet("client", 10);
        Wallet provider = wallet("provider", 20);
        int clientMovements = client.movements().size();
        int providerMovements = provider.movements().size();

        assertThatThrownBy(() -> SERVICE.pay(BOOKING_ID, client, provider, new SkillCoins(11), NOW.plusSeconds(60)))
                .isExactlyInstanceOf(InsufficientBalanceException.class)
                .hasMessage("Insufficient balance: 10 < 11");

        assertThat(client.balance()).isEqualTo(new SkillCoins(10));
        assertThat(provider.balance()).isEqualTo(new SkillCoins(20));
        assertThat(client.movements()).hasSize(clientMovements);
        assertThat(provider.movements()).hasSize(providerMovements);
    }

    @Test
    void providerBalanceOverflowDoesNotDebitClient() {
        Wallet client = wallet("client", 100);
        Wallet provider = Wallet.open(new UserId("provider"));
        provider.topUp(new SkillCoins(Long.MAX_VALUE), NOW);
        int clientMovements = client.movements().size();

        assertThatThrownBy(() -> SERVICE.pay(BOOKING_ID, client, provider, new SkillCoins(1), NOW.plusSeconds(60)))
                .isInstanceOf(ArithmeticException.class);

        assertThat(client.balance()).isEqualTo(new SkillCoins(100));
        assertThat(provider.balance()).isEqualTo(new SkillCoins(Long.MAX_VALUE));
        assertThat(client.movements()).hasSize(clientMovements);
        assertThat(provider.movements()).hasSize(1);
    }

    @Test
    void refundReturnsCalculatedAmountAndCannotBeAppliedTwice() {
        Wallet client = wallet("client", 100);
        Wallet provider = wallet("provider", 0);
        SERVICE.pay(BOOKING_ID, client, provider, new SkillCoins(35), NOW.plusSeconds(60));

        SkillCoins refunded = SERVICE.refund(
                BOOKING_ID, client, provider, NOW.plus(Duration.ofHours(36)), NOW.plusSeconds(120));

        assertThat(refunded).isEqualTo(new SkillCoins(17));
        assertThat(client.balance()).isEqualTo(new SkillCoins(82));
        assertThat(provider.balance()).isEqualTo(new SkillCoins(18));
        assertThat(client.movements()).last().extracting(CreditMovement::type)
                .isEqualTo(CreditMovementType.REFUND);
        assertThat(provider.movements()).last().extracting(CreditMovement::type)
                .isEqualTo(CreditMovementType.DEBIT);

        assertThatThrownBy(() -> SERVICE.refund(
                BOOKING_ID, client, provider, NOW.plus(Duration.ofHours(36)), NOW.plusSeconds(180)))
                .isExactlyInstanceOf(BusinessRuleException.class)
                .hasMessage("A refund already exists for booking " + BOOKING_ID);
    }

    @Test
    void refundPolicyUsesConfigurableThresholdsAndRoundsHalfDown() {
        TieredRefundStrategy strategy = new TieredRefundStrategy(
                new RefundPolicy(Duration.ofHours(48), Duration.ofHours(24)));
        SkillCoins amount = new SkillCoins(35);
        Instant startsAt = NOW.plus(Duration.ofHours(48));

        assertThat(strategy.refundAmount(amount, startsAt, NOW)).isEqualTo(amount);
        assertThat(strategy.refundAmount(amount, startsAt, NOW.plus(Duration.ofHours(24))))
                .isEqualTo(new SkillCoins(17));
        assertThat(strategy.refundAmount(amount, startsAt, NOW.plus(Duration.ofHours(25))))
                .isEqualTo(SkillCoins.ZERO);
    }

    @Test
    void differentRefundStrategiesCanPriceTheSameCancellationDifferently() {
        SkillCoins paidAmount = new SkillCoins(35);
        Instant bookingStartsAt = NOW.plus(Duration.ofHours(12));
        Instant cancelledAt = NOW;
        RefundStrategy tiered = new TieredRefundStrategy(
                new RefundPolicy(Duration.ofDays(2), Duration.ofDays(1)));
        RefundStrategy full = new FullRefundStrategy();

        assertThat(tiered.refundAmount(paidAmount, bookingStartsAt, cancelledAt)).isEqualTo(SkillCoins.ZERO);
        assertThat(full.refundAmount(paidAmount, bookingStartsAt, cancelledAt)).isEqualTo(paidAmount);
    }

    @Test
    void aZeroRefundDoesNotCreateZeroMovements() {
        Wallet client = wallet("client", 100);
        Wallet provider = wallet("provider", 0);
        SERVICE.pay(BOOKING_ID, client, provider, new SkillCoins(35), NOW.plusSeconds(60));
        int clientMovements = client.movements().size();
        int providerMovements = provider.movements().size();

        SkillCoins refunded = SERVICE.refund(
                BOOKING_ID, client, provider, NOW.plus(Duration.ofHours(12)), NOW.plusSeconds(120));

        assertThat(refunded).isEqualTo(SkillCoins.ZERO);
        assertThat(client.balance()).isEqualTo(new SkillCoins(65));
        assertThat(provider.balance()).isEqualTo(new SkillCoins(35));
        assertThat(client.movements()).hasSize(clientMovements);
        assertThat(provider.movements()).hasSize(providerMovements);
    }

    @Test
    void insufficientProviderBalanceDoesNotPartiallyRefundClient() {
        Wallet client = wallet("client", 100);
        Wallet provider = wallet("provider", 0);
        SERVICE.pay(BOOKING_ID, client, provider, new SkillCoins(35), NOW.plusSeconds(60));
        Wallet thirdParty = wallet("third-party", 0);
        provider.payBookingTo(thirdParty, new SkillCoins(35), BOOKING_ID + 1, NOW.plusSeconds(120));

        assertThatThrownBy(() -> SERVICE.refund(
                BOOKING_ID,
                client,
                provider,
                NOW.plus(Duration.ofDays(3)),
                NOW.plusSeconds(180)))
                .isExactlyInstanceOf(InsufficientBalanceException.class)
                .hasMessage("Insufficient balance: 0 < 35");

        assertThat(client.balance()).isEqualTo(new SkillCoins(65));
        assertThat(provider.balance()).isEqualTo(SkillCoins.ZERO);
        assertThat(client.movements()).hasSize(2);
        assertThat(provider.movements()).hasSize(2);
    }

    private static Wallet wallet(String owner, long initialBalance) {
        Wallet wallet = Wallet.open(new UserId(owner));
        if (initialBalance > 0) {
            wallet.topUp(new SkillCoins(initialBalance), NOW);
        }
        return wallet;
    }
}
