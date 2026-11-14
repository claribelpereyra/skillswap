# Modelo UML de SkillSwap sacado del código (PRs #3, #4, #6, #8, #9, #10, #11, #12).
# kind: class | abstract | interface | enum | record
# ops: "vis name(params): ret" ; prefijo "static " o "abstract " opcional.

E = {}


def el(name, kind, pkg, attrs=(), ops=(), literals=(), note=None):
    E[name] = dict(name=name, kind=kind, pkg=pkg, attrs=list(attrs), ops=list(ops), literals=list(literals))


# ---------------- dominio / común ----------------
el("UserId", "record", "dominio.common", ["-value: String"])
el("SkillCoins", "record", "dominio.common", ["-amount: long"],
   ["+add(other: SkillCoins): SkillCoins", "+subtract(other: SkillCoins): SkillCoins",
    "+isEnoughFor(price: SkillCoins): boolean"])
el("StateHistory<S>", "class", "dominio.common", ["-transitions: List<StateTransition<S>>"],
   ["static +startingAt(initial: S, actor: UserId, at: Instant): StateHistory<S>",
    "static +rebuild(stored: List<StateTransition<S>>): StateHistory<S>",
    "+record(from: S, to: S, actor: UserId, at: Instant, reason: String)",
    "+currentState(): S", "+transitions(): List<StateTransition<S>>"])
el("StateTransition<S>", "record", "dominio.common",
   ["-from: S", "-to: S", "-actor: UserId", "-occurredAt: Instant", "-reason: String"])
el("BusinessRuleException", "class", "dominio.common")
el("InvalidTransitionException", "class", "dominio.common", ["-currentState: String", "-action: String"])

# ---------------- dominio / contratación (Clari) ----------------
el("Booking", "class", "dominio.booking",
   ["-id: Long", "-publicationId: long", "-clientId: UserId", "-providerId: UserId", "-message: String",
    "-goal: String", "-price: SkillCoins", "-history: StateHistory<BookingStatus>", "-slot: TimeSlot",
    "-state: BookingState", "-pendingReschedule: RescheduleProposal"],
   ["static +request(publicationId: long, client: UserId, provider: UserId, slot: TimeSlot, message: String, goal: String, price: SkillCoins): Booking",
    "+accept(actor: UserId)", "+reject(actor: UserId, reason: String)", "+confirmPayment(actor: UserId)",
    "+start(actor: UserId)", "+complete(actor: UserId)", "+cancel(actor: UserId, reason: String)",
    "+proposeReschedule(actor: UserId, newSlot: TimeSlot)", "+acceptReschedule(actor: UserId)",
    "+rejectReschedule(actor: UserId)", "+getStatus(): BookingStatus"])
el("BookingStatus", "enum", "dominio.booking",
   literals=["PENDING", "ACCEPTED", "CONFIRMED", "IN_PROGRESS", "COMPLETED", "REJECTED", "CANCELLED"])
el("BookingState", "interface", "dominio.booking.state", ops=[
    "+getStatus(): BookingStatus", "+accept(b: Booking, actor: UserId)",
    "+reject(b: Booking, actor: UserId, reason: String)", "+confirmPayment(b: Booking, actor: UserId)",
    "+start(b: Booking, actor: UserId)", "+complete(b: Booking, actor: UserId)",
    "+cancel(b: Booking, actor: UserId, reason: String)",
    "+proposeReschedule(b: Booking, actor: UserId, slot: TimeSlot)",
    "+acceptReschedule(b: Booking, actor: UserId)", "+rejectReschedule(b: Booking, actor: UserId)"])
el("BookingStateBase", "abstract", "dominio.booking.state", ops=["-invalid(action: String): InvalidTransitionException"])
for s in ["PendingState", "AcceptedState", "ConfirmedState", "InProgressState", "CompletedState",
          "RejectedState", "CancelledState"]:
    el(s, "class", "dominio.booking.state")
el("BookingStateFactory", "class", "dominio.booking.state", ops=["static +fromStatus(status: BookingStatus): BookingState"])
el("RescheduleProposal", "record", "dominio.booking", ["-newSlot: TimeSlot", "-proposedBy: UserId"])

