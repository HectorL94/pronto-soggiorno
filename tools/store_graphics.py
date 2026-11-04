"""Genera los gráficos de la tienda a partir del diseño del icono de la app.

Uso:  python tools/store_graphics.py
Crea, en fastlane/metadata/android/en-US/images/:
  - icon.png            512 x 512 (icono de Google Play / F-Droid)
  - featureGraphic.png  1024 x 500 (gráfico destacado de Google Play)

Las formas son las mismas que app/src/main/res/drawable/ic_launcher_foreground.xml
(lienzo de 108 x 108) y los colores los del tema de la app.
"""
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "fastlane/metadata/android/en-US/images"
FONT = ROOT / "app/src/main/res/font/inter_variable.ttf"

NAVY = "#17365D"
TEAL = "#16A085"
WHITE = "#FFFFFF"
SOFT = "#C9D6EA"
SS = 4  # supermuestreo para bordes suaves


def draw_glyph(draw: ImageDraw.ImageDraw, x0: float, y0: float, scale: float) -> None:
    """Dibuja el símbolo del icono. (x0, y0) es dónde cae el punto (0, 0) del lienzo de 108."""

    def p(x, y):
        return (x0 + x * scale, y0 + y * scale)

    def rrect(x1, y1, x2, y2, r, fill):
        draw.rounded_rectangle([p(x1, y1), p(x2, y2)], radius=r * scale, fill=fill)

    def circle(cx, cy, r, fill):
        draw.ellipse([p(cx - r, cy - r), p(cx + r, cy + r)], fill=fill)

    rrect(28, 34, 70, 64, 5, WHITE)  # tarjeta
    rrect(32, 40, 46, 56, 2, SOFT)  # foto
    rrect(50, 43, 65, 46, 0, SOFT)  # líneas de texto
    rrect(50, 49, 61, 52, 0, SOFT)
    circle(68, 64, 14, NAVY)  # anillo de separación
    circle(68, 64, 11, TEAL)  # insignia
    pts = [p(62.5, 64.5), p(66.3, 68.3), p(73.7, 60.7)]  # marca de "listo"
    w = 3 * scale
    draw.line(pts, fill=WHITE, width=round(w), joint="curve")
    for x, y in (pts[0], pts[-1]):
        draw.ellipse([x - w / 2, y - w / 2, x + w / 2, y + w / 2], fill=WHITE)


def icon(size: int = 512) -> Image.Image:
    big = size * SS
    img = Image.new("RGB", (big, big), NAVY)
    # Se muestra la zona central de 72 x 72 del lienzo de 108, como hace el lanzador de Android.
    scale = big / 72
    draw_glyph(ImageDraw.Draw(img), -18 * scale, -18 * scale, scale)
    return img.resize((size, size), Image.LANCZOS)


def font(size: int, weight: int) -> ImageFont.FreeTypeFont:
    f = ImageFont.truetype(str(FONT), size)
    axes = {a["name"].decode() if isinstance(a["name"], bytes) else a["name"]: a for a in f.get_variation_axes()}
    values = []
    for name, axis in axes.items():
        if name.lower().startswith("weight"):
            values.append(weight)
        elif name.lower().startswith("optical"):
            values.append(min(max(size / SS, axis["minimum"]), axis["maximum"]))
        else:
            values.append(axis["default"])
    f.set_variation_by_axes(values)
    return f


def fit(draw, text, max_width, size, weight):
    while size > 10:
        f = font(size, weight)
        if draw.textlength(text, font=f) <= max_width:
            return f
        size -= 2
    return font(size, weight)


def feature_graphic() -> Image.Image:
    w, h = 1024 * SS, 500 * SS
    img = Image.new("RGB", (w, h), NAVY)
    draw = ImageDraw.Draw(img)
    # Franja diagonal sutil, solo decorativa.
    draw.polygon([(w * 0.62, 0), (w, 0), (w, h), (w * 0.48, h)], fill="#1B3D69")

    # Icono a la izquierda (cuadrado redondeado, como en el teléfono).
    tile = 300 * SS
    tx, ty = 70 * SS, (h - tile) // 2
    draw.rounded_rectangle([tx, ty, tx + tile, ty + tile], radius=66 * SS, fill="#0F2747")
    scale = tile / 72
    draw_glyph(draw, tx - 18 * scale, ty - 18 * scale, scale)

    # Textos a la derecha.
    left = 420 * SS
    max_w = w - left - 50 * SS
    title = fit(draw, "Pronto Soggiorno", max_w, 76 * SS, 700)
    note = fit(draw, "Independent app · Not affiliated with the Polizia di Stato", max_w, 21 * SS, 400)
    y = 150 * SS
    draw.text((left, y), "Pronto Soggiorno", font=title, fill=WHITE)
    y += 110 * SS
    draw.text((left, y), "Know when your permesso", font=font(34 * SS, 500), fill="#D5E3FF")
    y += 46 * SS
    draw.text((left, y), "di soggiorno is ready", font=font(34 * SS, 500), fill="#D5E3FF")
    y += 74 * SS
    draw.text((left, y), "Independent app · Not affiliated with the Polizia di Stato", font=note, fill="#ACC8F7")
    return img.resize((1024, 500), Image.LANCZOS)


if __name__ == "__main__":
    OUT.mkdir(parents=True, exist_ok=True)
    icon().save(OUT / "icon.png", optimize=True)
    feature_graphic().save(OUT / "featureGraphic.png", optimize=True)
    for f in ("icon.png", "featureGraphic.png"):
        im = Image.open(OUT / f)
        print(f, im.size, im.mode)
