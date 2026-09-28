package com.barberiaesquina.turnos.servicio;

import com.barberiaesquina.turnos.modelo.Barbero;
import com.barberiaesquina.turnos.modelo.BarberoFoto;
import com.barberiaesquina.turnos.repositorio.BarberoFotoRepositorio;
import com.barberiaesquina.turnos.repositorio.BarberoRepositorio;
import com.barberiaesquina.turnos.servicio.excepcion.NoEncontradoException;
import com.barberiaesquina.turnos.servicio.excepcion.ReglaNegocioException;
import com.barberiaesquina.turnos.web.dto.BarberoDtos;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/** [Extensión] Foto de perfil de los peluqueros. */
@Service
@Transactional
public class FotoBarberoServicio {

    /** El front la manda recortada a 400x400 (pesa unos 30-60 KB); esto es un tope de seguridad. */
    public static final int TAMANO_MAXIMO = 2 * 1024 * 1024;

    private final BarberoRepositorio barberos;
    private final BarberoFotoRepositorio fotos;
    private final Calendario calendario;

    public FotoBarberoServicio(BarberoRepositorio barberos, BarberoFotoRepositorio fotos, Calendario calendario) {
        this.barberos = barberos;
        this.fotos = fotos;
        this.calendario = calendario;
    }

    public record Imagen(byte[] contenido, String tipo) {}

    @Transactional(readOnly = true)
    public Imagen ver(Long idBarbero) {
        return fotos.findById(idBarbero)
                .map(f -> new Imagen(f.getContenido(), f.getTipo()))
                .orElseThrow(() -> new NoEncontradoException("El peluquero no tiene foto"));
    }

    /** Devuelve la URL nueva de la foto. */
    public String guardar(Long idBarbero, byte[] contenido) {
        Barbero b = buscar(idBarbero);
        if (contenido == null || contenido.length == 0) throw new ReglaNegocioException("El archivo está vacío");
        if (contenido.length > TAMANO_MAXIMO) throw new ReglaNegocioException("La foto no puede pesar más de 2 MB");

        String tipo = tipoReal(contenido);
        BarberoFoto foto = fotos.findById(idBarbero).orElseGet(() -> {
            BarberoFoto nueva = new BarberoFoto();
            nueva.setIdBarbero(idBarbero);
            return nueva;
        });
        foto.setContenido(contenido);
        foto.setTipo(tipo);
        fotos.save(foto);
        b.setFotoActualizada(calendario.ahora());
        return BarberoDtos.urlFoto(b);
    }

    public void quitar(Long idBarbero) {
        Barbero b = buscar(idBarbero);
        fotos.deleteById(idBarbero);
        b.setFotoActualizada(null);
    }

    /**
     * El tipo se decide por los primeros bytes del archivo, no por lo que declara el navegador:
     * así no se puede subir otra cosa (un SVG con script, un HTML) renombrada como imagen.
     */
    static String tipoReal(byte[] b) {
        if (empieza(b, 0xFF, 0xD8, 0xFF)) return "image/jpeg";
        if (empieza(b, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)) return "image/png";
        if (b.length >= 12
                && "RIFF".equals(new String(b, 0, 4, StandardCharsets.US_ASCII))
                && "WEBP".equals(new String(b, 8, 4, StandardCharsets.US_ASCII))) return "image/webp";
        throw new ReglaNegocioException("Subí una imagen JPG, PNG o WEBP");
    }

    private static boolean empieza(byte[] b, int... firma) {
        if (b.length < firma.length) return false;
        byte[] esperada = new byte[firma.length];
        for (int i = 0; i < firma.length; i++) esperada[i] = (byte) firma[i];
        return Arrays.equals(Arrays.copyOf(b, firma.length), esperada);
    }

    private Barbero buscar(Long id) {
        return barberos.findById(id).orElseThrow(() -> NoEncontradoException.de("Peluquero", id));
    }
}
