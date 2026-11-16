# Validación de UI y MySQL

Fecha: 16/11/2026. Java 17 y MySQL Community Server 8.4.13 local, iniciado desde el ZIP oficial sin instalar un servicio Windows. Servidor de desarrollo en 127.0.0.1:3307. Los datos y binarios locales no se incluyen en Git.

## Pruebas reproducibles

La suite normal `mvn clean verify` usa H2 y conserva el control JaCoCo de 80 % en domain/application.

Para probar MySQL, se configura una URL de servidor **sin nombre de base**, terminada en `/`:

```powershell
$env:SKILLSWAP_MYSQL_TEST_URL='jdbc:mysql://127.0.0.1:3307/'
$env:SKILLSWAP_MYSQL_TEST_USER='root'
$env:SKILLSWAP_MYSQL_TEST_PASSWORD='<clave local>'
mvn -B -pl app -am '-Dtest=FullJourneyIntegrationTest,RealUiControllersIntegrationTest' -Dsurefire.failIfNoSpecifiedTests=false test
```

El usuario de pruebas necesita CREATE/DROP DATABASE. TestDatabase genera un nombre único `skillswap_it_<uuid>`, aplica las siete migraciones Flyway y Hibernate validate, y elimina solo esa base al cerrar. No trunca, migra ni elimina la base de la aplicación. Las variables de prueba son distintas de SKILLSWAP_DB_*. La colación usada es utf8mb4_0900_as_cs para conservar la igualdad sensible a mayúsculas de UserId. docker-compose fija esa colación para bases nuevas.

Se ejecutaron los tres recorridos FullJourneyIntegrationTest y los dos RealUiControllersIntegrationTest sobre MySQL: cinco pruebas, sin fallos ni errores. Cubren pago y doble pago, reprogramación, finalización, calificación única y reputación, reintegro configurable, notificaciones a dos canales, permisos, rollback externo, datos de demo idempotentes, paginación y el contrato de la interfaz.

La UI se inició también contra la base local skillswap y se renderizó la pantalla de inicio con datos reales. El JAR incluye dependencias y metadatos de servicios para Hibernate/JDBC/Flyway. La integración no requiere copiar JARs manualmente.

El workflow CI conserva build/H2 y agrega un job independiente MySQL 8.4 con las mismas cinco pruebas. Las pruebas de repositorios de otras áreas siguen usando H2; no se afirma haber portado toda la suite de infraestructura a MySQL.

## Arranque local

- Con Docker: `docker compose up -d`; conexión habitual en 3306.
- En la máquina preparada de Ian: `scripts/start-local-mysql.ps1` reutiliza la instancia portable y su configuración en la carpeta local de herramientas, sin servicio del sistema.
- `scripts/run-ui.ps1` abre el JAR con --demo y conexión local 3307; permite cambiar DatabaseUrl, JavaHome y UserId.

Los scripts no descargan ni instalan software. En otra máquina se debe preparar MySQL (por ejemplo Docker) y usar la conexión correspondiente. La carpeta de herramientas portable local no es parte de la entrega del repositorio.

## Límites conservados

Login y canales simulados, sin dinero real. Avisos síncronos después del commit con registro de fallos y sin outbox/reintento automático. El servicio de wallet actual carga el ledger completo; el adaptador calcula sus saldos por movimiento y pagina lo presentado. El kit usa int para montos: valores fuera de ese rango se rechazan explícitamente. Las fechas se muestran en SKILLSWAP_UI_ZONE (Buenos Aires por defecto).
