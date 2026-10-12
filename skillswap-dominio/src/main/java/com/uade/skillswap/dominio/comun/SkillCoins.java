package com.uade.skillswap.dominio.comun;

/**
 * Value object de ejemplo: un monto de SkillCoins nunca puede ser negativo.
 * Sirve de referencia para el estilo del dominio (inmutable, con invariantes propias).
 */
public record SkillCoins(long cantidad) {

    public static final SkillCoins CERO = new SkillCoins(0);

    public SkillCoins {
        if (cantidad < 0) {
            throw new IllegalArgumentException("Un monto de SkillCoins no puede ser negativo: " + cantidad);
        }
    }

    public SkillCoins sumar(SkillCoins otro) {
        return new SkillCoins(Math.addExact(cantidad, otro.cantidad));
    }

    public SkillCoins restar(SkillCoins otro) {
        if (otro.cantidad > cantidad) {
            throw new IllegalArgumentException("Saldo insuficiente: " + cantidad + " < " + otro.cantidad);
        }
        return new SkillCoins(cantidad - otro.cantidad);
    }

    public boolean alcanzaPara(SkillCoins monto) {
        return cantidad >= monto.cantidad;
    }
}
