package com.uade.skillswap.app.presentation;

import ar.edu.uade.skillswap.controllers.UiControllers;
import ar.edu.uade.skillswap.model.Models.*;
import com.uade.skillswap.application.common.Page;
import com.uade.skillswap.application.dto.BookingView;
import com.uade.skillswap.application.dto.publication.*;
import com.uade.skillswap.controllers.ControllerResult;
import com.uade.skillswap.controllers.publication.*;
import com.uade.skillswap.controllers.rating.RatingForm;
import java.time.*;
import java.util.*;
import java.util.function.IntFunction;
import java.util.function.Supplier;

/** Adapter del contrato de la cátedra. Solo conoce interfaces de controllers y sus DTO. */
public final class RealUiControllers implements UiControllers, UiControllers.BusquedaController,
        UiControllers.PublicacionController, UiControllers.ContratacionController, UiControllers.WalletController,
        UiControllers.NotificacionController, UiControllers.PerfilController {
    private static final int SIZE = 12;
    private final ApplicationControllers app;
    private final String user;
    private final ZoneId zone;
    private final Map<String, Integer> pages = new HashMap<>();
    private final Map<String, Boolean> next = new HashMap<>();
    // Snapshot del listado visible: permite dibujar reprogramaciones sin consultar por fila.
    // Las acciones siempre vuelven a consultar la contratación y sus permisos.
    private final Map<Long, BookingView> listedBookings = new HashMap<>();
    private final Map<Long, CatalogEntryView> catalog = new LinkedHashMap<>();

    public RealUiControllers(ApplicationControllers app, String user, ZoneId zone) {
        this.app = Objects.requireNonNull(app);
        this.user = Objects.requireNonNull(user);
        this.zone = Objects.requireNonNull(zone);
    }
    private <T> T call(Supplier<T> work) { return SwingCalls.call(work); }
    private void requireUser(String id) {
        if (!user.equals(id) || !user.equals(app.session().currentUserId()))
            throw new UiOperationException("La sesión cambió. Volvé a seleccionar el usuario.");
    }
    private static <T> T data(ControllerResult<T> result) {
        if (!result.isSuccess()) throw new UiOperationException(result.getMessage());
        return result.getData();
    }
    private static long id(String raw) {
        try { return Long.parseLong(raw); }
        catch (NumberFormatException error) { throw new UiOperationException("Identificador inválido"); }
    }
    private static int integer(long value) {
        try { return Math.toIntExact(value); }
        catch (ArithmeticException error) { throw new UiOperationException("El valor supera el rango admitido por el kit"); }
    }
    private LocalDateTime date(Instant instant) { return LocalDateTime.ofInstant(instant, zone); }
    @Override public int pagina(String key) { return pages.getOrDefault(key, 0); }
    @Override public boolean haySiguiente(String key) { return next.getOrDefault(key, false); }
    @Override public void cambiarPagina(String key, int page) { pages.put(key, Math.max(0, page)); }
    private <T> List<T> page(String key, Page<T> result) {
        next.put(key, result.hasNext());
        return result.items();
    }
    private <T> List<T> all(IntFunction<Page<T>> query) {
        List<T> rows = new ArrayList<>();
        for (int p = 0; ; p++) {
            var result = query.apply(p); rows.addAll(result.items());
            if (!result.hasNext()) return rows;
        }
    }
    private Map<Long, CatalogEntryView> catalog() {
        if (catalog.isEmpty()) all(p -> data(app.catalog().list(p, 100))).forEach(row -> catalog.put(row.id(), row));
        return catalog;
    }
    private Long category(String name) {
        if (name == null || name.equals("Todas")) return null;
        return catalog().values().stream().filter(c -> c.name().equals(name)).map(CatalogEntryView::id)
                .findFirst().orElseThrow(() -> new UiOperationException("Categoría desconocida"));
    }
    private String category(Long id) { var entry = catalog().get(id); return entry == null ? "Sin categoría" : entry.name(); }
    private static String modality(Modalidad mode) {
        if (mode == null) return null;
        return switch (mode) {
            case REMOTA -> "REMOTE";
            case PRESENCIAL -> "ON_SITE";
            case HIBRIDA -> throw new UiOperationException("La modalidad híbrida no está prevista en el dominio; elegí remota o presencial");
        };
    }
    private SearchForm filters(FiltrosBusqueda f, int page) {
        String order = switch (f.orden() == null ? "Relevancia" : f.orden()) {
            case "Menor precio" -> "LOWEST_PRICE";
            case "Mejor reputación" -> "BEST_REPUTATION";
            case "Más reciente" -> "MOST_RECENT";
            default -> "RELEVANCE";
        };
        return new SearchForm(f.texto(), category(f.categoria()), modality(f.modalidad()), null,
                f.precioMaximo() <= 0 ? null : (long) f.precioMaximo(), null, null, order, page, SIZE);
    }
    private Publicacion publication(PublicationSummary p, boolean recommended) {
        return new Publicacion("" + p.id(), p.title(), p.description(), category(p.categoryId()),
                p.modality() == com.uade.skillswap.domain.publication.Modality.REMOTE ? Modalidad.REMOTA : Modalidad.PRESENCIAL,
                integer(p.durationMinutes()), integer(p.basePrice()), p.authorReputation(), p.authorDisplayName(),
                p.tags().stream().sorted().toList(), EstadoPublicacion.PUBLICADA, recommended);
    }
    private Publicacion publication(PublicationView p) {
        return new Publicacion("" + p.id(), p.title(), p.description(), category(p.categoryId()),
                p.modality() == com.uade.skillswap.domain.publication.Modality.REMOTE ? Modalidad.REMOTA : Modalidad.PRESENCIAL,
                integer(p.durationMinutes()), integer(p.basePrice()), 0, p.authorId(), p.tags().stream().sorted().toList(),
                switch(p.status()) { case DRAFT -> EstadoPublicacion.BORRADOR; case PUBLISHED -> EstadoPublicacion.PUBLICADA;
                    case PAUSED -> EstadoPublicacion.PAUSADA; case CLOSED -> EstadoPublicacion.CERRADA; case CANCELLED -> EstadoPublicacion.CANCELADA; }, false);
    }
    @Override public List<Publicacion> buscar(FiltrosBusqueda f) { return call(() -> {
        requireUser(user);
        return page("buscar", data(app.search().search(filters(f, pagina("buscar"))))).stream().map(p -> publication(p, false)).toList();
    }); }
    @Override public List<Publicacion> recomendar(String userId) { return call(() -> {
        requireUser(userId);
        return data(app.recommendations().recommend(user, new RecommendationForm(null,
                filters(new FiltrosBusqueda("", "Todas", null, 0, "Relevancia"), 0)))).items().stream()
                .limit(3).map(p -> publication(p, true)).toList();
    }); }
    @Override public List<String> categorias() { return call(() -> {
        List<String> names = new ArrayList<>(); names.add("Todas");
        catalog().values().stream().filter(c -> c.kind().equals("CATEGORY")).map(CatalogEntryView::name).forEach(names::add);
        return List.copyOf(names);
    }); }
    @Override public List<Publicacion> listarPropias(String userId) { return call(() -> {
        requireUser(userId);
        return page("publicaciones", data(app.publications().listMine(user, null, pagina("publicaciones"), SIZE)))
                .stream().map(this::publication).toList();
    }); }
    @Override public List<Franja> franjas(String userId, String publicationId) { return call(() -> {
        requireUser(userId);
        return data(app.publications().getDetail(user, id(publicationId))).slots().stream()
                .map(s -> new Franja("" + s.id(), date(s.startsAt()), date(s.endsAt()), s.occupied())).toList();
    }); }
    @Override public Publicacion crearBorrador(String u, String title, String description, String category,
            Modalidad mode, int duration, int price) {
        return crearBorrador(u, title, description, category, mode, duration, price, "MENTORING", List.of());
    }
    @Override public Publicacion crearBorrador(String u, String title, String description, String category,
            Modalidad mode, int duration, int price, String type, List<Franja> slots) { return call(() -> {
        requireUser(u);
        var form = new PublicationForm(type, title, description, category(category), modality(mode), duration, 1,
                (long) price, Set.of(), slots.stream().map(s -> new PublicationForm.SlotForm(
                        s.inicio().atZone(zone).toInstant(), s.fin().atZone(zone).toInstant())).toList());
        return publication(data(app.publications().createDraft(user, form)));
    }); }
    @Override public Publicacion cambiarEstado(String u, String publicationId, EstadoPublicacion target) {
        return cambiarEstado(u,publicationId,target,null);
    }
    @Override public Publicacion cambiarEstado(String u, String publicationId, EstadoPublicacion target, String reason) { return call(() -> {
        requireUser(u);
        var current = data(app.publications().getDetail(user, id(publicationId)));
        String action = switch(target) {
            case PUBLICADA -> current.status() == com.uade.skillswap.domain.publication.PublicationStatus.PAUSED ? "RESUME" : "PUBLISH";
            case PAUSADA -> "PAUSE"; case CERRADA -> "CLOSE"; case CANCELADA -> "CANCEL";
            default -> throw new UiOperationException("Usá una transición con motivo explícito");
        };
        return publication(data(app.publications().changeStatus(user, current.id(), action, reason)));
    }); }
    private Contratacion contract(BookingView b) {
        String other = b.clientId().equals(user) ? b.providerDisplayName() : b.clientDisplayName();
        return new Contratacion("" + b.id(), b.publicationTitle(), other, date(b.slotStartsAt()), integer(b.price()),
                switch(b.status()) { case PENDING -> EstadoContratacion.PENDIENTE; case ACCEPTED -> EstadoContratacion.ACEPTADA;
                    case CONFIRMED -> EstadoContratacion.CONFIRMADA; case IN_PROGRESS -> EstadoContratacion.EN_CURSO;
                    case COMPLETED -> EstadoContratacion.COMPLETADA; case REJECTED -> EstadoContratacion.RECHAZADA; case CANCELLED -> EstadoContratacion.CANCELADA; });
    }
    @Override public List<Contratacion> listarContrataciones(String u) { return call(() -> {
        requireUser(u); int p = pagina("contrataciones");
        var client = data(app.bookings().listAsClient(user, p, SIZE));
        var provider = data(app.bookings().listAsProvider(user, p, SIZE));
        next.put("contrataciones", client.hasNext() || provider.hasNext());
        List<BookingView> both = new ArrayList<>(client.items()); both.addAll(provider.items());
        listedBookings.clear();
        both.forEach(b -> listedBookings.put(b.id(), b));
        return both.stream().sorted(Comparator.comparing(BookingView::slotStartsAt).reversed()).map(this::contract).toList();
    }); }
    private BookingView booking(String raw) {
        // Otro participante puede haber cambiado el estado desde la última pantalla.
        long key = id(raw);
        listedBookings.remove(key);
        return data(app.bookings().get(user, key));
    }
    private Contratacion updated(BookingView booking) {
        listedBookings.remove(booking.id());
        return contract(booking);
    }
    @Override public Contratacion solicitar(String u, String pub, String objective) {
        throw new UiOperationException("Elegí una franja y escribí el mensaje de solicitud");
    }
    @Override public Contratacion solicitar(String u, String pub, String slot, String message, String objective) { return call(() -> {
        requireUser(u); return updated(data(app.bookings().request(user, id(pub), id(slot), message, objective)));
    }); }
    @Override public Contratacion avanzar(String u, String raw) { return call(() -> {
        requireUser(u); var b = booking(raw);
        return updated(data(switch(b.status()) {
            case PENDING -> app.bookings().accept(user, b.id()); case ACCEPTED -> app.bookings().pay(user, b.id());
            case CONFIRMED -> app.bookings().start(user, b.id()); case IN_PROGRESS -> app.bookings().complete(user, b.id());
            default -> throw new UiOperationException("La contratación no admite avanzar de estado");
        }));
    }); }
    @Override public Contratacion cancelar(String u, String raw) { throw new UiOperationException("Indicá el motivo de cancelación"); }
    @Override public Contratacion cancelar(String u, String raw, String reason) { return call(() -> {
        requireUser(u); return updated(data(app.bookings().cancel(user, id(raw), reason)));
    }); }
    @Override public void rechazar(String u,String raw,String reason) { call(()->{
        requireUser(u);return updated(data(app.bookings().reject(user,id(raw),reason)));
    }); }
    @Override public List<Franja> franjasParaContrato(String u, String raw) { return call(() -> {
        requireUser(u); return franjas(u, "" + booking(raw).publicationId()).stream().filter(s -> !s.ocupada()).toList();
    }); }
    @Override public void reprogramar(String u, String raw, String slot) { call(() -> {
        requireUser(u); return updated(data(app.bookings().proposeReschedule(user, id(raw), id(slot))));
    }); }
    @Override public boolean tieneReprogramacion(String u, String raw) { return call(() -> {
        requireUser(u);
        var b = listedBookings.get(id(raw));
        if (b == null) b = booking(raw);
        return b.proposedSlotId() != null && !user.equals(b.rescheduleProposedBy());
    }); }
    @Override public void responderReprogramacion(String u, String raw, boolean accept) { call(() -> {
        requireUser(u); return updated(data(accept ? app.bookings().acceptReschedule(user,id(raw)) : app.bookings().rejectReschedule(user,id(raw))));
    }); }
    @Override public void calificar(String u, String raw, int score, String comment) { call(() -> {
        requireUser(u); var b = booking(raw); String other = user.equals(b.clientId()) ? b.providerId() : b.clientId();
        return data(app.ratings().registerRating(user, new RatingForm(b.id(), other, score, comment)));
    }); }
    @Override public String comprobante(String u, String raw) { return call(() -> {
        requireUser(u); var receipt = data(app.receipts().getReceipt(id(raw)));
        return "Contratación " + receipt.bookingId() + "\nPago: " + receipt.amount() + " SkillCoins\nFecha: " + date(receipt.paidAt());
    }); }
    @Override public int consultarSaldo(String u) { return call(() -> { requireUser(u); return integer(data(app.walletQuery().getMyWallet()).balance()); }); }
    @Override public List<Movimiento> listarMovimientos(String u) { return call(() -> {
        requireUser(u);
        var entries = all(p -> data(app.walletQuery().getMyWallet(p,100)).movements());
        List<Movimiento> rows = new ArrayList<>(); long balance = 0;
        for (var m : entries) {
            long amount = m.type().equals("DEBIT") ? -m.amount() : m.amount(); balance += amount;
            rows.add(new Movimiento(m.type() + (m.bookingId() == null ? "" : " #" + m.bookingId()),
                    date(m.occurredAt()), integer(amount), integer(balance)));
        }
        Collections.reverse(rows);
        int start = (int)Math.min((long)pagina("movimientos") * SIZE, rows.size());
        int end = Math.min(start + SIZE, rows.size());
        next.put("movimientos",end < rows.size());
        return List.copyOf(rows.subList(start,end));
    }); }
    @Override public int acreditarDemo(String u, int amount) { return call(() -> {
        requireUser(u); return integer(data(app.wallet().loadCredits(user,(long)amount)).balance());
    }); }
    @Override public List<Notificacion> listarNotificaciones(String u) { return call(() -> {
        requireUser(u); return page("notificaciones",data(app.notifications().listar(pagina("notificaciones"),SIZE))).stream()
                .map(n -> new Notificacion(n.id().toString(),n.type(),n.message(),date(n.occurredAt()),n.read(),n.type())).toList();
    }); }
    @Override public void marcarLeida(String u,String raw) { call(() -> { requireUser(u); return data(app.notifications().marcarLeida(UUID.fromString(raw))); }); }
    @Override public void marcarTodasLeidas(String u) { call(() -> {
        requireUser(u); all(p -> data(app.notifications().listar(p,100))).stream().filter(n -> !n.read())
                .forEach(n -> data(app.notifications().marcarLeida(n.id()))); return null;
    }); }
    @Override public String canal(String u) { return call(() -> { requireUser(u); return data(app.notificationPreferences().getChannel()); }); }
    @Override public void cambiarCanal(String u,String channel) { call(() -> { requireUser(u); return data(app.notificationPreferences().changeChannel(channel)); }); }
    @Override public Perfil verPerfil(String u) { return call(() -> {
        requireUser(u); var p = data(app.profiles().getPublicProfile(user,0,1));
        var both = all(i -> data(app.bookings().listAsClient(user,i,100)));
        both.addAll(all(i -> data(app.bookings().listAsProvider(user,i,100))));
        int completed = integer(both.stream().filter(b -> b.status() == com.uade.skillswap.domain.booking.BookingStatus.COMPLETED).count());
        return new Perfil(user,p.displayName(),p.accountType().name().equals("PROFESSIONAL") ? "Profesional" : "Organización",p.bio(),p.skillIds().stream()
                .map(skill -> catalog().containsKey(skill) ? catalog.get(skill).name() : "Habilidad #" + skill).toList(),
                p.reputation(),p.ratingCount(),completed);
    }); }
    @Override public BusquedaController busqueda() { return this; }
    @Override public PublicacionController publicaciones() { return this; }
    @Override public ContratacionController contrataciones() { return this; }
    @Override public WalletController wallet() { return this; }
    @Override public NotificacionController notificaciones() { return this; }
    @Override public PerfilController perfiles() { return this; }
}
