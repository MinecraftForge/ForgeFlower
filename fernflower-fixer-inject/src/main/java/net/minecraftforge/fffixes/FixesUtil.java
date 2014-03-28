/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.fffixes;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import de.fernflower.main.ClassesProcessorClassNode;
import de.fernflower.modules.decompiler.exps.Exprent;
import de.fernflower.modules.decompiler.exps.VarExprent;
import de.fernflower.modules.decompiler.vars.VarVersionPaar;

public class FixesUtil {
    private static <T> List<T> toList(Iterator<T> itr) {
        List<T> ret = new ArrayList<T>();
        while (itr.hasNext())
            ret.add(itr.next());
        return ret;
    }

    private static final Comparator<VarExprent> VAREXPRENT_SORTER = new Comparator<VarExprent>() {
        @Override
        public int compare(VarExprent o1, VarExprent o2) {
            return o1.getIndex() - o2.getIndex();
        }
    };
    public static Iterator<Exprent> sortVarDefintions(Iterator<Exprent> itr) {
        List<Exprent> ret = new ArrayList<Exprent>();
        List<VarExprent> defs = new ArrayList<VarExprent>();

        while (itr.hasNext()) {
            Exprent exp = itr.next();
            boolean isDef = exp instanceof VarExprent && ((VarExprent)exp).isDefinition();
            if (!isDef) {
                if (defs.size() > 0) {
                    Collections.sort(defs, VAREXPRENT_SORTER);
                    ret.addAll(defs);
                    defs.clear();
                }
                ret.add(exp);
            } else {
                defs.add((VarExprent)exp);
            }
        }

        if (defs.size() > 0) {
            Collections.sort(defs, VAREXPRENT_SORTER);
            ret.addAll(defs);
        }
        return ret.iterator();
    }

    private static final Comparator<VarVersionPaar> VAR_SORTER = new Comparator<VarVersionPaar>() {
        @Override
        public int compare(VarVersionPaar o1, VarVersionPaar o2) {
            return o1.var != o2.var ?  o1.var - o2.var : o1.version - o2.version;
        }
    };
    public static Iterator<VarVersionPaar> sortVarVersionPaar(Iterator<VarVersionPaar> itr) {
        List<VarVersionPaar> list = toList(itr);
        Collections.sort(list, VAR_SORTER);
        return list.iterator();
    }

    private static final Comparator<Entry<String, ClassesProcessorClassNode>> ROOT_CLASS_SORTER = new Comparator<Entry<String, ClassesProcessorClassNode>>() {
        @Override
        public int compare(Entry<String, ClassesProcessorClassNode> o1, Entry<String, ClassesProcessorClassNode> o2) {
            return o1.getKey().compareTo(o2.getKey());
        }
    };
    public static Iterator<Entry<String, ClassesProcessorClassNode>> sortRootClasses(Iterator<Entry<String, ClassesProcessorClassNode>> itr) {
        List<Entry<String, ClassesProcessorClassNode>> list = toList(itr);
        Collections.sort(list, ROOT_CLASS_SORTER);
        return list.iterator();
    }

    private static Map<String, List<String>> CLASS_ORDER = new HashMap<String, List<String>>();
    private static Comparator<String> NATURAL_CLASS_SORTER = new Comparator<String>() {
        private final boolean isDigit(char ch) {
            return ((ch >= '0') && (ch <= '9'));
        }
        private final int digits(String s) {
            int ret = 0;
            while (ret < s.length() && isDigit(s.charAt(ret)))
                ret++;
            return ret;
        }

        @Override
        public int compare(String o1, String o2) {
            int i1 = o1.indexOf('$');
            int i2 = o2.indexOf('$');
            String prefix1 = i1 == -1 ? o1 : o1.substring(0, i1);
            String prefix2 = i2 == -1 ? o2 : o2.substring(0, i2);

            int ret = compareNatural(prefix1, prefix2);
            if (ret != 0)
                return ret;

            if (i1 == -1 && i2 != -1)
                return -1;
            if (i1 != -1 && i2 == -1)
                return 1;

            String suffix1 = o1.substring(i1 + 1);
            String suffix2 = o2.substring(i2 + 1);
            return compare(suffix1, suffix2);
        }

        private int compareNatural(String o1, String o2) {
            int d1 = digits(o1);
            int d2 = digits(o2);
            if (d1 != 0 && d2 != 0) {
                if (d1 != d2)
                    return d1 - d2;
            }
            return o1.compareTo(o2);
        }
    };

    public static Iterator<String> sortInnerClasses(Iterator<String> itr, ClassesProcessorClassNode node) {
        List<String> classes = toList(itr);
        if (classes.size() <= 1)
            return classes.iterator();

        final List<String> order = CLASS_ORDER.get(node.classStruct.qualifiedName);
        if (order == null || order.isEmpty()) {
            Collections.sort(classes, NATURAL_CLASS_SORTER);
        } else {
            Collections.sort(classes, new Comparator<String>() {
                @Override
                public int compare(String o1, String o2) {
                    int i1 = order.indexOf(o1);
                    int i2 = order.indexOf(o2);
                    if (i1 != -1 && i2 != -1)
                        return i1 - i2;
                    return NATURAL_CLASS_SORTER.compare(o1, o2);
                }
            });
        }

        System.out.println("Sort Classes: " + node.classStruct.qualifiedName);
        for (String name : classes)
            System.out.println("  " + Integer.toHexString(name.hashCode()) + " " + name);


        return classes.iterator();
    }

    public static String[] processArgs(String[] args) throws IOException {
        if (args == null || args.length == 0)
            return args;
        List<String> input = new ArrayList<String>(args.length);
        for (String str : args)
            input.add(str);

        List<String> params = new ArrayList<String>(args.length);
        for (int x = 0; x < input.size(); x++) {
            String arg = input.get(x);
            String next = x + 1 < input.size() ? input.get(x + 1) : null;
            if (arg.startsWith("-cfg")) {
                String path = null;
                if (arg.startsWith("-cfg="))
                    path = arg.substring(5);
                else if (next != null) {
                    path = next;
                    x++;
                } else {
                    System.out.println("Must specify a file when using -cfg argument.");
                    return null;
                }
                File file = new File(path);
                if (!file.exists()) {
                    System.out.println("error: missing config '" + path + "'");
                    return null;
                }

                BufferedReader reader = null;
                try {
                    reader = new BufferedReader(new FileReader(file));
                    int offset = x + 1;
                    for (String line; (line = reader.readLine()) != null; )
                        input.add(offset++, line);
                } catch (IOException e) {
                    System.out.println("error: Failed to read config file '" + path + "'");
                    throw e;
                } finally {
                    if (reader != null)
                        reader.close();
                }
            } else if (arg.startsWith("-sort")) {
                String value;
                if (arg.startsWith("-sort=")) {
                    value = arg.substring(6);
                } else if (next != null) {
                    value = next;
                    x++;
                } else {
                    System.out.println("Must specify a value for -sort option");
                    return null;
                }

                String[] parts = value.split(",");
                List<String> children = new ArrayList<String>();
                for (int y = 1; y < parts.length; y++)
                    children.add(parts[0] + '$' + parts[y]);
                CLASS_ORDER.put(parts[0], children);
            } else {
                params.add(arg);
            }
        }
        args = params.toArray(new String[params.size()]);

        return args;
    }
}
