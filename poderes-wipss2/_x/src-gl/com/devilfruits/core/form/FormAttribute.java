package com.devilfruits.core.form;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;

public record FormAttribute(Holder<Attribute> attribute, double amount, Operation operation) {
}
