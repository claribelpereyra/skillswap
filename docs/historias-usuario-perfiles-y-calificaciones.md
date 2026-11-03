# Historias de Usuario: Perfiles, Calificaciones y Reputación

**Materia:** Procesos de Desarrollo de Software (PDS - UADE)  
**Responsable:** Andy (§4.1, §4.4, §5, §12)  
**Casos de aceptación cubiertos:** Caso 10 y tests negativos de calificación  

---

## Módulo: Perfiles de Usuario (§4.1)

### HU-01: Creación de Perfil Inicial
* **Como:** Usuario autenticado en el sistema (mediante el servicio de identidad simulado).  
* **Quiero:** Registrar mi perfil completando mis datos personales y tipo de cuenta.  
* **Para:** Poder publicar ofertas en la plataforma o solicitar servicios de otros miembros.  

#### Criterios de Aceptación:
* **Escenario 1: Creación exitosa de perfil profesional**
  * **Dado** un identificador de usuario válido `userId = "usr-100"` no registrado previamente,
  * **Cuando** solicito la creación de perfil con nombre visible `"Lucas Gómez"`, biografía `"Desarrollador Java"`, zona horaria `"America/Argentina/Buenos_Aires"` y tipo de cuenta `PROFESIONAL`,
  * **Entonces** el perfil se crea en estado `ACTIVO`, con reputación inicial en `0.0`, `0` calificaciones registradas y fecha de creación actual.
* **Escenario 2: Creación exitosa de perfil organización**
  * **Dado** un `userId = "org-200"` válido,
  * **Cuando** solicito la creación de perfil con nombre `"Tech UADE Org"` y tipo `ORGANIZACION`,
  * **Entonces** el perfil se crea correctamente con su tipo corporativo asignado.
* **Escenario 3: Rechazo por datos obligatorios vacíos o nulos**
  * **Dado** un `userId` válido,
  * **Cuando** intento crear un perfil con nombre visible nulo, vacío o compuesto solo por espacios en blanco,
  * **Entonces** el dominio rechaza la operación lanzando una excepción `PerfilInvalidoException`.

---

### HU-02: Gestión de Habilidades e Intereses
* **Como:** Usuario con perfil activo.  
* **Quiero:** Agregar y remover habilidades técnicas e intereses personales en mi perfil.  
* **Para:** Que el motor de recomendación por afinidad me sugiera publicaciones relevantes y los demás usuarios conozcan mis competencias.  

#### Criterios de Aceptación:
* **Escenario 1: Incorporación de habilidades e intereses**
  * **Dado** un perfil activo,
  * **Cuando** agrego las habilidades `"Java"`, `"Spring"` y el interés `"Arquitectura de Software"`,
  * **Entonces** quedan vinculadas a mi perfil sin duplicados (conjunto unificado insensible a mayúsculas/minúsculas).
* **Escenario 2: Eliminación de habilidad**
  * **Dado** un perfil con la habilidad `"Python"`,
  * **Cuando** solicito remover `"Python"`,
  * **Entonces** la habilidad se retira del perfil manteniéndose el resto inalterado.

---

### HU-03: Baja Lógica del Perfil (§5)
* **Como:** Usuario que desea retirarse temporal o definitivamente de la plataforma.  
* **Quiero:** Dar de baja mi perfil de forma lógica.  
* **Para:** Que mis publicaciones no reciban nuevas contrataciones, pero mis registros históricos, comprobantes y calificaciones no se pierdan.  

#### Criterios de Aceptación:
* **Escenario 1: Desactivación lógica preservando auditoría**
  * **Dado** un perfil activo con contrataciones previas y calificaciones acumuladas,
  * **Cuando** el usuario solicita la baja de su cuenta,
  * **Entonces** el perfil pasa a estado inactivo (`activo = false`),
  * **Y** se preservan intactas en la base de datos todas las contrataciones pasadas, movimientos de saldo y calificaciones históricas.
* **Escenario 2: Restricción operativa en perfil inactivo**
  * **Dado** un perfil que ha sido dado de baja,
  * **Cuando** intenta realizar una operación que requiera estar activo (ej. crear una nueva publicación o solicitar un servicio),
  * **Entonces** el dominio rechaza la operación lanzando `PerfilInactivoException`.

---

### HU-04: Visualización de Perfil Público (Patrón Facade - §4.1)
* **Como:** Usuario de la plataforma.  
* **Quiero:** Consultar el perfil público de otro miembro.  
* **Para:** Evaluar su reputación, trayectoria, habilidades y publicaciones vigentes antes de contratarlo.  

#### Criterios de Aceptación:
* **Escenario 1: Consulta unificada de perfil público**
  * **Dado** un usuario proveedor con 5 calificaciones verificadas y 2 publicaciones activas,
  * **Cuando** un cliente consulta su perfil público mediante el *Facade*,
  * **Entonces** recibe en una única estructura: nombre visible, biografía, tipo de cuenta, promedio de reputación, total de calificaciones, lista de habilidades y sus publicaciones disponibles.

