# 2. Instalación y puesta en marcha

## Requisitos

| Programa | Versión | Para qué |
|---|---|---|
| Java (JDK) | 21 | La API. Recomendado: [Eclipse Temurin 21](https://adoptium.net/) |
| MySQL Server | 8.x | La base de datos |
| MySQL Workbench | cualquiera | Crear la base y mirar las tablas (opcional pero cómodo) |
| Node.js | 18 o más | El frontend |
| Git | cualquiera | Clonar el repo |

Maven **no** hace falta instalarlo: el proyecto trae su propio *wrapper* (`mvnw` / `mvnw.cmd`) que lo descarga solo.
Docker tampoco se usa.

## Primera vez

### 1. Clonar

```bash
git clone https://github.com/valencastaldi/barberia-esquina.git
cd barberia-esquina
```

### 2. Crear la base (una sola vez)

La base **no viene en el repo**: la arma la API. Solo hay que crear una base vacía y un usuario:

1. Abrir MySQL Workbench y conectarse como **root**.
2. *File → Open SQL Script* → `backend/db/crear-base.sql`.
3. Ejecutar con el rayo ⚡. Tiene que aparecer `esquina_turnos` en el resultado.

Eso crea la base `esquina_turnos` y el usuario `esquina` / `esquina`, que solo tiene permisos sobre esa base.

### 3. Levantar la API

```bash
cd backend
./mvnw spring-boot:run        # en Windows (cmd o PowerShell): mvnw.cmd spring-boot:run
```

La primera vez tarda un poco (descarga dependencias). Al arrancar:

- **Flyway** crea las 9 tablas (`db/migration/V1__esquema.sql`);
- se crea el **dueño** y se cargan **datos de ejemplo**: 3 peluqueros, 5 servicios, 150 clientes y unos 550 turnos
  de las últimas 6 semanas y la próxima.

Queda escuchando en <http://localhost:8080/api/v1>. Prueba rápida: abrir <http://localhost:8080/api/v1/servicios>.

### 4. Levantar el frontend (otra terminal)

```bash
cd frontend
npm install        # solo la primera vez
npm run dev
```

Abrir:

- <http://localhost:5173> — sitio del cliente
- <http://localhost:5173/admin> — panel de gestión (o el link "Acceso del equipo" al pie del sitio)

## Usuarios de desarrollo

| Email | Contraseña | Rol |
|---|---|---|
| `agustin@barberiaesquina.com` | `esquina1290` | Dueño |
| `santiago@barberiaesquina.com` | `esquina1290` | Barbero |
| `joaquin@barberiaesquina.com` | `esquina1290` | Barbero |

> ⚠ Son públicas (están en este repo). Sirven solo para desarrollo. En producción se configuran otras (ver abajo).

## Día a día

Cada vez que se prende la PC hacen falta **las dos** partes corriendo:

```bash
cd backend && ./mvnw spring-boot:run
```

```bash
cd frontend && npm run dev
```

MySQL corre como servicio de Windows (`MySQL80`), así que normalmente ya está prendido.

## Probar desde el celular

El celular tiene que estar en **el mismo WiFi** que la PC (mismo router). En lugar de `npm run dev`:

```bash
cd frontend
npm run dev:red
```

Vite muestra una línea `Network: http://192.168.x.x:5173/`: esa dirección se abre en el navegador del celular
(y `/admin` para el panel). Solo hace falta el front visible en la red: la API la alcanza a través de él.

Si no abre:
- **La PC y el celular están en redes distintas** (datos móviles, WiFi de invitados): conectá el celular al mismo WiFi.
- **El firewall de Windows lo bloquea:** la primera vez Windows pregunta si permitir Node.js; hay que aceptar.
- **La IP cambió** (el router la reasigna): mirala de nuevo en la salida de `npm run dev:red`.

> Los links de los emails (cancelar, encuesta) apuntan a `FRONTEND_URL`, que en desarrollo es `localhost`: desde el
> celular no abren. Para probarlos, copiá el link y cambiá `localhost` por la IP de la PC.

## Probar el envío de emails

En desarrollo los emails están apagados (se anotan en la consola). Para que salgan de verdad, con la cuenta de Gmail de la
barbería:

1. La cuenta necesita la verificación en 2 pasos activada. Después, en
   <https://myaccount.google.com/apppasswords> se crea una **contraseña de aplicación** (16 letras). La contraseña
   normal de la cuenta no sirve.
2. En IntelliJ: **Run → Edit Configurations… → (configuración del backend) → Environment variables**:
   ```
   MAIL_HABILITADO=true;MAIL_HOST=smtp.gmail.com;MAIL_PORT=587;MAIL_USER=cuenta@gmail.com;MAIL_PASSWORD=clave-de-16-letras;MAIL_REMITENTE=Barbería Esquina <cuenta@gmail.com>
   ```
3. Reiniciar la API y reservar un turno con un email propio. En la consola: `Email '…' enviado a …`.

> ⚠ La contraseña de aplicación **nunca** va en el repo (ni en `application.yml`): se pasa por privado. Mejor que cada
> uno cree la suya en la misma cuenta, así se puede revocar una sin cortar las demás.

`535 Username and Password not accepted` → la clave está mal o IntelliJ sigue con la configuración vieja
(**File → Reload All from Disk** y volver a arrancar).

## Compartir una base con datos

No hace falta para trabajar: cada uno tiene su base con los mismos datos de ejemplo. Pero si se quiere pasar **la base
exacta** de alguien (con los turnos que cargó), se hace con un backup:

**Generar** (en la PC que tiene los datos):

```bash
mysqldump -uesquina -pesquina --no-tablespaces --single-transaction esquina_turnos > backup/esquina_turnos.sql
```

**Cargar** (en la otra PC, después del paso 2):
*Workbench → Server → Data Import → Import from Self-Contained File* → elegir el `.sql`, *Default Target Schema:*
`esquina_turnos` → *Start Import*. Después se arranca la API normalmente: al ver que la base ya tiene datos, no crea nada.

> La carpeta `backup/` está en el `.gitignore`: los backups se pasan por privado, **nunca** se suben al repo público
> (tienen datos de clientes).

## Empezar de cero

En Workbench: `DROP DATABASE esquina_turnos;`, volver a correr `crear-base.sql` y reiniciar la API. Se vuelve a crear todo
con datos de ejemplo nuevos.

## Perfiles de configuración

La misma API se configura distinto según el perfil (`SPRING_PROFILES_ACTIVE`):

| Perfil | Base | Emails | Datos de ejemplo | Para qué |
|---|---|---|---|---|
| *(ninguno)* | MySQL local | Solo al log | Sí | Desarrollo normal |
| `h2` | H2 en archivo (`backend/data/`), modo MySQL | Solo al log | Sí | Probar sin MySQL |
| `prod` | MySQL del servidor | SMTP real | No | Producción |

Sin MySQL: `./mvnw spring-boot:run -Dspring-boot.run.profiles=h2`.

## Variables de entorno

Todas tienen un valor por defecto para desarrollo (`application.yml`). En `prod` las marcadas son obligatorias: si falta
alguna, la API no arranca.

| Variable | Por defecto (dev) | En prod |
|---|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/esquina_turnos…` | la del servidor |
| `DB_USER` / `DB_PASSWORD` | `esquina` / `esquina` | propias |
| `JWT_SECRET` | secreto de desarrollo | **obligatoria**, 32+ caracteres |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` | `agustin@…` / `esquina1290` | contraseña **obligatoria** |
| `FRONTEND_URL` | `http://localhost:5173` | dominio del sitio (se usa en los links de los emails) |
| `ORIGENES_PERMITIDOS` | `http://localhost:5173` | dominio del sitio (CORS) |
| `MAIL_HABILITADO` | `false` | `true` |
| `MAIL_HOST` / `MAIL_PORT` / `MAIL_USER` / `MAIL_PASSWORD` | `localhost` / `587` | SMTP real (por ejemplo Gmail con contraseña de aplicación) |
| `MAIL_REMITENTE` | `Barbería Esquina <turnos@barberiaesquina.com>` | con Gmail, la misma cuenta de `MAIL_USER`: `Barbería Esquina <cuenta@gmail.com>` |
| `DATOS_DEMO` | `true` | `false` (forzado por el perfil) |
| `PORT` | `8080` | el del servidor |

## Problemas comunes

| Síntoma | Causa | Solución |
|---|---|---|
| `Access denied for user 'esquina'` al arrancar la API | No se corrió `crear-base.sql` | Paso 2 |
| `Communications link failure` | MySQL apagado | Iniciar el servicio `MySQL80` (Servicios de Windows) |
| `JAVA_HOME is not set` / `java` no se reconoce | Java no instalado o no en el PATH | Instalar JDK 21 o definir `JAVA_HOME` |
| `Port 8080 was already in use` | Ya hay una API corriendo | Cerrar la otra terminal o cambiar `PORT` |
| La página abre pero todo dice "No pudimos conectarnos" | La API no está corriendo | Levantar el backend |
| `localhost:8080` se ve en blanco | Es normal: ahí está la API, no las pantallas | Ir a `localhost:5173` |
| La pantalla no tiene "Con quién" / los datos no cambian | Estás en la maqueta (`maqueta/`), no en la app | Usar `localhost:5173` |
| El login del panel dice "Tu sesión venció" | El token dura 8 h | Volver a ingresar |
