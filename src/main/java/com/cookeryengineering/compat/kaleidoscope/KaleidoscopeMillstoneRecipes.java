package com.cookeryengineering.compat.kaleidoscope;

import com.github.ysbbbbbb.kaleidoscopecookery.crafting.container.SimpleInput;
import com.github.ysbbbbbb.kaleidoscopecookery.crafting.recipe.MillstoneRecipe;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModRecipes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import java.util.List;

/** 兼容原 KC 的石磨配方 */
public final class KaleidoscopeMillstoneRecipes {
    private final RecipeManager.CachedCheck<SimpleInput, MillstoneRecipe> cache =
            RecipeManager.createCheck(ModRecipes.MILLSTONE_RECIPE);

    public @Nullable List<Output> find(Level level, ItemStack ingredient) {
        return cache.getRecipeFor(new SimpleInput(List.of(ingredient)), level)
                .map(holder -> holder.value().results().stream()
                        .map(result -> new Output(result.stack().copy(), result.chance())).toList())
                .orElse(null);
    }

    public record Output(ItemStack stack, float chance) { }
}
