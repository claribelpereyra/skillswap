# Tarea 91 — SOLID y GRASP en el diseño

Referencia: secciones 3 y 14 de la consigna y decisiones de las tareas 89 y 90. Se analiza el código disponible y el diseño propuesto por separado: una interfaz dibujada no demuestra por sí sola que el principio se cumpla en ejecución.

## Evidencia disponible

En este checkout el ejemplo de dominio implementado es `SkillCoins`. Hay configuración de persistencia y módulos, pero los paquetes de casos de uso, puertos y controllers todavía contienen solamente `package-info.java`. Las propuestas siguientes deben contrastarse con el código y el UML del equipo cuando se incorporen.

## SOLID

| Principio | Aplicación al modelo | Evidencia actual y validación pendiente |
| --- | --- | --- |
| S — Responsabilidad única | `SkillCoins` representa un monto y protege sus operaciones; no guarda datos ni muestra mensajes de UI. Casos de uso coordinan, repositorios persisten y controllers adaptan entradas. | `dominio/comun/SkillCoins.java` valida montos no negativos y saldo suficiente. `SkillCoinsTest` contiene casos de suma/resta, monto negativo y saldo insuficiente. La separación de responsabilidades de los futuros servicios debe revisarse cuando existan. |
| O — Abierto/cerrado | Agregar una estrategia de recomendación o un canal de notificación mediante otra implementación del mismo contrato, conservando al consumidor. | Propuesta en la tarea 89, sin implementaciones todavía. Verificar el mismo caso de uso con afinidad y popularidad, y el procesamiento de avisos con email y push. La fábrica o composición puede requerir registrar la variante nueva; el objetivo es mantener estable al consumidor. |
| L — Sustitución de Liskov | Los adaptadores y estrategias deben respetar las precondiciones, resultados y errores acordados por su interfaz. Un canal no puede requerir información que su contrato no permite suministrar. | Pendiente de contratos concretos. Ejecutar pruebas de contrato sobre cada adaptador: mismo significado de destinatario, referencia y resultado; documentar los fallos admitidos. Tener `implements` no demuestra sustitución correcta. |
| I — Segregación de interfaces | Separar búsqueda, pago, preferencias y bandeja evita que un controller dependa de todas las funciones de SkillSwap. Las interfaces de consulta pueden ser distintas de las de escritura. | Catálogo de entradas/salidas de la tarea 89. Revisar que cada consumidor use un contrato acotado y que los dobles no deban implementar operaciones ajenas. El tamaño final debe responder a necesidades reales, no a una interfaz por método como regla automática. |
| D — Inversión de dependencias | Los casos de uso dependen de repositorios y servicios definidos en aplicación; JPA y mocks implementan esos contratos desde infraestructura. Main selecciona implementaciones. | Los POMs ya orientan las dependencias de módulos hacia adentro. Los puertos e inyección por constructor siguen pendientes. Validar casos de uso con dobles sin MySQL ni UI y revisar que no importen clases concretas de infraestructura. |

## GRASP

| Principio | Responsabilidad asignada y motivo | Estado o comprobación necesaria |
| --- | --- | --- |
| Experto en información | `SkillCoins` conoce su cantidad, por eso decide si alcanza para otro monto y evita una resta inválida. La futura contratación conoce su estado y participantes; la publicación conoce disponibilidad y cupo. | El primer ejemplo existe en `SkillCoins.alcanzaPara` y `restar`. Los ejemplos de contratación y publicación son decisiones de diseño pendientes del dominio del equipo. |
| Creador | El caso de uso de solicitud coordina la creación con los datos de entrada; el objeto/fábrica de dominio elegido por el equipo construye la solicitud válida. Main crea los colaboradores de larga vida porque conoce la configuración y sus dependencias. | Main ya crea la configuración y fábrica JPA; todavía no arma los servicios. No se obliga a Main a crear cada entidad o DTO. Revisar el contrato definitivo de creación de solicitud. |
| Controller | `NotificacionController` recibe listar/marcar lectura y delega en la entrada de bandeja. No decide destinatarios, no envía email y no consulta JPA. | Diseño pendiente de la tarea 103. Probar delegación, validación de entrada y errores sin introducir reglas de negocio en el controller. |
| Bajo acoplamiento | Dominio no conoce JPA ni Swing/JavaFX; aplicación consume puertos y la presentación recibe resultados simples. | `SkillCoins` no tiene dependencias externas de producción. La separación Maven ayuda, pero no impide que un controller use indebidamente un repositorio definido en aplicación: requiere revisión adicional. |
| Alta cohesión | Preferencias administra la elección de canal; bandeja administra consulta y lectura; el procesamiento de eventos arma avisos. Cada parte tiene un propósito relacionado. | Propuesta de casos de uso de la tarea 89. Evitar una clase que concentre contratación, persistencia, envío y manejo de UI. |
| Polimorfismo | Las estrategias resuelven recomendación por afinidad/popularidad y los adaptadores resuelven envío por email/push/interna a través de sus contratos. | Pendiente de implementación y pruebas que cambien la variante conservando al consumidor. La selección en una fábrica no reemplaza el comportamiento polimórfico del canal. |
| Fabricación pura | Un repositorio JPA y una unidad de trabajo son servicios técnicos que permiten conservar a las entidades enfocadas en el negocio. | Existe `ConfiguracionPersistencia` como clase técnica separada; repositorios y unidad de trabajo todavía no existen. Revisar que las entidades no abran conexiones ni confirmen transacciones. |
| Indirección | Puertos y adaptadores median entre aplicación y base/proveedores. Observer permite reaccionar a eventos sin que contratación conozca los canales concretos. | Los paquetes están preparados y el diseño está en la tarea 89. Pendiente: publicación de eventos y suscriptores de las tareas 93 y 94, y emisión desde los flujos reales del equipo. |
| Variaciones protegidas | Se aíslan los cambios esperados: canal, recomendación, política de reintegro, reloj y persistencia. Las reglas consumen contratos o valores configurados. | La conexión ya se lee desde entorno en infraestructura. Las estrategias, el reloj y la configuración funcional requieren sus implementaciones; no crear abstracciones adicionales sin una variación concreta. |

## Ejemplo para explicar en la defensa

Ante un pago confirmado, el caso de uso guarda el resultado económico y el estado en una transacción y luego publica el hecho. Observer deriva el evento al procesamiento de notificaciones, que consulta preferencias y usa un canal reemplazable. El pago no necesita saber si el destinatario usa email o push.

En ese recorrido se esperan responsabilidad única, inversión de dependencias, indirección y polimorfismo. Para demostrarlo con código faltan el pago de Tomi, la coordinación transaccional compartida y las tareas de notificaciones de Ian. Por ahora es una explicación del diseño, no una evidencia de integración ejecutada.

## Qué falta para cerrar la evidencia del producto

- Asociar cada fila propuesta con clases y relaciones del StarUML que prepara Luqui, y luego con sus nombres definitivos en código.
- Revisar estados y responsabilidades de contratación con Clari; recomendación, wallet y políticas con Tomi; perfiles, reputación e identidad con Andy.
- Incorporar resultados de pruebas reales de sustitución de estrategias y canales, permisos, atomicidad y reglas negativas cuando existan. En esta tarea documental no se ejecutaron pruebas ni se agregó código de negocio.

La documentación de la tarea 91 queda preparada. La evidencia completa de implementación y pruebas deberá actualizarse cuando se integren esas partes; no se presenta todo SOLID y GRASP como demostrado por el esqueleto actual.
