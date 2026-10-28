/**
 * Datos fijos del local. No están en la base porque no los define la documentación.
 * Dirección y teléfono sacados de la página de reservas del local (AgendaPro) y su Facebook.
 * ⚠ Falta el Instagram real.
 */
export const BARBERIA = {
  nombre: "Neck's Hair Salon",
  rotulo: "Hair Salon",
  direccion: "Av. Goycoechea 977, Villa Allende",
  telefono: "+54 9 3543 69-2777",
  whatsapp: "5493543692777",
  instagram: "",   // falta el usuario real
};

/**
 * Datos del responsable y reglas para /privacidad y /terminos.
 * ⚠ Lo que está entre corchetes lo tiene que completar el dueño (o su contador) antes de publicar,
 * y los textos finales conviene que los revise un abogado.
 */
export const LEGAL = {
  razonSocial: "Neck's Hair Salon",
  cuit: "",   // todavía no lo pasaron: mientras esté vacío no se muestra
  domicilio: "Av. Goycoechea 977, Villa Allende, Córdoba",
  emailDatos: "franbossana@gmail.com",
  actualizado: "30 de septiembre de 2026",
  // Cuánto se guardan los datos de un cliente que no vuelve. Lo decide el dueño.
  conservacion: "[plazo, por ejemplo 2 años]",
  // Los mismos valores que app.turnos en el application.yml del backend.
  diasAnticipacion: 30,
  horasCancelacion: 48,
  // Qué pasa si el cliente no viene o llega tarde. Lo decide el dueño.
  tolerancia: "[por ejemplo: pasados 10 minutos de la hora del turno, el horario puede darse a otro cliente]",
};
