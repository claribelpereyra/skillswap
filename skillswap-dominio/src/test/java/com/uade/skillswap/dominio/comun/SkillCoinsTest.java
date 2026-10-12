package com.uade.skillswap.dominio.comun;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SkillCoinsTest {

    @Test
    void sumaYRestaMontos() {
        SkillCoins saldo = new SkillCoins(100);

        assertThat(saldo.sumar(new SkillCoins(50))).isEqualTo(new SkillCoins(150));
        assertThat(saldo.restar(new SkillCoins(30))).isEqualTo(new SkillCoins(70));
    }

    @Test
    void noPermiteMontosNegativos() {
        assertThatThrownBy(() -> new SkillCoins(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void noPermiteRestarMasQueElSaldo() {
        SkillCoins saldo = new SkillCoins(10);

        assertThatThrownBy(() -> saldo.restar(new SkillCoins(11)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Saldo insuficiente");
        assertThat(saldo.alcanzaPara(new SkillCoins(11))).isFalse();
    }
}
