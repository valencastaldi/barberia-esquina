import { useState } from "react";
import { urlDeApi } from "../api/api.js";

/**
 * Foto de perfil de un peluquero; si no tiene (o no carga), sus iniciales.
 * persona: { nombre, apellido, foto }. src permite mostrar una vista previa (data:…).
 */
export function Avatar({ persona, src, className = "avatar" }) {
  const [fallo, setFallo] = useState(false);
  const url = src ?? urlDeApi(persona.foto);
  const iniciales = `${persona.nombre?.[0] ?? ""}${persona.apellido?.[0] ?? ""}`;

  if (url && !fallo) {
    return (
      <span className={`${className} con-foto`}>
        <img src={url} alt="" loading="lazy" onError={() => setFallo(true)} />
      </span>
    );
  }
  return <span className={className} aria-hidden="true">{iniciales}</span>;
}
