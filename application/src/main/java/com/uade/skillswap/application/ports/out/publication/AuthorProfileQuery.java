package com.uade.skillswap.application.ports.out.publication;

import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.profile.AccountType;

import java.util.Optional;

// Puerto de salida hacia perfiles (lo implementa el área de Andy): qué tipo de cuenta tiene el autor.
// Vacío si el usuario no tiene perfil activo.
public interface AuthorProfileQuery {

    Optional<AccountType> accountTypeOf(UserId userId);
}
