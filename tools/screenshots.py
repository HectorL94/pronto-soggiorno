"""Capturas de pantalla para la tienda, en el emulador.

Uso (con el emulador encendido y la app compilada):
    ./gradlew :app:assembleDebug :app:assembleDebugAndroidTest
    python tools/screenshots.py

- Desinstala e instala la app limpia (se pierden los datos de la app en el emulador).
- Corta TODA la red (modo avión + Wi-Fi + datos) y comprueba que no hay internet antes de
  empezar: así ni la verificación automática ni nada más puede consultar el portal.
- Usa datos de demostración ficticios (androidTest ScreenshotSeeder: números 00ZZ…).
- Guarda las capturas en fastlane/metadata/android/<idioma>/images/phoneScreenshots/,
  rellenadas a 1200 x 2400 (Google Play no admite un lado más del doble que el otro).
"""
import os
import re
import subprocess
import time
import xml.etree.ElementTree as ET
from io import BytesIO
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
ADB = str(Path(os.environ.get("LOCALAPPDATA", "")) / "Android/Sdk/platform-tools/adb.exe")
PKG = "io.github.hectorl94.prontosoggiorno"
ACTIVITY = f"{PKG}/app.prontosoggiorno.MainActivity"
APKS = [
    ROOT / "app/build/outputs/apk/debug/app-debug.apk",
    ROOT / "app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk",
]
# carpeta de fastlane -> (etiqueta de idioma de la app, carpeta de recursos)
LOCALES = {
    "en-US": ("en", "values"),
    "it-IT": ("it", "values-it"),
    "es-ES": ("es", "values-es"),
    "ar": ("ar", "values-ar"),
}


def adb(*args, binary=False):
    out = subprocess.run([ADB, *args], capture_output=True, check=False)
    return out.stdout if binary else out.stdout.decode("utf-8", "replace")


def strings(folder):
    values = {}
    for f in (ROOT / "app/src/main/res" / folder).glob("strings*.xml"):
        for el in ET.parse(f).getroot().iter("string"):
            text = (el.text or "").replace("\\'", "'").replace('\\"', '"')
            values[el.get("name")] = text
    return values


