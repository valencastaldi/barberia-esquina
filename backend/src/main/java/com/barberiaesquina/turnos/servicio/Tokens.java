package com.barberiaesquina.turnos.servicio;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/** Tokens de los links que llegan por email (cancelar turno, responder encuesta). */
public final class Tokens {

    private static final SecureRandom AZAR = new SecureRandom();

    private Tokens() {}

    /** 32 bytes aleatorios en Base64 URL: 43 caracteres, imposible de adivinar. */
    public static String nuevo() {
        byte[] bytes = new byte[32];
        AZAR.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** Comparación en tiempo constante, para no filtrar información por el tiempo de respuesta. */
    public static boolean iguales(String a, String b) {
        if (a == null || b == null) return false;
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
