package com.uade.skillswap.controllers.notification;

import com.uade.skillswap.application.common.Page;
import com.uade.skillswap.application.dto.notification.NotificationView;
import com.uade.skillswap.controllers.ControllerResult;
import java.util.UUID;

/** La UI depende de este contrato; el destinatario siempre sale de la identidad activa. */
public interface NotificationController {
    ControllerResult<Page<NotificationView>> listar(int page, int size);
    ControllerResult<Boolean> marcarLeida(UUID notificationId);
}
