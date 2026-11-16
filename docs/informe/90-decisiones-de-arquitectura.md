# Tarea 90 — Decisiones de arquitectura

Este documento explica las decisiones de estructura de SkillSwap tomando como base la sección 6 de la consigna y los módulos existentes del repositorio. Complementa el bosquejo de contratos de la tarea 89. Se distingue lo que ya está en código de lo que falta implementar.

## 1. Separar el proyecto en módulos con dependencias hacia el dominio

**Problema.** Las reglas de publicaciones, contrataciones y SkillCoins deben poder probarse sin iniciar la interfaz ni conectarse a MySQL.

**Decisión.** Usar Clean Architecture con cinco módulos Maven:

| Módulo | Responsabilidad | Dependencias internas directas |
| --- | --- | --- |
| `skillswap-dominio` | Entidades, value objects, estados, políticas y eventos | Ninguna |
| `skillswap-aplicacion` | Casos de uso, contratos de entrada/salida y coordinación transaccional | dominio |
| `skillswap-infraestructura` | Persistencia, adaptadores mock y configuración técnica | aplicación |
| `skillswap-controllers` | Recibir acciones, validar entradas y permisos y delegar | aplicación |
| `skillswap-app` | Interfaz y armado del grafo de objetos en Main | controllers e infraestructura |

**Alternativa.** Un solo módulo con paquetes por capa sería más sencillo al comienzo, pero permitiría importar infraestructura desde dominio sin cambiar el build. Los módulos hacen visibles varias dependencias indebidas durante la compilación.

**Consecuencias.** Hay más archivos de configuración y contratos que mantener. La separación Maven no verifica todas las reglas: un controller puede acceder a un puerto de repositorio de aplicación porque pertenece a una dependencia permitida, y app ve dependencias transitivas. Evitar esos usos requiere revisión de código; no alcanza con que compile.

**Evidencia actual.** El POM raíz declara los cinco módulos y sus POMs tienen las dependencias de la tabla. `SkillCoins` es un record del dominio sin imports de JPA ni UI. Los casos de uso y controllers todavía no están implementados.

## 2. Definir puertos en aplicación e inyectar sus implementaciones

**Problema.** Si un caso de uso crea directamente un repositorio JPA o un cliente de email, queda atado a esa tecnología y es más difícil aislarlo en las pruebas.

**Decisión.** Los controllers consumen puertos de entrada; los casos de uso los implementan y reciben por constructor los puertos de salida necesarios. Infraestructura implementa esas salidas. Dominio conserva las invariantes y aplicación coordina las operaciones.

**Alternativa.** Llamar a repositorios concretos desde los controllers reduciría clases, pero mezclaría entrada de usuario, persistencia y coordinación de negocio.

**Consecuencias.** Se pueden reemplazar adaptadores y probar casos de uso con dobles. Las interfaces deben representar necesidades concretas, sin crear contratos vacíos ni un repositorio genérico que exponga detalles de JPA.

**Evidencia actual.** Existen los paquetes `aplicacion.puertos.entrada`, `aplicacion.puertos.salida` y `aplicacion.casosdeuso` con su responsabilidad documentada. Los contratos de la tarea 89 son una propuesta; todavía no hay implementaciones Java de esos puertos.

## 3. Usar Java sin Spring y conectar los objetos en Main

**Problema.** La consigna pide Java con Hibernate/JPA, sin Spring Boot, y una interfaz que se ejecuta dentro de la misma aplicación.

**Decisión.** Mantener clases Java normales e inyección por constructor. Main será el punto de composición: seleccionará adaptadores y políticas, construirá casos de uso y controllers y los entregará al Mock UI Kit. No se incorporará Spring ni un contenedor de inyección para este alcance.

**Alternativa.** Un framework de inyección automatizaría el armado, pero agregaría configuración y mecanismos que no son necesarios para este TP. Spring Boot, además, está excluido por la consigna.

**Consecuencias.** Las dependencias son explícitas y se puede explicar cómo se arma la aplicación. Main deberá mantenerse ordenado y cerrar los recursos compartidos. Ser el punto de composición no significa que toda creación de objetos deba pasar por él: comandos, entidades y value objects se crean donde corresponda; una fábrica puede encapsular la selección de canales.

**Evidencia actual.** Main carga la configuración de persistencia, ejecuta migraciones, crea el `EntityManagerFactory` y registra su cierre. El armado de repositorios, casos de uso, controllers y UI sigue marcado como TODO. No hay dependencias de Spring en los POMs revisados.

## 4. Separar el modelo de dominio de las entidades JPA

**Problema.** Las necesidades de tablas, relaciones y carga diferida no deberían determinar cómo se expresan las reglas del dominio.

**Decisión.** Mantener las entidades JPA y sus mappers en infraestructura. Los repositorios convierten entre representación persistente y objetos del dominio. Los contratos de aplicación no devuelven entidades JPA a la UI.

**Alternativa.** Anotar directamente las entidades de dominio simplificaría el mapeo, pero haría que dominio dependiera de JPA y expondría decisiones de persistencia al modelo de negocio.

**Consecuencias.** Se duplica parte de la estructura de datos y hay que probar los mappers. A cambio, dominio queda independiente de Hibernate. Las consultas de lectura pueden devolver proyecciones/DTO para evitar cargar relaciones innecesarias y problemas N+1.

**Evidencia actual.** README establece esta convención y `persistence.xml` excluye clases no listadas. Todavía no se registraron entidades JPA de negocio; no se puede afirmar que sus mapeos o consultas estén resueltos.

