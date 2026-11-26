package dev.julius.heavyanchor.item;

import dev.julius.heavyanchor.entity.HeavyAnchorEntity;
import dev.julius.heavyanchor.index.HeavyAnchorEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class HeavyAnchorItem extends Item {
    public HeavyAnchorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            Vec3 look = player.getLookAngle();
            Vec3 spawn = player.getEyePosition().add(look.scale(1.15)).add(0.0, -0.12, 0.0);
            HeavyAnchorEntity anchor = new HeavyAnchorEntity(HeavyAnchorEntities.HEAVY_ANCHOR.get(), level);
            anchor.setPos(spawn.x, spawn.y, spawn.z);
            anchor.setDeltaMovement(look.scale(0.62).add(0.0, -0.12, 0.0));
            level.addFreshEntity(anchor);
            if (!player.getAbilities().instabuild) stack.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("In der Luft: 0 kg").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Am Boden: 10.000 kg").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("Mit dem Rope Winch wieder einholen").withStyle(ChatFormatting.DARK_GRAY));
    }
}
