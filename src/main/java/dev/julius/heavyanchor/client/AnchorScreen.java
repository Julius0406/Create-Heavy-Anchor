package dev.julius.heavyanchor.client;
import dev.julius.heavyanchor.menu.AnchorMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
public class AnchorScreen extends AbstractContainerScreen<AnchorMenu> {
    public AnchorScreen(AnchorMenu menu, Inventory inventory, Component title) {
        super(menu,inventory,title); imageWidth=176; imageHeight=183; inventoryLabelY=89;
    }
    @Override public void render(GuiGraphics g,int mx,int my,float partial) {
        super.render(g,mx,my,partial); renderTooltip(g,mx,my);
    }
    @Override protected void renderBg(GuiGraphics g,float p,int mx,int my) {
        int x=leftPos,y=topPos;
        g.fill(x,y,x+176,y+183,0xffc6c6c6);
        g.fill(x,y,x+176,y+2,0xffeeeeee);g.fill(x,y+181,x+176,y+183,0xff555555);
        g.fill(x+7,y+22,x+169,y+49,0xff883c39);
        g.fill(x+7,y+54,x+169,y+81,0xff3e608d);
        for(var slot:menu.slots) {
            int a=x+slot.x,b=y+slot.y;
            g.fill(a-1,b-1,a+17,b+17,0xff555555);
            g.fill(a,b,a+16,b+16,0xff8b8b8b);
        }
    }
    @Override protected void renderLabels(GuiGraphics g,int mx,int my) {
        super.renderLabels(g,mx,my);
        g.drawString(font,Component.translatable("heavyanchor.menu.on"),10,31,0xffffff,false);
        g.drawString(font,Component.translatable("heavyanchor.menu.off"),10,63,0xffffff,false);
        g.drawString(font,Component.literal("+"),109,31,0xffffff,false);
        g.drawString(font,Component.literal("-"),109,63,0xffffff,false);
    }
}
