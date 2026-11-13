package com.combatascended.combat;

import com.combatascended.config.ConfigFolder;
import com.combatascended.data.JsonUtil;
import com.combatascended.data.WeaponData;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Le rapport de {@code /combatascended scan} : ce que chaque arme a reçu, pourquoi, et ce qui a été ignoré. */
public final class WeaponAudit {
    private static final String REPORT_FILE = "scan.json";

    private WeaponAudit() {
    }

    public record Entry(ResourceLocation item, WeaponData.ItemProfile profile) {
    }

    public record Report(List<Entry> entries, List<ResourceLocation> ignored, Map<String, Integer> bySource) {
    }

    public static Report run() {
        Map<ResourceLocation, WeaponData.ItemProfile> table = WeaponProfiles.table();
        List<Entry> entries = new ArrayList<>();
        Map<String, Integer> bySource = new TreeMap<>();
        table.forEach((item, profile) -> {
            entries.add(new Entry(item, profile));
            bySource.merge(profile.source().getSerializedName(), 1, Integer::sum);
        });
        List<ResourceLocation> ignored = new ArrayList<>();
        for (Holder.Reference<Item> holder : BuiltInRegistries.ITEM.holders().toList()) {
            ResourceLocation id = holder.key().location();
            if (!table.containsKey(id) && WeaponDetector.isWeapon(holder.value(), holder)) ignored.add(id);
        }
        return new Report(entries, ignored, bySource);
    }

    /** @return le chemin du rapport, tel qu'affiché au joueur */
    public static String export(Report report) throws IOException {
        JsonArray entries = new JsonArray();
        for (Entry entry : report.entries()) {
            JsonObject line = new JsonObject();
            line.addProperty("item", entry.item().toString());
            line.addProperty("profile", entry.profile().profile().toString());
            line.addProperty("reason", entry.profile().source().getSerializedName());
            line.addProperty("keeps_own_right_click", entry.profile().keepsOwnRightClick());
            entries.add(line);
        }
        JsonArray ignored = new JsonArray();
        report.ignored().forEach(id -> ignored.add(id.toString()));
        JsonObject root = new JsonObject();
        root.add("weapons", entries);
        root.add("ignored_weapons", ignored);

        Path file = ConfigFolder.root().resolve(REPORT_FILE);
        Files.createDirectories(file.getParent());
        Files.writeString(file, JsonUtil.PRETTY.toJson(root) + "\n", StandardCharsets.UTF_8);
        return ConfigFolder.displayPath(file);
    }
}