## 5. Mantener el esquema con Flyway y validar con Hibernate

**Problema.** El equipo necesita reproducir la base sin depender de que Hibernate cree o altere tablas de forma implícita.

**Decisión.** Usar MySQL con migraciones versionadas de Flyway. Al iniciar, aplicar migraciones antes de crear la fábrica JPA; Hibernate valida la correspondencia entre entidades y esquema mediante `hbm2ddl.auto=validate`.

**Alternativa.** Generar el esquema con `create` o `update` sería cómodo para una prueba rápida, pero no ofrece un historial explícito de cambios y no cumple la condición de entrega de la consigna.

**Consecuencias.** Cada cambio persistente necesita su migración y las entidades deben coincidir con las tablas. No se edita una migración mergeada: se agrega otra versión y se coordinan los números con el equipo. Las pruebas con H2 pueden servir para casos acotados, pero no sustituyen comprobar restricciones, SQL y concurrencia en MySQL.

**Evidencia actual.** `ConfiguracionPersistencia.migrar()` usa Flyway; Main lo llama antes de crear JPA. `persistence.xml` configura validación. `V1__esquema_inicial.sql` solo crea `info_esquema`: el esquema de negocio sigue pendiente.

## 6. Coordinar transacciones desde los casos de uso

**Problema.** Aceptar una solicitud no puede reservar cupo sin guardar la contratación, y un pago no puede debitar una wallet sin acreditar la otra.

**Decisión propuesta.** Los casos de uso determinan el límite de cada operación atómica mediante un puerto de unidad de trabajo. Su adaptador JPA comparte el contexto transaccional con los repositorios participantes y revierte todos los cambios si falla la operación. El control concurrente debe proteger tanto cupos como saldos y pagos repetidos.

**Alternativa.** Confirmar cada repositorio por separado sería más fácil localmente, pero permitiría estados parciales que violan las reglas de negocio.

**Consecuencias.** Los repositorios no deben abrir y confirmar transacciones independientes en medio de una operación. Hay que validar rollback, último cupo concurrente y doble pago con persistencia real. Los eventos de éxito se publican después del commit; una entrega en memoria puede perderse ante una caída. La recuperación y los reintentos necesitan una decisión adicional al implementar notificaciones.

**Evidencia y estado.** La unidad de trabajo está bosquejada en la tarea 89 y los paquetes de aplicación describen la coordinación transaccional. `persistence.xml` usa `RESOURCE_LOCAL`. Todavía no existe la implementación ni pruebas que acrediten atomicidad o concurrencia.

## 7. Conectar la interfaz por controllers Java y simular servicios externos

**Problema.** La UI provista debe usar la lógica real sin conocer SQL, entidades JPA ni detalles de proveedores.

**Decisión.** Mantener controllers delgados y contratos con comandos y resultados simples. No agregar endpoints HTTP ni API REST. El userId proviene de identidad mock y se usa para validar permisos. Email, push y notificación interna se consumen por interfaces reemplazables.

**Alternativa.** Crear una API REST agregaría servidor, serialización y transporte sin necesidad: UI y lógica se ejecutan en el mismo proceso. Usar servicios reales de identidad o mensajería ampliaría el alcance de la consigna.

**Consecuencias.** La integración depende de conocer las interfaces del Mock UI Kit. El manejo uniforme de errores y los modelos de vista se implementarán en sus tareas específicas. Los mocks permiten demostrar variaciones de canal, pero no prueban entrega real a proveedores.

**Evidencia y estado.** El módulo controllers depende de aplicación y app está preparado para depender de controllers e infraestructura. No hay Mock UI Kit ni controllers funcionales en este checkout. La tarea 88 define el comportamiento de notificaciones y la 89 propone los puertos correspondientes.

## 8. Leer configuración fuera de las reglas de negocio

**Problema.** La conexión de base y las variaciones de políticas no deberían quedar dispersas como constantes dentro de casos de uso.

**Decisión.** Infraestructura lee configuración externa y Main entrega valores o implementaciones al armar la aplicación. Dominio recibe parámetros válidos, sin consultar variables de entorno. Se prevé configurar umbrales de cancelación y estrategia de recomendación en la tarea 101.

**Alternativa.** Leer variables desde cada entidad o caso de uso evitaría pasar parámetros, pero acoplaría las reglas al entorno y dificultaría reproducir pruebas.

**Consecuencias.** Hay que validar los valores antes de usarlos y definir defaults explícitos. Los valores de conexión actuales son para desarrollo local; no se deben registrar contraseñas en mensajes de error ni agregar credenciales reales al repositorio.

**Evidencia actual.** `ConfiguracionPersistencia.desdeEntorno()` lee `SKILLSWAP_DB_URL`, `SKILLSWAP_DB_USER` y `SKILLSWAP_DB_PASSWORD`, con defaults compatibles con el entorno local. Los umbrales y la estrategia activa todavía no se leen de configuración.

## Alcance de esta tarea

Quedan documentadas las decisiones, alternativas, consecuencias y referencias al código existente. No se cambian POMs, migraciones ni contratos del equipo. Esta es la fuente Markdown para el informe; el PDF de entrega se preparará al consolidar la documentación del hito.

La evidencia detallada de SOLID y GRASP corresponde a la tarea 91. El ensamblado final, las transacciones y las integraciones necesitan las implementaciones de sus responsables; este documento no las presenta como terminadas.
