/* Íconos SVG en línea (heredan el color con currentColor). */

const trazo = { fill: "none", stroke: "currentColor", strokeLinecap: "round", strokeLinejoin: "round" };

export function IconoVolver() {
  return (
    <svg width="17" height="17" viewBox="0 0 24 24" strokeWidth="2.2" {...trazo}>
      <path d="M15 18l-6-6 6-6" />
    </svg>
  );
}

export function IconoOk({ tamano = 38 }) {
  return (
    <svg width={tamano} height={tamano} viewBox="0 0 24 24" strokeWidth="2.4" {...trazo}>
      <path d="M20 6L9 17l-5-5" />
    </svg>
  );
}

export function IconoPregunta() {
  return (
    <svg width="34" height="34" viewBox="0 0 24 24" strokeWidth="2" {...trazo}>
      <circle cx="12" cy="12" r="9" />
      <path d="M12 7v6M12 16.5v.01" />
    </svg>
  );
}

export function IconoCruz() {
  return (
    <svg width="34" height="34" viewBox="0 0 24 24" strokeWidth="2.2" {...trazo}>
      <path d="M18 6L6 18M6 6l12 12" />
    </svg>
  );
}

export function IconoEstrella() {
  return (
    <svg width="34" height="34" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
      <path d="M12 2.6l2.9 5.9 6.5.9-4.7 4.6 1.1 6.5-5.8-3-5.8 3 1.1-6.5L2.6 9.4l6.5-.9z" />
    </svg>
  );
}
