"""Vérifie que le code commun des deux mods est identique, au nom du package près. À lancer avant chaque sortie."""
import sys
from pathlib import Path

SHARED = ["platform/PlatformHolder.java", "platform/Registered.java", "data/FieldSet.java", "data/JsonTree.java", "data/JsonUtil.java", "data/ModFiles.java"]
bw = Path(sys.argv[1]) / "common/src/main/java/com/betterweaponry"
cr = Path(sys.argv[2]) / "common/src/main/java/com/combatreforged"


def normal(text):
    return text.replace("Combat Reforged", "Better Weaponry").replace("com.combatreforged", "com.betterweaponry").replace("CombatReforged", "BetterWeaponry").replace("combat_reforged", "better_weaponry").replace("combatreforged", "betterweaponry")


different = [name for name in SHARED if not (bw / name).exists() or normal((cr / name).read_text(encoding="utf-8")) != normal((bw / name).read_text(encoding="utf-8"))]
print("Differences:" if different else "Shared code is identical.")
for name in different:
    print(" -", name)
sys.exit(1 if different else 0)
