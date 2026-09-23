/* =========================================================
   Datos de demostración de la maqueta.
   Reemplazar por las respuestas reales de la API:
     GET /api/v1/servicios
     GET /api/v1/disponibilidad
     GET /api/v1/turnos
   ========================================================= */

const BARBERIA = {
  nombre: "Barbería Esquina",
  rotulo: "Esquina 1290",
  direccion: "Av. Rivadavia 1290, esquina Sarmiento",
  telefono: "+54 9 351 000-0000",
  instagram: "@barberiaesquina",
  barbero: { nombre: "Agustín", apellido: "Rosas", email: "agustin@barberiaesquina.com" },
};

/* Servicio: id, nombre, descripcion, duracion_minutos, precio, activo */
const SERVICIOS = [
  { id: 1, nombre: "Corte Corto",   descripcion: "Máquina y tijera, lavado incluido",     duracion: 30, precio: 9000,  activo: true },
  { id: 2, nombre: "Corte Largo",   descripcion: "Tijera, texturizado y peinado",          duracion: 45, precio: 11000, activo: true },
  { id: 3, nombre: "Corte + Barba", descripcion: "Corte completo, perfilado y toalla caliente", duracion: 60, precio: 15000, activo: true },
  { id: 4, nombre: "Perfilado de Barba", descripcion: "Diseño, navaja y aceite",           duracion: 30, precio: 7000,  activo: true },
  { id: 5, nombre: "Corte Niño",    descripcion: "Hasta 12 años",                          duracion: 30, precio: 7500,  activo: false },
];

/* Horario_Atencion: dia_semana 0=Dom ... 6=Sáb */
const HORARIOS = [
  { dia: 0, nombre: "Domingo",   inicio: null,    fin: null,    slot: 30, activo: false },
  { dia: 1, nombre: "Lunes",     inicio: "10:00", fin: "20:00", slot: 30, activo: true  },
  { dia: 2, nombre: "Martes",    inicio: "10:00", fin: "20:00", slot: 30, activo: true  },
  { dia: 3, nombre: "Miércoles", inicio: "10:00", fin: "20:00", slot: 30, activo: true  },
  { dia: 4, nombre: "Jueves",    inicio: "10:00", fin: "21:00", slot: 30, activo: true  },
  { dia: 5, nombre: "Viernes",   inicio: "10:00", fin: "21:00", slot: 30, activo: true  },
  { dia: 6, nombre: "Sábado",    inicio: "09:00", fin: "18:00", slot: 30, activo: true  },
];

/* ---------- Extensión: equipo, clientes y pagos ----------
   No está en la documentación original (pensada para un solo barbero).
   Endpoints a sumar en la API:
     GET/POST/PUT /api/v1/peluqueros · PATCH /api/v1/peluqueros/:id/estado
     GET /api/v1/clientes · GET /api/v1/clientes/:id
     GET /api/v1/pagos · POST /api/v1/pagos (registrar cobro de un turno)
   --------------------------------------------------------- */

/* Peluquero: comision = % que se lleva el peluquero de lo que factura.
   dias = dias_semana que atiende (0=Dom ... 6=Sáb). */
const PELUQUEROS = [
  { id: 1, nombre: "Agustín",  apellido: "Rosas",  rol: "Dueño",   email: "agustin@barberiaesquina.com",  telefono: "351 555-0101", comision: 0,  servicios: [1, 2, 3, 4, 5], dias: [1, 2, 3, 4, 5, 6], activo: true,  alta: "mar 2023" },
  { id: 2, nombre: "Santiago", apellido: "Molina", rol: "Barbero", email: "santiago@barberiaesquina.com", telefono: "351 555-0144", comision: 50, servicios: [1, 2, 3, 4],    dias: [2, 3, 4, 5, 6],    activo: true,  alta: "ago 2024" },
  { id: 3, nombre: "Joaquín",  apellido: "Vera",   rol: "Barbero", email: "joaquin@barberiaesquina.com",  telefono: "351 555-0187", comision: 45, servicios: [1, 4, 5],       dias: [1, 2, 4, 5, 6],    activo: true,  alta: "feb 2025" },
  { id: 4, nombre: "Emiliano", apellido: "Ríos",   rol: "Barbero", email: "emiliano@barberiaesquina.com", telefono: "351 555-0112", comision: 45, servicios: [1, 2],          dias: [4, 5, 6],          activo: false, alta: "may 2024" },
];

