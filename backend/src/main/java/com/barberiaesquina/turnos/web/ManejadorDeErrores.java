package com.barberiaesquina.turnos.web;

import com.barberiaesquina.turnos.servicio.excepcion.ConflictoException;
import com.barberiaesquina.turnos.servicio.excepcion.CredencialesInvalidasException;
import com.barberiaesquina.turnos.servicio.excepcion.NoEncontradoException;
import com.barberiaesquina.turnos.servicio.excepcion.ReglaNegocioException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Todos los errores salen en formato Problem Details (RFC 9457):
 * {"title": ..., "status": ..., "detail": "mensaje para mostrar"}.
 * Los de validación agregan "errores": {campo: mensaje}.
 */
@RestControllerAdvice
public class ManejadorDeErrores extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ManejadorDeErrores.class);

    @ExceptionHandler(NoEncontradoException.class)
    ProblemDetail noEncontrado(NoEncontradoException e) {
        return problema(HttpStatus.NOT_FOUND, "No encontrado", e.getMessage());
    }

    @ExceptionHandler(ConflictoException.class)
    ProblemDetail conflicto(ConflictoException e) {
        return problema(HttpStatus.CONFLICT, "Conflicto", e.getMessage());
    }

    @ExceptionHandler(ReglaNegocioException.class)
    ProblemDetail reglaNegocio(ReglaNegocioException e) {
        return problema(HttpStatus.UNPROCESSABLE_CONTENT, "No se puede realizar", e.getMessage());
    }

    @ExceptionHandler(CredencialesInvalidasException.class)
    ProblemDetail credenciales(CredencialesInvalidasException e) {
        return problema(HttpStatus.UNAUTHORIZED, "No autorizado", e.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail sinPermiso(AccessDeniedException e) {
        return problema(HttpStatus.FORBIDDEN, "Sin permiso", "No tenés permiso para hacer esto");
    }

    /** Última defensa ante carreras que llegan a la base (email repetido, encuesta doble, etc.). */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail integridad(DataIntegrityViolationException e) {
        log.warn("Violación de integridad: {}", e.getMostSpecificCause().getMessage());
        return problema(HttpStatus.CONFLICT, "Conflicto", "El pedido choca con datos ya guardados");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        Map<String, String> errores = new LinkedHashMap<>();
        for (FieldError campo : ex.getBindingResult().getFieldErrors()) {
            errores.putIfAbsent(campo.getField(), campo.getDefaultMessage());
        }
        ProblemDetail p = problema(HttpStatus.BAD_REQUEST, "Datos inválidos", "Revisá los datos marcados");
        p.setProperty("errores", errores);
        return ResponseEntity.badRequest().body(p);
    }

    private static ProblemDetail problema(HttpStatus status, String titulo, String detalle) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(status, detalle);
        p.setTitle(titulo);
        return p;
    }
}
