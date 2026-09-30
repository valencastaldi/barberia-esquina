/**
 * Datos fijos del local. No están en la base porque no los define la documentación.
 * ⚠ Dirección, teléfono e Instagram son de relleno: faltan los reales de la barbería.
 */
export const BARBERIA = {
  nombre: "Barbería Esquina",
  rotulo: "Esquina 1290",
  direccion: "Av. Rivadavia 1290, esquina Sarmiento",
  telefono: "+54 9 351 000-0000",
  whatsapp: "5493510000000",
  instagram: "barberiaesquina",
};

/**
 * Datos del responsable y reglas para /privacidad y /terminos.
 * ⚠ Lo que está entre corchetes lo tiene que completar el dueño (o su contador) antes de publicar,
 * y los textos finales conviene que los revise un abogado.
 */
export const LEGAL = {
  razonSocial: "[Razón social o nombre del titular]",
  cuit: "[CUIT]",
  domicilio: "[Domicilio legal]",
  emailDatos: "[Email para consultas sobre datos personales]",
  actualizado: "30 de septiembre de 2026",
  // Cuánto se guardan los datos de un cliente que no vuelve. Lo decide el dueño.
  conservacion: "[plazo, por ejemplo 2 años]",
  // Los mismos valores que app.turnos en el application.yml del backend.
  diasAnticipacion: 30,
  horasCancelacion: 48,
  // Qué pasa si el cliente no viene o llega tarde. Lo decide el dueño.
  tolerancia: "[por ejemplo: pasados 10 minutos de la hora del turno, el horario puede darse a otro cliente]",
};
