package dev.comfortableitems.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractContainerScreen.class)
public interface ContainerAccess {
    @Invoker("slotClicked") void comfortableItems$click(Slot slot, int index, int button, ClickType action);
    @Invoker("findSlot") Slot comfortableItems$slotAt(double x, double y);
    @Invoker("hasClickedOutside") boolean comfortableItems$outside(double x, double y, int left, int top, int button);
    @Accessor("leftPos") int comfortableItems$left();
    @Accessor("topPos") int comfortableItems$top();
}
