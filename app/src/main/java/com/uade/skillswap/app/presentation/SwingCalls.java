package com.uade.skillswap.app.presentation;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

/** Ejecuta los controllers fuera del EDT manteniendo el contrato síncrono del kit. */
final class SwingCalls {
    private SwingCalls() { }
    static <T> T call(Supplier<T> operation) {
        if (!SwingUtilities.isEventDispatchThread()) return operation.get();
        var owner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
        var waiting = GraphicsEnvironment.isHeadless() ? null : new JDialog(owner, "SkillSwap", Dialog.ModalityType.APPLICATION_MODAL);
        var progress = new JProgressBar();
        progress.setIndeterminate(true);
        if (waiting != null) {
        waiting.add(new JLabel(" Procesando operación… "), BorderLayout.NORTH);
        waiting.add(progress);
        waiting.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        waiting.pack();
        waiting.setLocationRelativeTo(owner);
        }
        var worker = new SwingWorker<T, Void>() {
            @Override protected T doInBackground() { return operation.get(); }
            @Override protected void done() { if (waiting != null) waiting.dispose(); }
        };
        worker.execute();
        if (waiting != null) waiting.setVisible(true);
        try {
            return worker.get();
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new UiOperationException("Operación interrumpida");
        } catch (ExecutionException error) {
            if (error.getCause() instanceof UiOperationException known) throw known;
            java.util.logging.Logger.getLogger(SwingCalls.class.getName()).log(
                    java.util.logging.Level.WARNING, "UI operation failed", error.getCause());
            throw new UiOperationException("No se pudo completar la operación");
        }
    }
}
