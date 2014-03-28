/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.fffixer.fixes;

import static org.objectweb.asm.Opcodes.ICONST_1;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;

import de.fernflower.main.decompiler.helper.PrintStreamLogger;
import net.minecraftforge.fffixer.Patch;
import net.minecraftforge.fffixer.Patcher;
import net.minecraftforge.fffixer.Util;

// Just enables StackTraces in the Default logger.
public class EnableStackTracesInLog {
    public static final Patch INSTANCE = Patch.of(PrintStreamLogger.class, EnableStackTracesInLog::process);
    private static void process(ClassNode node) {
        MethodNode mtd = Util.getMethod(node, "getShowStacktrace", "()Z");
        mtd.instructions.set(mtd.instructions.getFirst(), new InsnNode(ICONST_1));
        Patcher.log.info("  Enabeling printing stack traces in StreamLogger");
    }
}