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
package org.jetbrains.java.decompiler;

import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences;
import org.junit.Test;

public class SingleClassesTest extends SingleClassesTestBase {
  protected DecompilerTestFixture fixture;
  
  @Override
  protected String[] getDecompilerOptions() {
    return new String[] {
      IFernflowerPreferences.BYTECODE_SOURCE_MAPPING, "1",
      IFernflowerPreferences.DUMP_ORIGINAL_LINES, "1"
    };
  }

  @Test public void testClassFields() { doTest("pkg/TestClassFields"); }
  @Test public void testInterfaceFields() { doTest("pkg/TestInterfaceFields"); }
  @Test public void testClassLambda() { doTest("pkg/TestClassLambda"); }
  @Test public void testClassLoop() { doTest("pkg/TestClassLoop"); }
  @Test public void testClassSwitch() { doTest("pkg/TestClassSwitch"); }
  @Test public void testClassTypes() { doTest("pkg/TestClassTypes"); }
  @Test public void testClassVar() { doTest("pkg/TestClassVar"); }
  @Test public void testClassNestedInitializer() { doTest("pkg/TestClassNestedInitializer"); }
  @Test public void testClassCast() { doTest("pkg/TestClassCast"); }
  @Test public void testDeprecations() { doTest("pkg/TestDeprecations"); }
  @Test public void testExtendsList() { doTest("pkg/TestExtendsList"); }
  @Test public void testMethodParameters() { doTest("pkg/TestMethodParameters"); }
  @Test public void testCodeConstructs() { doTest("pkg/TestCodeConstructs"); }
  @Test public void testConstants() { doTest("pkg/TestConstants"); }
  @Test public void testEnum() { doTest("pkg/TestEnum"); }
  @Test public void testDebugSymbols() { doTest("pkg/TestDebugSymbols"); }
  @Test public void testInvalidMethodSignature() { doTest("InvalidMethodSignature"); }
  @Test public void testAnonymousClassConstructor() { doTest("pkg/TestAnonymousClassConstructor"); }
  @Test public void testInnerClassConstructor() { doTest("pkg/TestInnerClassConstructor"); }
  @Test public void testInnerClassConstructor11() { doTest("v11/TestInnerClassConstructor"); }
  @Test public void testTryCatchFinally() { doTest("pkg/TestTryCatchFinally"); }
  @Test public void testAmbiguousCall() { doTest("pkg/TestAmbiguousCall"); }
  @Test public void testAmbiguousCallWithDebugInfo() { doTest("pkg/TestAmbiguousCallWithDebugInfo"); }
  @Test public void testSimpleBytecodeMapping() { doTest("pkg/TestClassSimpleBytecodeMapping"); }
  @Test public void testSynchronizedMapping() { doTest("pkg/TestSynchronizedMapping"); }
  @Test public void testAbstractMethods() { doTest("pkg/TestAbstractMethods"); }
  @Test public void testLocalClass() { doTest("pkg/TestLocalClass"); }
  @Test public void testAnonymousClass() { doTest("pkg/TestAnonymousClass"); }
  @Test public void testThrowException() { doTest("pkg/TestThrowException"); }
  @Test public void testInnerLocal() { doTest("pkg/TestInnerLocal"); }
  @Test public void testInnerLocalPkg() { doTest("pkg/TestInnerLocalPkg"); }
  @Test public void testInnerSignature() { doTest("pkg/TestInnerSignature"); }
  @Test public void testAnonymousSignature() { doTest("pkg/TestAnonymousSignature"); }
  @Test public void testLocalsSignature() { doTest("pkg/TestLocalsSignature"); }
  @Test public void testParameterizedTypes() { doTest("pkg/TestParameterizedTypes"); }
  @Test public void testShadowing() { doTest("pkg/TestShadowing", "pkg/Shadow", "ext/Shadow"); }
  @Test public void testStringConcat() { doTest("pkg/TestStringConcat"); }
  @Test public void testJava9StringConcat() { doTest("java9/TestJava9StringConcat"); }
  @Test public void testMethodReferenceSameName() { doTest("pkg/TestMethodReferenceSameName"); }
  @Test public void testMethodReferenceLetterClass() { doTest("pkg/TestMethodReferenceLetterClass"); }
  @Test public void testConstructorReference() { doTest("pkg/TestConstructorReference"); }
  @Test public void testMemberAnnotations() { doTest("pkg/TestMemberAnnotations"); }
  @Test public void testMoreAnnotations() { doTest("pkg/MoreAnnotations"); }
  @Test public void testTypeAnnotations() { doTest("pkg/TypeAnnotations"); }
  @Test public void testStaticNameClash() { doTest("pkg/TestStaticNameClash"); }
  @Test public void testExtendingSubclass() { doTest("pkg/TestExtendingSubclass"); }
  @Test public void testSyntheticAccess() { doTest("pkg/TestSyntheticAccess"); }
  @Test public void testIllegalVarName() { doTest("pkg/TestIllegalVarName"); }
  @Test public void testIffSimplification() { doTest("pkg/TestIffSimplification"); }
  @Test public void testKotlinConstructor() { doTest("pkg/TestKotlinConstructorKt"); }
  @Test public void testAsserts() { doTest("pkg/TestAsserts"); }
  @Test public void testLocalsNames() { doTest("pkg/TestLocalsNames"); }
  @Test public void testAnonymousParamNames() { doTest("pkg/TestAnonymousParamNames"); }
  @Test public void testAnonymousParams() { doTest("pkg/TestAnonymousParams"); }
  @Test public void testAccessReplace() { doTest("pkg/TestAccessReplace"); }
  @Test public void testStringLiterals() { doTest("pkg/TestStringLiterals"); }
  @Test public void testPrimitives() { doTest("pkg/TestPrimitives"); }
  @Test public void testClashName() { doTest("pkg/TestClashName", "pkg/SharedName1",
          "pkg/SharedName2", "pkg/SharedName3", "pkg/SharedName4", "pkg/NonSharedName",
          "pkg/TestClashNameParent", "ext/TestClashNameParent","pkg/TestClashNameIface", "ext/TestClashNameIface"); }
  @Test public void testSwitchOnEnum() { doTest("pkg/TestSwitchOnEnum");}
  @Test public void testVarArgCalls() { doTest("pkg/TestVarArgCalls"); }
  @Test public void testLambdaParams() { doTest("pkg/TestLambdaParams"); }
  @Test public void testInterfaceMethods() { doTest("pkg/TestInterfaceMethods"); }
  @Test public void testConstType() { doTest("pkg/TestConstType"); }

  // TODO: fix all below
  //@Test public void testPackageInfo() { doTest("pkg/package-info"); }
  //@Test public void testSwitchOnStrings() { doTest("pkg/TestSwitchOnStrings");}
  //@Test public void testUnionType() { doTest("pkg/TestUnionType"); }
  //@Test public void testInnerClassConstructor2() { doTest("pkg/TestInner2"); }
  //@Test public void testInUse() { doTest("pkg/TestInUse"); }
  //@Test public void testInterfaceSuper() { doTest("pkg/TestInterfaceSuper"); }
}