def nodes():
    adb("shell", "uiautomator", "dump", "/sdcard/ui.xml")
    xml = adb("shell", "cat", "/sdcard/ui.xml")
    xml = xml[xml.find("<?xml"):]
    for n in ET.fromstring(xml).iter("node"):
        x1, y1, x2, y2 = map(int, re.findall(r"\d+", n.get("bounds")))
        yield n.get("text"), n.get("content-desc"), ((x1 + x2) // 2, (y1 + y2) // 2)


def wait_for(label, timeout=15):
    end = time.time() + timeout
    while time.time() < end:
        for text, desc, center in nodes():
            if label in (text, desc):
                return center
        time.sleep(0.7)
    raise RuntimeError(f"No aparece en pantalla: {label!r}")


def tap(label):
    x, y = wait_for(label)
    adb("shell", "input", "tap", str(x), str(y))
    time.sleep(1.5)


def tap_fab():
    """El botón flotante no expone su texto al sistema: se busca el botón de abajo a la derecha."""
    adb("shell", "uiautomator", "dump", "/sdcard/ui.xml")
    xml = adb("shell", "cat", "/sdcard/ui.xml")
    root = ET.fromstring(xml[xml.find("<?xml"):])
    best = None
    for n in root.iter("node"):
        if n.get("class") == "android.widget.Button":
            x1, y1, x2, y2 = map(int, re.findall(r"\d+", n.get("bounds")))
            if best is None or y2 > best[3]:
                best = (x1, y1, x2, y2)
    adb("shell", "input", "tap", str((best[0] + best[2]) // 2), str((best[1] + best[3]) // 2))
    time.sleep(1.5)


def back():
    adb("shell", "input", "keyevent", "4")
    time.sleep(1.2)


def shot(locale, number):
    png = adb("exec-out", "screencap", "-p", binary=True)
    img = Image.open(BytesIO(png)).convert("RGB")
    w, h = img.size
    width = max(w, (h + 1) // 2)  # lado largo como máximo el doble del corto
    canvas = Image.new("RGB", (width, h), img.getpixel((2, h // 2)))
    canvas.paste(img, ((width - w) // 2, 0))
    out = ROOT / "fastlane/metadata/android" / locale / "images/phoneScreenshots"
    out.mkdir(parents=True, exist_ok=True)
    canvas.save(out / f"{number}.png", optimize=True)
    print(f"  {locale}/{number}.png {canvas.size}")


def open_app(lang):
    adb("shell", "cmd", "locale", "set-app-locales", PKG, "--locales", lang)
    adb("shell", "am", "force-stop", PKG)
    adb("shell", "am", "start", "-n", ACTIVITY)
    time.sleep(2.5)


def demo_mode(on):
    b = ["shell", "am", "broadcast", "-a", "com.android.systemui.demo", "-e", "command"]
    if not on:
        adb(*b, "exit")
        return
    adb("shell", "settings", "put", "global", "sysui_demo_allowed", "1")
    adb(*b, "enter")
    adb(*b, "clock", "-e", "hhmm", "1000")
    adb(*b, "battery", "-e", "level", "100", "-e", "plugged", "false")
    adb(*b, "network", "-e", "wifi", "show", "-e", "level", "4", "-e", "fully", "true")
    adb(*b, "network", "-e", "mobile", "show", "-e", "level", "4", "-e", "datatype", "none")
    adb(*b, "network", "-e", "airplane", "hide")
    adb(*b, "notifications", "-e", "visible", "false")


def network(on):
    state = "enable" if on else "disable"
    adb("shell", "cmd", "connectivity", "airplane-mode", "disable" if on else "enable")
    adb("shell", "svc", "wifi", state)  # en modo avión Android puede dejar el Wi-Fi encendido
    adb("shell", "svc", "data", state)
    if not on:
        assert_offline()


def assert_offline(timeout=30):
    """Espera a que Android no tenga NINGUNA red; si no, se detiene (para no consultar el portal)."""
    end = time.time() + timeout
    while time.time() < end:
        if "Active default network: none" in adb("shell", "dumpsys", "connectivity"):
            return
        time.sleep(1)
    raise RuntimeError("Sigue habiendo red: no se hacen capturas para no consultar el portal.")


def main():
    print("Cortando la red e instalando la app limpia…")
    network(False)
    adb("uninstall", PKG)
    for apk in APKS:
        adb("install", "-r", "-t", str(apk))
    adb("shell", "pm", "grant", PKG, "android.permission.POST_NOTIFICATIONS")
    network(False)
    demo_mode(True)
    try:
        # Primera pantalla de bienvenida (antes de cargar los datos de demostración).
        adb("shell", "am", "start", "-n", ACTIVITY)
        time.sleep(3)
        for locale, (lang, folder) in LOCALES.items():
            s = strings(folder)
            open_app(lang)
            wait_for(s["onb_welcome_title"])
            shot(locale, 4)

        assert_offline()
        print("Cargando datos de demostración…")
        result = adb("shell", "am", "instrument", "-w", "-e", "seedScreenshots", "true", "-e", "class",
                     "app.prontosoggiorno.ScreenshotSeeder", f"{PKG}.test/androidx.test.runner.AndroidJUnitRunner")
        if "OK (1 test)" not in result:
            raise RuntimeError("Fallo al cargar los datos de demostración:\n" + result)

        for locale, (lang, folder) in LOCALES.items():
            s = strings(folder)
            assert_offline()
            open_app(lang)
            wait_for("Amina")
            wait_for(s["status_in_progress"])  # Marco sigue "en trámite": nadie ha consultado el portal
            shot(locale, 1)
            tap("Amina"); wait_for(s["detail_ready_title"]); shot(locale, 2); back()
            tap("Marco"); wait_for(s["status_in_progress"]); shot(locale, 3); back()
            tap_fab(); wait_for(s["add_title"]); shot(locale, 5); back()
            tap(s["settings_title"]); wait_for(s["settings_language"]); shot(locale, 6); back()
    finally:
        demo_mode(False)
        # Primero se borran los datos de demostración y DESPUÉS vuelve la red: si no, la verificación
        # automática se pondría en marcha en cuanto hubiera conexión y consultaría los números ficticios.
        adb("shell", "pm", "clear", PKG)
        network(True)
    print("Listo.")


if __name__ == "__main__":
    main()
