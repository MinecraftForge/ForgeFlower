/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.fffixer.fixes;

import static org.objectweb.asm.Opcodes.ACONST_NULL;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.ASTORE;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.GOTO;
import static org.objectweb.asm.Opcodes.IFNULL;

import java.util.HashMap;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.VarInsnNode;

import de.fernflower.main.ClassesProcessorClassNode;
import de.fernflower.main.rels.MethodWrapper;
import de.fernflower.main.rels.NestedMemberAccess;
import de.fernflower.modules.decompiler.exps.Exprent;
import de.fernflower.modules.decompiler.exps.FieldExprent;
import net.minecraftforge.fffixer.Patch;
import net.minecraftforge.fffixer.Patcher;
import net.minecraftforge.fffixer.Util;


/**
 * Fix an issue in the 'NestedMemberAccess' class when decompiling inner classes that causes
 * it to error with a NPE by adding the following code to the m_1145_
 * directly after the first line:
 *
 *     if ((var5 == null) || (var5.mapFieldsToVars == null)) {
 *           return null;
 *     }
 *
 * @author LexManos, From old research done on StackOverflow and the MCP team ages ago.
 *
 */
public class InnerClassNPEFixer {
    public static final Patch INSTANCE = Patch.of(NestedMemberAccess.class, InnerClassNPEFixer::process);

    private static void process(ClassNode node) {
        var name = "m_1145_";
        var desc = Util.methodDesc(Exprent.class, ClassesProcessorClassNode.class, MethodWrapper.class, FieldExprent.class);

        var mtd = Util.getMethod(node, name, desc);

        var toAdd = new InsnList();
        var ret = new LabelNode();
        var end = new LabelNode();
        toAdd.add(new VarInsnNode (ALOAD, 0));
        toAdd.add(new JumpInsnNode(IFNULL, ret)); // if (var0 == null)
        toAdd.add(new VarInsnNode (ALOAD, 0));
        toAdd.add(new FieldInsnNode(GETFIELD, Type.getInternalName(ClassesProcessorClassNode.class), "mapFieldsToVars", Type.getDescriptor(HashMap.class)));
        toAdd.add(new JumpInsnNode(IFNULL, ret));//    || (var0.h == null)
        toAdd.add(new JumpInsnNode(GOTO, end));
        toAdd.add(ret);
        toAdd.add(new InsnNode(ACONST_NULL));
        toAdd.add(new InsnNode(ARETURN));        //        return null
        toAdd.add(end);

        for (var itr = mtd.instructions.iterator(); itr.hasNext(); ) {
            var insn = itr.next();
            if (insn instanceof VarInsnNode var) {
                if (var.getOpcode() == ASTORE && var.var == 0) {
                    Patcher.log.info("  Injecting InnerClass NPE Fix");
                    mtd.instructions.insert(insn, toAdd);
                    return;
                }
            }
        }

        throw new IllegalStateException("Could not find patch target");
    }
}

