/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.fffixer;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.FileOutputStream;
import java.util.logging.StreamHandler;

import de.fernflower.main.Fernflower;

import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import net.minecraftforge.fffixes.FixesUtil;

public class Main {
    private final static Logger log = Logger.getLogger("FFFixer");

    public static void main(String[] args) throws Exception {
        var version = JarVersionInfo.of("FernFlowerFixer", Main.class).implementation();

        var ff = Util.getPath(Fernflower.class).toFile();
        var inject = Util.getPath(FixesUtil.class).toFile();

        var parser = new OptionParser();
        var helpO = parser.accepts("help", "Prints usage info and exits").forHelp();
        var versionO = parser.accepts("version", "Prints version info and exists").forHelp();
        var outO = parser.accepts("out", "Output Jar file to place patched files").withRequiredArg().ofType(File.class);
        var logO = parser.accepts("log", "Log file").withRequiredArg().ofType(File.class);

        var options = parser.parse(args);
        if (options.has(helpO)) {
            System.out.println(version);
            parser.printHelpOn(System.out);
            return;
        } else if (options.has(versionO)) {
            System.out.println(version);
            return;
        }

        try {
            var jarIn   = ff;
            var jarOut  = options.valueOf(outO);
            var log     = options.valueOf(logO);

            Main.log.setUseParentHandlers(false);
            Main.log.setLevel(Level.ALL);

            if (log != null) {
                StreamHandler filehandler = new StreamHandler(new FileOutputStream(log), new Formatter() {
                    @Override
                    public synchronized String format(LogRecord record) {
                        StringBuffer sb = new StringBuffer();
                        String message = this.formatMessage(record);
                        sb.append(record.getLevel().getName());
                        sb.append(": ");
                        sb.append(message);
                        sb.append("\n");
                        if (record.getThrown() != null) {
                            try {
                                StringWriter sw = new StringWriter();
                                PrintWriter pw = new PrintWriter(sw);
                                record.getThrown().printStackTrace(pw);
                                pw.close();
                                sb.append(sw.toString());
                            } catch (Exception ex){}
                        }
                        return sb.toString();
                    }

                });
                Main.log.addHandler(filehandler);
            }

            Main.log.addHandler(new Handler() {
                @Override
                public void publish(LogRecord record) {
                    if (log != null && record.getLevel().intValue() < Level.INFO.intValue()) return;
                    System.out.println(String.format(record.getMessage(), record.getParameters()));
                }
                @Override public void flush() {}
                @Override public void close() throws SecurityException {}
            });

            log(version);
            log("Input:  " + jarIn);
            log("Output: " + jarOut);
            log("Inject: " + inject);
            log("Log:    " + log);

            try {
                new Patcher().processJar(jarIn, jarOut, inject);
                Main.log.fine("Processed " + jarIn);
            } catch (Exception e) {
                System.err.println("ERROR: " + e.getMessage());
                throw e;
            }
        } catch (OptionException e) {
            System.err.println("ERROR: " + e.getMessage());
            parser.printHelpOn(System.out);
            throw e;
        }
    }

    private static void log(String line) {
        log.info(line);
    }
}
