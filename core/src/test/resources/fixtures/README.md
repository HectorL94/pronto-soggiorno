# Fixtures

Respuestas **reales** del feed `questure.poliziadistato.it/stranieri/?lang=italian&mime=4&pratica=…`,
anonimizadas: el número se sustituye siempre por `00ZZ000000` (formato válido pero imposible:
`ZZ` no es ninguna provincia).

| Archivo | Origen | Fecha |
|---|---|---|
| `ready.xml` | Pratica real del autor, con permiso listo. Número sustituido; espacios aproximados. | 2026-11-04 |
| `not_found.xml` | Consulta con el código inválido `ABC` (no pertenece a nadie). | 2026-11-04 |
| `in_progress.xml` | Pratica en trámite, en italiano. Compartida por el autor con el número ya oculto. | 2026-11-04 |
| `in_progress_es.xml` | La misma pratica en trámite, **en español** (`lang=espanol`): documenta que otros idiomas no se clasifican. | 2026-11-04 |

Reglas:
- **Nunca** consultar números ajenos para obtener muestras. Solo números propios o textos
  compartidos voluntariamente por sus titulares.
- Antes de añadir una fixture, sustituir el número por `00ZZ000000` y revisar que no queden
  datos personales.
- Cada patrón nuevo en `StatusPatterns` necesita su fixture real y su test.
