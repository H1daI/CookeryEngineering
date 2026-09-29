package com.cookeryengineering.compat.kaleidoscope;

import com.github.ysbbbbbb.kaleidoscopecookery.crafting.container.StockpotInput;
import com.github.ysbbbbbb.kaleidoscopecookery.crafting.recipe.FlexStockpotRecipe;
import com.github.ysbbbbbb.kaleidoscopecookery.crafting.recipe.RiceBowlRecipe;
import com.github.ysbbbbbb.kaleidoscopecookery.crafting.recipe.StockpotRecipe;
import com.github.ysbbbbbb.kaleidoscopecookery.crafting.soupbase.FluidSoupBase;
import com.github.ysbbbbbb.kaleidoscopecookery.crafting.soupbase.SoupBaseManager;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModRecipes;
import com.github.ysbbbbbb.kaleidoscopecookery.item.quality.Quality;
import com.github.ysbbbbbb.kaleidoscopecookery.item.quality.QualityEvaluator;
import com.github.ysbbbbbb.kaleidoscopecookery.item.quality.QualityUtils;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 围绕 KC 汤锅 API 的兼容
 * 机器状态和 IE 集成不需要了解配方内部实现
 */
public final class KaleidoscopeStockpotRecipes {
    private KaleidoscopeStockpotRecipes() {
    }

    public static @Nullable ResourceLocation soupBaseFor(FluidStack fluid) {
        if (fluid.isEmpty()) {
            return null;
        }
        for (var entry : SoupBaseManager.getAllSoupBases().entrySet()) {
            if (entry.getValue() instanceof FluidSoupBase fluidSoupBase
                    && fluidSoupBase.getFluid() == fluid.getFluid()) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * 输入物品时，解析物品形态的汤底
     * 这是因为森罗物语的汤底有一些是鱼桶
     * 不过没有在gui中做显示，以后可能会做
     */
    public static @Nullable ResourceLocation soupBaseBucketFor(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasCraftingRemainingItem()
                || !stack.getCraftingRemainingItem().is(net.minecraft.world.item.Items.BUCKET)) {
            return null;
        }
        for (var entry : SoupBaseManager.getAllSoupBases().entrySet()) {
            if (entry.getValue().isSoupBase(stack)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public static boolean isCookedRice(ItemStack stack) {
        return RiceBowlRecipe.COOKED_RICE.test(stack);
    }

    /**
     * 配方缓存，精确配方优于flex配方。
     */
    public static @Nullable Match find(
            Level level,
            NonNullList<ItemStack> inputs,
            ResourceLocation soupBase,
            @Nullable CachedRecipe previous
    ) {
        StockpotInput stockpotInput = new StockpotInput(inputs, soupBase);
        RecipeManager recipes = level.getRecipeManager();

        Match cached = matchCached(level, recipes, stockpotInput, previous);
        if (cached != null) {
            return cached;
        }

        for (RecipeHolder<StockpotRecipe> holder : recipes.getAllRecipesFor(ModRecipes.STOCKPOT_RECIPE)) {
            if (holder.value().matches(stockpotInput, level)) {
                return exactMatch(level, stockpotInput, holder);
            }
        }
        for (RecipeHolder<FlexStockpotRecipe> holder : recipes.getAllRecipesFor(ModRecipes.FLEX_STOCKPOT_RECIPE)) {
            if (holder.value().matches(stockpotInput, level)) {
                return flexMatch(level, inputs, stockpotInput, holder);
            }
        }
        return null;
    }

    private static @Nullable Match matchCached(
            Level level,
            RecipeManager recipes,
            StockpotInput input,
            @Nullable CachedRecipe previous
    ) {
        if (previous == null) {
            return null;
        }
        if (previous.flex()) {
            for (RecipeHolder<FlexStockpotRecipe> holder : recipes.getAllRecipesFor(ModRecipes.FLEX_STOCKPOT_RECIPE)) {
                if (holder.id().equals(previous.id()) && holder.value().matches(input, level)) {
                    return flexMatch(level, input.getInputs(), input, holder);
                }
            }
        } else {
            for (RecipeHolder<StockpotRecipe> holder : recipes.getAllRecipesFor(ModRecipes.STOCKPOT_RECIPE)) {
                if (holder.id().equals(previous.id()) && holder.value().matches(input, level)) {
                    return exactMatch(level, input, holder);
                }
            }
        }
        return null;
    }

    private static Match exactMatch(
            Level level,
            StockpotInput input,
            RecipeHolder<StockpotRecipe> holder
    ) {
        return new Match(
                new CachedRecipe(holder.id(), false),
                holder.value().assemble(input, level.registryAccess()),
                holder.value().carrier()
        );
    }

    private static Match flexMatch(
            Level level,
            List<ItemStack> inputs,
            StockpotInput input,
            RecipeHolder<FlexStockpotRecipe> holder
    ) {
        ItemStack result = holder.value().assemble(input, level.registryAccess());
        if (level instanceof ServerLevel serverLevel) {
            Quality quality = QualityEvaluator.evaluate(
                    inputs, holder.value().ingredients(), holder.id(), serverLevel.getSeed());
            QualityUtils.setQuality(result, quality);
        }
        return new Match(new CachedRecipe(holder.id(), true), result, holder.value().carrier());
    }

    public static @Nullable Ingredient resolveCarrier(Level level, CachedRecipe recipe) {
        RecipeManager recipes = level.getRecipeManager();
        if (recipe.flex()) {
            for (RecipeHolder<FlexStockpotRecipe> holder : recipes.getAllRecipesFor(ModRecipes.FLEX_STOCKPOT_RECIPE)) {
                if (holder.id().equals(recipe.id())) {
                    return holder.value().carrier();
                }
            }
        } else {
            for (RecipeHolder<StockpotRecipe> holder : recipes.getAllRecipesFor(ModRecipes.STOCKPOT_RECIPE)) {
                if (holder.id().equals(recipe.id())) {
                    return holder.value().carrier();
                }
            }
        }
        return null;
    }

    public static @Nullable RiceBowlMatch findRiceBowl(
            Level level,
            ItemStack cookedDish,
            ItemStack cookedRice,
            @Nullable ResourceLocation previous
    ) {
        NonNullList<ItemStack> stacks = NonNullList.of(
                ItemStack.EMPTY, cookedDish.copyWithCount(1), cookedRice.copyWithCount(1));
        CraftingInput input = CraftingInput.of(2, 1, stacks);
        List<RecipeHolder<CraftingRecipe>> craftingRecipes = level.getRecipeManager()
                .getAllRecipesFor(RecipeType.CRAFTING);

        if (previous != null) {
            for (RecipeHolder<CraftingRecipe> holder : craftingRecipes) {
                if (holder.id().equals(previous)
                        && holder.value() instanceof RiceBowlRecipe riceBowl
                        && riceBowl.matches(input, level)) {
                    return new RiceBowlMatch(holder.id(), riceBowl.assemble(input, level.registryAccess()));
                }
            }
        }

        for (RecipeHolder<CraftingRecipe> holder : craftingRecipes) {
            if ((previous == null || !holder.id().equals(previous))
                    && holder.value() instanceof RiceBowlRecipe riceBowl
                    && riceBowl.matches(input, level)) {
                return new RiceBowlMatch(holder.id(), riceBowl.assemble(input, level.registryAccess()));
            }
        }
        return null;
    }

    public record CachedRecipe(ResourceLocation id, boolean flex) {
    }

    public record Match(CachedRecipe recipe, ItemStack result, Ingredient carrier) {
    }

    public record RiceBowlMatch(ResourceLocation recipeId, ItemStack result) {
    }
}
