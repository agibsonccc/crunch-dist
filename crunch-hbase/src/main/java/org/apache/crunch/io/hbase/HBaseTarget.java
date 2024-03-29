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
package org.apache.crunch.io.hbase;

import java.io.IOException;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.crunch.CrunchRuntimeException;
import org.apache.crunch.SourceTarget;
import org.apache.crunch.io.CrunchOutputs;
import org.apache.crunch.io.FormatBundle;
import org.apache.crunch.io.MapReduceTarget;
import org.apache.crunch.io.OutputHandler;
import org.apache.crunch.types.PType;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableMapReduceUtil;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class HBaseTarget implements MapReduceTarget {

  private static final Log LOG = LogFactory.getLog(HBaseTarget.class);
  
  protected String table;

  public HBaseTarget(String table) {
    this.table = table;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other)
      return true;
    if (other == null)
      return false;
    if (!other.getClass().equals(getClass()))
      return false;
    HBaseTarget o = (HBaseTarget) other;
    return table.equals(o.table);
  }

  @Override
  public int hashCode() {
    HashCodeBuilder hcb = new HashCodeBuilder();
    return hcb.append(table).toHashCode();
  }

  @Override
  public String toString() {
    return "HBaseTable(" + table + ")";
  }

  @Override
  public boolean accept(OutputHandler handler, PType<?> ptype) {
    if (Put.class.equals(ptype.getTypeClass()) || Delete.class.equals(ptype.getTypeClass())) {
      handler.configure(this, ptype);
      return true;
    }
    return false;
  }

  @Override
  public void configureForMapReduce(Job job, PType<?> ptype, Path outputPath, String name) {
    final Configuration conf = job.getConfiguration();
    HBaseConfiguration.addHbaseResources(conf);
    Class<?> typeClass = ptype.getTypeClass(); // Either Put or Delete
    
    try {
      TableMapReduceUtil.addDependencyJars(job);
      FileOutputFormat.setOutputPath(job, outputPath);
    } catch (IOException e) {
      throw new CrunchRuntimeException(e);
    }

    if (null == name) {
      job.setOutputFormatClass(TableOutputFormat.class);
      job.setOutputKeyClass(ImmutableBytesWritable.class);
      job.setOutputValueClass(typeClass);
      conf.set(TableOutputFormat.OUTPUT_TABLE, table);
    } else {
      FormatBundle<TableOutputFormat> bundle = FormatBundle.forOutput(
          TableOutputFormat.class);
      bundle.set(TableOutputFormat.OUTPUT_TABLE, table);
      CrunchOutputs.addNamedOutput(job, name,
          bundle,
          ImmutableBytesWritable.class,
          typeClass);
    }
  }

  @Override
  public <T> SourceTarget<T> asSourceTarget(PType<T> ptype) {
    return null;
  }

  @Override
  public void handleExisting(WriteMode strategy, Configuration conf) {
    LOG.info("HBaseTarget ignores checks for existing outputs...");
  }
}
