package com.uade.skillswap.app;

import ar.edu.uade.skillswap.ui.SkillSwapView;
import ar.edu.uade.skillswap.ui.Theme;
import com.uade.skillswap.app.presentation.RealUiControllers;
import com.uade.skillswap.infrastructure.persistence.PersistenceConfiguration;
import javax.swing.*;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.time.*;
import java.util.*;

public final class Main {
    private Main() { }
    public static void main(String[] args) throws Exception {
        var environment = new HashMap<>(System.getenv());
        String user = "user-mateo", screenshot = null;
        for (String argument : args) {
            if (argument.equals("--demo")) environment.put("SKILLSWAP_DEMO","true");
            else if (argument.startsWith("--user=")) user = argument.substring(7);
            else if (argument.startsWith("--screenshot=")) screenshot = argument.substring(13);
            else throw new IllegalArgumentException("Unknown argument: " + argument);
        }
        if (screenshot != null) System.setProperty("java.awt.headless","true");
        var runtime = CompositionRoot.open(PersistenceConfiguration.fromEnvironment(), Clock.systemUTC(), environment);
        Runtime.getRuntime().addShutdownHook(new Thread(runtime::close));
        var signed = runtime.controllers().session().signIn(user);
        if (!signed.isSuccess()) { runtime.close(); throw new IllegalArgumentException(signed.getMessage()); }
        ZoneId zone = ZoneId.of(environment.getOrDefault("SKILLSWAP_UI_ZONE", "America/Argentina/Buenos_Aires"));
        final String initialUser = user, imagePath = screenshot;
        if (imagePath != null) {
            try {
                SwingUtilities.invokeAndWait(() -> {
                    Theme.install();
                    var view = new SkillSwapView(new RealUiControllers(runtime.controllers(),initialUser,zone),initialUser);
                    view.setSize(1365,820); layoutTree(view);
                    var image = new BufferedImage(1365,820,BufferedImage.TYPE_INT_ARGB);
                    var graphics = image.createGraphics();view.printAll(graphics);graphics.dispose();
                    try { ImageIO.write(image,"png",Path.of(imagePath).toFile()); }
                    catch (java.io.IOException error) { throw new IllegalStateException(error); }
                });
            } finally { runtime.close(); }
            return;
        }
        SwingUtilities.invokeLater(() -> {
            Theme.install();
            var frame = new JFrame("SkillSwap · Controllers reales");
            frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            frame.addWindowListener(new WindowAdapter() { @Override public void windowClosed(WindowEvent e) { runtime.close(); } });
            frame.setMinimumSize(new Dimension(1120,700));frame.setSize(1365,820);frame.setLocationRelativeTo(null);
            var container = new JPanel(new BorderLayout());
            var users = new JComboBox<>(new String[]{initialUser, initialUser.equals("user-sofia") ? "user-mateo" : "user-sofia"});
            var login = new JPanel(new FlowLayout(FlowLayout.LEFT));login.add(new JLabel("Sesión simulada:"));login.add(users);
            container.add(login,BorderLayout.NORTH);
            final String[] active = {initialUser}; final boolean[] changing = {false};
            users.addActionListener(event -> {
                if(changing[0]) return;
                String selected = (String)users.getSelectedItem();
                try {
                    runtime.controllers().session().signIn(selected);
                    var view = new SkillSwapView(new RealUiControllers(runtime.controllers(),selected,zone),selected);
                    if(container.getComponentCount()>1) container.remove(1);
                    container.add(view,BorderLayout.CENTER);active[0]=selected;container.revalidate();container.repaint();
                } catch (RuntimeException error) {
                    runtime.controllers().session().signIn(active[0]);changing[0]=true;users.setSelectedItem(active[0]);changing[0]=false;
                    JOptionPane.showMessageDialog(frame,error.getMessage(),"No se pudo abrir la sesión",JOptionPane.WARNING_MESSAGE);
                }
            });
            try {
                container.add(new SkillSwapView(new RealUiControllers(runtime.controllers(),initialUser,zone),initialUser),BorderLayout.CENTER);
                frame.setContentPane(container);frame.setVisible(true);
            } catch (RuntimeException error) {
                runtime.close();frame.dispose();JOptionPane.showMessageDialog(null,error.getMessage(),"No se pudo iniciar SkillSwap",JOptionPane.ERROR_MESSAGE);
            }
        });
    }
    private static void layoutTree(Container container) {
        container.doLayout();
        for(Component component : container.getComponents()) if(component instanceof Container child) layoutTree(child);
    }
}
