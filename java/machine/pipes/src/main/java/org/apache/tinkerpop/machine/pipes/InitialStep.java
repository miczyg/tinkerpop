/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.tinkerpop.machine.pipes;

import org.apache.tinkerpop.machine.functions.InitialFunction;
import org.apache.tinkerpop.machine.traversers.Traverser;

import java.util.Iterator;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class InitialStep<C, S> extends AbstractStep<C, S, S> {

    private final Iterator<Traverser<C, S>> objects;

    public InitialStep(final InitialFunction<C, S> initialFunction) {
        super(EmptyStep.instance(), initialFunction);
        this.objects = initialFunction.get();
    }

    @Override
    public boolean hasNext() {
        return this.objects.hasNext();
    }

    @Override
    public Traverser<C, S> next() {
        return this.objects.next();
    }

}