/*
 * Copyright 2000-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.java.decompiler.modules.decompiler;

import org.jetbrains.java.decompiler.code.CodeConstants;
import org.jetbrains.java.decompiler.main.ClassesProcessor;
import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.main.extern.IFernflowerLogger;
import org.jetbrains.java.decompiler.main.rels.MethodWrapper;
import org.jetbrains.java.decompiler.modules.decompiler.exps.*;
import org.jetbrains.java.decompiler.modules.decompiler.sforms.DirectGraph;
import org.jetbrains.java.decompiler.modules.decompiler.stats.SwitchStatement;
import org.jetbrains.java.decompiler.struct.StructField;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SwitchHelper {
  public static void simplify(SwitchStatement switchStatement) {
    SwitchExprent switchExprent = (SwitchExprent)switchStatement.getHeadexprent();
    Exprent value = switchExprent.getValue();
    if (isEnumArray(value)) {
      List<List<Exprent>> caseValues = switchStatement.getCaseValues();
      Map<Exprent, Exprent> mapping = new HashMap<>(caseValues.size());
      ArrayExprent array = (ArrayExprent)value;
      FieldExprent arrayField = (FieldExprent)array.getArray();
      ClassesProcessor.ClassNode classNode =
        DecompilerContext.getClassProcessor().getMapRootClasses().get(arrayField.getClassname());
      if (classNode != null) {
        MethodWrapper wrapper = classNode.getWrapper().getMethodWrapper(CodeConstants.CLINIT_NAME, "()V");
        if (wrapper != null) {
          wrapper.getOrBuildGraph().iterateExprents(new DirectGraph.ExprentIterator() {
            @Override
            public int processExprent(Exprent exprent) {
              if (exprent instanceof AssignmentExprent) {
                AssignmentExprent assignment = (AssignmentExprent)exprent;
                Exprent left = assignment.getLeft();
                if (left.type == Exprent.EXPRENT_ARRAY && ((ArrayExprent)left).getArray().equals(arrayField)) {
                  mapping.put(assignment.getRight(), ((InvocationExprent)((ArrayExprent)left).getIndex()).getInstance());
                }
              }
              return 0;
            }
          });
        }
      }

      List<List<Exprent>> realCaseValues = new ArrayList<>(caseValues.size());
      for (List<Exprent> caseValue : caseValues) {
        List<Exprent> values = new ArrayList<>(caseValue.size());
        realCaseValues.add(values);
        for (Exprent exprent : caseValue) {
          if (exprent == null) {
            values.add(null);
          }
          else {
            Exprent realConst = mapping.get(exprent);
            if (realConst == null) {
              DecompilerContext.getLogger()
                .writeMessage("Unable to simplify switch on enum: " + exprent + " not found, available: " + mapping,
                              IFernflowerLogger.Severity.ERROR);
              return;
            }
            values.add(realConst.copy());
          }
        }
      }
      caseValues.clear();
      caseValues.addAll(realCaseValues);
      switchExprent.replaceExprent(value, ((InvocationExprent)array.getIndex()).getInstance().copy());
    }
  }

  static final int STATIC_FINAL_SYNTHETIC = CodeConstants.ACC_STATIC | CodeConstants.ACC_STATIC | CodeConstants.ACC_FINAL | CodeConstants.ACC_SYNTHETIC;
  /**
   * When Java introduced Enums they aded the ability to use them in Switch statements.
   * This was done in a purely syntax sugar way using the old switch on int methods.
   * The compiler creates a synthetic class with a static int array field.
   * To support enums changing post compile, It initializes this field with a length of the current enum length.
   * And then for every referenced enum value it adds a mapping in the form of:
   *   try {
   *     field[Enum.VALUE.ordinal()] = 1;
   *   } catch (FieldNotFoundException e) {}
   *
   * If a class has multiple switches on multiple enums, the compiler adds the init and try list to the BEGINNING of the static initalizer.
   * But they add the field to the END of the fields list.
   * 
   * Note: SOME compilers name the field starting with $SwitchMap, so if we do not have full context this can be a guess.
   * But Obfuscated/renamed code could cause issues
   */
  private static boolean isEnumArray(Exprent exprent) {
    if (exprent instanceof ArrayExprent) {
      Exprent tmp = ((ArrayExprent)exprent).getArray();
      if (tmp instanceof FieldExprent) {
        FieldExprent field = (FieldExprent)tmp;
        ClassesProcessor.ClassNode classNode = DecompilerContext.getClassProcessor().getMapRootClasses().get(field.getClassname());
        
        if (classNode == null || !"[I".equals(field.getDescriptor().descriptorString)) {
          return field.getName().startsWith("$SwitchMap"); //This is non-standard but we don't have any more information so..
        }
        
        StructField stField = classNode.getWrapper().getClassStruct().getField(field.getName(), field.getDescriptor().descriptorString);
        if ((stField.getAccessFlags() & STATIC_FINAL_SYNTHETIC) != STATIC_FINAL_SYNTHETIC) {
          return false;
        }
        
        if ((classNode.getWrapper().getClassStruct().getAccessFlags() & CodeConstants.ACC_SYNTHETIC) == CodeConstants.ACC_SYNTHETIC) {
          return true; //TODO: Find a way to check the structure of the initalizer?
          //Exprent init = classNode.getWrapper().getStaticFieldInitializers().getWithKey(InterpreterUtil.makeUniqueKey(field.getName(), field.getDescriptor().descriptorString));
          //Above is null because we haven't preocess the class yet?
        }
      }
    }
    return false;
  }
}