# ---------------- dominio / publicaciones (Luqui) ----------------
el("Publication", "class", "dominio.publication",
   ["-id: Long", "-authorId: UserId", "-type: PublicationType", "-details: PublicationDetails",
    "-slots: List<TimeSlot>", "-history: StateHistory<PublicationStatus>", "-state: PublicationState"],
   ["static +draft(author: UserId, accountType: AccountType, type: PublicationType, details: PublicationDetails, now: Instant): Publication",
    "static +restore(id: Long, author: UserId, type: PublicationType, details: PublicationDetails, slots: List<TimeSlot>, history: List<StateTransition>): Publication",
    "+edit(actor: UserId, details: PublicationDetails)",
    "+addSlot(actor: UserId, start: Instant, end: Instant): TimeSlot", "+removeSlot(actor: UserId, slot: TimeSlot)",
    "+publish(actor: UserId, now: Instant)", "+pause(actor: UserId, now: Instant)",
    "+resume(actor: UserId, now: Instant)", "+close(actor: UserId, now: Instant)",
    "+cancel(actor: UserId, reason: String, now: Instant)", "+isVisibleTo(viewer: UserId): boolean",
    "+acceptsRequests(): boolean", "+checkCanBeRequested(client: UserId, slot: TimeSlot)",
    "+reserve(slot: TimeSlot)", "+release(slot: TimeSlot)", "+availableSpots(): int",
    "~checkCompleteForPublishing()"])
el("PublicationDetails", "record", "dominio.publication",
   ["-title: String", "-description: String", "-categoryId: Long", "-modality: Modality",
    "-estimatedDuration: Duration", "-capacity: int", "-basePrice: SkillCoins", "-tags: Set<String>"])
el("PublicationType", "enum", "dominio.publication", ops=["+canBePublishedBy(t: AccountType): boolean"],
   literals=["MENTORING", "SERVICE", "CHALLENGE"])
el("Modality", "enum", "dominio.publication", literals=["REMOTE", "ON_SITE"])
el("PublicationStatus", "enum", "dominio.publication",
   literals=["DRAFT", "PUBLISHED", "PAUSED", "CLOSED", "CANCELLED"])
el("AccountType", "enum", "dominio.profile", literals=["PROFESSIONAL", "ORGANIZATION"])
el("PublicationState", "abstract", "dominio.publication", ops=[
    "abstract ~status(): PublicationStatus", "~publish(p: Publication): PublicationState",
    "~pause(): PublicationState", "~resume(): PublicationState", "~close(): PublicationState",
    "~cancel(): PublicationState", "~checkEditable()", "~acceptsRequests(): boolean", "~isPublic(): boolean",
    "~allowsReservations(): boolean", "static ~of(status: PublicationStatus): PublicationState"])
for s in ["Draft", "Published", "Paused", "Closed", "Cancelled"]:
    el(s, "class", "dominio.publication")
el("TimeSlot", "class", "dominio.publication",
   ["-id: Long", "-startsAt: Instant", "-endsAt: Instant", "-occupied: boolean"],
   ["+overlaps(other: TimeSlot): boolean", "~occupy()", "~release()", "+isOccupied(): boolean"])
el("IncompletePublicationException", "class", "dominio.publication", ["-missing: List<String>"])

# ---------------- dominio / catálogo (Luqui) ----------------
el("CatalogItem", "abstract", "dominio.catalog", ["-id: long", "-name: String"],
   ["abstract +idsInSubtree(): Set<Long>", "abstract +skills(): List<Skill>",
    "abstract +find(id: long): Optional<CatalogItem>", "+includes(id: long): boolean"])
el("Category", "class", "dominio.catalog", ["-children: List<CatalogItem>"],
   ["+add(child: CatalogItem): Category", "+idsInSubtree(): Set<Long>", "+skills(): List<Skill>",
    "+find(id: long): Optional<CatalogItem>"])
el("Skill", "class", "dominio.catalog", [],
   ["+idsInSubtree(): Set<Long>", "+skills(): List<Skill>", "+find(id: long): Optional<CatalogItem>"])

# ---------------- aplicación / común ----------------
el("UnitOfWork", "interface", "aplicacion.ports.out", ops=["+execute(work: Supplier<T>): T"])
el("Page<T>", "record", "aplicacion.common", ["-items: List<T>", "-request: PageRequest", "-totalItems: long"],
   ["+totalPages(): long", "+hasNext(): boolean", "+map(f: Function<T,R>): Page<R>"])
el("PageRequest", "record", "aplicacion.common", ["-number: int", "-size: int"], ["+offset(): long"])

