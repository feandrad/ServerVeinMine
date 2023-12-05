package digital.naomie;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class VeinMine implements ModInitializer {

    public static final String MOD_ID = "serverveinmine";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static HashMap<Block, Block> equivalentBlocks = new HashMap<>();
    public VeinMineConfig CONFIG;
    private List<Block> blockList;

    @Override
    public void onInitialize() {
        LOGGER.info("Server Vein Mine is initializing!");
        CONFIG = VeinMineConfig.load();
        blockList = CONFIG.veinMineableBlocks.stream()
                .map(blockName -> Registries.BLOCK.get(Identifier.tryParse(blockName)))
                .toList();

        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, entity) -> {
            mineVein(world, pos, state, player);
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

    private void mineVein(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (!(world instanceof ServerWorld)
                || !(player.isSneaking() == CONFIG.ShiftToActivate)
                || !(blockList.contains(state.getBlock()))) {
            return;
        }

        if (player.getMainHandStack().isSuitableFor(state)) {
            ArrayList<BlockPos> blocks = new ArrayList<>();
            blocks.add(pos);
            int i = 0;
            while (i < blocks.size()) {
                BlockPos currentPos = blocks.get(i);
                for (BlockPos neighbor : new BlockPos[]{
                        currentPos.add(1, 0, 0), // right
                        currentPos.add(-1, 0, 0), // left
                        currentPos.add(0, 1, 0), // up
                        currentPos.add(0, -1, 0), // down
                        currentPos.add(0, 0, 1), // front
                        currentPos.add(0, 0, -1), // back
                        currentPos.add(1, 1, 0), // right-up
                        currentPos.add(-1, 1, 0), // left-up
                        currentPos.add(1, -1, 0), // right-down
                        currentPos.add(-1, -1, 0), // left-down
                        currentPos.add(1, 0, 1), // right-front
                        currentPos.add(-1, 0, 1), // left-front
                        currentPos.add(1, 0, -1), // right-back
                        currentPos.add(-1, 0, -1), // left-back
                        currentPos.add(0, 1, 1), // up-front
                        currentPos.add(0, -1, 1), // down-front
                        currentPos.add(0, 1, -1), // up-back
                        currentPos.add(0, -1, -1), // down-back
                        currentPos.add(1, 1, 1), // right-up-front
                        currentPos.add(-1, 1, 1), // left-up-front
                        currentPos.add(1, -1, 1), // right-down-front
                        currentPos.add(-1, -1, 1), // left-down-front
                        currentPos.add(1, 1, -1), // right-up-back
                        currentPos.add(-1, 1, -1), // left-up-back
                        currentPos.add(1, -1, -1), // right-down-back
                        currentPos.add(-1, -1, -1) // left-down-back
                }) {
                    Block neighbourBlock = world.getBlockState(neighbor).getBlock();
                    if ((Objects.equals(neighbourBlock, state.getBlock()) || Objects.equals(equivalentBlocks.getOrDefault(neighbourBlock, null), state.getBlock())) && !blocks.contains(neighbor)) {
                        blocks.add(neighbor);
                    }
                }
                i++;
            }
            ServerPlayerEntity serverPlayerEntity = (ServerPlayerEntity) player;
            if (blocks.size() > CONFIG.maxBlocks) {
                serverPlayerEntity.sendMessage(Text.of("You're trying to vein mine more blocks than allowed"), true);
                return;
            }
            blocks.forEach(block -> {
                if (!PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(world, player, pos,
                        world.getBlockState(pos), world.getBlockEntity(pos))) {
                    LOGGER.info("Block ineligible for vein mine");
                } else {
                    serverPlayerEntity.interactionManager.tryBreakBlock(block);
                }
            });
        }

    }
}