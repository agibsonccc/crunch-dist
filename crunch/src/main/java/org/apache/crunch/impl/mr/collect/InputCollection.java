/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.crunch.impl.mr.collect;

import java.util.List;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.crunch.Source;
import org.apache.crunch.impl.mr.MRPipeline;
import org.apache.crunch.impl.mr.plan.DoNode;
import org.apache.crunch.types.PType;

import com.google.common.collect.ImmutableList;

public class InputCollection<S> extends PCollectionImpl<S> {

  private final Source<S> source;

  public InputCollection(Source<S> source, MRPipeline pipeline) {
    super(source.toString());
    this.source = source;
    this.pipeline = pipeline;
  }

  @Override
  public PType<S> getPType() {
    return source.getType();
  }

  public Source<S> getSource() {
    return source;
  }

  @Override
  protected long getSizeInternal() {
    long sz = source.getSize(pipeline.getConfiguration());
    if (sz < 0) {
      throw new IllegalStateException("Input source " + source + " does not exist!");
    }
    return sz;
  }

  @Override
  protected void acceptInternal(PCollectionImpl.Visitor visitor) {
    visitor.visitInputCollection(this);
  }

  @Override
  public List<PCollectionImpl<?>> getParents() {
    return ImmutableList.of();
  }

  @Override
  public DoNode createDoNode() {
    return DoNode.createInputNode(source);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof InputCollection)) {
      return false;
    }
    return source.equals(((InputCollection) obj).source);
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder().append(source).toHashCode();
  }
}