const CLIENTES = [
  { id: 1,  nombre: "Mateo Giménez",     telefono: "351 415-2233" },
  { id: 2,  nombre: "Bruno Actis",       telefono: "351 622-8841" },
  { id: 3,  nombre: "Nicolás Paz",       telefono: "351 300-1190" },
  { id: 4,  nombre: "Lucas Ferreyra",    telefono: "351 711-0043" },
  { id: 5,  nombre: "Tomás Quiroga",     telefono: "351 588-9012" },
  { id: 6,  nombre: "Iván Ledesma",      telefono: "351 204-3317" },
  { id: 7,  nombre: "Franco Britos",     telefono: "351 909-6654" },
  { id: 8,  nombre: "Julián Sosa",       telefono: "351 130-7729" },
  { id: 9,  nombre: "Ramiro Castro",     telefono: "351 477-3310" },
  { id: 10, nombre: "Ezequiel Moreno",   telefono: "351 382-5561" },
  { id: 11, nombre: "Facundo Oviedo",    telefono: "351 690-2284" },
  { id: 12, nombre: "Gonzalo Luna",      telefono: "351 245-7719" },
  { id: 13, nombre: "Valentín Herrera",  telefono: "351 813-4402" },
  { id: 14, nombre: "Lautaro Díaz",      telefono: "351 356-9087" },
  { id: 15, nombre: "Martín Cabrera",    telefono: "351 520-1146" },
  { id: 16, nombre: "Santino Rojas",     telefono: "351 734-6628" },
  { id: 17, nombre: "Benjamín Suárez",   telefono: "351 261-0935" },
  { id: 18, nombre: "Thiago Romero",     telefono: "351 948-3371" },
  { id: 19, nombre: "Emanuel Acosta",    telefono: "351 407-8852" },
  { id: 20, nombre: "Diego Villalba",    telefono: "351 619-2047" },
  { id: 21, nombre: "Leandro Funes",     telefono: "351 385-7613" },
  { id: 22, nombre: "Maximiliano Ortiz", telefono: "351 702-4498" },
];
/* Clientes ocasionales generados, para que la base tenga un tamaño creíble. */
(function () {
  const nombres = ["Agustín", "Alejo", "Bautista", "Camilo", "Dante", "Elías", "Felipe", "Gael", "Hernán", "Ignacio",
                   "Juan Cruz", "Kevin", "Lisandro", "Marcos", "Nahuel", "Octavio", "Pablo", "Renzo", "Sebastián", "Tobías"];
  const apellidos = ["Aguirre", "Bustos", "Carranza", "Domínguez", "Escudero", "Figueroa", "Godoy", "Heredia", "Juárez", "Lucero",
                     "Maldonado", "Navarro", "Olmedo", "Pereyra", "Quinteros", "Ramírez", "Sánchez", "Toledo", "Urquiza", "Vázquez"];
  for (let i = 0; i < 140; i++) {
    CLIENTES.push({
      id: CLIENTES.length + 1,
      nombre: nombres[(i * 7) % 20] + " " + apellidos[(i * 13 + Math.floor(i / 20)) % 20],
      telefono: "351 " + String(200 + (i * 37) % 800) + "-" + String(1000 + (i * 7919) % 9000),
    });
  }
})();

CLIENTES.forEach(function (c) {
  c.email = c.nombre.toLowerCase().normalize("NFD").replace(/[̀-ͯ]/g, "").replace(/ /g, ".") + "@gmail.com";
});

const MEDIOS = { efectivo: "Efectivo", transferencia: "Transferencia", mercadopago: "Mercado Pago" };

/* Turnos de hoy para la agenda. medio = cómo se cobró (null = sin cobrar). */
const TURNOS_HOY = [
  { hora: "10:00", fin: "10:30", idCliente: 1,  peluquero: 1, idServicio: 1, estado: "completado", medio: "efectivo" },
  { hora: "10:00", fin: "10:45", idCliente: 9,  peluquero: 2, idServicio: 2, estado: "completado", medio: "transferencia" },
  { hora: "10:30", fin: "11:30", idCliente: 2,  peluquero: 1, idServicio: 3, estado: "completado", medio: "mercadopago" },
  { hora: "10:30", fin: "11:00", idCliente: 14, peluquero: 3, idServicio: 1, estado: "completado", medio: "mercadopago" },
  { hora: "11:00", fin: "11:30", idCliente: 10, peluquero: 2, idServicio: 1, estado: "completado", medio: null },
  { hora: "11:30", fin: "12:00", idCliente: 3,  peluquero: 1, idServicio: 1, estado: "ausente",    medio: null },
  { hora: "12:00", fin: "13:00", idCliente: 11, peluquero: 2, idServicio: 3, estado: "pendiente",  medio: null },
  { hora: "13:00", fin: "13:30", idCliente: 15, peluquero: 3, idServicio: 4, estado: "ausente",    medio: null },
  { hora: "14:00", fin: "14:45", idCliente: 4,  peluquero: 1, idServicio: 2, estado: "pendiente",  medio: null },
  { hora: "15:00", fin: "16:00", idCliente: 5,  peluquero: 1, idServicio: 3, estado: "pendiente",  medio: null },
  { hora: "15:30", fin: "16:00", idCliente: 16, peluquero: 3, idServicio: 1, estado: "pendiente",  medio: null },
  { hora: "16:00", fin: "16:30", idCliente: 12, peluquero: 2, idServicio: 4, estado: "pendiente",  medio: null },
  { hora: "16:30", fin: "17:00", idCliente: 6,  peluquero: 1, idServicio: 4, estado: "pendiente",  medio: null },
  { hora: "17:00", fin: "17:45", idCliente: 13, peluquero: 2, idServicio: 2, estado: "pendiente",  medio: null },
  { hora: "17:30", fin: "18:00", idCliente: 7,  peluquero: 1, idServicio: 1, estado: "cancelado",  medio: null },
  { hora: "18:30", fin: "19:30", idCliente: 8,  peluquero: 1, idServicio: 3, estado: "pendiente",  medio: null },
];

