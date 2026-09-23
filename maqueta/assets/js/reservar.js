/* =========================================================
   Flujo de reserva del cliente — 3 pasos (RNF-11).
   Maqueta: el estado vive en memoria y se pasa a la pantalla
   de confirmación por querystring. En la app real cada paso
   consulta la API (servicios / disponibilidad / turnos).
   ========================================================= */

var estado = {
  paso: 1,
  servicio: null,   // objeto de SERVICIOS
  diaOffset: null,  // días desde hoy
  hora: null,       // "HH:mm"
};

var NOMBRES_PASO = { 1: "Servicio", 2: "Día y horario", 3: "Tus datos" };

var $ = function (sel) { return document.querySelector(sel); };

/* ---------------- Paso 1: catálogo ---------------- */

function pintarServicios() {
  var cont = $("#lista-servicios");
  cont.innerHTML = SERVICIOS.filter(function (s) { return s.activo; }).map(function (s) {
    return '<button type="button" class="servicio" data-id="' + s.id + '" aria-pressed="false">' +
             '<h3>' + s.nombre + '</h3>' +
             '<div class="precio">' + pesos(s.precio) + '</div>' +
             '<p class="detalle">' + s.descripcion + '</p>' +
             '<div class="duracion">' + s.duracion + ' min</div>' +
           '</button>';
  }).join("");

  cont.addEventListener("click", function (e) {
    var btn = e.target.closest(".servicio");
    if (!btn) return;
    elegirServicio(Number(btn.dataset.id));
  });
}

function elegirServicio(id) {
  estado.servicio = SERVICIOS.find(function (s) { return s.id === id; });
  estado.diaOffset = null;
  estado.hora = null;
  document.querySelectorAll("#lista-servicios .servicio").forEach(function (b) {
    b.setAttribute("aria-pressed", String(Number(b.dataset.id) === id));
  });
  actualizarCTA();
}

/* ---------------- Paso 2: días + horarios ---------------- */

function pintarDias() {
  var cont = $("#lista-dias");
  var html = "";
  for (var i = 0; i < 14; i++) {
    var d = new Date();
    d.setDate(d.getDate() + i);
    var abierto = HORARIOS[d.getDay()].activo;
    var libres = abierto ? generarSlots(i, estado.servicio.duracion).filter(function (s) { return s.libre; }).length : 0;
    html += '<button type="button" class="dia" data-offset="' + i + '" aria-pressed="false"' +
            (libres === 0 ? " disabled" : "") + '>' +
              "<small>" + (i === 0 ? "Hoy" : DIAS_CORTOS[d.getDay()]) + "</small>" +
              "<b>" + d.getDate() + "</b>" +
              "<i>" + (libres === 0 ? "—" : libres + " libres") + "</i>" +
            "</button>";
  }
  cont.innerHTML = html;

  // Recordamos el servicio elegido para que no haya que volver al paso 1.
  $("#bajada-2").innerHTML = estado.servicio.nombre + " · " + estado.servicio.duracion +
    " min · " + pesos(estado.servicio.precio) +
    ' <a href="#" id="cambiar-servicio" style="color:var(--azul-claro)">Cambiar</a>';
  $("#cambiar-servicio").addEventListener("click", function (e) {
    e.preventDefault();
    irA(1);
  });

  // Arrancamos en el primer día con lugar, así el cliente ve horarios enseguida.
  if (estado.diaOffset === null) {
    var primero = cont.querySelector(".dia:not([disabled])");
    if (primero) {
      estado.diaOffset = Number(primero.dataset.offset);
      primero.setAttribute("aria-pressed", "true");
    }
  } else {
    var actual = cont.querySelector('.dia[data-offset="' + estado.diaOffset + '"]');
    if (actual) actual.setAttribute("aria-pressed", "true");
  }
}

function pintarHorarios() {
  var cont = $("#lista-horarios");
  if (estado.diaOffset === null) {
    cont.innerHTML = '<div class="vacio">Elegí un día para ver los horarios libres.</div>';
    return;
  }

  var slots = generarSlots(estado.diaOffset, estado.servicio.duracion);
  if (!slots.length || !slots.some(function (s) { return s.libre; })) {
    cont.innerHTML = '<div class="vacio">No quedan horarios para ' + estado.servicio.nombre +
                     " este día.<br>Probá con otra fecha.</div>";
    return;
  }

  var franjas = [
    { titulo: "Mañana", desde: 0,  hasta: 13 },
    { titulo: "Tarde",  desde: 13, hasta: 18 },
    { titulo: "Noche",  desde: 18, hasta: 24 },
  ];

  cont.innerHTML = franjas.map(function (f) {
    var delTramo = slots.filter(function (s) {
      var h = Number(s.hora.split(":")[0]);
      return h >= f.desde && h < f.hasta;
    });
    if (!delTramo.length) return "";
    return '<div class="franja"><h3>' + f.titulo + "</h3><div class=\"horarios\">" +
      delTramo.map(function (s) {
        return '<button type="button" class="hora" data-hora="' + s.hora + '" aria-pressed="false"' +
               (s.libre ? "" : " disabled") + ">" + s.hora + "</button>";
      }).join("") + "</div></div>";
  }).join("");
}