# ---------------- aplicación / publicaciones (Luqui) ----------------
for uc, op in [("CreateDraftUseCase", "+createDraft(c: CreateDraftCommand): PublicationView"),
               ("EditDraftUseCase", "+editDraft(c: EditDraftCommand): PublicationView"),
               ("ChangePublicationStatusUseCase", "+changeStatus(c: ChangePublicationStatusCommand): PublicationView"),
               ("GetPublicationUseCase", "+get(viewer: UserId, id: long): PublicationView"),
               ("ListMyPublicationsUseCase", "+listMine(author: UserId, status: PublicationStatus, page: PageRequest): Page<PublicationView>"),
               ("SearchPublicationsUseCase", "+search(f: SearchFilters): Page<PublicationSummary>")]:
    el(uc, "interface", "aplicacion.ports.in.publication", ops=[op])
for svc in ["CreateDraftService", "EditDraftService", "ChangePublicationStatusService", "GetPublicationService",
            "ListMyPublicationsService", "SearchPublicationsService"]:
    el(svc, "class", "aplicacion.usecases.publication")
el("PublicationRepository", "interface", "aplicacion.ports.out.publication", ops=[
    "+findById(id: long): Optional<Publication>", "+save(p: Publication): Publication",
    "+findByAuthor(author: UserId, status: PublicationStatus, page: PageRequest): Page<Publication>"])
el("AuthorProfileQuery", "interface", "aplicacion.ports.out.publication",
   ops=["+accountTypeOf(user: UserId): Optional<AccountType>"])
el("PublicationSearchQuery", "interface", "aplicacion.ports.out.publication",
   ops=["+search(c: SearchCriteria, page: PageRequest): Page<PublicationSummary>"])
el("CategoryTree", "interface", "aplicacion.ports.out.publication", ops=["+idsUnder(categoryId: long): Optional<Set<Long>>"])
el("CreateDraftCommand", "record", "aplicacion.ports.in.publication",
   ["-authorId: UserId", "-type: PublicationType", "-details: PublicationDetails", "-slots: List<SlotInput>"])
el("ChangePublicationStatusCommand", "record", "aplicacion.ports.in.publication",
   ["-actorId: UserId", "-publicationId: long", "-action: PublicationAction", "-reason: String"])
el("PublicationAction", "enum", "aplicacion.ports.in.publication",
   literals=["PUBLISH", "PAUSE", "RESUME", "CLOSE", "CANCEL"])
el("SearchFilters", "record", "aplicacion.ports.in.publication",
   ["-text: String", "-categoryId: Long", "-modality: Modality", "-minPrice: Long", "-maxPrice: Long",
    "-skill: String", "-minReputation: Double", "-sort: SearchSort", "-page: PageRequest"])
el("SearchSort", "enum", "aplicacion.ports.in.publication",
   literals=["RELEVANCE", "BEST_REPUTATION", "LOWEST_PRICE", "MOST_RECENT"])
el("PublicationView", "record", "aplicacion.ports.in.publication",
   ["-id: Long", "-authorId: String", "-status: PublicationStatus", "-title: String", "-availableSpots: int",
    "-slots: List<SlotView>", "..."], ["static +from(p: Publication): PublicationView"])
el("PublicationSummary", "record", "aplicacion.ports.in.publication",
   ["-id: long", "-title: String", "-authorReputation: double", "-basePrice: long", "-availableSpots: int",
    "-publishedAt: Instant", "..."])

# ---------------- aplicación / contratación (Clari, #10) ----------------
el("BookingService", "interface", "aplicacion.ports.in", ops=[
    "+request(c: RequestBookingCommand): BookingView", "+accept(id: Long, actor: UserId): BookingView",
    "+reject(id: Long, actor: UserId, reason: String): BookingView", "+start(id: Long, actor: UserId): BookingView",
    "+complete(id: Long, actor: UserId): BookingView",
    "+proposeReschedule(id: Long, actor: UserId, slotId: long): BookingView",
    "+acceptReschedule(id: Long, actor: UserId): BookingView", "+rejectReschedule(id: Long, actor: UserId): BookingView",
    "+history(id: Long, actor: UserId): List<BookingHistoryEntry>",
    "+listAsClient(actor: UserId, page: PageRequest): Page<BookingView>",
    "+listAsProvider(actor: UserId, page: PageRequest): Page<BookingView>"])
el("BookingServiceImpl", "class", "aplicacion.usecases")
el("BookingRepository", "interface", "aplicacion.ports.out", ops=[
    "+save(b: Booking): Booking", "+findById(id: Long): Optional<Booking>",
    "+findByClient(client: UserId, page: PageRequest): Page<Booking>",
    "+findByProvider(provider: UserId, page: PageRequest): Page<Booking>"])
