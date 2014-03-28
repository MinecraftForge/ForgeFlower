/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.fffixer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import net.minecraftforge.fffixer.fixes.CLIArgFixer;
import net.minecraftforge.fffixer.fixes.EnableStackTracesInLog;
import net.minecraftforge.fffixer.fixes.HashMapFixer;
import net.minecraftforge.fffixer.fixes.InnerClassNPEFixer;
import net.minecraftforge.fffixer.fixes.InnerClassOrderFixer;

public class Patcher {
    public final static Logger log = Logger.getLogger("FFFixer");
    private final Map<String, List<Patch>> patches = new HashMap<>();
    private void add(Patch patch) {
        patches.computeIfAbsent(patch.target(), _ -> new ArrayList<>()).add(patch);
    }

    public Patcher() {
        add(CLIArgFixer.INSTANCE);
        add(InnerClassNPEFixer.INSTANCE);
        add(InnerClassOrderFixer.INSTANCE);
        //VariableNumberFixer.PATCHES.forEach(this::add);
        add(EnableStackTracesInLog.INSTANCE);
    }

    private static void makeParents(File file) {
        if (file == null)
            return;
        var parent = file.getAbsoluteFile().getParentFile();
        if (parent != null && !parent.exists())
            parent.mkdirs();
    }

    public void processJar(File inFile, File outFile, File inject) throws IOException {
        if (!inFile.exists())
            throw new IllegalArgumentException("Input file does not exist: " + inFile.getAbsolutePath());

        makeParents(outFile);

        var seen = new HashSet<String>();
        try (var zin = new ZipInputStream(new FileInputStream(inFile));
             var zout = new ZipOutputStream(outFile == null ? new ByteArrayOutputStream() : new FileOutputStream(outFile))) {

            for (ZipEntry entry; (entry = zin.getNextEntry()) != null; ) {
                if (entry.isDirectory())
                    continue;

                var name = entry.getName();
                var newEntry = new ZipEntry(name);
                newEntry.setTime(entry.getTime());
                zout.putNextEntry(newEntry);


                if (!entry.getName().endsWith(".class")) {
                    zin.transferTo(zout);
                } else {

                    var data = zin.readAllBytes();
                    var reader = new ClassReader(data);
                    var node = new ClassNode();
                    var hashMaps = new HashMapFixer(node);
                    reader.accept(hashMaps, 0);

                    var patches = this.patches.getOrDefault(name, List.of());
                    if (hashMaps.didWork() || !patches.isEmpty()) {
                        log.fine("Processing " + entry.getName());
                        if (hashMaps.didWork())
                            log.fine("  Hash");
                        for (var patch : patches)
                            patch.func().accept(node);
                        var writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                        node.accept(writer);
                        data = writer.toByteArray();
                    }
                    zout.write(data);

                }
                zout.closeEntry();
            }

            // Add Our Util classes:
            log.info("Injecting: " + inject);
            if (inject.isFile()) {
                try (var zip = new ZipInputStream(new FileInputStream(inject))) {
                   for (ZipEntry entry; (entry = zip.getNextEntry()) != null; ) {
                       var name = entry.getName();
                       if (entry.isDirectory() || name.startsWith("META-INF/"))
                           continue;

                       log.info("  " + name);
                       var newEntry = new ZipEntry(name);
                       newEntry.setTime(entry.getTime());
                       zout.putNextEntry(newEntry);
                       zip.transferTo(zout);
                       zout.closeEntry();
                   }
                }
            } else {
                var root = inject.toPath();
                for (var file : Files.walk(root).toList()) {
                    var name = root.relativize(file).toString().replace('\\', '/');

                    if (Files.isDirectory(file) || name.startsWith("META-INF/"))
                        continue;

                    log.info("  " + name);
                    var newEntry = new ZipEntry(name);
                    newEntry.setTime(Files.getLastModifiedTime(file).toMillis());
                    zout.putNextEntry(newEntry);
                    zout.write(Files.readAllBytes(file));
                    zout.closeEntry();
                }
            }
        }

        seen.stream().sorted().forEach(System.out::println);
    }
}
