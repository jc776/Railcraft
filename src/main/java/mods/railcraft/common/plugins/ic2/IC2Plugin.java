/*------------------------------------------------------------------------------
 Copyright (c) CovertJaguar, 2011-2017
 http://railcraft.info

 This code is the property of CovertJaguar
 and may only be used with explicit written
 permission unless otherwise specified on the
 license page at http://railcraft.info/wiki/info:license.
 -----------------------------------------------------------------------------*/
package mods.railcraft.common.plugins.ic2;

import ic2.api.energy.EnergyNet;
import ic2.api.energy.event.EnergyTileLoadEvent;
import ic2.api.energy.event.EnergyTileUnloadEvent;
import ic2.api.energy.tile.IEnergyTile;
import ic2.api.item.ElectricItem;
import ic2.api.item.IC2Items;
import ic2.api.item.IElectricItem;
import ic2.api.recipe.*;
import mods.railcraft.common.plugins.misc.Mod;
import mods.railcraft.common.util.inventory.InvTools;
import mods.railcraft.common.util.misc.Game;
import mods.railcraft.common.util.misc.ItemStackCache;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Items;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.MinecraftForge;

import javax.annotation.Nullable;
import java.util.Iterator;

import static mods.railcraft.common.util.inventory.InvTools.setSize;
import static mods.railcraft.common.util.inventory.InvTools.sizeOf;

/**
 * @author CovertJaguar <http://www.railcraft.info>
 */
public class IC2Plugin {
    public static final ItemStackCache ITEMS = new ItemStackCache("IC2", IC2Items.class, () -> Mod.anyLoaded(Mod.IC2, Mod.IC2_CLASSIC), s -> {
        String[] tokens = s.split("#");
        return IC2Items.getItem(tokens[0], tokens.length == 2 ? tokens[1] : null);
    });

    @Nullable
    public static ItemStack getItem(String tag) {
        return ITEMS.get(tag);
    }

    @Nullable
    public static IBlockState getBlockState(String name, String variant) {
        return IC2Items.getItemAPI().getBlockState(name, variant);
    }

    public static boolean addTileToNet(TileEntity tile) {
        try {
            if (EnergyNet.instance != null && tile instanceof IEnergyTile) {
                MinecraftForge.EVENT_BUS.post(new EnergyTileLoadEvent((IEnergyTile) tile));
                return true;
            }
        } catch (Throwable error) {
            Game.logErrorAPI("IC2", error, EnergyTileLoadEvent.class);
        }
        return false;
    }

    public static void removeTileFromNet(TileEntity tile) {
        try {
            if (tile instanceof IEnergyTile)
                MinecraftForge.EVENT_BUS.post(new EnergyTileUnloadEvent((IEnergyTile) tile));
        } catch (Throwable error) {
            Game.logErrorAPI("IC2", error, EnergyTileUnloadEvent.class);
        }
    }

    public static boolean isEnergyItem(ItemStack stack) {
        try {
            return !InvTools.isEmpty(stack) && stack.getItem() instanceof IElectricItem;
        } catch (Throwable error) {
            Game.logErrorAPI("IC2", error, IElectricItem.class);
        }
        return false;
    }

    /**
     * @return energy used
     */
    public static double chargeItem(ItemStack stack, double energy, int tier) {
        try {
            if (!InvTools.isEmpty(stack) && stack.getItem() instanceof IElectricItem && energy > 0)
                return ElectricItem.manager.charge(stack, energy, tier, false, false);
        } catch (Throwable error) {
            Game.logErrorAPI("IC2", error, ElectricItem.class);
        }
        return 0;
    }

    /**
     * @return energy received
     */
    public static double dischargeItem(ItemStack stack, double energyNeeded, int tier) {
        try {
            if (!InvTools.isEmpty(stack) && stack.getItem() instanceof IElectricItem && ((IElectricItem) stack.getItem()).canProvideEnergy(stack))
                return ElectricItem.manager.discharge(stack, energyNeeded, tier, false, true, false);
        } catch (Throwable error) {
            Game.logErrorAPI("IC2", error, ElectricItem.class);
        }
        return 0;
    }

