# Documentación — Barbería Esquina

Sistema web de turnos para **Barbería Esquina 1290** (Córdoba). Trabajo final de *Taller de Desarrollo de Aplicaciones*.

Esta carpeta explica **cómo está hecho el sistema, cómo correrlo y cómo seguirlo**, y vincula cada parte con lo que pide la
documentación de la materia (Etapas 0 a 5). Los diagramas están en Mermaid: GitHub los dibuja solos.

## Por dónde empezar

| Si querés… | Leé |
|---|---|
| Entender qué es y en qué estado está | [1. Visión general](01-vision-general.md) |
| Levantarlo en tu máquina | [2. Instalación y puesta en marcha](02-instalacion.md) |
| Ver cómo se conectan las partes | [3. Arquitectura](03-arquitectura.md) |
| Saber qué tablas hay y por qué | [4. Modelo de datos](04-modelo-de-datos.md) |
| Consumir o modificar la API | [5. API REST](05-api.md) |
| Tocar pantallas | [6. Frontend](06-frontend.md) |
| Saber quién puede hacer qué | [7. Roles y permisos](07-roles-y-permisos.md) |
| Entender cómo se calculan los horarios, cancelaciones, cobros | [8. Reglas de negocio](08-reglas-de-negocio.md) |
| Relacionar el sistema con los requerimientos de la tesis | [9. Trazabilidad con la tesis](09-trazabilidad.md) |
| Correr o escribir tests | [10. Pruebas](10-pruebas.md) |
| Sumar una funcionalidad sin romper nada | [11. Guía de desarrollo](11-guia-de-desarrollo.md) |
| Ver qué falta | [12. Pendientes](12-pendientes.md) |

## En una línea por carpeta

```
peluquerias/
├── backend/    API REST en Java 21 + Spring Boot 4 + MySQL      → docs 3, 4, 5, 8
├── frontend/   Sitio del cliente y panel de gestión en React    → docs 3, 6
├── maqueta/    Prototipo HTML navegable (referencia de diseño)  → doc 6
└── docs/       Esta documentación
```

## Datos rápidos

| | |
|---|---|
| Backend | 82 clases Java · 30 endpoints · 44 tests de integración |
| Base de datos | MySQL 8 · 9 tablas · esquema versionado con Flyway |
| Frontend | React 18 + Vite 6 · 5 pantallas del cliente · 9 del panel |
| Repo | <https://github.com/valencastaldi/barberia-esquina> |
