package com.uade.skillswap.app;

import ar.edu.uade.skillswap.ui.SkillSwapView;
import com.uade.skillswap.app.presentation.RealUiControllers;
import org.junit.jupiter.api.Test;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.time.*;
import java.util.*;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class SkillSwapViewIntegrationTest {
    @Test void allProvidedScreensRenderWithTheRealControllers() throws Exception {
        System.setProperty("java.awt.headless","true");
        try(var database=new TestDatabase();var runtime=CompositionRoot.open(database.configuration(),Clock.systemUTC(),Map.of("SKILLSWAP_DEMO","true"))) {
            runtime.controllers().session().signIn("user-mateo");
            SwingUtilities.invokeAndWait(()->{
                var view=new SkillSwapView(new RealUiControllers(runtime.controllers(),"user-mateo",ZoneOffset.UTC),"user-mateo");
                view.setSize(1365,820);
                for(String screen:List.of("Inicio","Explorar","Mis publicaciones","Contrataciones","SkillCoins","Notificaciones","Mi perfil")) {
                    buttons(view).stream().filter(b->b.getText().endsWith(screen)).findFirst().orElseThrow().doClick();
                    layout(view);
                    var image=new BufferedImage(1365,820,BufferedImage.TYPE_INT_ARGB);
                    var graphics=image.createGraphics();view.printAll(graphics);graphics.dispose();
                    assertThat(labels(view)).anySatisfy(label->assertThat(label.getText()).isEqualTo(screen));
                }
            });
        }
    }
    private static List<JButton> buttons(Container parent) {
        List<JButton> result=new ArrayList<>();
        for(Component child:parent.getComponents()) {
            if(child instanceof JButton button)result.add(button);
            if(child instanceof Container container)result.addAll(buttons(container));
        }
        return result;
    }
    private static void layout(Container parent) {
        parent.doLayout();for(Component child:parent.getComponents())if(child instanceof Container container)layout(container);
    }
    private static List<JLabel> labels(Container parent) {
        List<JLabel> result=new ArrayList<>();
        for(Component child:parent.getComponents()) {
            if(child instanceof JLabel label)result.add(label);
            if(child instanceof Container container)result.addAll(labels(container));
        }
        return result;
    }
}
