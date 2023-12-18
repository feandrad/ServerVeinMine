package digital.naomie;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static digital.naomie.VeinMine.MOD_ID;

public class VeinMineConfig {

    public boolean ShiftToActivate = true;
    public int maxBlocks = 16;
    public int maxDistance = 8;
    public List<String> veinMineableBlocks = List.of(
            Registries.BLOCK.getId(Blocks.COAL_ORE).toString(),
            Registries.BLOCK.getId(Blocks.IRON_ORE).toString(),
            Registries.BLOCK.getId(Blocks.GOLD_ORE).toString(),
            Registries.BLOCK.getId(Blocks.DIAMOND_ORE).toString(),
            Registries.BLOCK.getId(Blocks.EMERALD_ORE).toString(),
            Registries.BLOCK.getId(Blocks.LAPIS_ORE).toString(),
            Registries.BLOCK.getId(Blocks.REDSTONE_ORE).toString(),
            Registries.BLOCK.getId(Blocks.NETHER_QUARTZ_ORE).toString(),
            Registries.BLOCK.getId(Blocks.NETHER_GOLD_ORE).toString(),
            Registries.BLOCK.getId(Blocks.ANCIENT_DEBRIS).toString(),
            Registries.BLOCK.getId(Blocks.COPPER_ORE).toString(),
            Registries.BLOCK.getId(Blocks.DEEPSLATE_IRON_ORE).toString(),
            Registries.BLOCK.getId(Blocks.DEEPSLATE_GOLD_ORE).toString(),
            Registries.BLOCK.getId(Blocks.DEEPSLATE_COAL_ORE).toString(),
            Registries.BLOCK.getId(Blocks.DEEPSLATE_DIAMOND_ORE).toString(),
            Registries.BLOCK.getId(Blocks.DEEPSLATE_EMERALD_ORE).toString(),
            Registries.BLOCK.getId(Blocks.DEEPSLATE_LAPIS_ORE).toString(),
            Registries.BLOCK.getId(Blocks.DEEPSLATE_REDSTONE_ORE).toString(),
            Registries.BLOCK.getId(Blocks.DEEPSLATE_COPPER_ORE).toString(),
            Registries.BLOCK.getId(Blocks.GLOWSTONE).toString()
        );

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH;

    static {
        CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID + ".json");
    }

    public static VeinMineConfig load() {
        try {
            if (!Files.exists(CONFIG_PATH)) {
                VeinMineConfig config = new VeinMineConfig();

                FileWriter writer = new FileWriter(CONFIG_PATH.toFile());
                GSON.toJson(config, writer);
                writer.close();

                return config;
            } else {
                FileReader reader = new FileReader(CONFIG_PATH.toFile());
                VeinMineConfig config = GSON.fromJson(reader, VeinMineConfig.class);
                reader.close();

                return config;
            }
        } catch (IOException e) {
            throw new RuntimeException("[" + MOD_ID + "] Failed to load config", e);
        }
    }
}