el("PublicationCatalog", "interface", "aplicacion.ports.out", ops=["+findBookable(publicationId: long): Optional<BookablePublication>"])
el("PublicationAvailability", "interface", "aplicacion.ports.out",
   ops=["+reserve(publicationId: long, slotId: long)", "+release(publicationId: long, slotId: long)"])
el("IdentityService", "interface", "aplicacion.ports.out", ops=["+currentUserId(): UserId"])
el("BookablePublication", "record", "aplicacion.ports.out",
   ["-id: long", "-authorId: UserId", "-published: boolean", "-price: SkillCoins", "-slots: List<TimeSlot>"],
   ["+findSlot(slotId: long): Optional<TimeSlot>"])
el("BookingView", "record", "aplicacion.dto", ["-id: Long", "-status: BookingStatus", "..."])
el("RequestBookingCommand", "record", "aplicacion.dto", ["-publicationId: long", "-slotId: long", "-message: String", "-goal: String"])

# ---------------- relaciones ----------------
# ("assoc", origen, destino, mult_origen, mult_destino, rol, compuesta)
# ("gen", hijo, padre) ("real", clase, interfaz) ("dep", desde, hacia, etiqueta)
R = []


def assoc(a, b, ma="1", mb="1", role="", composite=False):
    R.append(("assoc", a, b, ma, mb, role, composite))


def gen(child, parent):
    R.append(("gen", child, parent))


def real(cls, iface):
    R.append(("real", cls, iface))


def dep(a, b, label=""):
    R.append(("dep", a, b, label))


gen("InvalidTransitionException", "BusinessRuleException")
assoc("StateHistory<S>", "StateTransition<S>", "1", "1..*", "transitions", True)
assoc("StateTransition<S>", "UserId", "*", "1", "actor")

assoc("Booking", "BookingState", "*", "1", "state")
assoc("Booking", "StateHistory<S>", "1", "1", "history", True)
assoc("Booking", "TimeSlot", "*", "1", "slot")
assoc("Booking", "RescheduleProposal", "1", "0..1", "pendingReschedule", True)
assoc("Booking", "SkillCoins", "*", "1", "price")
assoc("Booking", "UserId", "*", "2", "clientId, providerId")
dep("BookingState", "BookingStatus")
real("BookingStateBase", "BookingState")
for s in ["PendingState", "AcceptedState", "ConfirmedState", "InProgressState", "CompletedState",
          "RejectedState", "CancelledState"]:
    gen(s, "BookingStateBase")
dep("BookingStateFactory", "BookingState", "«create»")
dep("BookingStateBase", "InvalidTransitionException", "«throws»")
assoc("RescheduleProposal", "TimeSlot", "*", "1", "newSlot")

assoc("Publication", "PublicationDetails", "1", "1", "details", True)
assoc("Publication", "TimeSlot", "1", "0..*", "slots", True)
assoc("Publication", "PublicationState", "*", "1", "state")
assoc("Publication", "StateHistory<S>", "1", "1", "history", True)
assoc("Publication", "PublicationType", "*", "1", "type")
assoc("Publication", "UserId", "*", "1", "authorId")
assoc("PublicationDetails", "Modality", "*", "0..1", "modality")
assoc("PublicationDetails", "SkillCoins", "*", "0..1", "basePrice")
dep("PublicationType", "AccountType")
dep("PublicationState", "PublicationStatus")
for s in ["Draft", "Published", "Paused", "Closed", "Cancelled"]:
    gen(s, "PublicationState")
dep("Publication", "IncompletePublicationException", "«throws»")
gen("IncompletePublicationException", "BusinessRuleException")

gen("Category", "CatalogItem")
gen("Skill", "CatalogItem")
assoc("Category", "CatalogItem", "0..1", "0..*", "children", True)

for uc, svc in [("CreateDraftUseCase", "CreateDraftService"), ("EditDraftUseCase", "EditDraftService"),
                ("ChangePublicationStatusUseCase", "ChangePublicationStatusService"),
                ("GetPublicationUseCase", "GetPublicationService"),
                ("ListMyPublicationsUseCase", "ListMyPublicationsService"),
                ("SearchPublicationsUseCase", "SearchPublicationsService")]:
    real(svc, uc)
for svc in ["CreateDraftService", "EditDraftService", "ChangePublicationStatusService", "GetPublicationService",
            "ListMyPublicationsService"]:
    dep(svc, "PublicationRepository")
