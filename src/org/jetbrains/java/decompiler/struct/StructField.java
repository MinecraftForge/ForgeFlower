/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package org.jetbrains.java.decompiler.struct;

import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences;
import org.jetbrains.java.decompiler.struct.attr.StructGeneralAttribute;
import org.jetbrains.java.decompiler.struct.attr.StructGenericSignatureAttribute;
import org.jetbrains.java.decompiler.struct.consts.ConstantPool;
import org.jetbrains.java.decompiler.struct.gen.generics.GenericFieldDescriptor;
import org.jetbrains.java.decompiler.struct.gen.generics.GenericMain;
import org.jetbrains.java.decompiler.util.DataInputFullStream;

import java.io.IOException;

/*
  field_info {
    u2 access_flags;
    u2 name_index;
    u2 descriptor_index;
    u2 attributes_count;
    attribute_info attributes[attributes_count];
   }
*/
public class StructField extends StructMember {

  private final String name;
  private final String descriptor;
  private GenericFieldDescriptor signature;

  public StructField(DataInputFullStream in, StructClass clStruct) throws IOException {
    accessFlags = in.readUnsignedShort();
    int nameIndex = in.readUnsignedShort();
    int descriptorIndex = in.readUnsignedShort();

    ConstantPool pool = clStruct.getPool();
    String[] values = pool.getClassElement(ConstantPool.FIELD, clStruct.qualifiedName, nameIndex, descriptorIndex);
    name = values[0];
    descriptor = values[1];

    attributes = readAttributes(in, pool);
  }

  public String getName() {
    return name;
  }

  public String getDescriptor() {
    return descriptor;
  }

  @Override
  public String toString() {
    return name;
  }

  public GenericFieldDescriptor getSignature() {
    return signature;
  }

  @Override
  protected StructGeneralAttribute readAttribute(DataInputFullStream in, ConstantPool pool, String name) throws IOException {
    StructGeneralAttribute attribute = super.readAttribute(in, pool, name);
    if ("Signature".equals(name) && DecompilerContext.getOption(IFernflowerPreferences.DECOMPILE_GENERIC_SIGNATURES)) {
      StructGenericSignatureAttribute signature = (StructGenericSignatureAttribute)attribute;
      this.signature = GenericMain.parseFieldSignature(signature.getSignature());
    }
    return attribute;
  }
}
