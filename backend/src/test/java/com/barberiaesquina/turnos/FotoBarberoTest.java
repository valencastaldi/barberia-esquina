package com.barberiaesquina.turnos;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** [Extensión] Foto de perfil de los peluqueros. */
class FotoBarberoTest extends PruebaDeIntegracion {

    /** Los primeros bytes de un PNG real, más relleno: alcanza para que se reconozca como PNG. */
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 13, 'I', 'H', 'D', 'R'};

    /** token null = sin login. (En Spring 7 este builder no es el mismo tipo que usa conToken.) */
    private MockMultipartHttpServletRequestBuilder subir(Long idBarbero, byte[] contenido, String token) {
        var pedido = multipart(HttpMethod.POST, "/api/v1/barberos/{id}/foto", idBarbero)
                .file(new MockMultipartFile("archivo", "foto.png", "image/png", contenido));
        if (token != null) pedido.header("Authorization", "Bearer " + token);
        return pedido;
    }

    @Test
    void elDuenoSubeLaFotoYSeVeSinLogin() throws Exception {
        mvc.perform(get("/api/v1/barberos")).andExpect(jsonPath("$[?(@.nombre == 'Santiago')].foto", contains(nullValue())));

        String dueno = login("agustin@test.com");
        mvc.perform(subir(santiago.getId(), PNG, dueno))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.foto", startsWith("/api/v1/barberos/" + santiago.getId() + "/foto?v=")));

        // La lista pública ya trae la URL, y la imagen se sirve sin login con su tipo real.
        mvc.perform(get("/api/v1/barberos"))
                .andExpect(jsonPath("$[?(@.nombre == 'Santiago')].foto", contains(startsWith("/api/v1/barberos/"))));
        mvc.perform(get("/api/v1/barberos/{id}/foto", santiago.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(PNG))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));

        mvc.perform(conToken(delete("/api/v1/barberos/{id}/foto", santiago.getId()), dueno))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/barberos/{id}/foto", santiago.getId())).andExpect(status().isNotFound());
    }

    @Test
    void soloSeAceptanImagenesDeVerdad() throws Exception {
        String dueno = login("agustin@test.com");
        // Un SVG o un HTML con extensión .png no pasa: se mira el contenido, no el nombre.
        byte[] svg = "<svg xmlns='http://www.w3.org/2000/svg'><script>alert(1)</script></svg>".getBytes();
        mvc.perform(subir(santiago.getId(), svg, dueno))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("Subí una imagen JPG, PNG o WEBP"));
    }

    @Test
    void unBarberoNoCambiaFotos() throws Exception {
        String barbero = login("santiago@test.com");
        mvc.perform(subir(santiago.getId(), PNG, barbero)).andExpect(status().isForbidden());
        mvc.perform(subir(santiago.getId(), PNG, null)).andExpect(status().isUnauthorized());
    }
}
