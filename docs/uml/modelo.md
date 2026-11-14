# Modelo de clases de SkillSwap

Diagramas de clases de **dominio** y **aplicación** generados a partir del código, con los mismos elementos que `skillswap.mdj` (StarUML). GitHub los renderiza para revisarlos sin abrir StarUML.

Patrones que se ven en los diagramas:

- **State**: `BookingState` y sus estados (contratación); `PublicationState` y sus estados (publicación).
- **Composite**: `CatalogItem` → `Category` (compuesto) y `Skill` (hoja).
- **Puertos y adaptadores (Clean Architecture)**: casos de uso como interfaces de entrada, repositorios y consultas como puertos de salida.

## Dominio - Común

```mermaid
classDiagram
    class UserId["UserId"] {
        <<record>>
        -value: String
    }
    class SkillCoins["SkillCoins"] {
        <<record>>
        -amount: long
        +add(other): SkillCoins
        +subtract(other): SkillCoins
        +isEnoughFor(price): boolean
    }
    class BusinessRuleException["BusinessRuleException"] {
    }
    class InvalidTransitionException["InvalidTransitionException"] {
        -currentState: String
        -action: String
    }
    class StateHistory["StateHistory&lt;S&gt;"] {
        -transitions: List~StateTransition~S~~
        +startingAt(initial, actor, at): StateHistory~S~$
        +rebuild(stored): StateHistory~S~$
        +record(from, to, actor, at, reason)
        +currentState(): S
        +transitions(): List~StateTransition~S~~
    }
    class StateTransition["StateTransition&lt;S&gt;"] {
        <<record>>
        -from: S
        -to: S
        -actor: UserId
        -occurredAt: Instant
        -reason: String
    }
    BusinessRuleException <|-- InvalidTransitionException
    StateHistory "1" *-- "1..*" StateTransition : transitions
    StateTransition "*" --> "1" UserId : actor
```

## Dominio - Contratación

```mermaid
classDiagram
    class Booking["Booking"] {
        -id: Long
        -publicationId: long
        -clientId: UserId
        -providerId: UserId
        -message: String
        -goal: String
        -price: SkillCoins
        -history: StateHistory~BookingStatus~
        -slot: TimeSlot
        -state: BookingState
        -pendingReschedule: RescheduleProposal
        +request(publicationId, client, provider, slot, message, goal, price): Booking$
        +accept(actor)
        +reject(actor, reason)
        +confirmPayment(actor)
        +start(actor)
        +complete(actor)
        +cancel(actor, reason)
        +proposeReschedule(actor, newSlot)
        +acceptReschedule(actor)
        +rejectReschedule(actor)
        +getStatus(): BookingStatus
    }
    class RescheduleProposal["RescheduleProposal"] {
        <<record>>
        -newSlot: TimeSlot
        -proposedBy: UserId
    }
    class TimeSlot["TimeSlot"] {
        -id: Long
        -startsAt: Instant
        -endsAt: Instant
        -occupied: boolean
        +overlaps(other): boolean
        ~occupy()
        ~release()
        +isOccupied(): boolean
    }
    class UserId["UserId"] {
        <<record>>
        -value: String
    }
    class StateHistory["StateHistory&lt;S&gt;"] {
        -transitions: List~StateTransition~S~~
        +startingAt(initial, actor, at): StateHistory~S~$
        +rebuild(stored): StateHistory~S~$
        +record(from, to, actor, at, reason)
        +currentState(): S
        +transitions(): List~StateTransition~S~~
    }
    class SkillCoins["SkillCoins"] {
        <<record>>
        -amount: long
        +add(other): SkillCoins
        +subtract(other): SkillCoins
        +isEnoughFor(price): boolean
    }
    class BookingState["BookingState"] {
        <<interface>>
        +getStatus(): BookingStatus
        +accept(b, actor)
        +reject(b, actor, reason)
        +confirmPayment(b, actor)
        +start(b, actor)
        +complete(b, actor)
        +cancel(b, actor, reason)
        +proposeReschedule(b, actor, slot)
        +acceptReschedule(b, actor)
        +rejectReschedule(b, actor)
    }
    class BookingStatus["BookingStatus"] {
        <<enumeration>>
        PENDING
        ACCEPTED
        CONFIRMED
        IN_PROGRESS
        COMPLETED
        REJECTED
        CANCELLED
    }
    class BookingStateFactory["BookingStateFactory"] {
        +fromStatus(status): BookingState$
    }
    class BookingStateBase["BookingStateBase"] {
        <<abstract>>
        -invalid(action): InvalidTransitionException
    }
    class InvalidTransitionException["InvalidTransitionException"] {
        -currentState: String
        -action: String
    }
    class PendingState["PendingState"] {
    }
    class AcceptedState["AcceptedState"] {
    }
    class ConfirmedState["ConfirmedState"] {
    }
    class InProgressState["InProgressState"] {
    }
    class CompletedState["CompletedState"] {
    }
    class RejectedState["RejectedState"] {
    }
    class CancelledState["CancelledState"] {
    }
    Booking "*" --> "1" BookingState : state
    Booking "1" *-- "1" StateHistory : history
    Booking "*" --> "1" TimeSlot : slot
    Booking "1" *-- "0..1" RescheduleProposal : pendingReschedule
    Booking "*" --> "1" SkillCoins : price
    Booking "*" --> "2" UserId : clientId, providerId
    BookingState ..> BookingStatus
    BookingState <|.. BookingStateBase
    BookingStateBase <|-- PendingState
    BookingStateBase <|-- AcceptedState
    BookingStateBase <|-- ConfirmedState
    BookingStateBase <|-- InProgressState
    BookingStateBase <|-- CompletedState
    BookingStateBase <|-- RejectedState
    BookingStateBase <|-- CancelledState
    BookingStateFactory ..> BookingState : «create»
    BookingStateBase ..> InvalidTransitionException : «throws»
    RescheduleProposal "*" --> "1" TimeSlot : newSlot
```

