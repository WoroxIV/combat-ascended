package com.combatascended.combat;

import com.combatascended.CombatAscended;
import com.combatascended.data.JsonUtil;
import com.combatascended.data.WeaponData;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Lit les fichiers que les mods d'armes livrent pour Better Combat ({@code data/<mod>/weapon_attributes/<objet>.json})
 * et Epic Fight ({@code data/<mod>/capabilities/weapons/<objet>.json}). Ces fichiers restent lisibles même sans ces mods,
 * et disent mieux que le nom de l'objet de quelle arme il s'agit.
 */
public final class ForeignWeaponData {
    private static final String BETTER_COMBAT_FOLDER = "weapon_attributes";
    private static final String EPIC_FIGHT_FOLDER = "capabilities/weapons";
    private static final String JSON_SUFFIX = ".json";
    private static final String PARENT_KEY = "parent";
    private static final String TYPE_KEY = "type";

    private static volatile Map<ResourceLocation, Found> found = Map.of();

    private ForeignWeaponData() {
    }

    private record Found(ResourceLocation archetype, WeaponData.Source source) {
    }

    /** Relit les fichiers. Appelé à chaque rechargement des données. Better Combat passe avant Epic Fight. */
    public static void reload(ResourceManager resourceManager) {
        Map<ResourceLocation, Found> result = new HashMap<>();
        read(resourceManager, EPIC_FIGHT_FOLDER, TYPE_KEY, WeaponData.Source.EPIC_FIGHT, result);
        read(resourceManager, BETTER_COMBAT_FOLDER, PARENT_KEY, WeaponData.Source.BETTER_COMBAT, result);
        found = Map.copyOf(result);
    }

    private static void read(ResourceManager resourceManager, String folder, String key, WeaponData.Source source, Map<ResourceLocation, Found> result) {
        resourceManager.listResources(folder, path -> path.getPath().endsWith(JSON_SUFFIX)).forEach((location, resource) -> {
            String path = location.getPath().substring(folder.length() + 1, location.getPath().length() - JSON_SUFFIX.length());
            ResourceLocation item = new ResourceLocation(location.getNamespace(), path);
            value(resource, key).ifPresent(archetype -> result.put(item, new Found(archetype, source)));
        });
    }

    private static Optional<ResourceLocation> value(Resource resource, String key) {
        try (Reader reader = resource.openAsReader()) {
            JsonElement root = JsonUtil.parseLenient(new String(readAll(reader)));
            if (!root.isJsonObject()) return Optional.empty();
            JsonObject object = root.getAsJsonObject();
            if (!object.has(key) || !object.get(key).isJsonPrimitive()) return Optional.empty();
            return Optional.ofNullable(ResourceLocation.tryParse(object.get(key).getAsString()));
        } catch (IOException | JsonParseException e) {
            CombatAscended.LOGGER.debug("[Combat Ascended] Could not read {}", resource.sourcePackId(), e);
            return Optional.empty();
        }
    }

    private static char[] readAll(Reader reader) throws IOException {
        StringBuilder builder = new StringBuilder();
        char[] buffer = new char[2048];
        int read;
        while ((read = reader.read(buffer)) != -1) builder.append(buffer, 0, read);
        return builder.toString().toCharArray();
    }

    static Optional<ResourceLocation> archetype(ResourceLocation item) {
        return Optional.ofNullable(found.get(item)).map(Found::archetype);
    }

    static WeaponData.Source source(ResourceLocation item) {
        Found value = found.get(item);
        return value == null ? WeaponData.Source.NAME : value.source();
    }
}
