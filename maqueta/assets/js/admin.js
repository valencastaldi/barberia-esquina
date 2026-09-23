/* =========================================================
   Panel del barbero — barra lateral compartida.
   ========================================================= */

var ICONOS = {
  agenda:    '<path d="M8 2v4M16 2v4M3 10h18M5 4h14a2 2 0 012 2v14a2 2 0 01-2 2H5a2 2 0 01-2-2V6a2 2 0 012-2z"/>',
  dashboard: '<path d="M3 3v18h18M7 15v3M12 10v8M17 6v12"/>',
  servicios: '<path d="M6 3v12M18 3v12M6 15a3 3 0 100 6 3 3 0 000-6zM18 15a3 3 0 100 6 3 3 0 000-6z"/>',
  horarios:  '<circle cx="12" cy="12" r="9"/><path d="M12 7v5l3.2 1.9"/>',
  pagos:     '<rect x="2" y="5" width="20" height="14" rx="2"/><path d="M2 10h20M6 15h4"/>',
  clientes:  '<path d="M16 21v-2a4 4 0 00-4-4H6a4 4 0 00-4 4v2M9 11a4 4 0 100-8 4 4 0 000 8zM22 21v-2a4 4 0 00-3-3.87M16 3.13a4 4 0 010 7.75"/>',
  peluqueros:'<circle cx="12" cy="7" r="4"/><path d="M5.5 21a6.5 6.5 0 0113 0M9 14.5l3 3 3-3"/>',
  salir:     '<path d="M9 21H5a2 2 0 01-2-2V5a2 2 0 012-2h4M16 17l5-5-5-5M21 12H9"/>',
};

function icono(nombre) {
  return '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" ' +
         'stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">' + ICONOS[nombre] + "</svg>";
}

function pintarLateral(activo) {
  var grupos = [
    { titulo: "Operación", secciones: [
      { id: "agenda",     txt: "Agenda",     href: "agenda.html" },
      { id: "pagos",      txt: "Pagos",      href: "pagos.html" },
      { id: "clientes",   txt: "Clientes",   href: "clientes.html" },
    ] },
    { titulo: "Negocio", secciones: [
      { id: "dashboard",  txt: "Dashboard",  href: "dashboard.html" },
      { id: "peluqueros", txt: "Peluqueros", href: "peluqueros.html" },
      { id: "servicios",  txt: "Servicios",  href: "servicios.html" },
      { id: "horarios",   txt: "Horarios",   href: "horarios.html" },
    ] },
  ];

  var iniciales = BARBERIA.barbero.nombre[0] + BARBERIA.barbero.apellido[0];

  document.querySelector(".lateral").innerHTML =
    '<a class="marca" href="../index.html">' +
      '<img src="../assets/img/logo.jpg" alt="">' +
      "<span><b>Barbería Esquina</b><small>Panel de gestión</small></span>" +
    "</a>" +
    '<nav class="nav">' +
      grupos.map(function (g) {
        return '<span class="nav-grupo">' + g.titulo + "</span>" +
          g.secciones.map(function (s) {
            return '<a href="' + s.href + '"' + (s.id === activo ? ' class="activo" aria-current="page"' : "") + ">" +
                   icono(s.id) + s.txt + "</a>";
          }).join("");
      }).join("") +
    "</nav>" +
    '<div class="abajo">' +
      '<div class="usuario">' +
        '<span class="avatar">' + iniciales + "</span>" +
        "<span><b>" + BARBERIA.barbero.nombre + " " + BARBERIA.barbero.apellido + "</b>" +
        "<small>Dueño · Super admin</small></span>" +
      "</div>" +
      '<nav class="nav"><a href="login.html">' + icono("salir") + "Cerrar sesión</a></nav>" +
    "</div>";
}

/* Chip de medio de pago; sin medio = pendiente de cobro. */
function chipMedio(medio) {
  return medio
    ? '<span class="medio medio-' + medio + '">' + MEDIOS[medio] + "</span>"
    : '<span class="medio medio-nada">Sin cobrar</span>';
}

/* Cierra cualquier <dialog> al hacer clic en el fondo oscuro. */
document.addEventListener("click", function (e) {
  if (e.target.tagName === "DIALOG" && e.target.open) e.target.close();
});
