/*
 * Fija el tema antes de pintar: el elegido (src/lib/tema.js) o, si no hay, el del sistema.
 * Va en un archivo y no inline en index.html porque la CSP del build solo deja correr
 * scripts del propio sitio. Es chico y bloqueante a propósito: así no hay destello del otro tema.
 */
(function () {
  var tema;
  try { tema = localStorage.getItem("necks.tema"); } catch (e) {}
  if (tema !== "claro" && tema !== "oscuro") {
    tema = window.matchMedia("(prefers-color-scheme: dark)").matches ? "oscuro" : "claro";
  }
  document.documentElement.dataset.tema = tema;
  if (tema === "oscuro") document.querySelector('meta[name="theme-color"]').content = "#0F0E0D";
})();
