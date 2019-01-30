/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oracle.truffle.js.nodes.access;

import java.util.Set;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.instrumentation.StandardTags.ExpressionTag;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.cast.ToArrayIndexNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTaggedExecutionNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.WriteElementExpressionTag;
import com.oracle.truffle.js.runtime.JSContext;

public class CompoundWriteElementNode extends WriteElementNode {
    @Child private RequireObjectCoercibleNode requireObjectCoercibleNode;
    @Child private ToArrayIndexNode arrayIndexNode;
    @Child private JSWriteFrameSlotNode writeIndexNode;

    public static CompoundWriteElementNode create(JavaScriptNode targetNode, JavaScriptNode indexNode, JavaScriptNode valueNode, JSWriteFrameSlotNode writeIndexNode, JSContext context,
                    boolean isStrict) {
        return create(targetNode, indexNode, valueNode, writeIndexNode, context, isStrict, false);
    }

    private static CompoundWriteElementNode create(JavaScriptNode targetNode, JavaScriptNode indexNode, JavaScriptNode valueNode, JSWriteFrameSlotNode writeIndexNode, JSContext context,
                    boolean isStrict, boolean writeOwn) {
        return new CompoundWriteElementNode(targetNode, indexNode, valueNode, writeIndexNode, context, isStrict, writeOwn);
    }

    protected CompoundWriteElementNode(JavaScriptNode targetNode, JavaScriptNode indexNode, JavaScriptNode valueNode, JSWriteFrameSlotNode writeIndexNode, JSContext context, boolean isStrict,
                    boolean writeOwn) {
        super(targetNode, indexNode, valueNode, context, isStrict, writeOwn);
        this.requireObjectCoercibleNode = RequireObjectCoercibleNode.create();
        this.writeIndexNode = writeIndexNode;
    }

    @Override
    protected Object executeWithTargetAndIndex(VirtualFrame frame, Object target, Object index) {
        return super.executeWithTargetAndIndex(frame, requireObjectCoercibleNode.execute(target), writeIndex(frame, toArrayIndexNode().execute(index)));
    }

    @Override
    protected Object executeWithTargetAndIndex(VirtualFrame frame, Object target, int index) {
        return super.executeWithTargetAndIndex(frame, requireObjectCoercibleNode.execute(target), writeIndex(frame, index));
    }

    @Override
    protected int executeWithTargetAndIndexInt(VirtualFrame frame, Object target, Object index) throws UnexpectedResultException {
        return super.executeWithTargetAndIndexInt(frame, requireObjectCoercibleNode.execute(target), writeIndex(frame, toArrayIndexNode().execute(index)));
    }

    @Override
    protected int executeWithTargetAndIndexInt(VirtualFrame frame, Object target, int index) throws UnexpectedResultException {
        return super.executeWithTargetAndIndexInt(frame, requireObjectCoercibleNode.execute(target), writeIndex(frame, index));
    }

    @Override
    protected double executeWithTargetAndIndexDouble(VirtualFrame frame, Object target, Object index) throws UnexpectedResultException {
        return super.executeWithTargetAndIndexDouble(frame, requireObjectCoercibleNode.execute(target), writeIndex(frame, toArrayIndexNode().execute(index)));
    }

    @Override
    protected double executeWithTargetAndIndexDouble(VirtualFrame frame, Object target, int index) throws UnexpectedResultException {
        return super.executeWithTargetAndIndexDouble(frame, requireObjectCoercibleNode.execute(target), writeIndex(frame, index));
    }

    private Object writeIndex(VirtualFrame frame, Object index) {
        if (writeIndexNode != null) {
            writeIndexNode.executeWrite(frame, index);
        }
        return index;
    }

    private int writeIndex(VirtualFrame frame, int index) {
        if (writeIndexNode != null) {
            writeIndexNode.executeWrite(frame, index);
        }
        return index;
    }

    private ToArrayIndexNode toArrayIndexNode() {
        if (arrayIndexNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            arrayIndexNode = ToArrayIndexNode.create();
        }
        return arrayIndexNode;
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return create(cloneUninitialized(targetNode), cloneUninitialized(indexNode), cloneUninitialized(valueNode), cloneUninitialized(writeIndexNode), getContext(), isStrict(), writeOwn());
    }

    @Override
    public InstrumentableNode materializeInstrumentableNodes(Set<Class<? extends Tag>> materializedTags) {
        if (materializationNeeded() && materializedTags.contains(WriteElementExpressionTag.class)) {
            JavaScriptNode clonedTarget = targetNode == null || targetNode.hasSourceSection() ? cloneUninitialized(targetNode) : JSTaggedExecutionNode.createFor(targetNode, this, ExpressionTag.class);
            JavaScriptNode clonedIndex = indexNode == null || indexNode.hasSourceSection() ? cloneUninitialized(indexNode) : JSTaggedExecutionNode.createFor(indexNode, this, ExpressionTag.class);
            JavaScriptNode clonedValue = valueNode == null || valueNode.hasSourceSection() ? cloneUninitialized(valueNode) : JSTaggedExecutionNode.createFor(valueNode, this, ExpressionTag.class);
            JavaScriptNode cloned = CompoundWriteElementNode.create(clonedTarget, clonedIndex, clonedValue, cloneUninitialized(writeIndexNode), getContext(), isStrict(), writeOwn());
            transferSourceSectionAndTags(this, cloned);
            return cloned;
        }
        return this;
    }
}
