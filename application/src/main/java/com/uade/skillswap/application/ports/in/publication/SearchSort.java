package com.uade.skillswap.application.ports.in.publication;

// Orden de los resultados de búsqueda.
public enum SearchSort {

    // Relevancia respecto del texto buscado. Puntaje por publicación:
    //   +3 si el título contiene el texto,
    //   +2 si alguna etiqueta contiene el texto,
    //   +1 si la descripción contiene el texto
    // (sin distinguir mayúsculas). A igual puntaje, desempata la mejor reputación del autor y después
    // la publicación más reciente. Sin texto no hay nada que puntuar, así que se ordena como BEST_REPUTATION.
    RELEVANCE,

    // Mejor reputación del autor primero; a igual reputación, la más reciente.
    BEST_REPUTATION,

    // Menor precio base primero; a igual precio, la más reciente.
    LOWEST_PRICE,

    // Publicada más recientemente primero. Cuenta la ÚLTIMA vez que pasó a PUBLISHED: si el autor pausa y
    // reanuda, vuelve a subir, porque para quien busca es una oferta que se "reactivó".
    MOST_RECENT,

    // Recomendación por afinidad de habilidades: primero aparecen las publicaciones con más coincidencias
    // con la habilidad elegida o con el perfil del usuario; a igualdad, mejor reputación y luego más recientes.
    SKILL_MATCH,
    SKILL_AFFINITY,
    AFFINITY,

    // Recomendación por popularidad reciente: prioriza publicaciones con mayor actividad reciente, y como
    // proxy disponible en esta iteración usa la fecha de publicación más reciente, desempatando por reputación.
    RECENT_POPULARITY,
    POPULARITY_RECENT,
    MOST_POPULAR_RECENT
}
