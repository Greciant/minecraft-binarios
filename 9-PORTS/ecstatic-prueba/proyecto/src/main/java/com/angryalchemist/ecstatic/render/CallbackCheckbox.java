package com.angryalchemist.ecstatic.render;

import java.util.function.Consumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.network.chat.Component;

// Port 1.20.1: Checkbox no tiene builder() ni onValueChange; esta subclase hace lo mismo.
final class CallbackCheckbox extends Checkbox {
    private final Consumer<Boolean> onChange;

    CallbackCheckbox(int x, int y, Font font, Component message, boolean selected, Consumer<Boolean> onChange) {
        super(x, y, font.width(message) + 24, 20, message, selected);
        this.onChange = onChange;
    }

    @Override
    public void onPress() {
        super.onPress();
        this.onChange.accept(this.selected());
    }
}
