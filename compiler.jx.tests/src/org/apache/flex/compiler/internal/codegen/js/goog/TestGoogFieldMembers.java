/*
 *
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.flex.compiler.internal.codegen.js.goog;

import org.apache.flex.compiler.driver.IBackend;
import org.apache.flex.compiler.internal.codegen.as.TestFieldMembers;
import org.apache.flex.compiler.internal.driver.js.goog.GoogBackend;
import org.apache.flex.compiler.tree.as.IVariableNode;
import org.junit.Test;

/**
 * This class tests the production of 'goog' JavaScript for Class Field members.
 * 
 * @author Michael Schmalle
 * @author Erik de Bruin
 */
public class TestGoogFieldMembers extends TestFieldMembers
{
    //--------------------------------------------------------------------------
    // Fields
    //--------------------------------------------------------------------------

    @Override
    @Test
    public void testField()
    {
        IVariableNode node = getField("var foo;");
        asBlockWalker.visitVariable(node);
        assertOut("/**\n * @type {*}\n */\nA.prototype.foo");
    }

    @Override
    @Test
    public void testField_withType()
    {
        IVariableNode node = getField("var foo:int;");
        asBlockWalker.visitVariable(node);
        assertOut("/**\n * @type {number}\n */\nA.prototype.foo");
    }

    @Override
    @Test
    public void testField_withTypeValue()
    {
        IVariableNode node = getField("var foo:int = 420;");
        asBlockWalker.visitVariable(node);
        assertOut("/**\n * @type {number}\n */\nA.prototype.foo = 420");
    }

    @Test
    public void testField_withTypeValue_Negative()
    {
        IVariableNode node = getField("var foo:int = -420;");
        asBlockWalker.visitVariable(node);
        assertOut("/**\n * @type {number}\n */\nA.prototype.foo = -420");
    }

    @Override
    @Test
    public void testField_withNamespaceTypeValue()
    {
        IVariableNode node = getField("private var foo:int = 420;");
        asBlockWalker.visitVariable(node);
        assertOut("/**\n * @private\n * @type {number}\n */\nA.prototype.foo = 420");
    }

    @Override
    @Test
    public void testField_withCustomNamespaceTypeValue()
    {
        IVariableNode node = getField("mx_internal var foo:int = 420;");
        asBlockWalker.visitVariable(node);
        // (erikdebruin) we ignore custom namespaces completely (are there side effects I'm missing?)
        assertOut("/**\n * @type {number}\n */\nA.prototype.foo = 420");
    }

    @Override
    @Test
    public void testField_withNamespaceTypeCollection()
    {
        IVariableNode node = getField("protected var foo:Vector.<Foo>;");
        asBlockWalker.visitVariable(node);
        assertOut("/**\n * @protected\n * @type {Vector.<Foo>}\n */\nA.prototype.foo");
    }

    @Override
    @Test
    public void testField_withNamespaceTypeCollectionComplex()
    {
        IVariableNode node = getField("protected var foo:Vector.<Vector.<Vector.<Foo>>>;");
        asBlockWalker.visitVariable(node);
        assertOut("/**\n * @protected\n * @type {Vector.<Vector.<Vector.<Foo>>>}\n */\nA.prototype.foo");
    }

    @Override
    @Test
    public void testField_withNamespaceTypeValueComplex()
    {
        IVariableNode node = getField("protected var foo:Foo = new Foo('bar', 42);");
        asBlockWalker.visitVariable(node);
        assertOut("/**\n * @protected\n * @type {Foo}\n */\nA.prototype.foo = new Foo('bar', 42)");
    }

    @Override
    @Test
    public void testField_withList()
    {
        IVariableNode node = getField("protected var a:int = 4, b:int = 11, c:int = 42;");
        asBlockWalker.visitVariable(node);
        assertOut("/**\n * @protected\n * @type {number}\n */\nA.prototype.a = 4;\n\n/**\n * @protected\n * @type {number}\n */\nA.prototype.b = 11;\n\n/**\n * @protected\n * @type {number}\n */\nA.prototype.c = 42");
    }

    //--------------------------------------------------------------------------
    // Constants
    //--------------------------------------------------------------------------

    @Override
    @Test
    public void testConstant()
    {
        IVariableNode node = getField("static const foo;");
        asBlockWalker.visitVariable(node);
        assertOut("/**\n * @const\n * @type {*}\n */\nA.foo");
    }

    @Test
    public void testConstant_nonStatic()
    {
        IVariableNode node = getField("const foo;");
        asBlockWalker.visitVariable(node);
        assertOut("/**\n * @const\n * @type {*}\n */\nA.prototype.foo");
    }

    @Override
    @Test
    public void testConstant_withType()
    {
        IVariableNode node = getField("static const foo:int;");
        asBlockWalker.visitVariable(node);
        assertOut("/**\n * @const\n * @type {number}\n */\nA.foo");
    }

    @Test
    public void testConstant_withType_nonStatic()
    {
        IVariableNode node = getField("const foo:int;");
        asBlockWalker.visitVariable(node);
        assertOut("/**\n * @const\n * @type {number}\n */\nA.prototype.foo");
    }

    @Override
    @Test
    public void testConstant_withTypeValue()
    {
        IVariableNode node = getField("static const foo:int = 420;");
        asBlockWalker.visitVariable(node);
        assertOut("/**\n * @const\n * @type {number}\n */\nA.foo = 420");
    }

    @Test
    public void testConstant_withTypeValue_nonStatic()
    {
        IVariableNode node = getField("const foo:int = 420;");
        asBlockWalker.visitVariable(node);
        assertOut("/**\n * @const\n * @type {number}\n */\nA.prototype.foo = 420");
    }

    @Override
    @Test
    public void testConstant_withNamespaceTypeValue()
    {
        IVariableNode node = getField("private static const foo:int = 420;");
        asBlockWalker.visitVariable(node);
        assertOut("/**\n * @private\n * @const\n * @type {number}\n */\nA.foo = 420");
    }

    @Test
    public void testConstant_withNamespaceTypeValue_nonStatic()
    {
        IVariableNode node = getField("private const foo:int = 420;");
        asBlockWalker.visitVariable(node);
        assertOut("/**\n * @private\n * @const\n * @type {number}\n */\nA.prototype.foo = 420");
    }

    @Override
    @Test
    public void testConstant_withCustomNamespaceTypeValue()
    {
        IVariableNode node = getField("mx_internal static const foo:int = 420;");
        asBlockWalker.visitVariable(node);
        // (erikdebruin) we ignore custom namespaces completely (are there side effects I'm missing?)
        assertOut("/**\n * @const\n * @type {number}\n */\nA.foo = 420");
    }

    @Test
    public void testConstant_withCustomNamespaceTypeValue_nonStatic()
    {
        IVariableNode node = getField("mx_internal const foo:int = 420;");
        asBlockWalker.visitVariable(node);
        // (erikdebruin) we ignore custom namespaces completely (are there side effects I'm missing?)
        assertOut("/**\n * @const\n * @type {number}\n */\nA.prototype.foo = 420");
    }

    @Override
    protected IBackend createBackend()
    {
        return new GoogBackend();
    }
}
