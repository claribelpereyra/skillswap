package com.uade.skillswap.controllers.session;

import com.uade.skillswap.controllers.ControllerResult;

/** Login simulado local; no autentica credenciales ni reemplaza un proveedor real. */
public interface SessionController {
    ControllerResult<String> signIn(String userId);
    void signOut();
    String currentUserId();
}
