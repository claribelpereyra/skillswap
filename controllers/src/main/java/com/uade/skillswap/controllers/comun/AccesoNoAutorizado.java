package com.uade.skillswap.controllers.comun;

public final class AccesoNoAutorizado extends RuntimeException {
    public AccesoNoAutorizado() { super("Se requiere un usuario activo"); }
}
