/*
 * El logo de Neck's (corchetes, NECK'S y HAIR SALON), redibujado en vector a partir del
 * original: se ve nítido a cualquier tamaño y toma los colores del tema (la tinta es
 * currentColor = --texto, y HAIR SALON va en --texto-2), así en oscuro sale blanco solo.
 * Mide 1008 × 560: se le pasa el ancho y el alto sale solo.
 */
const ANCHO = 1008;
const ALTO = 560;

export function Logo({ ancho, className = "", alt = "" }) {
  return (
    <svg className={`logo-marca ${className}`.trim()} viewBox="24 24 1008 560"
         width={ancho} height={Math.round((ancho * ALTO) / ANCHO)}
         role={alt ? "img" : undefined} aria-label={alt || undefined} aria-hidden={alt ? undefined : true}>
      <g fill="currentColor">
        {/* Corchetes */}
        <path d="M30 30h80v13H44v522h66v13H30z" />
        <path d="M1026 30h-80v13h66v522h-66v13h80z" />
        {/* N: astas con remates y espolones, y la diagonal gruesa que termina en una cola curva */}
        <path d="M138 120h66v12h-14v260h10v12h-62v-12h12V132h-12z" />
        <path d="M150 258l-14 12 14 12zM190 258l12 12-12 12z" />
        <path d="M272 120h40v12h-12v270h-14V132h-14z" />
        <path d="M286 258l-10 12 10 12zM300 258l10 12-10 12z" />
        <path d="M150 132h52c26 84 62 170 98 246c22 46 50 86 98 114c-62-10-108-48-136-96C230 334 190 238 150 168z" />
      </g>
      {/* ECK´S */}
      <g fill="none" stroke="currentColor" strokeWidth="10">
        <path d="M420 245H359v128h61M359 309h53" />
        <path d="M552 266A66 66 0 1 0 552 352" />
        <path d="M606 240v138M692 241l-80 78M638 294l76 84" />
        <path d="M779 230l-15 36" strokeWidth="9" />
        <path d="M918 263c-12-14-26-20-43-20c-24 0-39 13-39 31c0 20 18 27 41 33c25 7 44 15 44 37c0 20-18 31-44 31c-20 0-36-8-48-23" />
      </g>
      {/* HAIR SALON */}
      <g fill="none" stroke="var(--texto-2)" strokeWidth="7.5">
        <path d="M252 514v56M296 514v56M252 542h44" />
        <path d="M312 570l27-56l27 56M322 551h34" />
        <path d="M392 514v56" />
        <path d="M420 570v-56h24c12 0 20 7 20 17s-8 17-20 17h-24M444 548l22 22" />
        <g transform="translate(12 0)">
          <path d="M538 522c-6-6-13-9-21-9c-12 0-19 6-19 14c0 9 9 12 20 15c12 3 22 7 22 17c0 9-9 15-22 15c-10 0-18-4-24-11" />
          <path d="M556 570l27-56l27 56M566 551h34" />
          <path d="M632 514v52h36" />
          <ellipse cx="712" cy="542" rx="28" ry="29" />
          <path d="M762 570v-56l40 56v-56" />
        </g>
      </g>
    </svg>
  );
}
