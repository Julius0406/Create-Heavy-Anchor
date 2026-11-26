package dev.julius.heavyanchor.index;
import dev.julius.heavyanchor.CreateHeavyAnchor;
import dev.julius.heavyanchor.menu.AnchorMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.flag.FeatureFlags;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.function.Supplier;
public class AnchorMenus {
    public static final DeferredRegister<MenuType<?>> MENUS=DeferredRegister.create(Registries.MENU,CreateHeavyAnchor.MODID);
    public static final Supplier<MenuType<AnchorMenu>> ANCHOR=MENUS.register("anchor",()->new MenuType<>(AnchorMenu::new,FeatureFlags.DEFAULT_FLAGS));
}
