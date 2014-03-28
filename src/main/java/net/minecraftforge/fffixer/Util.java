/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.fffixer;

import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class Util {
    public static String className(Class<?> cls) {
        return Type.getInternalName(cls);
    }

    public static MethodNode getMethod(ClassNode cls, String name, String desc) {
        for (MethodNode method : cls.methods) {
            if (method.name.equals(name) && method.desc.equals(desc))
                return method;
        }
        return null;
    }

    public static String methodDesc(Class<?> ret, Class<?>... args) {
        var types = new ArrayList<Type>(args.length);
        for (var arg : args)
            types.add(Type.getType(arg));
        return Type.getMethodDescriptor(Type.getType(ret), types.toArray(Type[]::new));
    }

    public static Path getPath(Class<?> cls) {
        return getPath(className(cls) + ".class", cls.getClassLoader());
    }

    public static Path getPath(String resource) {
        return getPath(resource, Util.class.getClassLoader());
    }

    public static Path getPath(String resource, ClassLoader cl) {
        var url = cl.getResource(resource);
        if (url == null)
            throw new IllegalStateException("Could not find " + resource + " in classloader " + cl);

        var str = url.toString();
        int len = resource.length();
        if ("jar".equalsIgnoreCase(url.getProtocol())) {
            str = url.getFile();
            len += 2;
        }
        str = str.substring(0, str.length() - len);
        var path = Path.of(URI.create(str));
        return path;
    }
}
