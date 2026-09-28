/**
 * Prepara una foto de perfil antes de subirla: la recorta cuadrada desde el centro y
 * la achica a 400x400 en JPEG. Así pesa unos 30-60 KB aunque venga de un celular de 12 MP,
 * y al redibujarla se pierden los datos ocultos del archivo (ubicación GPS, modelo del celular).
 *
 * Devuelve { blob, vistaPrevia } o lanza un Error con un mensaje para mostrar.
 */
const LADO = 400;

export async function prepararFoto(archivo) {
  if (!/^image\/(jpeg|png|webp)$/.test(archivo.type)) {
    throw new Error("Elegí una imagen JPG, PNG o WEBP.");
  }
  if (archivo.size > 15 * 1024 * 1024) {
    throw new Error("La imagen es muy pesada (más de 15 MB).");
  }

  let imagen;
  try {
    // createImageBitmap respeta la orientación que guarda el celular (foto parada o acostada).
    imagen = await createImageBitmap(archivo, { imageOrientation: "from-image" });
  } catch {
    throw new Error("No pudimos leer la imagen. Probá con otra.");
  }

  const lado = Math.min(imagen.width, imagen.height);
  const canvas = document.createElement("canvas");
  canvas.width = LADO;
  canvas.height = LADO;
  const ctx = canvas.getContext("2d");
  ctx.fillStyle = "#14171C";                        // fondo para PNG con transparencia
  ctx.fillRect(0, 0, LADO, LADO);
  ctx.drawImage(imagen, (imagen.width - lado) / 2, (imagen.height - lado) / 2, lado, lado, 0, 0, LADO, LADO);
  imagen.close?.();

  const blob = await new Promise((listo) => canvas.toBlob(listo, "image/jpeg", 0.86));
  if (!blob) throw new Error("No pudimos procesar la imagen. Probá con otra.");
  return { blob, vistaPrevia: canvas.toDataURL("image/jpeg", 0.86) };
}
