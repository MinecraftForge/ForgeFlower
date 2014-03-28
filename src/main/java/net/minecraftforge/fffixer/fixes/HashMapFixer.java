/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.fffixer.fixes;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import net.minecraftforge.fffixes.J7HashMap;
import net.minecraftforge.fffixes.J7HashSet;

public class HashMapFixer extends ClassVisitor {
    private static final Map<String, String> replacements = Map.of(
        Type.getInternalName(HashMap.class), Type.getInternalName(J7HashMap.class),
        Type.getInternalName(HashSet.class), Type.getInternalName(J7HashSet.class)
    );

    private boolean didWork = false;

    public HashMapFixer(ClassVisitor classVisitor) {
        super(Opcodes.ASM9, classVisitor);
    }

    public boolean didWork() {
        return didWork;
    }

    @Override
    public void visit(final int version, final int access, final String name, final String signature, final String superName, final String[] interfaces) {
        var newSuper = replacements.getOrDefault(superName, superName);
        if (newSuper != superName)
            didWork = true;
        super.visit(version, access, name, signature, newSuper, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(final int access, final String name, final String descriptor, final String signature, final String[] exceptions) {
        var parent = super.visitMethod(access, name, descriptor, signature, exceptions);
        return new MethodVisitor(Opcodes.ASM9, parent) {
            @Override
            public void visitTypeInsn(final int opcode, final String type) {
                var newType = replacements.getOrDefault(type, type);
                if (newType != type)
                    didWork = true;
                super.visitTypeInsn(opcode, newType);
            }

            @Override
            public void visitMethodInsn(final int opcode, final String owner, final String name, final String descriptor, final boolean isInterface) {
                var newOwner = "<init>".equals(name) ? replacements.getOrDefault(owner, owner) : owner;
                if (newOwner != owner) {
                    //System.out.println(owner + '.' + name + descriptor);
                    didWork = true;
                }
                super.visitMethodInsn(opcode, newOwner, name, descriptor, isInterface);
            }
        };
    }
};
