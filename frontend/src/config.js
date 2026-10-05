/**
 * Datos fijos del local. No están en la base porque no los define la documentación.
 * ⚠ El Instagram es de relleno: falta el real de la barbería.
 */
export const BARBERIA = {
  nombre: "Barbería Esquina",
  rotulo: "Esquina 1290",
  direccion: "Av. San Martín 992",
  telefono: "+54 9 3517 59-0663",
  whatsapp: "5493517590663",
  instagram: "barberiaesquina",
};

/**
 * Datos del responsable y reglas para /privacidad y /terminos.
 * ⚠ Lo que está entre corchetes lo tiene que completar el dueño (o su contador) antes de publicar,
 * y los textos finales conviene que los revise un abogado.
 */
export const LEGAL = {
  razonSocial: "Barbería Esquina 1290",
  cuit: "",   // todavía no lo pasaron: mientras esté vacío no se muestra
  domicilio: "Av. San Martín 992",
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
