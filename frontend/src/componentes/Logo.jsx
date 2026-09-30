/*
 * El escudo de la barbería, en archivos del tamaño justo (public/img) en lugar del JPG
 * original de 1254 px. AVIF sin mezclar colores en los bordes (queda igual de nítido y
 * pesa 6 a 36 KB); WebP sin pérdida para los navegadores que no leen AVIF (Safari < 16.4).
 * tamano: lo que mide en pantalla, en px (el CSS de cada lugar fija lo mismo).
 */
const GRANDE = {
  avif: "/img/logo-264.avif 264w, /img/logo-396.avif 396w, /img/logo-528.avif 528w",
  webp: "/img/logo-264.webp 264w, /img/logo-396.webp 396w",
  src: "/img/logo-264.webp",
};
const CHICO = {
  avif: "/img/logo-128.avif 128w",
  webp: "/img/logo-128.webp 128w",
  src: "/img/logo-128.webp",
};

export function Logo({ tamano, className, alt = "" }) {
  const archivos = tamano > 64 ? GRANDE : CHICO;
  const sizes = `${tamano}px`;
  return (
    <picture>
      <source type="image/avif" srcSet={archivos.avif} sizes={sizes} />
      <img className={className} src={archivos.src} srcSet={archivos.webp} sizes={sizes}
           width={tamano} height={tamano} alt={alt} />
    </picture>
  );
}
