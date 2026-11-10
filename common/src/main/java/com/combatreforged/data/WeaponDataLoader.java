package com.combatreforged.data;

import com.combatreforged.CombatReforged;
import com.combatreforged.combat.ForeignWeaponData;
import com.combatreforged.combat.WeaponProfiles;
import com.combatreforged.config.ConfigFolder;
import com.combatreforged.config.LegacyBetterWeaponry;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;

/**
 * Charge les archétypes, les fichiers d'armes et les règles, au chargement d'un monde et à chaque {@code /reload}.
 * <p>
 * Pour chaque fichier, les couches sont fusionnées dans cet ordre (la dernière gagne) :
 * <ol>
 *   <li>les valeurs par défaut des mods ;</li>
 *   <li>les réglages de Better Weaponry 2.0 propres à un monde, le temps d'une transition ;</li>
 *   <li>le dossier {@code config/combat_reforged/} (tous les mondes) ;</li>
 *   <li>les datapacks du monde ({@code <monde>/datapacks}), pour des réglages propres à un monde
 *       (dont celui écrit par l'éditeur en jeu, {@value #EDITOR_PACK}).</li>
 * </ol>
 * Chemins dans un mod ou un datapack : {@code data/<mod>/combat_reforged/archetypes/<id>.json},
 * {@code .../weapons/<fichier>.json} et {@code .../detection/<fichier>.json}. Les règles sont dans
 * {@code data/combat_reforged/combat_reforged/rules.json} et {@code gear.json}.
 */
public final class WeaponDataLoader extends SimplePreparableReloadListener<WeaponDataLoader.Result> {
    /** Dossier des fichiers de ce mod dans un datapack : data/<mod>/combat_reforged/... */
    public static final String DATA_DIRECTORY = "combat_reforged";
    public static final String WEAPONS_DIRECTORY = "weapons";
    public static final String ARCHETYPES_DIRECTORY = "archetypes";
    public static final String DETECTION_DIRECTORY = "detection";
    public static final String RULES_FILE = "rules.json";
    public static final String GEAR_FILE = "gear.json";
    /** Liste des archétypes du jar : le jar ne se liste pas lui-même avant le chargement d'un monde. */
    private static final String ARCHETYPE_INDEX = "archetype_index.json";
    private static final String DEFAULT_DETECTION = DETECTION_DIRECTORY + "/default.json";

    /** Les datapacks placés dans le dossier du monde ont un identifiant qui commence par "file/". */
    private static final String WORLD_DATAPACK_PREFIX = "file/";
    /** Dossier du datapack écrit par l'éditeur en jeu pour les réglages "ce monde seulement". */
    public static final String EDITOR_PACK = "combat_reforged_editor";
    public static final String EDITOR_PACK_ID = WORLD_DATAPACK_PREFIX + EDITOR_PACK;

    private static final String JSON_SUFFIX = ".json";

    /** Problèmes du dernier chargement, montrés aux opérateurs qui se connectent. */
    private static volatile List<DataProblem> lastProblems = List.of();

    public record Result(WeaponData.Snapshot snapshot, List<DataProblem> problems) {
    }

    public static List<DataProblem> lastProblems() {
        return lastProblems;
    }

    // --- Rechargement (monde, /reload) ---

