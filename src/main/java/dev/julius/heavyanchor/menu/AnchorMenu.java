package dev.julius.heavyanchor.menu;
import dev.julius.heavyanchor.block.HeavyAnchorBlockEntity;
import dev.julius.heavyanchor.index.AnchorMenus;
import dev.ryanhcode.sable.Sable;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class AnchorMenu extends AbstractContainerMenu {
    private final HeavyAnchorBlockEntity anchor;
    private final SimpleContainer ghosts = new SimpleContainer(4);
    public AnchorMenu(int id, Inventory inv) { this(id, inv, null); }
    public AnchorMenu(int id, Inventory inv, HeavyAnchorBlockEntity anchor) {
        super(AnchorMenus.ANCHOR.get(), id);
        this.anchor=anchor;
        if(anchor!=null) for(int i=0;i<4;i++) ghosts.setItem(i,anchor.frequency(i));
        for(int i=0;i<4;i++) addSlot(new Slot(ghosts,i, (i%2)*24+65, i<2?27:59) {
            @Override public boolean mayPlace(ItemStack stack) {return false;}
            @Override public boolean mayPickup(Player player) {return false;}
        });
        for(int row=0;row<3;row++) for(int col=0;col<9;col++) addSlot(new Slot(inv,col+row*9+9,8+col*18,101+row*18));
        for(int i=0;i<9;i++) addSlot(new Slot(inv,i,8+i*18,159));
    }
    @Override public boolean stillValid(Player player) {
        if(anchor==null) return true;
        if(anchor.isRemoved() || player.level()!=anchor.getLevel()) return false;
        Vec3 pos=Sable.HELPER.projectOutOfSubLevel(player.level(), Vec3.atCenterOf(anchor.getBlockPos()));
        return player.position().distanceToSqr(pos)<100;
    }
    @Override public void clicked(int id,int button,ClickType type,Player player) {
        if(id>=0 && id<4) {
            if(type!=ClickType.PICKUP || (button!=0 && button!=1)) return;
            if(!stillValid(player)) return;
            ItemStack stack=button==1 ? ItemStack.EMPTY : getCarried().copyWithCount(1);
            ghosts.setItem(id,stack);
            if(anchor!=null) anchor.setFrequency(id,stack);
            broadcastChanges();
            return;
        }
        if(type==ClickType.QUICK_CRAFT || type==ClickType.PICKUP_ALL) return;
        super.clicked(id,button,type,player);
    }
    @Override public ItemStack quickMoveStack(Player player,int index) {return ItemStack.EMPTY;}
}
