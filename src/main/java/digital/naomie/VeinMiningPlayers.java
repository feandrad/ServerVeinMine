package digital.naomie;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class VeinMiningPlayers {

    private static final Map<UUID, Long> CURRENT_MINERS = new ConcurrentHashMap<>();
    private static final Map<World, Map<BlockPos, BlockPos>> MINING_BLOCKS =
            new ConcurrentHashMap<>();


    public static boolean isVeinMining(PlayerEntity player) {
        return CURRENT_MINERS.containsKey(player.getUuid());
    }

    public static void startVeinMining(PlayerEntity player) {
        CURRENT_MINERS.put(player.getUuid(), player.getWorld().getTime());
    }

    public static void stopVeinMining(PlayerEntity player) {
        CURRENT_MINERS.remove(player.getUuid());
    }

    public static void addMiningBlock(World world, BlockPos pos, BlockPos spawnPos) {
        MINING_BLOCKS.computeIfAbsent(world, (k) -> new HashMap<>()).put(pos, spawnPos);
    }

    public static void removeMiningBlock(World world, BlockPos pos) {
        Map<BlockPos, BlockPos> map = MINING_BLOCKS.get(world);

        if (map != null) {
            map.remove(pos);
        }
    }

    public static Optional<BlockPos> getNewSpawnPosForDrop(World world, BlockPos pos) {
        Map<BlockPos, BlockPos> map = MINING_BLOCKS.get(world);

        if (map != null) {
            return Optional.ofNullable(map.get(pos));
        }
        return Optional.empty();
    }
}
