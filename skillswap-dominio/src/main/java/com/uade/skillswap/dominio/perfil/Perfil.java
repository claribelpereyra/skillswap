package com.uade.skillswap.dominio.perfil;

import com.uade.skillswap.dominio.perfil.excepciones.PerfilInactivoException;
import com.uade.skillswap.dominio.perfil.excepciones.PerfilInvalidoException;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Agregado raíz del dominio que representa el perfil y reputación de un usuario.
 * <p>
 * Un usuario puede actuar tanto como cliente como proveedor de servicios (Usuario Dual),
 * manteniendo una identidad única, un único historial y una única reputación consolidada.
 */
public class Perfil {

    private Long id;
    private final String userId;
    private String nombreVisible;
    private String biografia;
    private ZoneId zonaHoraria;
    private TipoCuenta tipoCuenta;
    private double reputacion;
    private int cantidadCalificaciones;
    private final Set<Habilidad> habilidades;
    private final Set<String> intereses;
    private Disponibilidad disponibilidad;
    private boolean activo;
    private final LocalDateTime fechaCreacion;

    public Perfil(Long id,
                  String userId,
                  String nombreVisible,
                  String biografia,
                  ZoneId zonaHoraria,
                  TipoCuenta tipoCuenta) {
        validarDatosIniciales(userId, nombreVisible, tipoCuenta);
        this.id = id;
        this.userId = userId.trim();
        this.nombreVisible = nombreVisible.trim();
        this.biografia = biografia != null ? biografia.trim() : "";
        this.zonaHoraria = zonaHoraria != null ? zonaHoraria : ZoneId.systemDefault();
        this.tipoCuenta = tipoCuenta;
        this.reputacion = 0.0;
        this.cantidadCalificaciones = 0;
        this.habilidades = new HashSet<>();
        this.intereses = new HashSet<>();
        this.disponibilidad = Disponibilidad.NO_ESPECIFICADA;
        this.activo = true;
        this.fechaCreacion = LocalDateTime.now();
    }

    public Perfil(String userId,
                  String nombreVisible,
                  String biografia,
                  ZoneId zonaHoraria,
                  TipoCuenta tipoCuenta) {
        this(null, userId, nombreVisible, biografia, zonaHoraria, tipoCuenta);
    }

    private void validarDatosIniciales(String userId, String nombreVisible, TipoCuenta tipoCuenta) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new PerfilInvalidoException("El userId no puede ser nulo ni vacío.");
        }
        if (nombreVisible == null || nombreVisible.trim().isEmpty()) {
            throw new PerfilInvalidoException("El nombre visible no puede ser nulo ni vacío.");
        }
        if (tipoCuenta == null) {
            throw new PerfilInvalidoException("El tipo de cuenta no puede ser nulo.");
        }
    }

    // --- Comportamiento de Perfil e Información Personal (§4.1) ---

    public void actualizarDatos(String nuevoNombreVisible, String nuevaBiografia, ZoneId nuevaZonaHoraria) {
        exigirPerfilActivo();
        if (nuevoNombreVisible == null || nuevoNombreVisible.trim().isEmpty()) {
            throw new PerfilInvalidoException("El nuevo nombre visible no puede ser nulo ni vacío.");
        }
        this.nombreVisible = nuevoNombreVisible.trim();
        this.biografia = nuevaBiografia != null ? nuevaBiografia.trim() : "";
        if (nuevaZonaHoraria != null) {
            this.zonaHoraria = nuevaZonaHoraria;
        }
    }

    public void agregarHabilidad(Habilidad habilidad) {
        exigirPerfilActivo();
        if (habilidad != null) {
            this.habilidades.add(habilidad);
        }
    }

    public void quitarHabilidad(Habilidad habilidad) {
        exigirPerfilActivo();
        if (habilidad != null) {
            this.habilidades.remove(habilidad);
        }
    }

    public void agregarInteres(String interes) {
        exigirPerfilActivo();
        if (interes != null && !interes.trim().isEmpty()) {
            this.intereses.add(interes.trim());
        }
    }

    public void quitarInteres(String interes) {
        exigirPerfilActivo();
        if (interes != null) {
            this.intereses.remove(interes.trim());
        }
    }

    public void actualizarDisponibilidad(Disponibilidad nuevaDisponibilidad) {
        exigirPerfilActivo();
        this.disponibilidad = nuevaDisponibilidad != null ? nuevaDisponibilidad : Disponibilidad.NO_ESPECIFICADA;
    }

    // --- Baja Lógica (§5) ---

    /**
     * Da de baja lógica el perfil. No se borra físicamente la información histórica
     * (contrataciones, pagos, calificaciones) para preservar la trazabilidad del sistema.
     */
    public void desactivar() {
        this.activo = false;
    }

    public void reactivar() {
        this.activo = true;
    }

    public boolean estaActivo() {
        return this.activo;
    }

    public void exigirPerfilActivo() {
        if (!this.activo) {
            throw new PerfilInactivoException("La operación no puede realizarse porque el perfil está inactivo.");
        }
    }

    // --- Recálculo Automático de Reputación (§4.1, Caso 10) ---

    /**
     * Registra una nueva calificación verificada y recalcula la reputación media.
     * La reputación es inmutable por fuera de este flujo oficial del dominio.
     */
    public void registrarCalificacion(int puntaje) {
        if (puntaje < 1 || puntaje > 5) {
            throw new PerfilInvalidoException("El puntaje a registrar debe estar entre 1 y 5.");
        }
        double sumaTotal = (this.reputacion * this.cantidadCalificaciones) + puntaje;
        this.cantidadCalificaciones++;
        this.reputacion = Math.round((sumaTotal / this.cantidadCalificaciones) * 100.0) / 100.0;
    }

    // --- Soporte de Facade para Perfil Público (§4.1) ---

    public ResumenPerfilPublico obtenerResumenPublico() {
        return new ResumenPerfilPublico(
                this.userId,
                this.nombreVisible,
                this.biografia,
                this.tipoCuenta,
                this.reputacion,
                this.cantidadCalificaciones,
                Collections.unmodifiableSet(this.habilidades),
                this.disponibilidad,
                this.activo
        );
    }

    // --- Getters de Acceso de Dominio ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public String getNombreVisible() {
        return nombreVisible;
    }

    public String getBiografia() {
        return biografia;
    }

    public ZoneId getZonaHoraria() {
        return zonaHoraria;
    }

    public TipoCuenta getTipoCuenta() {
        return tipoCuenta;
    }

    public double getReputacion() {
        return reputacion;
    }

    public int getCantidadCalificaciones() {
        return cantidadCalificaciones;
    }

    public Set<Habilidad> getHabilidades() {
        return Collections.unmodifiableSet(habilidades);
    }

    public Set<String> getIntereses() {
        return Collections.unmodifiableSet(intereses);
    }

    public Disponibilidad getDisponibilidad() {
        return disponibilidad;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Perfil perfil = (Perfil) o;
        return Objects.equals(userId, perfil.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }
}
