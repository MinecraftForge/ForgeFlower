/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.fffixer.fixes;

import static org.objectweb.asm.Opcodes.*;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import de.fernflower.main.ClassesProcessor;
import de.fernflower.main.ClassesProcessorClassNode;
import net.minecraftforge.fffixer.Patch;
import net.minecraftforge.fffixer.Patcher;
import net.minecraftforge.fffixer.Util;
import net.minecraftforge.fffixes.FixesUtil;

/**
 * Fixes decompiler differences between JVM versions caused by HashSet's sorting order changing between JVM implementations.
 * Simple solution is to hijack the Iterator to make it use a properly sorted one.
 * Thanks to fry for finding this issue and pointing me in the right direction.s
 */
public class InnerClassOrderFixer {
    public static final Patch INSTANCE = Patch.of(ClassesProcessor.class, InnerClassOrderFixer::process);
    private static final String SET = Type.getInternalName(Set.class);
    private static final String HASH_SET = Type.getInternalName(HashSet.class);
    private static final String UTILS = Type.getInternalName(FixesUtil.class);

    /// There are two iterators that we are interested in
    /// First:
    ///   var8 = this.mapRootClasses.entrySet().iterator();
    ///   GETFIELD de/fernflower/main/ClassesProcessor.mapRootClasses : Ljava/util/HashMap;
    ///   INVOKEVIRTUAL java/util/HashMap.entrySet ()Ljava/util/Set;
    ///   INVOKEINTERFACE java/util/Set.iterator ()Ljava/util/Iterator; (itf)
    ///   ASTORE 8
    ///
    /// This is the root classes, it shouldn't matter what order they are processed in, but for debugging sake
    /// I sort them by name so that there is a known order to their processing.
    ///
    /// Second:
    ///   Iterator var28 = var26.iterator();
    ///   ALOAD 13
    ///   INVOKEVIRTUAL java/util/HashSet.iterator ()Ljava/util/Iterator;
    ///   ASTORE 15
    ///
    /// This Iterates over the inner classes of each root class, adding to the list of classes
    /// to be written. Which is luckally stored in an array list so sorting is maintained.
    /// So we sort this to the order of classes we want to write.
    ///
    /// Luckally these two invocations are the only invocations of HashSet/Set.iterator() so it's east to find them
    ///
    private static void process(ClassNode node) {
        MethodNode mtd = Util.getMethod(node, "<init>", "(Lde/fernflower/struct/StructContext;)V");

        var foundRoot = false;

        for (var itr = mtd.instructions.iterator(); itr.hasNext(); ) {
            var insn = itr.next();

            if (insn.getType() != AbstractInsnNode.METHOD_INSN)
                continue;

            var v = (MethodInsnNode)insn;
            if (v.getOpcode() != INVOKEVIRTUAL && v.getOpcode() != INVOKEINTERFACE) // int comparisons are fast so do that first
                continue;
            if (!"iterator".equals(v.name) || !"()Ljava/util/Iterator;".equals(v.desc))
                continue;

            if (!foundRoot && insn.getOpcode() == INVOKEINTERFACE && SET.equals(v.owner)) {
                foundRoot = true;
                Patcher.log.info("  Injecting InnerClass Root Order Fix");
                mtd.instructions.insert(insn,
                    new MethodInsnNode(INVOKESTATIC,
                        UTILS,
                        "sortRootClasses",
                        Util.methodDesc(Iterator.class, Iterator.class)
                    )
                );
            } else if (insn.getOpcode() == INVOKEVIRTUAL && HASH_SET.equals(v.owner)) {
                if (!foundRoot)
                    throw new IllegalStateException("Found child iterator before root");

                Patcher.log.info("  Injecting InnerClass Child Order Fix");
                var toAdd = new InsnList();
                toAdd.add(new VarInsnNode(ALOAD, 12)); // 12 is the class node from rootClasses
                toAdd.add(new MethodInsnNode(INVOKESTATIC,
                    UTILS,
                    "sortInnerClasses",
                    Util.methodDesc(Iterator.class, Iterator.class, ClassesProcessorClassNode.class)
                ));

                mtd.instructions.insert(insn, toAdd);

                return;
            }
        }
        throw new IllegalStateException("Could not find injection target");
    }
}