/* Horas ya ocupadas por fecha (offset en días desde hoy) — alimenta la maqueta de disponibilidad */
const OCUPADOS = {
  0: ["10:00", "10:30", "11:00", "11:30", "14:00", "15:00", "15:30", "16:30", "18:30", "19:00"],
  1: ["10:00", "12:00", "12:30", "16:00", "17:00", "17:30"],
  2: ["11:00", "11:30", "13:00", "18:00"],
  3: ["10:30", "14:30", "15:00", "19:00", "19:30"],
  4: ["16:00"],
  5: [],
  6: ["09:00", "09:30", "10:00", "10:30", "11:00", "13:00", "14:00", "14:30", "15:00", "16:00", "16:30", "17:00", "17:30"],
};

/* ---------- Utilidades compartidas ---------- */

const DIAS_CORTOS = ["Dom", "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb"];
const MESES_CORTOS = ["ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic"];
const MESES = ["enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"];

function pesos(n) {
  return "$" + n.toLocaleString("es-AR");
}

function fechaLarga(d) {
  return DIAS_CORTOS[d.getDay()].toLowerCase() + " " + d.getDate() + " de " + MESES[d.getMonth()];
}

function sumarMinutos(hhmm, minutos) {
  const [h, m] = hhmm.split(":").map(Number);
  const t = h * 60 + m + minutos;
  return String(Math.floor(t / 60) % 24).padStart(2, "0") + ":" + String(t % 60).padStart(2, "0");
}

/* Genera los slots de un día según el horario configurado, la duración
   del servicio y los turnos ya ocupados. Es la versión de maqueta de
   CalcularSlotsDisponibles (RF-27). */
function generarSlots(offsetDias, duracionServicio) {
  const fecha = new Date();
  fecha.setDate(fecha.getDate() + offsetDias);
  const horario = HORARIOS[fecha.getDay()];
  if (!horario.activo) return [];

  const ocupados = OCUPADOS[offsetDias] || [];
  const slots = [];
  let cursor = horario.inicio;

  while (sumarMinutos(cursor, duracionServicio) <= horario.fin) {
    // Un slot está libre si ninguno de los bloques que ocupa está tomado.
    let libre = true;
    for (let m = 0; m < duracionServicio; m += horario.slot) {
      if (ocupados.includes(sumarMinutos(cursor, m))) { libre = false; break; }
    }
    // Si es hoy, los horarios que ya pasaron no se ofrecen.
    if (offsetDias === 0) {
      const ahora = new Date();
      const [h, m] = cursor.split(":").map(Number);
      if (h * 60 + m <= ahora.getHours() * 60 + ahora.getMinutes()) libre = false;
    }
    slots.push({ hora: cursor, libre: libre });
    cursor = sumarMinutos(cursor, horario.slot);
  }
  return slots;
}

/* ---------- Historial de turnos (extensión) ----------
   Genera 60 días de turnos pasados con una semilla fija, así
   clientes, pagos y peluqueros muestran números coherentes entre sí
   y siempre los mismos. En la app real esto es la tabla Turno + Pago. */

const DIAS_HISTORIAL = 60;

