# Tarea 101 — Configuración externa

`ConfiguracionFuncional.desdeEntorno()` lee las siguientes variables en infraestructura. También permite construir la configuración desde un mapa para probar sin modificar el entorno del equipo.

| Variable | Default local propuesto | Validación |
| --- | --- | --- |
| `SKILLSWAP_CANCELACION_TOTAL_HORAS` | 48 | Horas enteras, mayor que el umbral parcial |
| `SKILLSWAP_CANCELACION_PARCIAL_HORAS` | 24 | Horas enteras no negativas |
| `SKILLSWAP_RECOMENDACION` | AFINIDAD | AFINIDAD o POPULARIDAD; admite espacios externos y minúsculas |

Los defaults son una propuesta para desarrollo: la consigna exige anticipación configurable pero no fija 48/24 horas. Una variable ausente o en blanco usa el default; un valor inválido se rechaza. Los umbrales se entregan como `Duration` y la estrategia como una selección tipada, sin cargar clases por nombre ni decidir reglas dentro de la configuración.

## Integración pendiente

En Main se deberá leer esta configuración y pasar los umbrales a la política de cancelación de Tomi, y seleccionar su estrategia concreta de afinidad o popularidad. Los casos de uso seguirán recibiendo abstracciones del dominio, sin importar esta clase de infraestructura ni consultar `System.getenv()`.

Todavía no se invoca la configuración desde Main porque esas implementaciones no existen en el checkout. No se implementan aquí los porcentajes ni las condiciones de borde de cancelación: se acordarán con la política de Tomi para evitar dos reglas diferentes. La tarea queda avanzada en lectura y validación; falta demostrar el cambio de comportamiento real con esa política y con recomendación.
