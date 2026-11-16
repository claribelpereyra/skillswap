# Tarea 101 — Configuración externa integrada

`ConfiguracionFuncional` lee y valida las variables; `CompositionRoot` consume el resultado y entrega estrategias a los servicios. Los casos de uso no consultan `System.getenv()`.

| Variable | Default | Uso real |
| --- | --- | --- |
| `SKILLSWAP_CANCELACION_TOTAL_HORAS` | 48 | Umbral de reintegro del 100 % en `RefundPolicy` |
| `SKILLSWAP_CANCELACION_PARCIAL_HORAS` | 24 | Umbral del 50 %; por debajo se aplica 0 % |
| `SKILLSWAP_RECOMENDACION` | AFINIDAD | Estrategia predeterminada si el formulario no selecciona otra: AFINIDAD o POPULARIDAD |

Los umbrales se expresan en horas enteras no negativas y el total debe ser mayor que el parcial. Una variable ausente o vacía usa el default. Una configuración inválida falla antes de abrir la base. 48/24 son decisiones locales configurables, no valores exigidos por la consigna. La ventana de popularidad usa 30 días en el root.

El formulario puede elegir explícitamente `SKILL_AFFINITY` o `RECENT_POPULARITY` para demostrar el intercambio de estrategias. Esa selección tiene prioridad sobre el default externo.

Evidencia: `ConfiguracionFuncionalTest` cubre lectura y validación; `RecommendationControllerTest` verifica el default inyectado; `FullJourneyIntegrationTest` configura 240/24 y comprueba un reintegro real del 50 % cinco días antes, con saldos persistidos y aviso de cancelación.
