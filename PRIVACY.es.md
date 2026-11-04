# Pronto Soggiorno — Política de privacidad

*Disponible también en: [English](PRIVACY.md) · [Italiano](PRIVACY.it.md). En caso de diferencias prevalece la versión en inglés.*

**Fecha de entrada en vigor:** 4 de noviembre de 2026 · **Versión de la app:** 0.1.0 y posteriores

Pronto Soggiorno es una app Android gratuita y de código abierto que consulta la página pública
de estado del permiso de residencia italiano (`questure.poliziadistato.it`) y te avisa cuando
cambia el estado de tu solicitud. Es un proyecto **independiente**, **no afiliado** a la Polizia
di Stato, al Ministero dell'Interno ni a Poste Italiane.

## En resumen
- La app **no tiene servidores, cuentas, analíticas, publicidad ni envío de informes de errores**.
- Tus datos **se quedan en tu teléfono**. El desarrollador **no recibe** ninguno.
- La única conexión externa de la app es con `questure.poliziadistato.it`, para consultar el
  estado de los números que has añadido.

## Qué guarda la app en tu teléfono
| Dato | Para qué | Cómo se protege |
|---|---|---|
| Números que añades (*pratica* o *assicurata*) | Para consultar su estado | Cifrados (AES-256-GCM) con una clave guardada en el Keystore de Android |
| Nombres que les pones (p. ej. "Yo", "Mamá"), opcionales | Para mostrarlos en lugar del número | Cifrados de la misma forma |
| Historial de verificaciones (fecha, texto publicado por el portal, oficina de retirada, tipo de error técnico) | Para mostrar los cambios y detectar otros nuevos | En el almacenamiento privado de la app |
| Ajustes (frecuencia, preferencia de avisos, si ya viste la bienvenida) | Para recordar tus elecciones | En el almacenamiento privado de la app |

El almacenamiento privado de la app no lo pueden leer otras apps. **Las copias de seguridad de
Android están desactivadas** para esta app: estos datos no se copian a la nube ni a un teléfono nuevo.

## Qué se envía y a quién
Cada verificación envía **una petición HTTPS** a `https://questure.poliziadistato.it/stranieri/`
que contiene:
- el número de la solicitud que se consulta (así funciona el portal: es la misma petición que hace
  tu navegador cuando consultas el estado en la web);
- la información técnica estándar de cualquier conexión: tu dirección IP, la cabecera
  `User-Agent: ProntoSoggiorno/<versión>` y el tipo de contenido solicitado.

No se envían nombres, alias, identificadores del teléfono ni ningún otro dato. La app no guarda ni
envía cookies. El portal lo gestiona la Polizia di Stato (y sus proveedores técnicos, como redes
de distribución de contenidos); a las peticiones que recibe se aplica su propia política.

Las verificaciones se hacen automáticamente en segundo plano con la frecuencia elegida (cada 6, 12
o 24 horas), justo después de añadir un número y cuando tocas "Verificar ahora". La app consulta los
números uno tras otro, nunca en paralelo. Si el portal no encuentra un número durante 30 días
seguidos, se pausa su verificación automática.

Garantías técnicas: la app solo puede conectarse a `questure.poliziadistato.it` por HTTPS
(cualquier otra dirección se bloquea dentro de la app), las conexiones sin cifrar están prohibidas
y no se siguen redirecciones.

## Enlaces que eliges abrir
"Abrir portal", "Horario de la oficina" y "Código fuente" abren tu **navegador**. La app no se
conecta a esas páginas; se aplica la política de tu navegador.

## Notificaciones
Las notificaciones se generan en tu teléfono. Muestran el nombre que diste a la solicitud, **nunca
el número**, y en la pantalla de bloqueo solo aparece un mensaje genérico.

## Permisos
- **Internet**: para contactar con `questure.poliziadistato.it`.
- **Notificaciones** (Android 13+, opcional): para avisarte de los cambios. La app funciona sin él.
- Permisos técnicos que usa el planificador de Android (WorkManager), concedidos automáticamente y sin
  relación con tus datos: estado de la red (para esperar a que haya conexión), arrancar tras reiniciar el
  teléfono (para mantener la programación) y mantener el teléfono despierto los pocos segundos que dura
  una verificación.

## Cuánto tiempo se guardan los datos
- Las verificaciones rutinarias (sin cambios o fallidas) se borran solas a los 60 días.
- Los cambios de estado y la última verificación de cada solicitud se guardan hasta que los borres.
- Puedes eliminar una solicitud (con su historial) desde su pantalla, o todo desde
  **Ajustes → Borrar todos los datos**. Desinstalar la app también borra todos sus datos.

## Tus derechos
Como todos los datos están en tu dispositivo y bajo tu control, puedes verlos, cambiarlos y
borrarlos cuando quieras desde la app. El desarrollador no guarda datos tuyos, así que no hay nada
que solicitarle. Para los datos de tu solicitud que tengan las autoridades, dirígete a la Polizia
di Stato / tu Questura.

## Menores
La app está pensada para adultos que realizan trámites de residencia. No está dirigida a menores.

## Añade solo números que tengas derecho a consultar
Añade solo tus números, o los de personas que te hayan pedido consultarlos.

## Código abierto
El código fuente completo es público en <https://github.com/HectorL94/pronto-soggiorno>
(GPL-3.0): cualquiera puede comprobar lo que dice esta política.

## Cambios
Los cambios se publican en este archivo del repositorio, con una nueva fecha de entrada en vigor.
El historial de cada cambio es visible en el repositorio.

## Contacto
Preguntas o dudas: escribe a <hlicea.dev@gmail.com> o abre una *issue* en
<https://github.com/HectorL94/pronto-soggiorno/issues>.
