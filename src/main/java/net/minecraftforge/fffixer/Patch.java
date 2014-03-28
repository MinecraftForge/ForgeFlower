/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.fffixer;

import java.util.function.Consumer;

import org.objectweb.asm.tree.ClassNode;

public record Patch(String target, Consumer<ClassNode> func) {
    public static Patch of(Class<?> target, Consumer<ClassNode> func) {
        return new Patch(Util.className(target) + ".class", func);
    }
}