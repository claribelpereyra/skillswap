package com.uade.skillswap.app.presentation;

import org.junit.jupiter.api.Test;
import javax.swing.SwingUtilities;
import java.util.concurrent.atomic.AtomicReference;
import static org.assertj.core.api.Assertions.*;

class SwingCallsTest {
    @Test void controllerWorkLeavesTheEventDispatchThreadAndPreservesBusinessErrors() throws Exception {
        System.setProperty("java.awt.headless","true");
        var workerThread=new AtomicReference<Boolean>();
        var message=new AtomicReference<String>();
        SwingUtilities.invokeAndWait(()->{
            workerThread.set(SwingCalls.call(()->SwingUtilities.isEventDispatchThread()));
            try { SwingCalls.call(()->{throw new UiOperationException("Saldo insuficiente");}); }
            catch(UiOperationException error) { message.set(error.getMessage()); }
        });
        assertThat(workerThread.get()).isFalse();
        assertThat(message.get()).isEqualTo("Saldo insuficiente");
    }
}
