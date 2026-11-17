# Validación de UI y MySQL

Java 17 y MySQL 8.4. La validación inicial se hizo el 16/11/2026 con MySQL Community Server 8.4.13. Para reproducirla se usa el servicio MySQL definido en Docker Compose (puerto 3306).

## Pruebas reproducibles

La suite normal `mvn clean verify` usa H2 y conserva el control JaCoCo de 80 % en domain/application.

Para probar MySQL, se configura una URL de servidor **sin nombre de base**, terminada en `/`:

```powershell
docker compose up -d
$env:SKILLSWAP_MYSQL_TEST_URL='jdbc:mysql://127.0.0.1:3306/'
$env:SKILLSWAP_MYSQL_TEST_USER='root'
$env:SKILLSWAP_MYSQL_TEST_PASSWORD='root'
mvn -B -pl app -am '-Dtest=FullJourneyIntegrationTest,RealUiControllersIntegrationTest' -Dsurefire.failIfNoSpecifiedTests=false test
```

El usuario de pruebas necesita CREATE/DROP DATABASE. TestDatabase genera un nombre único `skillswap_it_<uuid>`, aplica las siete migraciones Flyway y Hibernate validate, y elimina solo esa base al cerrar. No trunca, migra ni elimina la base de la aplicación. Las variables de prueba son distintas de SKILLSWAP_DB_*. La colación usada es utf8mb4_0900_as_cs para conservar la igualdad sensible a mayúsculas de UserId. docker-compose fija esa colación para bases nuevas.

Se ejecutaron los tres recorridos FullJourneyIntegrationTest y los dos RealUiControllersIntegrationTest sobre MySQL: cinco pruebas, sin fallos ni errores. Cubren pago y doble pago, reprogramación, finalización, calificación única y reputación, reintegro configurable, notificaciones a dos canales, permisos, rollback externo, datos de demo idempotentes, paginación y el contrato de la interfaz.

La UI se inició también contra la base local skillswap y se renderizó la pantalla de inicio con datos reales. El JAR incluye dependencias y metadatos de servicios para Hibernate/JDBC/Flyway. La integración no requiere copiar JARs manualmente.

El workflow CI conserva build/H2 y agrega un job independiente MySQL 8.4 con las mismas cinco pruebas. Las pruebas de repositorios de otras áreas siguen usando H2; no se afirma haber portado toda la suite de infraestructura a MySQL.

## Arranque local

```bash
docker compose up -d
mvn clean verify
java -jar app/target/app-0.1.0-SNAPSHOT.jar --demo
```

Se requieren JDK 17 y Maven 3.9+. La conexión por defecto coincide con el compose (localhost:3306, base/usuario/clave skillswap). Esperar a que MySQL acepte conexiones antes de iniciar el JAR. `--demo` es la única carga de ejemplo: crea Mateo/Sofía y no duplica datos ni reinicia saldos al volver a ejecutarla. Sin esa opción se usan los datos existentes.

## Límites conservados

Login y canales simulados, sin dinero real. Avisos síncronos después del commit con registro de fallos y sin outbox/reintento automático. La consulta de movimientos se pagina en SQL; el adaptador recorre esas páginas para calcular el saldo acumulado de cada fila. El kit usa int para montos: valores fuera de ese rango se rechazan explícitamente. Las fechas se muestran en SKILLSWAP_UI_ZONE (Buenos Aires por defecto).
