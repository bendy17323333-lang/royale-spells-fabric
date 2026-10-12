// SPDX-License-Identifier: MIT
package dev.royalespells.client;

import dev.royalespells.CardLevels;
import net.minecraft.client.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

/** Uses vanilla's creative inventory transaction, which the server validates.
 * Survival clients cannot use this to edit a stack or bypass upgrade recipes. */
public final class CardLevelScreen extends Screen {
    private static final KeyMapping KEY=new KeyMapping("key.royalespells.card_level",GLFW.GLFW_KEY_G,"key.categories.royalespells");
    private final InteractionHand hand;
    private final ItemStack card;
    private int selected;
    private CardLevelScreen(InteractionHand hand,ItemStack card){
        super(Component.translatable("screen.royalespells.card_levels"));this.hand=hand;this.card=card.copy();selected=CardLevels.get(card);
    }
    public static void install(net.neoforged.bus.api.IEventBus bus){
        bus.addListener((net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent e)->e.register(KEY));
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.ClientTickEvent.Post e)->{
            var c=Minecraft.getInstance();while(KEY.consumeClick()){
                if(c.screen!=null||c.player==null||!c.player.isCreative())continue;
                for(var hand:InteractionHand.values())if(CardLevels.supported(c.player.getItemInHand(hand))){c.setScreen(new CardLevelScreen(hand,c.player.getItemInHand(hand)));break;}
            }
        });
    }
    @Override protected void init(){
        for(int n=CardLevels.MIN;n<=CardLevels.MAX;n++){
            final int level=n;int i=n-1;
            addRenderableWidget(Button.builder(Component.literal(Integer.toString(level)),b->selected=level)
                .bounds(width/2-106+(i%4)*54,height/2-30+(i/4)*24,50,20).build());
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.done"),b->apply()).bounds(width/2-106,height/2+90,212,20).build());
    }
    private void apply(){
        var p=minecraft.player;
        if(p!=null&&p.isCreative()&&ItemStack.isSameItemSameComponents(card,p.getItemInHand(hand))){
            ItemStack updated=CardLevels.withLevel(card,selected);p.setItemInHand(hand,updated);
            int slot=hand==InteractionHand.MAIN_HAND?36+p.getInventory().selected:45;
            minecraft.gameMode.handleCreativeModeItemAdd(updated,slot);
        }
        onClose();
    }
    @Override public void render(GuiGraphics g,int x,int y,float partial){
        renderBackground(g,x,y,partial);super.render(g,x,y,partial);
        g.drawCenteredString(font,title,width/2,height/2-90,0xFFFFFF);
        g.renderItem(card,width/2-8,height/2-69);
        g.drawCenteredString(font,Component.translatable("tooltip.royalespells.card_level",selected),width/2,height/2-44,0xC9A8FF);
        g.drawCenteredString(font,Component.translatable("screen.royalespells.card_multiplier",String.format(java.util.Locale.ROOT,"%.3f",CardLevels.multiplier(selected))),width/2,height/2+73,0xC5D7E6);
    }
    @Override public boolean isPauseScreen(){return false;}
}
