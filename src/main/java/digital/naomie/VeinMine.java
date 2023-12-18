package digital.naomie;

import com.google.common.collect.Sets;
import digital.naomie.model.Candidate;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.OperatorBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class VeinMine implements ModInitializer {

    public static final String MOD_ID = "serverveinmine";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static HashMap<Block, Block> equivalentBlocks = new HashMap<>();
    public static VeinMineConfig CONFIG;
    private static List<Block> blockList;

    @Override
    public void onInitialize() {
        LOGGER.info("Server Vein Mine is initializing!");
        CONFIG = VeinMineConfig.load();
        blockList = CONFIG.veinMineableBlocks.stream()
                .map(blockName -> Registries.BLOCK.get(Identifier.tryParse(blockName)))
                .toList();

        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, entity) -> {
            if (!VeinMiningPlayers.isVeinMining(player)) {
                VeinMiningPlayers.startVeinMining(player);
                mineVein(world, pos, state, player, CONFIG.maxBlocks, CONFIG.maxDistance);
            }
        });

//        TODO Implement Bidirectional Hashmap
        equivalentBlocks.put(Blocks.DEEPSLATE_COAL_ORE, Blocks.COAL_ORE);
        equivalentBlocks.put(Blocks.COAL_ORE, Blocks.COAL_ORE);
        equivalentBlocks.put(Blocks.DEEPSLATE_COPPER_ORE, Blocks.COPPER_ORE);
        equivalentBlocks.put(Blocks.COPPER_ORE, Blocks.COPPER_ORE);
        equivalentBlocks.put(Blocks.DEEPSLATE_DIAMOND_ORE, Blocks.DIAMOND_ORE);
        equivalentBlocks.put(Blocks.DIAMOND_ORE, Blocks.DIAMOND_ORE);
        equivalentBlocks.put(Blocks.DEEPSLATE_EMERALD_ORE, Blocks.EMERALD_ORE);
        equivalentBlocks.put(Blocks.EMERALD_ORE, Blocks.EMERALD_ORE);
        equivalentBlocks.put(Blocks.DEEPSLATE_GOLD_ORE, Blocks.GOLD_ORE);
        equivalentBlocks.put(Blocks.GOLD_ORE, Blocks.GOLD_ORE);
        equivalentBlocks.put(Blocks.DEEPSLATE_IRON_ORE, Blocks.IRON_ORE);
        equivalentBlocks.put(Blocks.IRON_ORE, Blocks.IRON_ORE);
        equivalentBlocks.put(Blocks.DEEPSLATE_LAPIS_ORE, Blocks.LAPIS_ORE);
        equivalentBlocks.put(Blocks.LAPIS_ORE, Blocks.LAPIS_ORE);
        equivalentBlocks.put(Blocks.DEEPSLATE_REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE);
        equivalentBlocks.put(Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE);
        LOGGER.info("Server Vein Mine's config has been loaded");
    }

    private static void mineVein(World world, BlockPos pos, BlockState sourceState, PlayerEntity player, int maxBlocks, int maxDist) {
        Block sourceBlock = sourceState.getBlock();
        if (!(world instanceof ServerWorld)
                || !(player.isSneaking() == CONFIG.ShiftToActivate)
                || !(blockList.contains(sourceBlock))) {
            VeinMiningPlayers.stopVeinMining(player);
            return;
        }

        Set<BlockPos> visited = Sets.newHashSet(pos);
        HashSet<Candidate> candidates = new HashSet<>();
        HashSet<BlockPos> mined = new HashSet<>();
        addValidNeighbors(candidates, mined, pos, 1);

        int blocks = 1;

        while (!candidates.isEmpty() && blocks < maxBlocks) {
            Candidate c = nextCandidate(world, candidates);

            if (c == null || !world.isInBuildLimit(c.pos)) {
                VeinMiningPlayers.stopVeinMining(player);
                return;
            }

            BlockState blockState = world.getBlockState(c.pos);

            if (visited.add(c.pos) && isValidTarget(blockState, sourceBlock) &&
                    harvest((ServerPlayerEntity) player, c.pos, pos)
            ) {
                blocks++;
                mined.add(c.pos);
                candidates.remove(c);
                if (c.distance < maxDist) {
                    addValidNeighbors(candidates, mined, c.pos, c.distance + 1);
                }
            }
        }

        VeinMiningPlayers.stopVeinMining(player);
    }

    private static Candidate nextCandidate(World world, HashSet<Candidate> candidates) {
        while (!candidates.isEmpty()) {
            Candidate next = candidates.stream().min(Comparator.comparingInt(a -> a.distance)).get();
            BlockState blockState = world.getBlockState(next.pos);

            if (!blockState.isAir()) return next;

            candidates.remove(next);
        }

        return null;
    }

    private static void addValidNeighbors(
            HashSet<Candidate> candidates,
            HashSet<BlockPos> mined,
            BlockPos source,
            int distance
    ) {
        HashSet<BlockPos> neighbours = Sets.newHashSet(
                source.west(),
                source.east(),
                source.north(),
                source.south(),
                source.up(),
                source.down()
        );

        neighbours.forEach(it -> {
            if (!mined.contains(it)) {
                candidates.add(new Candidate(it, distance, false));
            }
        });
    }

    public static boolean isValidTarget(BlockState state, Block source) {
        return !state.isAir() && blockList.contains(state.getBlock());
    }

    // Extracted from player.interactionManager.tryBreakBlock(pos) except Cancel Event, After Event and Spawn Event
    public static boolean harvest(ServerPlayerEntity player, BlockPos pos, BlockPos originPos) {


        ServerWorld world = player.getServerWorld();
        BlockState blockState = world.getBlockState(pos);
        BlockEntity blockEntity = world.getBlockEntity(pos);
        Block block = blockState.getBlock();

        if (!player.getMainHandStack().getItem().canMine(blockState, world, pos, player)) {
            return false;
        }
        if (block instanceof OperatorBlock && !player.isCreativeLevelTwoOp()) {
            world.updateListeners(pos, blockState, blockState, Block.NOTIFY_ALL);
            return false;
        }
        if (player.isBlockBreakingRestricted(world, pos, player.interactionManager.getGameMode())) {
            return false;
        }

        // Cancel Event
        boolean result = PlayerBlockBreakEvents.BEFORE.invoker()
                .beforeBlockBreak(world, player, pos, blockState, blockEntity);
        if (!result) {
            PlayerBlockBreakEvents.CANCELED.invoker()
                    .onBlockBreakCanceled(world, player, pos, blockState, blockEntity);
            return false;
        }

        block.onBreak(world, pos, blockState, player);
        boolean bl = world.removeBlock(pos, false);
        if (bl) {
            // After Event
            PlayerBlockBreakEvents.AFTER.invoker()
                    .afterBlockBreak(world, player, pos, blockState, blockEntity);
            block.onBroken(world, pos, blockState);
        }
        if (player.interactionManager.isCreative()) {
            return true;
        }
        ItemStack itemStack = player.getMainHandStack();
        ItemStack itemStack2 = itemStack.copy();
        boolean bl2 = player.canHarvest(blockState);
        itemStack.postMine(world, blockState, pos, player);
        if (bl && bl2) {
            // Spawn Event
//            BlockPos spawnPos = VeinMiningConfig.SERVER.relocateDrops.get() ? originPos : pos;
//            FoodData foodData = player.getFoodData();
//            float currentExhaustion = foodData.getExhaustionLevel();
            VeinMiningPlayers.addMiningBlock(world, pos, originPos);
            block.afterBreak(world, player, pos, blockState, blockEntity, itemStack2);
            VeinMiningPlayers.removeMiningBlock(world, pos);
//            if (VeinMiningConfig.SERVER.addExhaustion.get()) {
//                float diff = foodData.getExhaustionLevel() - currentExhaustion;
//                foodData.setExhaustion(currentExhaustion);
//                foodData.addExhaustion(
//                        (float) (diff * VeinMiningConfig.SERVER.exhaustionMultiplier.get()));
//            } else {
//                foodData.setExhaustion(currentExhaustion);
//            }
        }
        return true;
    }

}