/* ---------------- Paso 3: resumen ---------------- */

function pintarResumen() {
  var d = new Date();
  d.setDate(d.getDate() + estado.diaOffset);
  $("#resumen-turno").innerHTML =
    '<div class="fila"><span>Servicio</span><strong>' + estado.servicio.nombre + "</strong></div>" +
    '<div class="fila"><span>Día</span><strong>' + fechaLarga(d) + "</strong></div>" +
    '<div class="fila"><span>Horario</span><strong>' + estado.hora + " a " +
      sumarMinutos(estado.hora, estado.servicio.duracion) + "</strong></div>" +
    '<div class="fila"><span>Barbero</span><strong>' + BARBERIA.barbero.nombre + "</strong></div>" +
    '<div class="fila total"><span>A pagar en el local</span><strong>' + pesos(estado.servicio.precio) + "</strong></div>";
}

/* ---------------- Navegación entre pasos ---------------- */

function irA(paso) {
  estado.paso = paso;
  document.querySelectorAll(".vista").forEach(function (v) { v.classList.remove("activa"); });
  $("#vista-" + paso).classList.add("activa");

  document.querySelectorAll(".paso").forEach(function (p) {
    var n = Number(p.dataset.paso);
    p.classList.toggle("activo", n === paso);
    p.classList.toggle("hecho", n < paso);
  });

  $("#paso-num").textContent = paso;
  $("#paso-nombre").textContent = NOMBRES_PASO[paso];
  window.scrollTo({ top: 0, behavior: "smooth" });

  if (paso === 2) { pintarDias(); pintarHorarios(); }
  if (paso === 3) { pintarResumen(); }
  actualizarCTA();
}

function actualizarCTA() {
  var btn = $("#btn-siguiente");
  var info = $("#cta-info");

  if (estado.paso === 1) {
    btn.textContent = "Continuar";
    btn.disabled = !estado.servicio;
    if (estado.servicio) {
      info.hidden = false;
      $("#cta-detalle").textContent = estado.servicio.duracion + " min";
      $("#cta-titulo").textContent = estado.servicio.nombre + " · " + pesos(estado.servicio.precio);
      btn.style.flex = "0 0 auto";
    } else {
      info.hidden = true;
      btn.style.flex = "1";
    }
  }

  if (estado.paso === 2) {
    btn.textContent = "Continuar";
    btn.disabled = estado.diaOffset === null || !estado.hora;
    if (!btn.disabled) {
      var d = new Date();
      d.setDate(d.getDate() + estado.diaOffset);
      info.hidden = false;
      $("#cta-detalle").textContent = fechaLarga(d);
      $("#cta-titulo").textContent = estado.hora + " hs";
      btn.style.flex = "0 0 auto";
    } else {
      info.hidden = true;
      btn.style.flex = "1";
    }
  }

  if (estado.paso === 3) {
    btn.textContent = "Confirmar turno";
    btn.disabled = false;
    info.hidden = true;
    btn.style.flex = "1";
  }
}

/* ---------------- Eventos ---------------- */

document.addEventListener("click", function (e) {
  var dia = e.target.closest(".dia");
  if (dia && !dia.disabled) {
    estado.diaOffset = Number(dia.dataset.offset);
    estado.hora = null;
    document.querySelectorAll(".dia").forEach(function (b) {
      b.setAttribute("aria-pressed", String(b === dia));
    });
    pintarHorarios();
    actualizarCTA();
    return;
  }

  var hora = e.target.closest(".hora");
  if (hora && !hora.disabled) {
    estado.hora = hora.dataset.hora;
    document.querySelectorAll(".hora").forEach(function (b) {
      b.setAttribute("aria-pressed", String(b === hora));
    });
    actualizarCTA();
  }
});

$("#btn-siguiente").addEventListener("click", function () {
  if (estado.paso < 3) { irA(estado.paso + 1); return; }

  // Paso 3 → POST /api/v1/turnos
  var form = $("#form-datos");
  var falta = Array.prototype.find.call(form.elements, function (el) {
    return el.required && !el.value.trim();
  });
  if (falta) {
    falta.focus();
    falta.style.borderColor = "var(--rojo)";
    return;
  }

  var datos = new FormData(form);
  var params = new URLSearchParams({
    servicio: estado.servicio.id,
    dia: estado.diaOffset,
    hora: estado.hora,
    nombre: datos.get("nombre"),
    email: datos.get("email"),
  });
  window.location.href = "confirmacion.html?" + params.toString();
});

$("#btn-volver").addEventListener("click", function () {
  if (estado.paso > 1) irA(estado.paso - 1);
  else window.location.href = "index.html";
});

/* ---------------- Arranque ---------------- */

pintarServicios();

// Si vino desde la home con un servicio ya elegido, salteamos el paso 1.
var preSeleccion = new URLSearchParams(location.search).get("servicio");
if (preSeleccion && SERVICIOS.some(function (s) { return s.id === Number(preSeleccion) && s.activo; })) {
  elegirServicio(Number(preSeleccion));
  irA(2);
} else {
  actualizarCTA();
}