---

## Módulo: Calificaciones y Reputación (§4.4, §5)

### HU-05: Registro de Calificación Verificada (Caso 10)
* **Como:** Participante (cliente o proveedor) de una contratación completada.  
* **Quiero:** Calificar a la contraparte con un puntaje del 1 al 5 y un comentario explicativo.  
* **Para:** Retroalimentar el desempeño del usuario y contribuir a la reputación pública de la comunidad.  

#### Criterios de Aceptación:
* **Escenario 1: Calificación válida de cliente a proveedor**
  * **Dado** una contratación entre el cliente `"usr-cliente"` y el proveedor `"usr-proveedor"` con estado `COMPLETADA`,
  * **Y** el cliente no ha emitido una calificación previa para esta contratación,
  * **Cuando** el cliente registra una calificación con puntaje `5` y comentario `"Excelente mentoría de patrones"`,
  * **Entonces** la calificación queda registrada con fecha/hora actual,
  * **Y** se dispara automáticamente el recálculo de la reputación del proveedor.
* **Escenario 2: Calificación válida de proveedor a cliente**
  * **Dado** la misma contratación `COMPLETADA`,
  * **Cuando** el proveedor califica al cliente con puntaje `4` y comentario `"Muy puntual y comprometido"`,
  * **Entonces** la calificación se guarda y se actualiza la reputación del cliente.

---

### HU-06: Validaciones y Rechazos de Calificación (Tests Negativos Obligatorios - §12)
* **Como:** Sistema de dominio de SkillSwap.  
* **Quiero:** Validar estrictamente la legitimidad de cada intento de calificación.  
* **Para:** Garantizar la autenticidad del sistema de reputación y prevenir fraude o manipulación.  

#### Criterios de Aceptación:
* **Escenario 1: Rechazo por contratación no completada**
  * **Dado** una contratación en estado `PENDIENTE`, `ACEPTADA`, `CONFIRMADA` o `EN_CURSO`,
  * **Cuando** una de las partes intenta calificar,
  * **Entonces** el sistema rechaza la operación indicando que solo se admiten calificaciones tras la finalización del servicio.
* **Escenario 2: Rechazo por intento de autocalificación**
  * **Dado** cualquier usuario,
  * **Cuando** intenta registrar una calificación donde `autorId` es idéntico a `destinatarioId`,
  * **Entonces** el dominio lanza `CalificacionInvalidaException` con el mensaje descriptivo `"Un usuario no puede calificarse a sí mismo"`.
* **Escenario 3: Rechazo por puntaje fuera de escala**
  * **Dado** un participante habilitado,
  * **Cuando** envía un puntaje menor a 1 (ej. `0`, `-2`) o mayor a 5 (ej. `6`, `10`),
  * **Entonces** el dominio lanza `CalificacionInvalidaException` exigiendo un valor en el rango `[1..5]`.
* **Escenario 4: Rechazo por duplicidad de calificación**
  * **Dado** un participante que ya calificó una contratación particular,
  * **Cuando** intenta emitir una segunda calificación para la misma contratación y destinatario,
  * **Entonces** la persistencia/caso de uso rechaza la operación garantizando unicidad estricta (1 calificación por parte y contratación).
* **Escenario 5: Rechazo por actor no participante**
  * **Dado** un usuario ajeno a la contratación,
  * **Cuando** intenta enviar una calificación sobre dicha contratación,
  * **Entonces** la operación es rechazada por falta de autorización.

---

### HU-07: Recálculo Matemático de Reputación (§4.1, Caso 10)
* **Como:** Perfil de usuario calificado.  
* **Quiero:** Que mi reputación se actualice automáticamente con cada nueva calificación verificada.  
* **Para:** Reflejar con exactitud matemática el promedio histórico de mis servicios.  

#### Criterios de Aceptación:
* **Escenario 1: Promedio ponderado de calificaciones**
  * **Dado** un perfil con `0` calificaciones y reputación `0.0`,
  * **Cuando** recibe su primera calificación con puntaje `5`,
  * **Entonces** su reputación pasa a ser `5.0` y la cantidad de calificaciones es `1`.
  * **Cuando** recibe una segunda calificación con puntaje `4`,
  * **Entonces** su reputación se actualiza a `4.5` y la cantidad es `2`.
  * **Cuando** recibe una tercera calificación con puntaje `3`,
  * **Entonces** su reputación se actualiza a `4.0` ($\frac{5 + 4 + 3}{3} = 4.0$) y la cantidad es `3`.
* **Escenario 2: Inmutabilidad manual**
  * **Dado** un perfil de usuario,
  * **Entonces** no existe ningún método setter directo para alterar el campo reputación; cualquier cambio proviene forzosamente de una calificación legítima verificada.
