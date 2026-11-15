package com.uade.skillswap.application.ports.out;

import com.uade.skillswap.domain.common.SkillCoins;
import com.uade.skillswap.domain.common.UserId;

import java.time.Instant;

// Lo que contratación necesita del área de wallet; lo implementa Tomi con las wallets de SkillCoins.
public interface BookingPayments {

    // Debita a la clienta y acredita al proveedor; tira InsufficientBalanceException si no alcanza.
    void pay(long bookingId, UserId client, UserId provider, SkillCoins price);

    // Devuelve lo que corresponda según la política de cancelación.
    void refund(long bookingId, UserId client, UserId provider, SkillCoins price, Instant slotStartsAt);
}