    @Override
    protected Result prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        return load(resourceManager);
    }

    @Override
    protected void apply(Result result, ResourceManager resourceManager, ProfilerFiller profiler) {
        install(result);
    }

    /** Lit et fusionne tous les fichiers (aussi utilisé par l'éditeur pour recharger sans /reload complet). */
    public static Result load(ResourceManager resourceManager) {
        List<DataProblem> problems = new ArrayList<>();
        ForeignWeaponData.reload(resourceManager);
        Map<ResourceLocation, WeaponDefinition> archetypes = resolveAll(mergeAll(resourceManager, ARCHETYPES_DIRECTORY, problems), Map.of(), problems);
        Map<ResourceLocation, WeaponDefinition> weapons = resolveAll(mergeAll(resourceManager, WEAPONS_DIRECTORY, problems),
                encodedAll(archetypes), problems);
        WeaponProfiles.reportConflicts(weapons, problems);

        RulesDefinition rules = decodeSingle(resourceManager, RULES_FILE, ConfigFolder.rulesFile(), RulesDefinition.FIELDS, RulesDefinition.CODEC, RulesDefinition.FALLBACK, problems);
        GearDefinition gear = decodeSingle(resourceManager, GEAR_FILE, ConfigFolder.gearFile(), GearDefinition.FIELDS, GearDefinition.CODEC, GearDefinition.FALLBACK, problems);
        return new Result(new WeaponData.Snapshot(archetypes, weapons, loadDetection(resourceManager, problems), rules, gear, Map.of()), List.copyOf(problems));
    }

    /** Met en vigueur un chargement et signale ses problèmes dans le log. */
    public static void install(Result result) {
        lastProblems = result.problems();
        for (DataProblem problem : result.problems()) {
            CombatReforged.LOGGER.warn("[Combat Reforged] {}", problem.toLogString());
        }
        WeaponData.install(result.snapshot());
        CombatReforged.LOGGER.info("Combat Reforged: combat data loaded ({} archetypes, {} weapon files, {} problem(s))",
                result.snapshot().archetypes().size(), result.snapshot().weapons().size(), result.problems().size());
    }

    private static <T> T decodeSingle(ResourceManager resourceManager, String file, Path configFile, FieldSet fields, Codec<T> codec, T fallback, List<DataProblem> problems) {
        JsonObject merged = fields.merge(collectLayers(resourceManager, CombatReforged.MODID, file, configFile, true, problems), problems);
        return decode(codec, merged, fallback, file, problems);
    }

    // --- Valeurs par défaut du jar (avant tout chargement de monde) ---

    /** Lit uniquement les fichiers du jar : sert de valeurs de départ, côté client comme côté serveur. */
    static WeaponData.Snapshot loadBuiltin() {
        List<DataProblem> problems = new ArrayList<>();
        Map<ResourceLocation, JsonObject> raw = new TreeMap<>();
        for (String name : builtinArchetypeNames()) {
            String file = ARCHETYPES_DIRECTORY + "/" + name + JSON_SUFFIX;
            ResourceLocation id = new ResourceLocation(CombatReforged.MODID, name);
            raw.put(id, WeaponDefinition.FIELDS.merge(builtinLayer(file, problems), problems));
        }
        Map<ResourceLocation, WeaponDefinition> archetypes = resolveAll(raw, Map.of(), problems);
        RulesDefinition rules = decode(RulesDefinition.CODEC, RulesDefinition.FIELDS.merge(builtinLayer(RULES_FILE, problems), problems),
                RulesDefinition.FALLBACK, RULES_FILE, problems);
        GearDefinition gear = decode(GearDefinition.CODEC, GearDefinition.FIELDS.merge(builtinLayer(GEAR_FILE, problems), problems),
                GearDefinition.FALLBACK, GEAR_FILE, problems);
        for (DataProblem problem : problems) {
            CombatReforged.LOGGER.error("[Combat Reforged] Problem in the mod's own default data: {}", problem.toLogString());
        }
        return new WeaponData.Snapshot(archetypes, Map.of(), builtinDetection(), rules, gear, Map.of());
    }

    public static List<String> builtinArchetypeNames() {
        JsonObject index = builtinJson(ARCHETYPE_INDEX).orElseGet(JsonObject::new);
        List<String> names = new ArrayList<>();
        if (index.has("archetypes")) index.getAsJsonArray("archetypes").forEach(name -> names.add(name.getAsString()));
        return names;
    }

    /** Fichier de l'archétype dans le jar, relatif au dossier des données. */
    public static String builtinArchetypePath(String name) {
        return ARCHETYPES_DIRECTORY + "/" + name + JSON_SUFFIX;
    }

    private static DetectionRules builtinDetection() {
        return builtinJson(DEFAULT_DETECTION).map(json -> DetectionRules.CODEC.parse(JsonOps.INSTANCE, json).result().orElse(DetectionRules.EMPTY)).orElse(DetectionRules.EMPTY);
    }

    /** Contenu JSON brut d'un fichier par défaut du jar, ex : {@code builtinJson("archetypes/katana.json")}. */
    public static Optional<JsonObject> builtinJson(String relativePath) {
        return ModFiles.read(builtinPathParts(relativePath)).flatMap(text -> {
            try {
                JsonElement element = JsonUtil.parseLenient(text);
                return element.isJsonObject() ? Optional.of(element.getAsJsonObject()) : Optional.empty();
            } catch (JsonParseException e) {
                return Optional.empty();
            }
        });
    }

    // --- Archétypes et armes ---

    /** Tous les fichiers d'un dossier, chacun fusionné avec ses couches : mods, ancienne version, config, datapacks du monde. */
    private static Map<ResourceLocation, JsonObject> mergeAll(ResourceManager resourceManager, String directory, List<DataProblem> problems) {
        Set<ResourceLocation> ids = new TreeSet<>(resourceIds(resourceManager, directory));
        ids.addAll(configIds(directory));
        Map<ResourceLocation, JsonObject> merged = new TreeMap<>();
        for (ResourceLocation id : ids) {
            String file = directory + "/" + id.getPath() + JSON_SUFFIX;
            Path configFile = ConfigFolder.dataFile(directory, id);
            List<FieldSet.Layer> layers = collectLayers(resourceManager, id.getNamespace(), file, configFile, true, problems);
            if (WEAPONS_DIRECTORY.equals(directory)) layers.addAll(0, LegacyBetterWeaponry.weaponLayers(resourceManager, id, problems));
            merged.put(id, WeaponDefinition.FIELDS.merge(layers, problems));
        }
        return merged;
    }

    /** Identifiants des fichiers présents dans les mods et les datapacks. */
    private static Set<ResourceLocation> resourceIds(ResourceManager resourceManager, String directory) {
        String prefix = DATA_DIRECTORY + "/" + directory;
        Set<ResourceLocation> ids = new HashSet<>();
        for (ResourceLocation location : resourceManager.listResources(prefix, path -> path.getPath().endsWith(JSON_SUFFIX)).keySet()) {
            String path = location.getPath().substring(prefix.length() + 1, location.getPath().length() - JSON_SUFFIX.length());
            ids.add(new ResourceLocation(location.getNamespace(), path));
        }
        return ids;
    }

    /** Identifiants des fichiers créés à la main dans config/combat_reforged/<dossier>/<mod>/<nom>.json. */
    private static Set<ResourceLocation> configIds(String directory) {
        Path root = ConfigFolder.root().resolve(directory);
        Set<ResourceLocation> ids = new HashSet<>();
        if (!Files.isDirectory(root)) return ids;
        try (Stream<Path> files = Files.walk(root)) {
            files.filter(file -> file.toString().endsWith(JSON_SUFFIX)).forEach(file -> {
                Path relative = root.relativize(file);
                if (relative.getNameCount() < 2) return;
                String path = relative.subpath(1, relative.getNameCount()).toString().replace('\\', '/');
                ResourceLocation id = ResourceLocation.tryBuild(relative.getName(0).toString(), path.substring(0, path.length() - JSON_SUFFIX.length()));
                if (id != null) ids.add(id);
            });
        } catch (IOException e) {
            CombatReforged.LOGGER.warn("[Combat Reforged] Could not list {}", root, e);
        }
        return ids;
    }

    /** Applique {@code parent} et {@code combo.finisher}, puis décode chaque fichier. */
    private static Map<ResourceLocation, WeaponDefinition> resolveAll(Map<ResourceLocation, JsonObject> raw, Map<ResourceLocation, JsonObject> otherParents, List<DataProblem> problems) {
        Map<ResourceLocation, JsonObject> resolved = new LinkedHashMap<>();
        Map<ResourceLocation, WeaponDefinition> definitions = new LinkedHashMap<>();
        for (ResourceLocation id : raw.keySet()) {
            JsonObject json = resolve(id, raw, otherParents, resolved, new HashSet<>(), problems);
            definitions.put(id, decode(WeaponDefinition.CODEC, json, WeaponDefinition.FALLBACK, id.toString(), problems));
        }
        return definitions;
    }

    private static JsonObject resolve(ResourceLocation id, Map<ResourceLocation, JsonObject> raw, Map<ResourceLocation, JsonObject> otherParents,
                                      Map<ResourceLocation, JsonObject> resolved, Set<ResourceLocation> visiting, List<DataProblem> problems) {
        JsonObject known = resolved.get(id);
        if (known != null) return known;
        JsonObject own = raw.get(id);
        JsonObject result = own;
        if (own.has(WeaponDefinition.PARENT.name()) && visiting.add(id)) {
            ResourceLocation parentId = ResourceLocation.tryParse(own.get(WeaponDefinition.PARENT.name()).getAsString());
            JsonObject parent = parentId == null || parentId.equals(DataField.NONE) ? null
                    : raw.containsKey(parentId) && !visiting.contains(parentId) ? resolve(parentId, raw, otherParents, resolved, visiting, problems)
                    : otherParents.get(parentId);
            if (parent == null) {
                if (parentId != null && !parentId.equals(DataField.NONE)) problems.add(new DataProblem(DataProblem.Kind.UNKNOWN_PARENT, id.toString(), WeaponDefinition.PARENT.name(), parentId.toString()));
            } else {
                JsonObject inherited = parent.deepCopy();
                // Une arme n'hérite pas des objets de son archétype.
                inherited.remove(WeaponDefinition.ITEMS.name());
                result = WeaponDefinition.FIELDS.merge(List.of(new FieldSet.Layer("parent " + parentId, inherited), new FieldSet.Layer(id.toString(), own)), problems);
            }
        }
        result = applyFinisher(result);
        resolved.put(id, result);
        return result;
    }

    /** Fusionne {@code combo.finisher} dans le dernier coup de {@code combo.steps}, pour pouvoir ne changer que lui. */
    static JsonObject applyFinisher(JsonObject definition) {
        if (!definition.has(WeaponDefinition.COMBO.name())) return definition;
        JsonObject combo = definition.getAsJsonObject(WeaponDefinition.COMBO.name());
        if (!combo.has(ComboDefinition.FINISHER.name())) return definition;

        JsonObject result = definition.deepCopy();
        JsonObject resultCombo = result.getAsJsonObject(WeaponDefinition.COMBO.name());
        JsonElement finisher = resultCombo.remove(ComboDefinition.FINISHER.name());
        if (resultCombo.has(ComboDefinition.STEPS.name()) && !resultCombo.getAsJsonArray(ComboDefinition.STEPS.name()).isEmpty()) {
            JsonArray steps = resultCombo.getAsJsonArray(ComboDefinition.STEPS.name());
            steps.set(steps.size() - 1, JsonUtil.deepMerge(steps.get(steps.size() - 1), finisher));
        }
        return result;
    }

    private static Map<ResourceLocation, JsonObject> encodedAll(Map<ResourceLocation, WeaponDefinition> definitions) {
        Map<ResourceLocation, JsonObject> encoded = new TreeMap<>();
        definitions.forEach((id, definition) -> encoded.put(id, DataResults.getOrThrow(WeaponDefinition.CODEC.encodeStart(JsonOps.INSTANCE, definition)).getAsJsonObject()));
        return encoded;
    }

    private static DetectionRules loadDetection(ResourceManager resourceManager, List<DataProblem> problems) {
        DetectionRules rules = DetectionRules.EMPTY;
        String prefix = DATA_DIRECTORY + "/" + DETECTION_DIRECTORY;
        Map<ResourceLocation, Resource> files = new TreeMap<>(resourceManager.listResources(prefix, path -> path.getPath().endsWith(JSON_SUFFIX)));
        for (Map.Entry<ResourceLocation, Resource> file : files.entrySet()) {
            Optional<FieldSet.Layer> layer = readResource(file.getValue(), file.getKey().toString(), problems);
            if (layer.isPresent()) rules = rules.plus(decodeDetection(layer.get(), problems));
        }
        Optional<FieldSet.Layer> custom = readConfigFile(ConfigFolder.detectionFile(), problems);
        if (custom.isPresent()) rules = rules.plus(decodeDetection(custom.get(), problems));
        return rules;
    }

    private static DetectionRules decodeDetection(FieldSet.Layer layer, List<DataProblem> problems) {
        DataResult<DetectionRules> result = DetectionRules.CODEC.parse(JsonOps.INSTANCE, layer.json());
        Optional<String> error = DataResults.error(result);
        if (error.isPresent()) {
            problems.add(new DataProblem(DataProblem.Kind.INVALID, layer.source(), "*", error.get()));
            return DetectionRules.EMPTY;
        }
        return DataResults.getOrThrow(result);
    }

    // --- Couches pour l'éditeur ---

    /** Les couches que l'éditeur compare entre elles pour savoir quoi écrire. */
    public enum Base {
        /** Valeurs par défaut des mods. */
        MOD_DEFAULTS,
        /** Valeurs des mods et dossier config/combat_reforged : ce que la cible "tous les mondes" peut écrire. */
        CONFIG_FOLDER,
        /** Tout sauf le datapack de l'éditeur (cible "ce monde seulement", qui écrit dans ce datapack). */
        BELOW_EDITOR_PACK,
        /** Toutes les couches : les valeurs en vigueur dans ce monde. */
        ALL
    }

    /** Fusion des couches qui forment cette base, pour un fichier (ex : "weapons/katana.json" du mod "better_weaponry"). */
    public static JsonObject mergedJson(ResourceManager resourceManager, FieldSet set, String namespace, String relativePath, Path configFile, Base base) {
        List<DataProblem> ignored = new ArrayList<>();
        List<FieldSet.Layer> layers = switch (base) {
            case MOD_DEFAULTS -> collectLayers(resourceManager, namespace, relativePath, null, false, ignored);
            case CONFIG_FOLDER -> collectLayers(resourceManager, namespace, relativePath, configFile, false, ignored);
            case BELOW_EDITOR_PACK -> collectLayersBelowEditor(resourceManager, namespace, relativePath, configFile, ignored);
            case ALL -> collectLayers(resourceManager, namespace, relativePath, configFile, true, ignored);
        };
        return set.merge(layers, ignored);
    }

    /** Fusion des valeurs du jar et du dossier config (menu principal : pas de monde, donc pas de datapack). */
    public static JsonObject localJson(FieldSet set, String relativePath, Path configFile, List<DataProblem> problems) {
        List<FieldSet.Layer> layers = new ArrayList<>(builtinLayer(relativePath, problems));
        readConfigFile(configFile, problems).ifPresent(layers::add);
        return set.merge(layers, problems);
    }

    // --- Lecture des couches ---

    /**
     * Couches d'un fichier, de la moins prioritaire à la plus prioritaire : mods, puis config/combat_reforged,
     * puis datapacks du monde, puis (toujours en dernier) le datapack de l'éditeur.
     *
     * @param configFile null = sans le dossier config
     * @param worldPacks avec les datapacks du monde
     */
    private static List<FieldSet.Layer> collectLayers(ResourceManager resourceManager, String namespace, String relativePath, @Nullable Path configFile,
                                                      boolean worldPacks, List<DataProblem> problems) {
        List<FieldSet.Layer> layers = collectLayersBelowEditor(resourceManager, namespace, relativePath, configFile, problems, worldPacks);
        if (worldPacks) {
            resourceManager.getResourceStack(resourceId(namespace, relativePath)).stream()
                    .filter(resource -> resource.sourcePackId().equals(EDITOR_PACK_ID))
                    .forEach(resource -> readResource(resource, relativePath, problems).ifPresent(layers::add));
        }
        return layers;
    }

    private static List<FieldSet.Layer> collectLayersBelowEditor(ResourceManager resourceManager, String namespace, String relativePath, @Nullable Path configFile, List<DataProblem> problems) {
        return collectLayersBelowEditor(resourceManager, namespace, relativePath, configFile, problems, true);
    }

    private static List<FieldSet.Layer> collectLayersBelowEditor(ResourceManager resourceManager, String namespace, String relativePath, @Nullable Path configFile,
                                                                 List<DataProblem> problems, boolean worldPacks) {
        // getResourceStack renvoie les fichiers du moins prioritaire au plus prioritaire.
        List<Resource> stack = resourceManager.getResourceStack(resourceId(namespace, relativePath));
        List<FieldSet.Layer> layers = new ArrayList<>();

        for (Resource resource : stack) {
            if (!isWorldDatapack(resource)) readResource(resource, relativePath, problems).ifPresent(layers::add);
        }
        if (configFile != null) readConfigFile(configFile, problems).ifPresent(layers::add);
        if (worldPacks) {
            for (Resource resource : stack) {
                if (isWorldDatapack(resource) && !resource.sourcePackId().equals(EDITOR_PACK_ID)) {
                    readResource(resource, relativePath, problems).ifPresent(layers::add);
                }
            }
        }
        return layers;
    }

    private static ResourceLocation resourceId(String namespace, String relativePath) {
        return new ResourceLocation(namespace, DATA_DIRECTORY + "/" + relativePath);
    }

    private static List<FieldSet.Layer> builtinLayer(String relativePath, List<DataProblem> problems) {
        String source = "mod default > " + relativePath;
        return ModFiles.read(builtinPathParts(relativePath))
                .flatMap(text -> parse(source, text, problems))
                .map(List::of)
                .orElse(List.of());
    }

    private static String[] builtinPathParts(String relativePath) {
        List<String> parts = new ArrayList<>(List.of("data", CombatReforged.MODID, DATA_DIRECTORY));
        parts.addAll(List.of(relativePath.split("/")));
        return parts.toArray(String[]::new);
    }

    public static boolean isWorldDatapack(Resource resource) {
        return resource.sourcePackId().startsWith(WORLD_DATAPACK_PREFIX);
    }

    public static Optional<FieldSet.Layer> readResource(Resource resource, String relativePath, List<DataProblem> problems) {
        String source = resource.sourcePackId() + " > " + relativePath;
        try (Reader reader = resource.openAsReader()) {
            return parse(source, readAll(reader), problems);
        } catch (IOException e) {
            problems.add(new DataProblem(DataProblem.Kind.IO, source, null, e.getMessage()));
            return Optional.empty();
        }
    }

    public static Optional<FieldSet.Layer> readConfigFile(Path file, List<DataProblem> problems) {
        if (!Files.isRegularFile(file)) return Optional.empty();
        String source = ConfigFolder.displayPath(file);
        try {
            return parse(source, Files.readString(file, StandardCharsets.UTF_8), problems);
        } catch (IOException e) {
            problems.add(new DataProblem(DataProblem.Kind.IO, source, null, e.getMessage()));
            return Optional.empty();
        }
    }

    private static Optional<FieldSet.Layer> parse(String source, String text, List<DataProblem> problems) {
        try {
            JsonElement element = JsonUtil.parseLenient(text);
            if (!element.isJsonObject()) {
                problems.add(new DataProblem(DataProblem.Kind.NOT_AN_OBJECT, source, null, null));
                return Optional.empty();
            }
            return Optional.of(new FieldSet.Layer(source, element.getAsJsonObject()));
        } catch (JsonParseException e) {
            String detail = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            problems.add(new DataProblem(DataProblem.Kind.SYNTAX, source, null, detail));
            return Optional.empty();
        }
    }

    private static String readAll(Reader reader) throws IOException {
        StringBuilder builder = new StringBuilder();
        char[] buffer = new char[4096];
        int read;
        while ((read = reader.read(buffer)) != -1) builder.append(buffer, 0, read);
        return builder.toString();
    }

    // --- Décodage ---

    /** Toutes les valeurs ont déjà été validées une par une : le décodage complet ne devrait jamais échouer. */
    private static <T> T decode(Codec<T> codec, JsonObject merged, T fallback, String file, List<DataProblem> problems) {
        DataResult<T> result = codec.parse(JsonOps.INSTANCE, merged);
        Optional<DataResult.PartialResult<T>> error = result.error();
        if (error.isPresent()) {
            problems.add(new DataProblem(DataProblem.Kind.INVALID, file, "*", error.get().message()));
            return fallback;
        }
        return DataResults.orThrow(result);
    }

}
