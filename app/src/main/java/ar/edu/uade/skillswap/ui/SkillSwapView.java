package ar.edu.uade.skillswap.ui;

import ar.edu.uade.skillswap.controllers.UiControllers;
import ar.edu.uade.skillswap.model.Models.*;

import javax.swing.*;
import java.time.LocalDateTime;
import com.uade.skillswap.app.presentation.UiOperationException;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static ar.edu.uade.skillswap.ui.Theme.*;

public final class SkillSwapView extends JPanel {
    private final String USER_ID;
    private JButton coins;
    private final UiControllers controllers;
    private final JPanel content=new JPanel(new BorderLayout());
    private final JLabel pageTitle=label("Inicio",H1,TEXT);
    private final JLabel badge=label("",new Font("SansSerif",Font.BOLD,10),Color.WHITE);

    public SkillSwapView(UiControllers controllers) { this(controllers, "user-mateo"); }

    public SkillSwapView(UiControllers controllers, String userId) {
        this.USER_ID=userId;
        this.controllers=controllers; setLayout(new BorderLayout()); setBackground(BG);
        add(sidebar(),BorderLayout.WEST); add(shell(),BorderLayout.CENTER); showHome();
    }

    private JComponent sidebar() {
        JPanel side=new JPanel(); side.setBackground(NAVY); side.setPreferredSize(new Dimension(228,720)); side.setLayout(new BoxLayout(side,BoxLayout.Y_AXIS)); side.setBorder(new EmptyBorder(24,16,18,16));
        JLabel logo=label("SkillSwap",new Font("SansSerif",Font.BOLD,25),Color.WHITE); logo.setAlignmentX(LEFT_ALIGNMENT); side.add(logo);
        JLabel sub=label("Aprendé. Compartí. Crecé.",SMALL,new Color(177,200,216)); sub.setAlignmentX(LEFT_ALIGNMENT); side.add(sub); side.add(Box.createVerticalStrut(28));
        side.add(nav("⌂  Inicio",this::showHome)); side.add(nav("⌕  Explorar",this::showExplore)); side.add(nav("▣  Mis publicaciones",this::showListings)); side.add(nav("✓  Contrataciones",this::showContracts)); side.add(nav("◈  SkillCoins",this::showWallet)); side.add(nav("●  Notificaciones",this::showNotifications)); side.add(nav("♙  Mi perfil",this::showProfile)); side.add(Box.createVerticalGlue());
        JPanel user=new JPanel(new BorderLayout(10,0)); user.setOpaque(false); user.setBorder(new EmptyBorder(8,6,4,4)); user.setMaximumSize(new Dimension(Integer.MAX_VALUE,56)); user.setAlignmentX(LEFT_ALIGNMENT);
        JLabel avatar=avatar(initials(controllers.perfiles().verPerfil(USER_ID).nombre()),36); user.add(avatar,BorderLayout.WEST); JPanel txt=new JPanel();txt.setOpaque(false);txt.setLayout(new BoxLayout(txt,BoxLayout.Y_AXIS));txt.add(label(controllers.perfiles().verPerfil(USER_ID).nombre(),H3,Color.WHITE));txt.add(label(controllers.perfiles().verPerfil(USER_ID).rol(),SMALL,new Color(177,200,216)));user.add(txt);side.add(user); return side;
    }