function _azar(semilla) {
  return function () {
    semilla |= 0; semilla = (semilla + 0x6D2B79F5) | 0;
    let t = Math.imul(semilla ^ (semilla >>> 15), 1 | semilla);
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

function _elegirPeso(azar, pares) {
  const total = pares.reduce(function (a, p) { return a + p[1]; }, 0);
  let r = azar() * total;
  for (const p of pares) { if ((r -= p[1]) < 0) return p[0]; }
  return pares[pares.length - 1][0];
}

function servicio(id) { return SERVICIOS.find(function (s) { return s.id === id; }); }
function peluquero(id) { return PELUQUEROS.find(function (p) { return p.id === id; }); }
function cliente(id) { return CLIENTES.find(function (c) { return c.id === id; }); }
function iniciales(nombre) { return nombre.split(" ").map(function (x) { return x[0]; }).slice(0, 2).join(""); }

/* Todos los turnos: historial + hoy. dias = días hacia atrás (0 = hoy). */
const TURNOS = (function () {
  const azar = _azar(1290);
  // Los primeros 16 son clientes fieles; el resto viene de vez en cuando.
  const fieles = [1.6, 1.6, 0.9, 1.3, 1.1, 1.1, 0.9, 1.3, 1.6, 1.1, 0.9, 0.9, 1.1, 1.3, 0.9, 1.1];
  const pesoCliente = CLIENTES.map(function (c, i) { return [c.id, i < 16 ? fieles[i] : i < 22 ? 2 : 0.6]; });
  const lista = [];

  for (let d = DIAS_HISTORIAL; d >= 1; d--) {
    const fecha = new Date(); fecha.setDate(fecha.getDate() - d);
    const horario = HORARIOS[fecha.getDay()];
    if (!horario.activo) continue;

    PELUQUEROS.forEach(function (p) {
      // Emiliano dejó de trabajar hace 3 semanas.
      if (!p.dias.includes(fecha.getDay()) || (!p.activo && d < 21)) return;

      const bloques = [];
      for (let h = horario.inicio; h < horario.fin; h = sumarMinutos(h, 30)) bloques.push(h);
      const cantidad = 3 + Math.floor(azar() * 5);
      const horas = bloques.sort(function () { return azar() - 0.5; }).slice(0, cantidad).sort();

      horas.forEach(function (hora) {
        const idServ = p.servicios.filter(function (s) { return s !== 5 || azar() < 0.3; });
        const s = servicio(idServ[Math.floor(azar() * idServ.length)]);
        // Del 17 al 22 son clientes que dejaron de venir: solo aparecen al principio.
        let idCliente;
        do { idCliente = _elegirPeso(azar, pesoCliente); }
        while (idCliente >= 17 && idCliente <= 22 && d < 25);

        const estado = _elegirPeso(azar, [["completado", 82], ["ausente", 7], ["cancelado", 11]]);
        lista.push({
          dias: d, hora: hora, fin: sumarMinutos(hora, s.duracion),
          idCliente: idCliente, peluquero: p.id, idServicio: s.id, estado: estado,
          medio: estado === "completado" ? _elegirPeso(azar, [["efectivo", 40], ["transferencia", 35], ["mercadopago", 25]]) : null,
          calificacion: estado === "completado" && azar() < 0.55 ? _elegirPeso(azar, [[5, 58], [4, 28], [3, 9], [2, 3], [1, 2]]) : null,
        });
      });
    });
  }

  TURNOS_HOY.forEach(function (t) { t.dias = 0; t.calificacion = null; lista.push(t); });

  lista.forEach(function (t, i) {
    const s = servicio(t.idServicio), c = cliente(t.idCliente);
    t.id = i + 1;
    t.servicio = s.nombre;
    t.precio = s.precio;
    t.cliente = c.nombre;
    t.telefono = c.telefono;
  });
  return lista;
})();

/* Resumen de un cliente a partir de sus turnos. */
function resumenCliente(idCliente) {
  const turnos = TURNOS.filter(function (t) { return t.idCliente === idCliente; })
                       .sort(function (a, b) { return a.dias - b.dias || (b.hora < a.hora ? -1 : 1); });
  const hechos = turnos.filter(function (t) { return t.estado === "completado"; });
  const cuenta = {};
  hechos.forEach(function (t) { cuenta[t.peluquero] = (cuenta[t.peluquero] || 0) + 1; });
  const habitual = Object.keys(cuenta).sort(function (a, b) { return cuenta[b] - cuenta[a]; })[0];
  const notas = hechos.filter(function (t) { return t.calificacion; });

  return {
    turnos: turnos,
    visitas: hechos.length,
    ausencias: turnos.filter(function (t) { return t.estado === "ausente"; }).length,
    gasto: hechos.reduce(function (a, t) { return a + t.precio; }, 0),
    ultima: hechos.length ? hechos[0].dias : null,
    primera: turnos.length ? turnos[turnos.length - 1].dias : null,
    proximo: turnos.find(function (t) { return t.estado === "pendiente"; }) || null,
    habitual: habitual ? peluquero(Number(habitual)) : null,
    promedio: notas.length ? notas.reduce(function (a, t) { return a + t.calificacion; }, 0) / notas.length : null,
  };
}

/* "hoy", "ayer", "lun 14 sep" */
function fechaRelativa(dias) {
  if (dias === 0) return "hoy";
  if (dias === 1) return "ayer";
  const f = new Date(); f.setDate(f.getDate() - dias);
  return DIAS_CORTOS[f.getDay()].toLowerCase() + " " + f.getDate() + " " + MESES_CORTOS[f.getMonth()];
}
