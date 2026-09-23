package com.barberiaesquina.turnos.web.dto;

import jakarta.validation.constraints.NotNull;

/** Cuerpo de los PATCH .../estado que activan o desactivan algo. */
public record ActivoPedido(@NotNull Boolean activo) {}
