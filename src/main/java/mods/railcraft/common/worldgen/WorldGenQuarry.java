/*------------------------------------------------------------------------------
 Copyright (c) CovertJaguar, 2011-2017
 http://railcraft.info

 This code is the property of CovertJaguar
 and may only be used with explicit written
 permission unless otherwise specified on the
 license page at http://railcraft.info/wiki/info:license.
 -----------------------------------------------------------------------------*/
package mods.railcraft.common.worldgen;

import mods.railcraft.common.plugins.forge.OreDictPlugin;
import mods.railcraft.common.plugins.forge.WorldPlugin;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenerator;
import net.minecraftforge.fluids.IFluidBlock;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * @author CovertJaguar <http://www.railcraft.info>
 */
public class WorldGenQuarry extends WorldGenerator {

    private static final int DISTANCE_OUTER_SQ = 8 * 8;
    private final IBlockState quarryStone;
    public final Set<Block> replaceable = new HashSet<Block>();

    public WorldGenQuarry(IBlockState quarryStone) {
        this.quarryStone = quarryStone;

        replaceable.add(Blocks.COAL_ORE);
        replaceable.add(Blocks.IRON_ORE);
        replaceable.add(Blocks.GOLD_ORE);
        replaceable.add(Blocks.DIAMOND_ORE);
        replaceable.add(Blocks.EMERALD_ORE);
        replaceable.add(Blocks.LAPIS_ORE);
        replaceable.add(Blocks.QUARTZ_ORE);
        replaceable.add(Blocks.REDSTONE_ORE);
        replaceable.add(Blocks.LIT_REDSTONE_ORE);
        replaceable.add(Blocks.DIRT);
        replaceable.add(Blocks.GRAVEL);
        replaceable.add(Blocks.GRASS);
        replaceable.add(Blocks.CLAY);

        replaceable.addAll(OreDictPlugin.getOreBlocks());
    }

    @Override
    public boolean generate(World world, Random rand, BlockPos position) {
        position = position.add(8, 0, 8);
//        Game.log(Level.INFO, "Generating Quarry at {0}, {1}, {2}", x, y, z);
        boolean clearTop = true;
        for (int x = -8; x < 8; x++) {
            for (int z = -8; z < 8; z++) {
                for (int y = 1; y < 4 && y + position.getY() < world.getActualHeight() - 1; y++) {
                    int distSq = x * x + z * z;
                    if (distSq <= DISTANCE_OUTER_SQ) {
                        IBlockState existingState = WorldPlugin.getBlockState(world, position.add(x, y, z));
                        if (isLiquid(existingState)) {
                            clearTop = false;
                            break;
                        }
                    }
                }
            }
        }
        if (clearTop)
            for (int x = -8; x < 8; x++) {
                for (int z = -8; z < 8; z++) {
                    for (int y = 1; y < 4 && y + position.getY() < world.getActualHeight() - 1; y++) {
                        int distSq = x * x + z * z;
                        if (distSq <= DISTANCE_OUTER_SQ) {
                            BlockPos targetPos = position.add(x, y, z);
                            IBlockState existingState = WorldPlugin.getBlockState(world, targetPos);
                            if (!placeAir(existingState, world, rand, targetPos))
                                break;
                        }
                    }
                }
            }
        for (int x = -8; x < 8; x++) {
            for (int z = -8; z < 8; z++) {
                for (int y = -8; y < 1 && y + position.getY() < world.getActualHeight() - 1; y++) {
                    int distSq = x * x + z * z + y * y;
                    if (distSq <= DISTANCE_OUTER_SQ) {
                        BlockPos targetPos = position.add(x, y, z);
                        IBlockState existingState = WorldPlugin.getBlockState(world, targetPos);
                        placeStone(existingState, world, rand, targetPos);
                    }
                }
            }
        }

        return true;
    }

    private boolean isLiquid(IBlockState existingState) {
        Block block = existingState.getBlock();
        return block instanceof BlockLiquid || block instanceof IFluidBlock;
    }

    private boolean placeAir(IBlockState existingState, World world, Random rand, BlockPos pos) {
//        if (!world.isBlockLoaded(x, y, z)) {
//            return false;
//        }
        BlockPos up = pos.up();
        if (!WorldPlugin.isBlockAir(world, up))
            return false;
        if (isLiquid(existingState))
            return false;

        for (EnumFacing side : EnumFacing.HORIZONTALS) {
            if (!WorldPlugin.isBlockAir(world, up.offset(side)))
                return false;
        }

        world.setBlockState(pos, Blocks.AIR.getDefaultState(), 2);
        return true;
    }

    private void placeStone(IBlockState existingState, World world, Random rand, BlockPos pos) {
//        if (!world.isBlockLoaded(x, y, z)) {
//            return;
//        }
        //Removes tall grass
        if (WorldPlugin.isBlockAt(world, pos.up(), Blocks.TALLGRASS))
            world.setBlockState(pos, Blocks.AIR.getDefaultState(), 2);

        if (isReplaceable(existingState, world, pos))
            world.setBlockState(pos, quarryStone, 2);
    }

    private boolean isReplaceable(IBlockState existingState, World world, BlockPos pos) {
        if (existingState.getBlock().isReplaceableOreGen(existingState, world, pos, GenTools.STONE::test))
            return true;
        return replaceable.contains(existingState.getBlock());
    }

}
