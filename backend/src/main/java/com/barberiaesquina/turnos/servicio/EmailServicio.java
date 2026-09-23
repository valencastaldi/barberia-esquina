package com.barberiaesquina.turnos.servicio;

import com.barberiaesquina.turnos.config.AppProperties;
import com.barberiaesquina.turnos.servicio.EventosTurno.DatosEmail;
import jakarta.mail.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.util.HtmlUtils;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Emails al cliente. Se mandan después del commit y en otro hilo: la reserva
 * responde enseguida y nunca sale un email de algo que no se guardó.
 * RNF: los emails tienen que llegar en menos de 2 minutos.
 */
@Service
public class EmailServicio {

    private static final Logger log = LoggerFactory.getLogger(EmailServicio.class);
    private static final Locale AR = Locale.forLanguageTag("es-AR");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", AR);

    private final ObjectProvider<JavaMailSender> mailSender;
    private final AppProperties props;

    public EmailServicio(ObjectProvider<JavaMailSender> mailSender, AppProperties props) {
        this.mailSender = mailSender;
        this.props = props;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alReservar(EventosTurno.TurnoReservado evento) {
        String link = props.frontendUrl() + "/cancelar/" + evento.tokenCancelacion();
        enviar(evento.datos(), "Tu turno está confirmado",
                "<p>¡Hola " + esc(evento.datos().nombreCliente()) + "! Tu turno quedó reservado.</p>"
                + resumen(evento.datos())
                + "<p>Si no podés venir, cancelalo desde este link (vence a las "
                + props.turnos().cancelacionHoras() + " h):<br>"
                + "<a href=\"" + link + "\">Cancelar mi turno</a></p>");
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alCompletar(EventosTurno.TurnoCompletado evento) {
        String link = props.frontendUrl() + "/encuesta/" + evento.idTurno() + "?token=" + evento.tokenEncuesta();
        enviar(evento.datos(), "¿Cómo te atendimos?",
                "<p>¡Gracias por venir, " + esc(evento.datos().nombreCliente()) + "!</p>"
                + "<p>¿Nos contás qué te pareció tu " + esc(evento.datos().servicio()) + " con "
                + esc(evento.datos().barbero()) + "? Es un minuto.</p>"
                + "<p><a href=\"" + link + "\">Calificar mi turno</a></p>");
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alCancelar(EventosTurno.TurnoCancelado evento) {
        enviar(evento.datos(), "Tu turno fue cancelado",
                "<p>Hola " + esc(evento.datos().nombreCliente()) + ", este turno quedó cancelado:</p>"
                + resumen(evento.datos())
                + "<p>Podés reservar otro cuando quieras en <a href=\"" + props.frontendUrl() + "\">"
                + props.frontendUrl() + "</a>.</p>");
    }

    private void enviar(DatosEmail datos, String asunto, String cuerpo) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (!props.mail().habilitado() || sender == null) {
            log.info("[email deshabilitado] Para {} — {}", datos.email(), asunto);
            return;
        }
        try {
            var mensaje = sender.createMimeMessage();
            var helper = new MimeMessageHelper(mensaje, "UTF-8");
            helper.setFrom(props.mail().remitente());
            helper.setTo(datos.email());
            helper.setSubject(asunto + " · Barbería Esquina");
            helper.setText(plantilla(cuerpo), true);
            sender.send(mensaje);
            log.info("Email '{}' enviado a {}", asunto, datos.email());
        } catch (MessagingException | MailException e) {
            // El turno ya está guardado: un email que falla no debe romper nada.
            log.error("No se pudo enviar '{}' a {}", asunto, datos.email(), e);
        }
    }

    private String resumen(DatosEmail d) {
        return "<table cellpadding=\"4\">"
                + fila("Servicio", d.servicio())
                + fila("Peluquero", d.barbero())
                + fila("Día", FECHA.format(d.fecha()))
                + fila("Hora", d.hora().toString())
                + fila("Precio", NumberFormat.getCurrencyInstance(AR).format(d.precio()))
                + "</table>";
    }

    private static String fila(String etiqueta, String valor) {
        return "<tr><td style=\"color:#666\">" + etiqueta + "</td><td><b>" + esc(valor) + "</b></td></tr>";
    }

    private static String plantilla(String cuerpo) {
        return "<div style=\"font-family:Arial,sans-serif;font-size:15px;color:#111;max-width:520px\">"
                + "<h2 style=\"margin:0 0 12px\">Barbería Esquina</h2>" + cuerpo
                + "<p style=\"color:#888;font-size:12px\">Este es un email automático, no hace falta responderlo.</p>"
                + "</div>";
    }

    private static String esc(String texto) {
        return HtmlUtils.htmlEscape(texto == null ? "" : texto);
    }
}
