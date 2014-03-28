/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.fffixer.fixes;

import static org.objectweb.asm.Opcodes.*;

import java.util.List;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import de.fernflower.modules.decompiler.ExprProcessor;
import de.fernflower.modules.decompiler.sforms.DirectGraph;
import de.fernflower.modules.decompiler.vars.VarTypeProcessor;
import de.fernflower.modules.decompiler.vars.VarVersionsProcessor;
import net.minecraftforge.fffixer.Patch;
import net.minecraftforge.fffixer.Patcher;
import net.minecraftforge.fffixer.Util;
import net.minecraftforge.fffixes.FixesUtil;

/**
 * Fixes decompiler differences between JVM versions caused by HashSet's sorting order changing between JVM implementations.
 * Simple solution is to hijack the Iterator to make it use a properly sorted one.
 * Thanks to fry for finding this issue with class names, which then led me to look for var names.
 *
 * Code injected in ExprProcessor:
 *   var = FixesUtil.sortIndexed(var)
 *
 * Code Injected in VarVersionsProcessor:
 *   var = FixesUtil.sortVarVersionPaar(var);
 */
public class VariableNumberFixer {
    public static List<Patch> PATCHES = List.of(
        Patch.of(ExprProcessor.class,        VariableNumberFixer::processExpProcessor),
        Patch.of(VarVersionsProcessor.class, VariableNumberFixer::processVarVersionsProcessor)
    );

    /* Old Code, Kept in case we wanna inject it again
    private void fix_aC(ClassNode node)
    {
        FFFixerImpl.log.info("Adding index getter to aC");
        node.interfaces.add(Type.getInternalName(Util.Indexed.class));

        String idx = Type.getInternalName(Util.Indexed.class);
        MethodNode mn = new MethodNode(ACC_PUBLIC | ACC_FINAL, "getIndex", "()I", null, null);
        mn.visitCode();
        mn.visitVarInsn(ALOAD, 0);
        mn.visitFieldInsn(GETFIELD, "aC", "d", "LaJ;");
        mn.visitTypeInsn(INSTANCEOF, idx);
        Label l0 = new Label();
        mn.visitJumpInsn(IFNE, l0);
        mn.visitInsn(ICONST_M1);
        mn.visitInsn(IRETURN);
        mn.visitLabel(l0);
        mn.visitVarInsn(ALOAD, 0);
        mn.visitFieldInsn(GETFIELD, "aC", "d", "LaJ;");
        mn.visitTypeInsn(CHECKCAST, idx);
        mn.visitMethodInsn(INVOKEINTERFACE, idx, "getIndex", "()I", true);
        mn.visitInsn(ICONST_M1);
        mn.visitInsn(IMUL);
        mn.visitInsn(IRETURN);
        mn.visitEnd();
        node.methods.add(mn);

        inst.setWorkDone();
    }
    */
    private static void processExpProcessor(ClassNode node) {
        var mtd = Util.getMethod(node, "listToJava", Util.methodDesc(String.class, List.class, int.class));

        for (var itr = mtd.instructions.iterator(); itr.hasNext(); ) {
            if (itr.next() instanceof MethodInsnNode v) {
                // first iterator call
                if(v.getOpcode() == INVOKEINTERFACE && (v.owner + "/" + v.name + v.desc).equals("java/util/List/iterator()Ljava/util/Iterator;")) {
                    Patcher.log.info("  Injecting Var Order Fix");
                    mtd.instructions.insert(v, new MethodInsnNode(
                        INVOKESTATIC,
                        Type.getInternalName(FixesUtil.class),
                        "sortVarDefintions",
                        "(Ljava/util/Iterator;)Ljava/util/Iterator;"
                    ));
                    return;
                }
            }
        }

        throw new IllegalStateException("Could not find patch target");
    }

    private static void processVarVersionsProcessor(ClassNode node) {
        var mtd = Util.getMethod(node, "setNewVarIndices", Util.methodDesc(void.class, VarTypeProcessor.class, DirectGraph.class));

        for (var itr = mtd.instructions.iterator(); itr.hasNext(); ) {
            if (itr.next() instanceof MethodInsnNode v) {
                if (v.getOpcode() == INVOKEVIRTUAL && (v.owner + "/" + v.name + v.desc).equals("java/util/HashSet/iterator()Ljava/util/Iterator;")) {
                    Patcher.log.info("  Injecting Var Order Fix");

                    var var = (VarInsnNode)itr.next(); //Pop off the next which is ASTORE 15
                    var toAdd = new InsnList();
                    toAdd.add(new VarInsnNode(ALOAD, var.var)); // var15 = fixInnerOrder(var15)
                    toAdd.add(new MethodInsnNode(INVOKESTATIC,
                        Type.getInternalName(FixesUtil.class),
                        "sortVarVersionPaar",
                        "(Ljava/util/Iterator;)Ljava/util/Iterator;"
                    ));
                    toAdd.add(new VarInsnNode(ASTORE, var.var));
                    mtd.instructions.insert(var, toAdd); // Inject static call
                    return;
                }
            }
        }

        throw new IllegalStateException("Could not find patch target");
    }
}