## Dominio - Publicaciones

```mermaid
classDiagram
    class Publication["Publication"] {
        -id: Long
        -authorId: UserId
        -type: PublicationType
        -details: PublicationDetails
        -slots: List~TimeSlot~
        -history: StateHistory~PublicationStatus~
        -state: PublicationState
        +draft(author, accountType, type, details, now): Publication$
        +restore(id, author, type, details, slots, history): Publication$
        +edit(actor, details)
        +addSlot(actor, start, end): TimeSlot
        +removeSlot(actor, slot)
        +publish(actor, now)
        +pause(actor, now)
        +resume(actor, now)
        +close(actor, now)
        +cancel(actor, reason, now)
        +isVisibleTo(viewer): boolean
        +acceptsRequests(): boolean
        +checkCanBeRequested(client, slot)
        +reserve(slot)
        +release(slot)
        +availableSpots(): int
        ~checkCompleteForPublishing()
    }
    class PublicationDetails["PublicationDetails"] {
        <<record>>
        -title: String
        -description: String
        -categoryId: Long
        -modality: Modality
        -estimatedDuration: Duration
        -capacity: int
        -basePrice: SkillCoins
        -tags: Set~String~
    }
    class Modality["Modality"] {
        <<enumeration>>
        REMOTE
        ON_SITE
    }
    class SkillCoins["SkillCoins"] {
        <<record>>
        -amount: long
        +add(other): SkillCoins
        +subtract(other): SkillCoins
        +isEnoughFor(price): boolean
    }
    class PublicationType["PublicationType"] {
        <<enumeration>>
        MENTORING
        SERVICE
        CHALLENGE
        +canBePublishedBy(t): boolean
    }
    class AccountType["AccountType"] {
        <<enumeration>>
        PROFESSIONAL
        ORGANIZATION
    }
    class TimeSlot["TimeSlot"] {
        -id: Long
        -startsAt: Instant
        -endsAt: Instant
        -occupied: boolean
        +overlaps(other): boolean
        ~occupy()
        ~release()
        +isOccupied(): boolean
    }
    class UserId["UserId"] {
        <<record>>
        -value: String
    }
    class StateHistory["StateHistory&lt;S&gt;"] {
        -transitions: List~StateTransition~S~~
        +startingAt(initial, actor, at): StateHistory~S~$
        +rebuild(stored): StateHistory~S~$
        +record(from, to, actor, at, reason)
        +currentState(): S
        +transitions(): List~StateTransition~S~~
    }
    class PublicationState["PublicationState"] {
        <<abstract>>
        ~status(): PublicationStatus*
        ~publish(p): PublicationState
        ~pause(): PublicationState
        ~resume(): PublicationState
        ~close(): PublicationState
        ~cancel(): PublicationState
        ~checkEditable()
        ~acceptsRequests(): boolean
        ~isPublic(): boolean
        ~allowsReservations(): boolean
        ~of(status): PublicationState$
    }
    class PublicationStatus["PublicationStatus"] {
        <<enumeration>>
        DRAFT
        PUBLISHED
        PAUSED
        CLOSED
        CANCELLED
    }
    class Draft["Draft"] {
    }
    class Published["Published"] {
    }
    class Paused["Paused"] {
    }
    class Closed["Closed"] {
    }
    class Cancelled["Cancelled"] {
    }
    class IncompletePublicationException["IncompletePublicationException"] {
        -missing: List~String~
    }
    class BusinessRuleException["BusinessRuleException"] {
    }
    Publication "1" *-- "1" PublicationDetails : details
    Publication "1" *-- "0..*" TimeSlot : slots
    Publication "*" --> "1" PublicationState : state
    Publication "1" *-- "1" StateHistory : history
    Publication "*" --> "1" PublicationType : type
    Publication "*" --> "1" UserId : authorId
    PublicationDetails "*" --> "0..1" Modality : modality
    PublicationDetails "*" --> "0..1" SkillCoins : basePrice
    PublicationType ..> AccountType
    PublicationState ..> PublicationStatus
    PublicationState <|-- Draft
    PublicationState <|-- Published
    PublicationState <|-- Paused
    PublicationState <|-- Closed
    PublicationState <|-- Cancelled
    Publication ..> IncompletePublicationException : «throws»
    BusinessRuleException <|-- IncompletePublicationException
```