for svc in ["CreateDraftService", "EditDraftService", "ChangePublicationStatusService"]:
    dep(svc, "UnitOfWork")
dep("CreateDraftService", "AuthorProfileQuery")
dep("SearchPublicationsService", "PublicationSearchQuery")
dep("SearchPublicationsService", "CategoryTree")
assoc("ChangePublicationStatusCommand", "PublicationAction", "*", "1", "action")
assoc("SearchFilters", "SearchSort", "*", "1", "sort")
assoc("SearchFilters", "PageRequest", "*", "1", "page")
assoc("Page<T>", "PageRequest", "*", "1", "request")

real("BookingServiceImpl", "BookingService")
for p in ["BookingRepository", "PublicationCatalog", "PublicationAvailability", "IdentityService", "UnitOfWork"]:
    dep("BookingServiceImpl", p)
dep("PublicationCatalog", "BookablePublication")

# ---------------- diagramas: (nombre, {elemento: (x, y)}) ----------------
D = [
    ("Dominio - Común", {
        "UserId": (60, 60), "SkillCoins": (320, 60), "BusinessRuleException": (760, 60),
        "InvalidTransitionException": (760, 180), "StateHistory<S>": (60, 300), "StateTransition<S>": (760, 300),
    }),
    ("Dominio - Contratación", {
        "Booking": (40, 40),
        "RescheduleProposal": (40, 680), "TimeSlot": (330, 680), "UserId": (780, 680),
        "StateHistory<S>": (40, 960), "SkillCoins": (720, 960),
        "BookingState": (960, 40), "BookingStatus": (1700, 160), "BookingStateFactory": (1700, 40),
        "BookingStateBase": (1000, 400), "InvalidTransitionException": (1700, 400),
        "PendingState": (960, 560), "AcceptedState": (1180, 560), "ConfirmedState": (1400, 560),
        "InProgressState": (1620, 560), "CompletedState": (1070, 660), "RejectedState": (1290, 660),
        "CancelledState": (1510, 660),
    }),
    ("Dominio - Publicaciones", {
        "Publication": (40, 40), "PublicationDetails": (800, 40), "Modality": (1200, 40),
        "SkillCoins": (1200, 180), "PublicationType": (800, 360), "AccountType": (1240, 380),
        "TimeSlot": (800, 560), "UserId": (1200, 560), "StateHistory<S>": (800, 780),
        "PublicationState": (40, 620), "PublicationStatus": (500, 620),
        "Draft": (40, 960), "Published": (180, 960), "Paused": (340, 960), "Closed": (480, 960),
        "Cancelled": (620, 960),
        "IncompletePublicationException": (800, 1000), "BusinessRuleException": (1200, 1000),
    }),
    ("Dominio - Catálogo", {
        "CatalogItem": (200, 40), "Category": (40, 300), "Skill": (440, 300),
    }),
    ("Aplicación - Publicaciones", {
        "CreateDraftUseCase": (40, 40), "EditDraftUseCase": (40, 140), "ChangePublicationStatusUseCase": (40, 240),
        "GetPublicationUseCase": (40, 340), "ListMyPublicationsUseCase": (40, 440), "SearchPublicationsUseCase": (40, 540),
        "CreateDraftService": (720, 40), "EditDraftService": (720, 140), "ChangePublicationStatusService": (720, 240),
        "GetPublicationService": (720, 340), "ListMyPublicationsService": (720, 440), "SearchPublicationsService": (720, 540),
        "PublicationRepository": (1100, 40), "UnitOfWork": (1100, 200), "AuthorProfileQuery": (1100, 300),
        "PublicationSearchQuery": (1100, 420), "CategoryTree": (1100, 540),
        "CreateDraftCommand": (40, 700), "ChangePublicationStatusCommand": (360, 700), "PublicationAction": (720, 700),
        "SearchFilters": (40, 900), "SearchSort": (360, 900), "PageRequest": (720, 900), "Page<T>": (1000, 900),
        "PublicationSummary": (40, 1140), "PublicationView": (360, 1140),
    }),
    ("Aplicación - Contratación", {
        "BookingService": (40, 40), "BookingServiceImpl": (680, 280), "BookingRepository": (960, 40),
        "PublicationCatalog": (960, 200), "PublicationAvailability": (960, 300), "IdentityService": (960, 420),
        "UnitOfWork": (960, 520), "BookablePublication": (1560, 180), "BookingView": (40, 440),
        "RequestBookingCommand": (300, 440),
    }),
]
