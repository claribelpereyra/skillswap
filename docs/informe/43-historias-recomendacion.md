# Caso 43: Historias de recomendación

## HU-43.1 Recomendar por afinidad de habilidades

**Como** usuario, **quiero** recibir publicaciones relacionadas con las habilidades e intereses de mi perfil, **para** encontrar ofertas relevantes sin tener que ingresar esas etiquetas en cada búsqueda.

### Criterios de aceptación

- Se consideran las habilidades e intereses del perfil activo del usuario autenticado.
- Las coincidencias se normalizan y comparan con las etiquetas de las publicaciones.
- Se prioriza la publicación con más coincidencias; los empates se resuelven por reputación, fecha de publicación e ID, en ese orden y de mayor a menor.
- La estrategia ordena todos los candidatos antes de aplicar la página solicitada.
- Un perfil inexistente o inactivo no aporta coincidencias.

## HU-43.2 Recomendar por popularidad reciente

**Como** usuario, **quiero** ver primero las publicaciones que generaron más contrataciones recientemente, **para** encontrar ofertas con actividad actual.

### Criterios de aceptación

- La ventana temporal es configurable y se calcula con un reloj inyectado.
- Se cuentan contrataciones aceptadas, confirmadas, en curso o completadas dentro de la ventana; se excluyen pendientes, rechazadas y canceladas.
- Los conteos de las publicaciones candidatas se consultan en lote.
- Se prioriza el mayor conteo; los empates se resuelven por reputación e ID, en ese orden y de mayor a menor.
- La selección de estrategia no altera los demás filtros ni el orden solicitado para la búsqueda común.

## Evidencia y pruebas

- `RecommendPublicationsServiceTest`: normalización de habilidades/intereses, ranking por conteos recientes, aplicación del ranking antes de paginar y estrategia explícita.
- `JpaRecommendationMetadataQueryTest`: carga solo el perfil activo y conteo por lote con exclusión de estados y fechas fuera de la ventana.
- `RecommendationControllerTest`: afinidad como estrategia predeterminada, selección de popularidad reciente, identidad activa y validación del formulario.