    private JButton nav(String text,Runnable action){JButton b=new JButton(text);b.setMaximumSize(new Dimension(Integer.MAX_VALUE,44));b.setHorizontalAlignment(SwingConstants.LEFT);b.setForeground(new Color(224,234,241));b.setBackground(NAVY);b.setBorder(new EmptyBorder(10,12,10,12));b.setFocusPainted(false);b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));b.addActionListener(e->safely(()->action.run()));return b;}

    private JComponent shell(){JPanel shell=new JPanel(new BorderLayout());shell.setBackground(BG);JPanel top=new JPanel(new BorderLayout());top.setBackground(Color.WHITE);top.setBorder(new EmptyBorder(16,26,16,26));top.add(pageTitle,BorderLayout.WEST);JPanel actions=new JPanel(new FlowLayout(FlowLayout.RIGHT,10,0));actions.setOpaque(false);badge.setOpaque(true);badge.setBackground(DANGER);badge.setBorder(new EmptyBorder(5,8,5,8));actions.add(badge);coins=button("◈  "+controllers.wallet().consultarSaldo(USER_ID)+" SkillCoins",false);coins.addActionListener(e->safely(()->showWallet()));actions.add(coins);top.add(actions,BorderLayout.EAST);shell.add(top,BorderLayout.NORTH);content.setBackground(BG);shell.add(content);return shell;}

    private void page(String title,JComponent view){pageTitle.setText(title);coins.setText("◈  "+controllers.wallet().consultarSaldo(USER_ID)+" SkillCoins");long unread=controllers.notificaciones().listarNotificaciones(USER_ID).stream().filter(n->!n.leida()).count();badge.setText(unread+" SIN LEER EN PÁGINA");content.removeAll();JScrollPane sp=new JScrollPane(view);sp.setBorder(null);sp.getVerticalScrollBar().setUnitIncrement(18);sp.getViewport().setBackground(BG);content.add(sp);content.revalidate();content.repaint();}

    private JPanel base(){JPanel p=new JPanel();p.setBackground(BG);p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));p.setBorder(new EmptyBorder(24,26,28,26));return p;}
    private void addSection(JPanel p,String title,String subtitle){JLabel h=label(title,H2,TEXT);h.setAlignmentX(LEFT_ALIGNMENT);p.add(h);if(subtitle!=null){JLabel s=label(subtitle,BODY,MUTED);s.setAlignmentX(LEFT_ALIGNMENT);p.add(Box.createVerticalStrut(3));p.add(s);}p.add(Box.createVerticalStrut(14));}

    private void showHome(){JPanel p=base();JPanel hero=new Theme.RoundedPanel(22,NAVY_2);hero.setLayout(new BorderLayout(22,0));hero.setBorder(new EmptyBorder(24,26,24,26));hero.setMaximumSize(new Dimension(Integer.MAX_VALUE,155));JPanel words=new JPanel();words.setOpaque(false);words.setLayout(new BoxLayout(words,BoxLayout.Y_AXIS));words.add(label("Hola, "+controllers.perfiles().verPerfil(USER_ID).nombre()+" 👋",new Font("SansSerif",Font.BOLD,25),Color.WHITE));words.add(Box.createVerticalStrut(7));words.add(label("¿Qué habilidad querés desbloquear hoy?",BODY,new Color(213,228,238)));words.add(Box.createVerticalStrut(16));JTextField search=new JTextField();search.putClientProperty("JTextField.placeholderText","Buscar mentorías, servicios o habilidades...");search.setBorder(new EmptyBorder(11,13,11,13));search.addActionListener(e->safely(()->showExplore(search.getText())));words.add(search);hero.add(words);JPanel stat=new JPanel();stat.setOpaque(false);stat.setLayout(new BoxLayout(stat,BoxLayout.Y_AXIS));stat.add(label("TU PROGRESO",SMALL,new Color(169,202,219)));stat.add(label(""+controllers.perfiles().verPerfil(USER_ID).proyectosCompletados(),new Font("SansSerif",Font.BOLD,34),Color.WHITE));stat.add(label("proyectos completados",SMALL,new Color(213,228,238)));hero.add(stat,BorderLayout.EAST);hero.setAlignmentX(LEFT_ALIGNMENT);p.add(hero);p.add(Box.createVerticalStrut(26));addSection(p,"Recomendado para vos","Selecciones basadas en tus habilidades e intereses");p.add(cardGrid(controllers.busqueda().recomendar(USER_ID)));p.add(Box.createVerticalStrut(26));addSection(p,"Explorá por categoría",null);JPanel cats=new JPanel(new FlowLayout(FlowLayout.LEFT,10,0));cats.setOpaque(false);for(String c:controllers.busqueda().categorias())if(!c.equals("Todas")){JButton b=button(c,false);b.addActionListener(e->safely(()->showExploreCategory(c)));cats.add(b);}cats.setAlignmentX(LEFT_ALIGNMENT);p.add(cats);page("Inicio",p);}

    private void showExplore(){showExplore("");}
    private void showExplore(String initial){controllers.cambiarPagina("buscar",0);showExploreWith(initial,"Todas");}
    private void showExploreCategory(String category){controllers.cambiarPagina("buscar",0);showExploreWith("",category);}
    private void showExploreWith(String initial,String category) {
        JPanel p=base(), filters=card(new FlowLayout(FlowLayout.LEFT,8,0));
        JTextField q=new JTextField(initial,18), maxPrice=new JTextField("0",5);
        JComboBox<String> cat=new JComboBox<>(controllers.busqueda().categorias().toArray(String[]::new));cat.setSelectedItem(category);
        JComboBox<String> mode=new JComboBox<>(new String[]{"Todas","REMOTA","PRESENCIAL"});
        JComboBox<String> order=new JComboBox<>(new String[]{"Relevancia","Mejor reputación","Menor precio","Más reciente"});
        JButton go=button("Buscar",true);filters.add(q);filters.add(cat);filters.add(mode);filters.add(new JLabel("Precio máx. (0=sin límite)"));filters.add(maxPrice);filters.add(order);filters.add(go);
        filters.setMaximumSize(new Dimension(Integer.MAX_VALUE,80));p.add(filters);p.add(Box.createVerticalStrut(20));
        JPanel results=new JPanel(),pages=new JPanel(new BorderLayout());results.setOpaque(false);pages.setOpaque(false);results.setLayout(new BoxLayout(results,BoxLayout.Y_AXIS));
        Runnable[] refresh=new Runnable[1];refresh[0]=()->{
            int price;try{price=Integer.parseInt(maxPrice.getText());}catch(NumberFormatException error){throw new UiOperationException("El precio debe ser un número entero");}
            if(price<0)throw new UiOperationException("El precio máximo no puede ser negativo");
            var selected=mode.getSelectedIndex()==0?null:Modalidad.valueOf((String)mode.getSelectedItem());
            var list=controllers.busqueda().buscar(new FiltrosBusqueda(q.getText(),(String)cat.getSelectedItem(),selected,price,(String)order.getSelectedItem()));
            results.removeAll();addSection(results,list.size()+" resultados en esta página","Mentorías y servicios disponibles");results.add(cardGrid(list));
            pages.removeAll();pages.add(pagination("buscar",refresh[0]));results.revalidate();pages.revalidate();results.repaint();pages.repaint();
        };
        go.addActionListener(e->safely(()->{controllers.cambiarPagina("buscar",0);refresh[0].run();}));
        q.addActionListener(e->safely(()->{controllers.cambiarPagina("buscar",0);refresh[0].run();}));
        p.add(results);p.add(pages);refresh[0].run();page("Explorar",p);
    }

    private JPanel cardGrid(List<Publicacion> list){JPanel grid=new JPanel(new GridLayout(0,3,14,14));grid.setOpaque(false);grid.setAlignmentX(LEFT_ALIGNMENT);for(Publicacion x:list)grid.add(listingCard(x));return grid;}
    private JComponent listingCard(Publicacion x){JPanel c=card(new BorderLayout(0,12));JPanel top=new JPanel(new BorderLayout());top.setOpaque(false);top.add(pill(x.categoria(),TEAL),BorderLayout.WEST);top.add(label("★ "+x.reputacion(),H3,GOLD),BorderLayout.EAST);c.add(top,BorderLayout.NORTH);JPanel mid=new JPanel();mid.setOpaque(false);mid.setLayout(new BoxLayout(mid,BoxLayout.Y_AXIS));JLabel t=label("<html><div style='width:210px'>"+x.titulo()+"</div></html>",H3,TEXT);mid.add(t);mid.add(Box.createVerticalStrut(7));mid.add(label("Por "+x.proveedor(),SMALL,MUTED));mid.add(Box.createVerticalStrut(9));mid.add(label("<html><div style='width:215px;color:#677480'>"+x.descripcion()+"</div></html>",BODY,MUTED));mid.add(Box.createVerticalStrut(9));mid.add(label(x.modalidad()+"  ·  "+x.duracionMinutos()+" min",SMALL,MUTED));c.add(mid);JPanel bottom=new JPanel(new BorderLayout());bottom.setOpaque(false);bottom.add(label(x.precio()+" ◈",new Font("SansSerif",Font.BOLD,20),NAVY),BorderLayout.WEST);JButton detail=button("Ver detalle",true);detail.addActionListener(e->safely(()->showDetail(x)));bottom.add(detail,BorderLayout.EAST);c.add(bottom,BorderLayout.SOUTH);return c;}

    private void showDetail(Publicacion x) {
        JPanel p=base(); JButton back=button("← Volver a explorar",false); back.addActionListener(e->safely(()->showExplore())); p.add(back);
        p.add(Box.createVerticalStrut(18)); JPanel card=card(new BorderLayout(24,12));
        JPanel info=new JPanel(); info.setOpaque(false);info.setLayout(new BoxLayout(info,BoxLayout.Y_AXIS));
        info.add(label(x.titulo(),H1,TEXT)); info.add(label(x.proveedor()+" · "+x.modalidad()+" · "+x.duracionMinutos()+" min",H3,MUTED));
        info.add(label("<html><div style='width:600px'>"+x.descripcion()+"</div></html>",BODY,TEXT)); card.add(info);
        JButton request=button("Solicitar · "+x.precio()+" SkillCoins",true);
        request.addActionListener(e->safely(()->{
            var slots=controllers.publicaciones().franjas(USER_ID,x.id()).stream().filter(f->!f.ocupada()).toArray(Franja[]::new);
            if(slots.length==0) throw new UiOperationException("No hay franjas disponibles");
            JComboBox<Franja> slot=new JComboBox<>(slots); JTextField message=new JTextField(); JTextField objective=new JTextField();
            JPanel form=new JPanel(new GridLayout(0,1,4,5));form.add(new JLabel("Franja"));form.add(slot);
            form.add(new JLabel("Mensaje"));form.add(message);form.add(new JLabel("Objetivo"));form.add(objective);
            if(JOptionPane.showConfirmDialog(this,form,"Solicitar",JOptionPane.OK_CANCEL_OPTION)==JOptionPane.OK_OPTION){
                controllers.contrataciones().solicitar(USER_ID,x.id(),((Franja)slot.getSelectedItem()).id(),message.getText(),objective.getText());
                message("Solicitud enviada","Podés seguirla desde Contrataciones.");showContracts();
            }
        }));card.add(request,BorderLayout.SOUTH);card.setMaximumSize(new Dimension(Integer.MAX_VALUE,320));p.add(card);page("Detalle",p);
    }

    private void showListings() {
        JPanel p=base(),title=new JPanel(new BorderLayout());title.setOpaque(false);title.add(label("Gestioná tus publicaciones",H2,TEXT));
        JButton create=button("+ Nueva publicación",true);create.addActionListener(e->safely(this::createListing));title.add(create,BorderLayout.EAST);title.setMaximumSize(new Dimension(Integer.MAX_VALUE,45));p.add(title);
        var own=controllers.publicaciones().listarPropias(USER_ID);
        if(own.isEmpty())p.add(empty("Todavía no publicaste servicios","Creá tu primera propuesta para compartir tu talento."));
        for(Publicacion x:own){
            JPanel row=card(new BorderLayout(8,8));row.setMaximumSize(new Dimension(Integer.MAX_VALUE,130));row.add(label(x.titulo(),H3,TEXT),BorderLayout.NORTH);row.add(pill(x.estado().name(),TEAL));
            JPanel actions=new JPanel(new FlowLayout(FlowLayout.RIGHT));actions.setOpaque(false);
            if(x.estado()==EstadoPublicacion.BORRADOR||x.estado()==EstadoPublicacion.PAUSADA||x.estado()==EstadoPublicacion.PUBLICADA){
                JButton toggle=button(x.estado()==EstadoPublicacion.PUBLICADA?"Pausar":"Publicar",true);toggle.addActionListener(e->safely(()->{controllers.publicaciones().cambiarEstado(USER_ID,x.id(),x.estado()==EstadoPublicacion.PUBLICADA?EstadoPublicacion.PAUSADA:EstadoPublicacion.PUBLICADA);showListings();}));actions.add(toggle);
                JButton cancel=button("Cancelar",false);cancel.addActionListener(e->safely(()->{String reason=JOptionPane.showInputDialog(this,"Motivo de cancelación");if(reason!=null){controllers.publicaciones().cambiarEstado(USER_ID,x.id(),EstadoPublicacion.CANCELADA,reason);showListings();}}));actions.add(cancel);
                if(x.estado()!=EstadoPublicacion.BORRADOR){JButton close=button("Cerrar",false);close.addActionListener(e->safely(()->{controllers.publicaciones().cambiarEstado(USER_ID,x.id(),EstadoPublicacion.CERRADA);showListings();}));actions.add(close);}
            }
            row.add(actions,BorderLayout.SOUTH);p.add(row);p.add(Box.createVerticalStrut(10));
        }
        p.add(pagination("publicaciones",this::showListings));page("Mis publicaciones",p);
    }

    private void createListing() {
        JTextField title=new JTextField(), description=new JTextField(), price=new JTextField("150"), duration=new JTextField("60");
        JTextField start=new JTextField(LocalDateTime.now().plusDays(7).withSecond(0).withNano(0).toString());
        JTextField end=new JTextField(LocalDateTime.now().plusDays(7).plusHours(1).withSecond(0).withNano(0).toString());
        JComboBox<String> category=new JComboBox<>(controllers.busqueda().categorias().stream().filter(c->!c.equals("Todas")).toArray(String[]::new));
        JComboBox<String> type=new JComboBox<>(new String[]{"MENTORING","SERVICE","CHALLENGE"});
        JComboBox<Modalidad> modality=new JComboBox<>(new Modalidad[]{Modalidad.REMOTA,Modalidad.PRESENCIAL});
        JPanel form=new JPanel(new GridLayout(0,2,5,5));
        form.add(new JLabel("Título"));form.add(title);form.add(new JLabel("Descripción"));form.add(description);
        form.add(new JLabel("Categoría"));form.add(category);form.add(new JLabel("Tipo"));form.add(type);
        form.add(new JLabel("Modalidad"));form.add(modality);form.add(new JLabel("Duración (minutos)"));form.add(duration);
        form.add(new JLabel("Precio (SkillCoins)"));form.add(price);form.add(new JLabel("Inicio (AAAA-MM-DDTHH:MM)"));form.add(start);
        form.add(new JLabel("Fin (AAAA-MM-DDTHH:MM)"));form.add(end);
        if(JOptionPane.showConfirmDialog(this,form,"Nueva publicación",JOptionPane.OK_CANCEL_OPTION)==JOptionPane.OK_OPTION){
            try {
                controllers.publicaciones().crearBorrador(USER_ID,title.getText(),description.getText(),(String)category.getSelectedItem(),
                    (Modalidad)modality.getSelectedItem(),Integer.parseInt(duration.getText()),Integer.parseInt(price.getText()),
                    (String)type.getSelectedItem(),List.of(new Franja(null,LocalDateTime.parse(start.getText()),LocalDateTime.parse(end.getText()),false)));
            }catch(NumberFormatException|java.time.format.DateTimeParseException ex){throw new UiOperationException("Revisá números y fechas del formulario");}
            showListings();
        }
    }

    private void showContracts() {
        JPanel p=base();addSection(p,"Tus contrataciones","Las acciones respetan el rol del usuario activo");
        for(Contratacion x:controllers.contrataciones().listarContrataciones(USER_ID)) {
            JPanel row=card(new BorderLayout(12,8));row.setMaximumSize(new Dimension(Integer.MAX_VALUE,155));
            row.add(label(x.titulo()+" · "+x.contraparte(),H3,TEXT),BorderLayout.NORTH);
            row.add(label(x.estado()+" · "+x.fecha().format(DateTimeFormatter.ofPattern("dd/MM HH:mm")),BODY,MUTED));
            JPanel actions=new JPanel(new FlowLayout(FlowLayout.LEFT,6,3));actions.setOpaque(false);
            if(x.estado()==EstadoContratacion.PENDIENTE||x.estado()==EstadoContratacion.ACEPTADA||x.estado()==EstadoContratacion.CONFIRMADA||x.estado()==EstadoContratacion.EN_CURSO){
                String caption=switch(x.estado()){case PENDIENTE->"Aceptar";case ACEPTADA->"Pagar";case CONFIRMADA->"Iniciar";default->"Completar";};
                JButton advance=button(caption,true);advance.addActionListener(e->safely(()->{controllers.contrataciones().avanzar(USER_ID,x.id());showContracts();}));actions.add(advance);
            }
            if(x.estado()==EstadoContratacion.PENDIENTE){
                JButton reject=button("Rechazar",false);reject.addActionListener(e->safely(()->{String reason=JOptionPane.showInputDialog(this,"Motivo de rechazo");if(reason!=null){controllers.contrataciones().rechazar(USER_ID,x.id(),reason);showContracts();}}));actions.add(reject);
            }
            if(x.estado()==EstadoContratacion.ACEPTADA||x.estado()==EstadoContratacion.CONFIRMADA){
                JButton cancel=button("Cancelar",false);cancel.addActionListener(e->safely(()->{String reason=JOptionPane.showInputDialog(this,"Motivo de cancelación");if(reason!=null){controllers.contrataciones().cancelar(USER_ID,x.id(),reason);showContracts();}}));actions.add(cancel);
                JButton reschedule=button("Reprogramar",false);reschedule.addActionListener(e->safely(()->{var slots=controllers.contrataciones().franjasParaContrato(USER_ID,x.id()).toArray(Franja[]::new);var chosen=(Franja)JOptionPane.showInputDialog(this,"Nueva franja","Reprogramar",JOptionPane.QUESTION_MESSAGE,null,slots,slots.length==0?null:slots[0]);if(chosen!=null){controllers.contrataciones().reprogramar(USER_ID,x.id(),chosen.id());showContracts();}}));actions.add(reschedule);
                if(controllers.contrataciones().tieneReprogramacion(USER_ID,x.id())){
                    JButton accept=button("Aceptar nueva franja",false);accept.addActionListener(e->safely(()->{controllers.contrataciones().responderReprogramacion(USER_ID,x.id(),true);showContracts();}));actions.add(accept);
                    JButton reject=button("Rechazar nueva franja",false);reject.addActionListener(e->safely(()->{controllers.contrataciones().responderReprogramacion(USER_ID,x.id(),false);showContracts();}));actions.add(reject);
                }
            }
            if(x.estado()==EstadoContratacion.COMPLETADA){
                JButton rate=button("Calificar",true);rate.addActionListener(e->safely(()->{var score=new JSpinner(new SpinnerNumberModel(5,1,5,1));var comment=new JTextField();var form=new JPanel(new GridLayout(0,1));form.add(new JLabel("Puntaje"));form.add(score);form.add(new JLabel("Comentario"));form.add(comment);if(JOptionPane.showConfirmDialog(this,form,"Calificar",JOptionPane.OK_CANCEL_OPTION)==JOptionPane.OK_OPTION){controllers.contrataciones().calificar(USER_ID,x.id(),(Integer)score.getValue(),comment.getText());message("Calificación registrada","Se actualizó la reputación.");}}));actions.add(rate);
                JButton receipt=button("Comprobante",false);receipt.addActionListener(e->safely(()->message("Comprobante",controllers.contrataciones().comprobante(USER_ID,x.id()))));actions.add(receipt);
            }
            row.add(actions,BorderLayout.SOUTH);p.add(row);p.add(Box.createVerticalStrut(10));
        }
        p.add(pagination("contrataciones",this::showContracts));page("Contrataciones",p);
    }

    private void showWallet(){JPanel p=base();JPanel balance=new Theme.RoundedPanel(22,NAVY_2);balance.setLayout(new BorderLayout());balance.setBorder(new EmptyBorder(24,26,24,26));balance.setMaximumSize(new Dimension(Integer.MAX_VALUE,120));JPanel btxt=new JPanel();btxt.setOpaque(false);btxt.setLayout(new BoxLayout(btxt,BoxLayout.Y_AXIS));btxt.add(label("SALDO DISPONIBLE",SMALL,new Color(184,209,222)));btxt.add(label(controllers.wallet().consultarSaldo(USER_ID)+" SkillCoins",new Font("SansSerif",Font.BOLD,30),Color.WHITE));balance.add(btxt);JButton add=button("+ Acreditar demo",true);add.addActionListener(e->safely(()->{controllers.wallet().acreditarDemo(USER_ID,500);showWallet();}));balance.add(add,BorderLayout.EAST);balance.setAlignmentX(LEFT_ALIGNMENT);p.add(balance);p.add(Box.createVerticalStrut(24));addSection(p,"Movimientos recientes",null);for(Movimiento m:controllers.wallet().listarMovimientos(USER_ID)){JPanel row=card(new BorderLayout());row.setMaximumSize(new Dimension(Integer.MAX_VALUE,72));JPanel t=new JPanel();t.setOpaque(false);t.setLayout(new BoxLayout(t,BoxLayout.Y_AXIS));t.add(label(m.concepto(),H3,TEXT));t.add(label(m.fecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy · HH:mm")),SMALL,MUTED));row.add(t);row.add(label((m.importe()>0?"+":"")+m.importe()+" ◈",H3,m.importe()>0?TEAL:DANGER),BorderLayout.EAST);p.add(row);p.add(Box.createVerticalStrut(8));}p.add(pagination("movimientos",this::showWallet));page("SkillCoins",p);}

    private void showNotifications(){JPanel p=base();JPanel head=new JPanel(new BorderLayout());head.setOpaque(false);head.add(label("Centro de notificaciones",H2,TEXT));JButton all=button("Marcar todas como leídas",false);all.addActionListener(e->safely(()->{controllers.notificaciones().marcarTodasLeidas(USER_ID);showNotifications();}));head.add(all,BorderLayout.EAST);head.setMaximumSize(new Dimension(Integer.MAX_VALUE,42));head.setAlignmentX(LEFT_ALIGNMENT);p.add(head);JComboBox<String> channel=new JComboBox<>(new String[]{"EMAIL","PUSH","INTERNA"});channel.setSelectedItem(controllers.notificaciones().canal(USER_ID));channel.addActionListener(e->safely(()->controllers.notificaciones().cambiarCanal(USER_ID,(String)channel.getSelectedItem())));p.add(channel);p.add(Box.createVerticalStrut(15));for(Notificacion n:controllers.notificaciones().listarNotificaciones(USER_ID)){JPanel row=card(new BorderLayout(14,0));row.setMaximumSize(new Dimension(Integer.MAX_VALUE,90));JLabel dot=label(n.leida()?"○":"●",H2,n.leida()?BORDER:TEAL);row.add(dot,BorderLayout.WEST);JPanel t=new JPanel();t.setOpaque(false);t.setLayout(new BoxLayout(t,BoxLayout.Y_AXIS));t.add(label(n.titulo(),H3,TEXT));t.add(label(n.detalle(),BODY,MUTED));t.add(label(n.fecha().format(DateTimeFormatter.ofPattern("dd/MM · HH:mm")),SMALL,MUTED));row.add(t);if(!n.leida()){JButton read=button("Marcar leída",false);read.addActionListener(e->safely(()->{controllers.notificaciones().marcarLeida(USER_ID,n.id());showNotifications();}));row.add(read,BorderLayout.EAST);}p.add(row);p.add(Box.createVerticalStrut(9));}p.add(pagination("notificaciones",this::showNotifications));page("Notificaciones",p);}

    private void showProfile(){Perfil x=controllers.perfiles().verPerfil(USER_ID);JPanel p=base();JPanel hero=card(new BorderLayout(20,0));hero.setMaximumSize(new Dimension(Integer.MAX_VALUE,180));hero.add(avatar(initials(x.nombre()),86),BorderLayout.WEST);JPanel data=new JPanel();data.setOpaque(false);data.setLayout(new BoxLayout(data,BoxLayout.Y_AXIS));data.add(label(x.nombre(),H1,TEXT));data.add(label(x.rol(),H3,TEAL_DARK));data.add(Box.createVerticalStrut(10));data.add(label("<html><div style='width:570px'>"+x.bio()+"</div></html>",BODY,MUTED));hero.add(data);JPanel stats=new JPanel(new GridLayout(3,1,0,4));stats.setOpaque(false);stats.add(label("★ "+x.reputacion()+" reputación",H3,GOLD));stats.add(label(x.resenas()+" reseñas",BODY,MUTED));stats.add(label(x.proyectosCompletados()+" proyectos",BODY,MUTED));hero.add(stats,BorderLayout.EAST);hero.setAlignmentX(LEFT_ALIGNMENT);p.add(hero);p.add(Box.createVerticalStrut(24));addSection(p,"Habilidades",null);JPanel tags=new JPanel(new FlowLayout(FlowLayout.LEFT,8,0));tags.setOpaque(false);for(String s:x.habilidades())tags.add(pill(s,NAVY_2));tags.setAlignmentX(LEFT_ALIGNMENT);p.add(tags);page("Mi perfil",p);}

    private JLabel pill(String text,Color color){JLabel l=label(text,new Font("SansSerif",Font.BOLD,10),Color.WHITE);l.setOpaque(true);l.setBackground(color);l.setBorder(new EmptyBorder(6,9,6,9));return l;}
    private JLabel avatar(String initials,int size){JLabel a=new JLabel(initials,SwingConstants.CENTER);a.setPreferredSize(new Dimension(size,size));a.setMinimumSize(new Dimension(size,size));a.setMaximumSize(new Dimension(size,size));a.setOpaque(true);a.setBackground(TEAL);a.setForeground(Color.WHITE);a.setFont(new Font("SansSerif",Font.BOLD,size/3));a.setBorder(BorderFactory.createLineBorder(new Color(255,255,255,100),2,true));return a;}
    private JComponent empty(String title,String subtitle){JPanel e=card(new GridBagLayout());e.setMaximumSize(new Dimension(Integer.MAX_VALUE,190));JPanel t=new JPanel();t.setOpaque(false);t.setLayout(new BoxLayout(t,BoxLayout.Y_AXIS));JLabel h=label(title,H2,TEXT);h.setAlignmentX(CENTER_ALIGNMENT);t.add(h);t.add(Box.createVerticalStrut(8));JLabel s=label(subtitle,BODY,MUTED);s.setAlignmentX(CENTER_ALIGNMENT);t.add(s);e.add(t);return e;}
    private void message(String title,String body){JOptionPane.showMessageDialog(this,body,title,JOptionPane.INFORMATION_MESSAGE);}

    private static String initials(String name) {
        return java.util.Arrays.stream(name.split("\\s+")).filter(s->!s.isBlank()).limit(2).map(s->s.substring(0,1)).collect(java.util.stream.Collectors.joining());
    }
    private void safely(Runnable action) {
        try { action.run(); }
        catch(UiOperationException error){JOptionPane.showMessageDialog(this,error.getMessage(),"No se pudo completar",JOptionPane.WARNING_MESSAGE);}
        catch(RuntimeException error){java.util.logging.Logger.getLogger(SkillSwapView.class.getName()).log(java.util.logging.Level.WARNING,"UI action failed",error);JOptionPane.showMessageDialog(this,"No se pudo completar la operación","Error",JOptionPane.ERROR_MESSAGE);}
    }
    private JComponent pagination(String key,Runnable refresh) {
        JPanel panel=new JPanel(new FlowLayout(FlowLayout.RIGHT));panel.setOpaque(false);
        JButton previous=button("Anterior",false),next=button("Siguiente",false);
        previous.setEnabled(controllers.pagina(key)>0);next.setEnabled(controllers.haySiguiente(key));
        previous.addActionListener(e->safely(()->{controllers.cambiarPagina(key,controllers.pagina(key)-1);refresh.run();}));
        next.addActionListener(e->safely(()->{controllers.cambiarPagina(key,controllers.pagina(key)+1);refresh.run();}));
        panel.add(previous);panel.add(new JLabel("Página "+(controllers.pagina(key)+1)));panel.add(next);return panel;
    }
}
