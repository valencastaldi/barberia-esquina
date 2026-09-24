# 11. Guía de desarrollo

Cómo sumar cosas sin romper lo que hay.

## Convenciones

- **Idioma:** todo en español: clases (`TurnoServicio`), métodos (`reservar`), variables, comentarios, mensajes y commits.
- **Nombres por capa (backend):** `…Controlador`, `…Servicio`, `…Repositorio`, `…Dtos`. Los DTO son `record` agrupados
  por módulo (`TurnoDtos.ReservaPedido`, `TurnoDtos.Detalle`).
- **Comentarios:** explican el *por qué* (una regla del negocio, una decisión), no lo obvio. Los requerimientos se citan
  (`RF-12`) y lo agregado sobre la tesis se marca `[Extensión]`.
- **Errores:** desde el servicio se lanza `NoEncontradoException` (404), `ConflictoException` (409),
  `ReglaNegocioException` (422) o `AccessDeniedException` (403), con un mensaje para el usuario final.
  `ManejadorDeErrores` los convierte en JSON.
- **Frontend:** componentes funcionales con hooks. Pedidos con `api()` o, en el panel, `pedir()` / `usePedidoAdmin()`.
  Nada de colores sueltos: se usan los tokens de `base.css`.

## Sumar un endpoint

Ejemplo: `GET /api/v1/turnos/{id}`.

1. **Repositorio** (si hace falta una consulta nueva): método en `TurnoRepositorio`.
2. **Servicio:** la lógica en `TurnoServicio`, con `@Transactional(readOnly = true)` si solo lee.
3. **DTO:** reutilizar uno de `TurnoDtos` o agregar un `record`.
4. **Controlador:** el método en `TurnoControlador`, delgado.
5. **Permisos:**
   - público → agregar la ruta en `SeguridadConfig`;
   - solo dueño → `@PreAuthorize("hasRole('DUENO')")`;
   - "solo lo suyo" → chequear con `SesionActual` en el servicio.
6. **Test** en la clase que corresponda (`FlujoDeTurnosTest`, `SeguridadTest`…).
7. **Documentar** en [5. API](05-api.md) y en `backend/README.md`.

## Cambiar la base

**Nunca** editar `V1__esquema.sql`: Flyway guarda un checksum y, si el archivo cambia, la API no arranca en las bases que ya
lo aplicaron. Hay que agregar una migración nueva:

```
backend/src/main/resources/db/migration/V2__agrega_notas_a_turno.sql
```

```sql
ALTER TABLE turno ADD COLUMN notas VARCHAR(300) NULL;
```

Después, actualizar la entidad (`Turno.java`), los DTO, los tests y [4. Modelo de datos](04-modelo-de-datos.md). Usar SQL
compatible con MySQL **y** con H2 en modo MySQL (lo usan los tests): evitar `ENGINE=`, `AFTER columna` y funciones propias
de MySQL.

## Sumar una pantalla al panel

1. Crear `frontend/src/admin/paginas/MiPantalla.jsx`. Usar `Cabecera`, `Kpi`, `Modal`, `EstadoCarga`… de `componentes/ui.jsx`.
2. Cargar datos con `usePedidoAdmin("/ruta", { parametros })`.
3. Agregar la ruta en `AdminApp.jsx` (con `<SoloDueno>` si corresponde).
4. Agregar el ítem en `GRUPOS` de `componentes/Lateral.jsx` (con `soloDueno: true` si corresponde) y su ícono en `ICONOS`.
5. Estilos nuevos en `estilos/panel.css`, dentro de `body.admin { … }`.

## Sumar una pantalla del cliente

1. `frontend/src/paginas/cliente/MiPantalla.jsx`, envuelta en `<LayoutCliente titulo="…">`.
2. Barra inferior con `<BarraCta>`, que ya se monta en `<body>`.
3. Ruta en `App.jsx`.
4. Probar a 320 px.

## Trabajar en equipo con git

```bash
git pull                                  # antes de empezar
git checkout -b nombre-del-cambio         # una rama por cambio
# … cambios …
cd backend && ./mvnw test                 # que sigan pasando
cd frontend && npm run build              # que compile
git add -A && git commit -m "Qué cambia y por qué"
git push -u origin nombre-del-cambio      # y abrir un pull request en GitHub
```

**Nunca subir:** el PDF de la tesis, `CONTEXTO.md`, backups de la base (`backup/`), archivos `.env` ni contraseñas reales.
El `.gitignore` ya los excluye; igual conviene mirar `git status` antes de cada commit.

## Herramientas útiles

- **Ver la base:** MySQL Workbench con el usuario `esquina` / `esquina`.
- **Probar la API a mano:** `curl`, Postman o la extensión *REST Client* de VS Code. Para el panel, primero `POST /auth/login`
  y después mandar `Authorization: Bearer <token>`.
- **Emails en desarrollo:** están apagados; se ven en la consola de la API como `[email deshabilitado] Para … — asunto`.
