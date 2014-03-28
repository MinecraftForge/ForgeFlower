/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.fffixer.fixes;

import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ASTORE;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import de.fernflower.main.decompiler.ConsoleDecompiler;
import net.minecraftforge.fffixer.Patch;
import net.minecraftforge.fffixer.Patcher;
import net.minecraftforge.fffixer.Util;
import net.minecraftforge.fffixes.FixesUtil;

/**
 * Add extra command line argument processing, so that we can support --cfg to lower the command line length
 *   public static void main(String[] args) {
 *     args = FixesUtil.processArgs(args);
 *     // Rest of the code
 *   }
 */
public class CLIArgFixer {
    public static final Patch INSTANCE = Patch.of(ConsoleDecompiler.class, CLIArgFixer::process);

    private static void process(ClassNode node) {
        var mtd = Util.getMethod(node, "main", "([Ljava/lang/String;)V");

        var toAdd = new InsnList();
        toAdd.add(new VarInsnNode(ALOAD, 0));
        toAdd.add(new MethodInsnNode(INVOKESTATIC, Type.getInternalName(FixesUtil.class), "processArgs", "([Ljava/lang/String;)[Ljava/lang/String;"));
        toAdd.add(new VarInsnNode(ASTORE, 0));

        Patcher.log.info("  Injecting CLI Enhancer");
        mtd.instructions.insertBefore(mtd.instructions.getFirst(), toAdd);
    }
}