    public static boolean canCharge(ItemStack stack, int tier) {
        try {
            if (!InvTools.isEmpty(stack) && stack.getItem() instanceof IElectricItem) {
                IElectricItem battery = (IElectricItem) stack.getItem();
                return tier >= battery.getTier(stack);
            }
        } catch (Throwable error) {
            Game.logErrorAPI("IC2", error, IElectricItem.class);
        }
        return false;
    }

    public static boolean canDischarge(ItemStack stack, int tier) {
        try {
            if (!InvTools.isEmpty(stack) && stack.getItem() instanceof IElectricItem) {
                IElectricItem battery = (IElectricItem) stack.getItem();
                return battery.canProvideEnergy(stack) && tier >= battery.getTier(stack);
            }
        } catch (Throwable error) {
            Game.logErrorAPI("IC2", error, IElectricItem.class);
        }
        return false;
    }

    public static void addMaceratorRecipe(@Nullable ItemStack input, @Nullable ItemStack output) {
        addMaceratorRecipe(input, 1, output, 1);
    }

    public static void addMaceratorRecipe(@Nullable ItemStack input, int numinput, @Nullable ItemStack output, int numoutput) {
        if (InvTools.isEmpty(input) || InvTools.isEmpty(output))
            return;
        output = output.copy();
        setSize(output, numoutput);
        try {
            Recipes.macerator.addRecipe(new RecipeInputItemStack(input, numinput), null, false, output);
        } catch (Throwable error) {
            Game.logErrorAPI("IC2", error, Recipes.class);
        }
    }

    public static void removeMaceratorRecipes(ItemStack... items) {
        try {

            Iterator<IMachineRecipeManager.RecipeIoContainer> it = Recipes.macerator.getRecipes().iterator();
            while (it.hasNext()) {
                IMachineRecipeManager.RecipeIoContainer recipe = it.next();
                if (doesRecipeRequire(recipe.input, items) || doesRecipeProduce(recipe.output, items))
                    it.remove();
            }
        } catch (Throwable error) {
            Game.logErrorAPI("IC2", error, Recipes.class);
        }
    }

    public static void addCanningRecipe(ItemStack container, @Nullable ItemStack input, @Nullable ItemStack output) {
        if (InvTools.isEmpty(input) || InvTools.isEmpty(output))
            return;
        try {
            Recipes.cannerBottle.addRecipe(new RecipeInputItemStack(container), new RecipeInputItemStack(input), output);
        } catch (Throwable error) {
            Game.logErrorAPI("IC2", error, Recipes.class);
        }
    }

    private static boolean doesRecipeRequire(IRecipeInput input, ItemStack... items) {
        for (ItemStack stack : input.getInputs()) {
            if (InvTools.isItemEqual(stack, items))
                return true;
        }
        return false;
    }

    private static boolean doesRecipeProduce(RecipeOutput recipe, ItemStack... items) {
        for (ItemStack output : recipe.items) {
            if (InvTools.isItemEqual(output, items))
                return true;
        }
        return false;
    }

    public static void removeMaceratorDustRecipes(ItemStack... items) {
        try {
            Iterator<IMachineRecipeManager.RecipeIoContainer> it = Recipes.macerator.getRecipes().iterator();
            while (it.hasNext()) {
                IMachineRecipeManager.RecipeIoContainer recipe = it.next();
                if (isInputBlock(recipe.input, items) && doesRecipeProduce(recipe.output, items))
                    it.remove();
            }
        } catch (Throwable error) {
            Game.logErrorAPI("IC2", error, Recipes.class);
        }
    }

    private static boolean isInputBlock(IRecipeInput input, ItemStack... items) {
        for (ItemStack stack : input.getInputs()) {
            if (!InvTools.isEmpty(stack) && stack.getItem() instanceof ItemBlock)
                return true;
        }
        return false;
    }

    public static void nerfSyntheticCoal() {
        for (IRecipe recipe : CraftingManager.getInstance().getRecipeList()) {
            try {
                ItemStack output = recipe.getRecipeOutput();
                if (!InvTools.isEmpty(output))
                    if (output.getItem() == Items.COAL && sizeOf(output) == 20)
                        setSize(output, 5);
            } catch (Throwable ignored) {
            }
        }
    }
}
