# Publicar en Google Play — respuestas para Play Console

Guía para rellenar Play Console con datos que coinciden con lo que la app hace de verdad
(ver [PRIVACY.md](../PRIVACY.md)). Los nombres de los menús pueden variar un poco; el sentido no.

## Datos generales
| Campo | Valor |
|---|---|
| Nombre de la app | Pronto Soggiorno |
| Identificador (package) | `io.github.hectorl94.prontosoggiorno` (no se puede cambiar nunca) |
| Idioma predeterminado | Inglés (en-US); traducciones: it-IT, es-ES, fr-FR, ar, ru-RU, uk |
| ¿App o juego? | App |
| ¿Gratis o de pago? | Gratis (no se puede pasar a pago después) |
| Categoría | Herramientas (*Tools*) |
| Correo de contacto (público) | hlicea.dev@gmail.com |
| Sitio web | https://github.com/HectorL94/pronto-soggiorno |
| Política de privacidad | https://github.com/HectorL94/pronto-soggiorno/blob/main/PRIVACY.md |

Textos de la ficha: copiar de `fastlane/metadata/android/<idioma>/`
(`title.txt`, `short_description.txt`, `full_description.txt`, `changelogs/1.txt`).

## Contenido de la app (*Policy → App content*)

### Acceso a la app
"Toda la funcionalidad está disponible sin acceso especial" (no hay cuentas ni contraseñas).

### Anuncios
"No, mi app no contiene anuncios".

### Clasificación de contenido (cuestionario IARC)
- Categoría: *Utilidad, productividad, comunicación u otra*.
- Todas las preguntas (violencia, sexo, lenguaje, drogas, apuestas, interacción entre usuarios,
  compartir ubicación, compras digitales…): **No**.
- Resultado esperado: PEGI 3 / Everyone.

### Público objetivo
- Edad: **18 y más** solamente (es un trámite de adultos).
- ¿Puede atraer a menores sin querer? **No**.

### ID de publicidad
"No, mi app no usa el ID de publicidad" (no hay Google Play Services ni librerías de anuncios).

### App de noticias / Salud / Finanzas / COVID-19
No / No / Ninguna función financiera / No.

### App gubernamental
**No**. La app no está desarrollada por ni para un organismo público. La ficha lo dice al
principio y cita la fuente (`questure.poliziadistato.it`), como exige la política de Google
sobre información de la Administración.

### Permisos (no requieren formulario)
`INTERNET`, `POST_NOTIFICATIONS` y, añadidos por WorkManager, `ACCESS_NETWORK_STATE`,
`RECEIVE_BOOT_COMPLETED` y `WAKE_LOCK`. Son permisos normales. **No** hay servicio en primer plano
(`FOREGROUND_SERVICE` se quita a propósito en el manifiesto), ni ubicación, ni alarmas exactas,
así que Play Console no debería pedir declaraciones de permisos.

## Seguridad de los datos (*Data safety*)

Criterio: Google considera "recopilado" todo dato que **sale del teléfono**, aunque vaya
directamente a un tercero y el desarrollador no lo reciba. El número de pratica sale del
teléfono (hacia el portal), así que se declara. Declarar de más no penaliza; declarar de menos
puede provocar el rechazo o la retirada de la app.

### Paso 1 — Recopilación y seguridad
| Pregunta | Respuesta |
|---|---|
| ¿Tu app recopila o comparte alguno de los tipos de datos de usuario obligatorios? | **Sí** |
| ¿Todos los datos se cifran en tránsito? | **Sí** (solo HTTPS; HTTP prohibido) |
| ¿Qué métodos de creación de cuenta admite? | **Mi app no permite crear cuentas** |
| ¿Los usuarios pueden pedir que se eliminen sus datos? | **Sí**: desde la app (eliminar una pratica o Ajustes → Borrar todos los datos); desinstalar también borra todo |

### Paso 2 — Tipos de datos
Marcar **solo**: *Información personal → Otra información* (el número de pratica o assicurata).

Todo lo demás **no** se marca: nombre, correo, ubicación, contactos, fotos, mensajes, actividad,
identificadores del dispositivo, diagnósticos/fallos… La app no los recoge. El alias y el
historial se quedan en el teléfono, y lo que no sale del teléfono no cuenta como recopilado.

### Paso 3 — Uso de "Otra información"
| Pregunta | Respuesta |
|---|---|
| ¿Se recopila? | **Sí** |
| ¿Se comparte? | **Sí**: se envía a `questure.poliziadistato.it` (un tercero) para consultar el estado |
| ¿Se procesa de forma efímera? | **No** (no podemos garantizar qué guarda el portal) |
| ¿Es obligatorio u opcional? | **Obligatorio** (sin número la app no tiene función) |
| Finalidad (recopilación y compartición) | **Funcionalidad de la app** únicamente |

Resultado que verán los usuarios en la ficha: "Comparte: otra información" y "Recopila: otra
información", "Datos cifrados en tránsito", "Puedes solicitar que se eliminen los datos".
Es exactamente lo que explica la política de privacidad.

## Gráficos de la ficha
| Recurso | Archivo |
|---|---|
| Icono 512 × 512 | `fastlane/metadata/android/en-US/images/icon.png` |
| Gráfico destacado 1024 × 500 | `fastlane/metadata/android/en-US/images/featureGraphic.png` |
| Capturas de teléfono (6 por idioma, 1200 × 2400) | `fastlane/metadata/android/<en-US, it-IT, es-ES, ar>/images/phoneScreenshots/1…6.png` |

Para los demás idiomas (fr-FR, ru-RU, uk) Google Play muestra las capturas del idioma
predeterminado (inglés).

Regenerarlos (p. ej. tras cambiar el diseño):
- `python tools/store_graphics.py` → icono y gráfico destacado.
- `./gradlew :app:assembleDebug :app:assembleDebugAndroidTest` y `python tools/screenshots.py`
  → capturas. Usa datos ficticios, corta toda la red del emulador durante el proceso y borra los
  datos de demostración antes de devolverla. Nunca hacer capturas con números reales.

## Primera publicación (cuenta personal nueva)
1. Generar el paquete: `./gradlew :app:bundleRelease` → `app/build/outputs/bundle/release/app-release.aab`
   (firmado con tu clave gracias a `keystore.properties`).
2. Aceptar **Play App Signing**: Google guarda la clave final; tu clave queda como "clave de subida"
   (si la pierdes, se puede sustituir con un trámite).
3. **Prueba cerrada**: crear la pista *Closed testing*, añadir al menos **12 testers** (por correo
   o con un grupo de Google) y mantenerla **14 días seguidos** con ellos inscritos.
4. Pasados los 14 días, solicitar el acceso a **Producción** desde el panel; Google hace unas
   preguntas sobre la prueba y revisa la app.
5. Cada versión nueva: subir `versionCode` en `app/build.gradle.kts` y añadir
   `fastlane/metadata/android/<idioma>/changelogs/<versionCode>.txt`.