## Dominio - Catálogo

```mermaid
classDiagram
    class CatalogItem["CatalogItem"] {
        <<abstract>>
        -id: long
        -name: String
        +idsInSubtree(): Set~Long~*
        +skills(): List~Skill~*
        +find(id): Optional~CatalogItem~*
        +includes(id): boolean
    }
    class Category["Category"] {
        -children: List~CatalogItem~
        +add(child): Category
        +idsInSubtree(): Set~Long~
        +skills(): List~Skill~
        +find(id): Optional~CatalogItem~
    }
    class Skill["Skill"] {
        +idsInSubtree(): Set~Long~
        +skills(): List~Skill~
        +find(id): Optional~CatalogItem~
    }
    CatalogItem <|-- Category
    CatalogItem <|-- Skill
    Category "0..1" *-- "0..*" CatalogItem : children
```

## Aplicación - Publicaciones

```mermaid
classDiagram
    class CreateDraftUseCase["CreateDraftUseCase"] {
        <<interface>>
        +createDraft(c): PublicationView
    }
    class EditDraftUseCase["EditDraftUseCase"] {
        <<interface>>
        +editDraft(c): PublicationView
    }
    class ChangePublicationStatusUseCase["ChangePublicationStatusUseCase"] {
        <<interface>>
        +changeStatus(c): PublicationView
    }
    class GetPublicationUseCase["GetPublicationUseCase"] {
        <<interface>>
        +get(viewer, id): PublicationView
    }
    class ListMyPublicationsUseCase["ListMyPublicationsUseCase"] {
        <<interface>>
        +listMine(author, status, page): Page~PublicationView~
    }
    class SearchPublicationsUseCase["SearchPublicationsUseCase"] {
        <<interface>>
        +search(f): Page~PublicationSummary~
    }
    class CreateDraftService["CreateDraftService"] {
    }
    class EditDraftService["EditDraftService"] {
    }
    class ChangePublicationStatusService["ChangePublicationStatusService"] {
    }
    class GetPublicationService["GetPublicationService"] {
    }
    class ListMyPublicationsService["ListMyPublicationsService"] {
    }
    class SearchPublicationsService["SearchPublicationsService"] {
    }
    class PublicationRepository["PublicationRepository"] {
        <<interface>>
        +findById(id): Optional~Publication~
        +save(p): Publication
        +findByAuthor(author, status, page): Page~Publication~
    }
    class UnitOfWork["UnitOfWork"] {
        <<interface>>
        +execute(work): T
    }
    class AuthorProfileQuery["AuthorProfileQuery"] {
        <<interface>>
        +accountTypeOf(user): Optional~AccountType~
    }
    class PublicationSearchQuery["PublicationSearchQuery"] {
        <<interface>>
        +search(c, page): Page~PublicationSummary~
    }
    class CategoryTree["CategoryTree"] {
        <<interface>>
        +idsUnder(categoryId): Optional~Set~Long~~
    }
    class CreateDraftCommand["CreateDraftCommand"] {
        <<record>>
        -authorId: UserId
        -type: PublicationType
        -details: PublicationDetails
        -slots: List~SlotInput~
    }
    class ChangePublicationStatusCommand["ChangePublicationStatusCommand"] {
        <<record>>
        -actorId: UserId
        -publicationId: long
        -action: PublicationAction
        -reason: String
    }
    class PublicationAction["PublicationAction"] {
        <<enumeration>>
        PUBLISH
        PAUSE
        RESUME
        CLOSE
        CANCEL
    }
    class SearchFilters["SearchFilters"] {
        <<record>>
        -text: String
        -categoryId: Long
        -modality: Modality
        -minPrice: Long
        -maxPrice: Long
        -skill: String
        -minReputation: Double
        -sort: SearchSort
        -page: PageRequest
    }
    class SearchSort["SearchSort"] {
        <<enumeration>>
        RELEVANCE
        BEST_REPUTATION
        LOWEST_PRICE
        MOST_RECENT
    }
    class PageRequest["PageRequest"] {
        <<record>>
        -number: int
        -size: int
        +offset(): long
    }
    class Page["Page&lt;T&gt;"] {
        <<record>>
        -items: List~T~
        -request: PageRequest
        -totalItems: long
        +totalPages(): long
        +hasNext(): boolean
        +map(f): Page~R~
    }
    class PublicationSummary["PublicationSummary"] {
        <<record>>
        -id: long
        -title: String
        -authorReputation: double
        -basePrice: long
        -availableSpots: int
        -publishedAt: Instant
        ...
    }
    class PublicationView["PublicationView"] {
        <<record>>
        -id: Long
        -authorId: String
        -status: PublicationStatus
        -title: String
        -availableSpots: int
        -slots: List~SlotView~
        ...
        +from(p): PublicationView$
    }
    CreateDraftUseCase <|.. CreateDraftService
    EditDraftUseCase <|.. EditDraftService
    ChangePublicationStatusUseCase <|.. ChangePublicationStatusService
    GetPublicationUseCase <|.. GetPublicationService
    ListMyPublicationsUseCase <|.. ListMyPublicationsService
    SearchPublicationsUseCase <|.. SearchPublicationsService
    CreateDraftService ..> PublicationRepository
    EditDraftService ..> PublicationRepository
    ChangePublicationStatusService ..> PublicationRepository
    GetPublicationService ..> PublicationRepository
    ListMyPublicationsService ..> PublicationRepository
    CreateDraftService ..> UnitOfWork
    EditDraftService ..> UnitOfWork
    ChangePublicationStatusService ..> UnitOfWork
    CreateDraftService ..> AuthorProfileQuery
    SearchPublicationsService ..> PublicationSearchQuery
    SearchPublicationsService ..> CategoryTree
    ChangePublicationStatusCommand "*" --> "1" PublicationAction : action
    SearchFilters "*" --> "1" SearchSort : sort
    SearchFilters "*" --> "1" PageRequest : page
    Page "*" --> "1" PageRequest : request
```

