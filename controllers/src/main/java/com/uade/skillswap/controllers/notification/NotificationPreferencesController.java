package com.uade.skillswap.controllers.notification;

import com.uade.skillswap.controllers.ControllerResult;

public interface NotificationPreferencesController {
    ControllerResult<String> getChannel();
    ControllerResult<String> changeChannel(String channel);
}
