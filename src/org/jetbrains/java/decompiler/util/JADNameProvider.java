package org.jetbrains.java.decompiler.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jetbrains.java.decompiler.code.CodeConstants;
import org.jetbrains.java.decompiler.main.extern.IVariableNameProvider;
import org.jetbrains.java.decompiler.main.extern.IVariableNamingFactory;
import org.jetbrains.java.decompiler.main.rels.MethodWrapper;
import org.jetbrains.java.decompiler.modules.decompiler.vars.VarVersionPair;
import org.jetbrains.java.decompiler.struct.StructMethod;

public class JADNameProvider implements IVariableNameProvider {
    private HashMap<String, Holder> last = null;
    private HashMap<String, String> remap = null;
    private static final Pattern CAPS_START = Pattern.compile("^[A-Z]");
    private static final Pattern ARRAY = Pattern.compile("(\\[|\\.\\.\\.)");

    public JADNameProvider(StructMethod wrapper) {
      last = new HashMap<String, Holder>();
      last.put("int",     new Holder(0, true,  "i", "j", "k", "l"));
      last.put("byte",    new Holder(0, false, "b"       ));
      last.put("char",    new Holder(0, false, "c"       ));
      last.put("short",   new Holder(1, false, "short"   ));
      last.put("boolean", new Holder(0, true,  "flag"    ));
      last.put("double",  new Holder(0, false, "d"       ));
      last.put("float",   new Holder(0, true,  "f"       ));
      last.put("File",    new Holder(1, true,  "file"    ));
      last.put("String",  new Holder(0, true,  "s"       ));
      last.put("Class",   new Holder(0, true,  "oclass"  ));
      last.put("Long",    new Holder(0, true,  "olong"   ));
      last.put("Byte",    new Holder(0, true,  "obyte"   ));
      last.put("Short",   new Holder(0, true,  "oshort"  ));
      last.put("Boolean", new Holder(0, true,  "obool"   ));
      last.put("Package", new Holder(0, true,  "opackage"));
      last.put("Enum",    new Holder(0, true,  "oenum"   ));

      remap = new HashMap<String, String>();
      remap.put("long", "int");
    }

    @Override
    public void addParentContext(IVariableNameProvider iparent) {
        JADNameProvider parent = (JADNameProvider) iparent;
        last = new HashMap<String, Holder>();
        for (Entry<String, Holder> e : parent.last.entrySet()) {
            Holder v = e.getValue();
            last.put(e.getKey(), new Holder(v.id, v.skip_zero, v.names));
        }

        remap = new HashMap<String, String>();
        for (Entry<String, String> e : parent.remap.entrySet()) {
            remap.put(e.getKey(), e.getValue());
        }
    }

    private static class Holder {
      public int id;
      public boolean skip_zero;
      public final List<String> names = new ArrayList<String>();

      public Holder(int t1, boolean skip_zero, String... names) {
        this.id = t1;
        this.skip_zero = skip_zero;
        Collections.addAll(this.names, names);
      }

      public Holder(int t1, boolean skip_zero, List<String> names) {
        this.id = t1;
        this.skip_zero = skip_zero;
        this.names.addAll(names);
      }
    }

    public Map<VarVersionPair,String> rename(Map<VarVersionPair, String> entries) {
      List<VarVersionPair> keys = new ArrayList<VarVersionPair>(entries.keySet());
      Collections.sort(keys, new Comparator<VarVersionPair>(){
        @Override
        public int compare(VarVersionPair o1, VarVersionPair o2) {
          if (o1.var != o2.var) return o1.var - o2.var;
          return o1.version - o2.version;
        }
      });
      Map<VarVersionPair,String> result = new LinkedHashMap<VarVersionPair,String>();
      for (VarVersionPair ver : keys) {
        String type = entries.get(ver);
        if ("this".equals(type)) {
          continue;
        }
        if (type.indexOf('<') != -1) {
          type = type.substring(0, type.indexOf('<'));
        }
        if (type.indexOf('.') != -1) {
          type = type.substring(type.lastIndexOf('.')+1);
        }
        result.put(ver, getNewName(type));
      }
      return result;
    }

    protected String getNewName(String type) {
        String index = null;
        String findtype = type;

        while (findtype.contains("[][]"))
        {
            findtype = findtype.replaceAll("\\[\\]\\[\\]", "[]");
        }
        if (last.containsKey(findtype))
        {
            index = findtype;
        }
        else if (last.containsKey(findtype.toLowerCase(Locale.ENGLISH)))
        {
            index = findtype.toLowerCase(Locale.ENGLISH);
        }
        else if (remap.containsKey(type))
        {
            index = remap.get(type);
        }

        if ((index == null || index.length() == 0) && (CAPS_START.matcher(type).find() || ARRAY.matcher(type).find()))
        {
            // replace multi things with arrays.
            type = type.replace("...", "[]");

            while (type.contains("[][]"))
            {
                type = type.replaceAll("\\[\\]\\[\\]", "[]");
            }

            String name = type.toLowerCase(Locale.ENGLISH);
            // Strip single dots that might happen because of inner class references
            name = name.replace(".", "");
            boolean skip_zero = true;

            if (Pattern.compile("\\[").matcher(type).find())
            {
                skip_zero = true;
                name = "a" + name;
                name = name.replace("[]", "").replace("...", "");
            }

            last.put(type.toLowerCase(Locale.ENGLISH), new Holder(0, skip_zero, name));
            index = type.toLowerCase(Locale.ENGLISH);
        }

        if (index == null || index.length() == 0)
        {
            return type.toLowerCase(Locale.ENGLISH);
        }

        Holder holder = last.get(index);
        int id = holder.id;
        List<String> names = holder.names;

        int ammount = names.size();

        String name;
        if (ammount == 1)
        {
            name = names.get(0) + (id == 0 && holder.skip_zero ? "" : id);
        }
        else
        {
            int num = id / ammount;
            name = names.get(id % ammount) + (id < ammount && holder.skip_zero ? "" : num);
        }

        holder.id++;
        return name;
    }

    @Override
    public String renameAbstractParameter(String abstractParam, int index) {
      return abstractParam;
    }


    public static class JADNameProviderFactory implements IVariableNamingFactory {
        @Override
        public IVariableNameProvider createFactory(StructMethod method) {
            return new JADNameProvider(method);
        }

    }


    public static class MCPNameProviderFactory implements IVariableNamingFactory {
        @Override
        public IVariableNameProvider createFactory(StructMethod method) {
            return new MCPNameProvider(method);
        }

        private static class MCPNameProvider extends JADNameProvider {
            private static final Pattern p = Pattern.compile("func_(\\d+)_.*");
            private final StructMethod wrapper;

            public MCPNameProvider(StructMethod wrapper)
            {
                super(wrapper);
                this.wrapper = wrapper;
            }

            @Override
            public String renameAbstractParameter(String abstractParam, int index)
            {
                String result = abstractParam;
                if ((wrapper.getAccessFlags() & CodeConstants.ACC_ABSTRACT) != 0) {
                    String methName = wrapper.getName();
                    Matcher m = p.matcher(methName);
                    if (m.matches()) {
                        result = String.format("p_%s_%d_", m.group(1), index);
                    }
                }
                return result;
            }
        }
    }
}