## Aplicación - Contratación

```mermaid
classDiagram
    class BookingService["BookingService"] {
        <<interface>>
        +request(c): BookingView
        +accept(id, actor): BookingView
        +reject(id, actor, reason): BookingView
        +start(id, actor): BookingView
        +complete(id, actor): BookingView
        +proposeReschedule(id, actor, slotId): BookingView
        +acceptReschedule(id, actor): BookingView
        +rejectReschedule(id, actor): BookingView
        +history(id, actor): List~BookingHistoryEntry~
        +listAsClient(actor, page): Page~BookingView~
        +listAsProvider(actor, page): Page~BookingView~
    }
    class BookingServiceImpl["BookingServiceImpl"] {
    }
    class BookingRepository["BookingRepository"] {
        <<interface>>
        +save(b): Booking
        +findById(id): Optional~Booking~
        +findByClient(client, page): Page~Booking~
        +findByProvider(provider, page): Page~Booking~
    }
    class PublicationCatalog["PublicationCatalog"] {
        <<interface>>
        +findBookable(publicationId): Optional~BookablePublication~
    }
    class PublicationAvailability["PublicationAvailability"] {
        <<interface>>
        +reserve(publicationId, slotId)
        +release(publicationId, slotId)
    }
    class IdentityService["IdentityService"] {
        <<interface>>
        +currentUserId(): UserId
    }
    class UnitOfWork["UnitOfWork"] {
        <<interface>>
        +execute(work): T
    }
    class BookablePublication["BookablePublication"] {
        <<record>>
        -id: long
        -authorId: UserId
        -published: boolean
        -price: SkillCoins
        -slots: List~TimeSlot~
        +findSlot(slotId): Optional~TimeSlot~
    }
    class BookingView["BookingView"] {
        <<record>>
        -id: Long
        -status: BookingStatus
        ...
    }
    class RequestBookingCommand["RequestBookingCommand"] {
        <<record>>
        -publicationId: long
        -slotId: long
        -message: String
        -goal: String
    }
    BookingService <|.. BookingServiceImpl
    BookingServiceImpl ..> BookingRepository
    BookingServiceImpl ..> PublicationCatalog
    BookingServiceImpl ..> PublicationAvailability
    BookingServiceImpl ..> IdentityService
    BookingServiceImpl ..> UnitOfWork
    PublicationCatalog ..> BookablePublication
```
