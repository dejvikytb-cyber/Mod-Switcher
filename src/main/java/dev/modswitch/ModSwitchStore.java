package dev.modswitch;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.util.*;

public final class ModSwitchStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG = FabricLoader.getInstance().getConfigDir().resolve("modswitch.json");
    private static final Path DISABLED_DIR = FabricLoader.getInstance().getGameDir().resolve("mods-disabled");
    private static final Set<String> QUEUED_DISABLE = new LinkedHashSet<>();
    private static final Set<String> QUEUED_ENABLE = new LinkedHashSet<>();

    private ModSwitchStore() {}

    public static synchronized void load() {
        QUEUED_DISABLE.clear();
        QUEUED_ENABLE.clear();
        if (!Files.exists(CONFIG)) return;
        try (Reader reader = Files.newBufferedReader(CONFIG)) {
            State state = GSON.fromJson(reader, State.class);
            if (state != null) {
                QUEUED_DISABLE.addAll(state.disableOnExit);
                QUEUED_ENABLE.addAll(state.enableOnExit);
            }
        } catch (Exception ignored) {}
    }

    public static List<ModContainer> getMods() {
        List<ModContainer> mods = new ArrayList<>(FabricLoader.getInstance().getAllMods());
        mods.sort(Comparator.comparing(m -> m.getMetadata().getName().toLowerCase(Locale.ROOT)));
        return mods;
    }

    public static synchronized boolean queuedForDisable(String modId) {
        return QUEUED_DISABLE.contains(modId);
    }

    public static synchronized boolean queuedForEnable(String modId) {
        return QUEUED_ENABLE.contains(modId);
    }

    public static synchronized void toggle(ModContainer mod) {
        String id = mod.getMetadata().getId();
        // Core loader and this manager cannot be queued through the UI.
        if (id.equals("fabricloader") || id.equals("minecraft") || id.equals("modswitch")) return;

        if (QUEUED_DISABLE.remove(id)) {
            QUEUED_ENABLE.add(id);
        } else if (QUEUED_ENABLE.remove(id)) {
            QUEUED_DISABLE.add(id);
        } else {
            QUEUED_DISABLE.add(id);
        }
        save();
    }

    private static synchronized void save() {
        try {
            Files.createDirectories(CONFIG.getParent());
            State state = new State();
            state.disableOnExit.addAll(QUEUED_DISABLE);
            state.enableOnExit.addAll(QUEUED_ENABLE);
            try (Writer writer = Files.newBufferedWriter(CONFIG)) {
                GSON.toJson(state, writer);
            }
        } catch (Exception ignored) {}
    }

    public static synchronized void applyQueuedChanges() {
        try {
            Path modsDir = FabricLoader.getInstance().getGameDir().resolve("mods");
            Files.createDirectories(modsDir);
            Files.createDirectories(DISABLED_DIR);

            // At shutdown, the loader has finished running. Move only JARs directly inside /mods.
            for (String id : new ArrayList<>(QUEUED_DISABLE)) {
                for (Path jar : jarFiles(modsDir)) {
                    if (containsModId(jar, id)) {
                        Path target = uniqueTarget(DISABLED_DIR.resolve(jar.getFileName()));
                        Files.move(jar, target, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }

            // Re-enable is performed by moving matching JARs back into /mods.
            for (String id : new ArrayList<>(QUEUED_ENABLE)) {
                for (Path jar : jarFiles(DISABLED_DIR)) {
                    if (containsModId(jar, id)) {
                        Path target = uniqueTarget(modsDir.resolve(jar.getFileName()));
                        Files.move(jar, target, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        } catch (Exception ignored) {
            // UI explains that the player should check mods-disabled and the log if a move fails.
        } finally {
            QUEUED_DISABLE.clear();
            QUEUED_ENABLE.clear();
            save();
        }
    }

    private static List<Path> jarFiles(Path dir) throws Exception {
        if (!Files.isDirectory(dir)) return List.of();
        try (var stream = Files.list(dir)) {
            return stream.filter(p -> Files.isRegularFile(p) &&
                    p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".jar")).toList();
        }
    }

    private static boolean containsModId(Path jar, String id) {
        // JAR filenames do not reliably equal mod IDs. We use a conservative filename match;
        // unsupported/non-matching jars are left untouched rather than guessing.
        String name = jar.getFileName().toString().toLowerCase(Locale.ROOT);
        String needle = id.toLowerCase(Locale.ROOT);
        return name.equals(needle + ".jar") || name.startsWith(needle + "-") ||
               name.startsWith(needle + "_") || name.startsWith(needle + ".");
    }

    private static Path uniqueTarget(Path target) {
        if (!Files.exists(target)) return target;
        String name = target.getFileName().toString();
        String stem = name.endsWith(".jar") ? name.substring(0, name.length() - 4) : name;
        return target.resolveSibling(stem + "-" + System.currentTimeMillis() + ".jar");
    }

    private static final class State {
        Set<String> disableOnExit = new LinkedHashSet<>();
        Set<String> enableOnExit = new LinkedHashSet<>();
    }
}
