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
package org.apache.crunch.io.impl;

import java.io.IOException;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.crunch.CrunchRuntimeException;
import org.apache.crunch.SourceTarget;
import org.apache.crunch.io.CrunchOutputs;
import org.apache.crunch.io.FileNamingScheme;
import org.apache.crunch.io.OutputHandler;
import org.apache.crunch.io.PathTarget;
import org.apache.crunch.types.Converter;
import org.apache.crunch.types.PType;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class FileTargetImpl implements PathTarget {

  private static final Log LOG = LogFactory.getLog(FileTargetImpl.class);
  
  protected final Path path;
  private final Class<? extends FileOutputFormat> outputFormatClass;
  private final FileNamingScheme fileNamingScheme;

  public FileTargetImpl(Path path, Class<? extends FileOutputFormat> outputFormatClass,
      FileNamingScheme fileNamingScheme) {
    this.path = path;
    this.outputFormatClass = outputFormatClass;
    this.fileNamingScheme = fileNamingScheme;
  }

  @Override
  public void configureForMapReduce(Job job, PType<?> ptype, Path outputPath, String name) {
    Converter converter = ptype.getConverter();
    Class keyClass = converter.getKeyClass();
    Class valueClass = converter.getValueClass();
    configureForMapReduce(job, keyClass, valueClass, outputFormatClass, outputPath, name);
  }

  protected void configureForMapReduce(Job job, Class keyClass, Class valueClass,
      Class outputFormatClass, Path outputPath, String name) {
    try {
      FileOutputFormat.setOutputPath(job, outputPath);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    if (name == null) {
      job.setOutputFormatClass(outputFormatClass);
      job.setOutputKeyClass(keyClass);
      job.setOutputValueClass(valueClass);
    } else {
      CrunchOutputs.addNamedOutput(job, name, outputFormatClass, keyClass, valueClass);
    }
  }

  @Override
  public boolean accept(OutputHandler handler, PType<?> ptype) {
    handler.configure(this, ptype);
    return true;
  }

  @Override
  public Path getPath() {
    return path;
  }

  @Override
  public FileNamingScheme getFileNamingScheme() {
    return fileNamingScheme;
  }

  @Override
  public boolean equals(Object other) {
    if (other == null || !getClass().equals(other.getClass())) {
      return false;
    }
    FileTargetImpl o = (FileTargetImpl) other;
    return path.equals(o.path);
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder().append(path).toHashCode();
  }

  @Override
  public String toString() {
    return new StringBuilder().append(outputFormatClass.getSimpleName()).append("(").append(path).append(")")
        .toString();
  }

  @Override
  public <T> SourceTarget<T> asSourceTarget(PType<T> ptype) {
    // By default, assume that we cannot do this.
    return null;
  }

  @Override
  public void handleExisting(WriteMode strategy, Configuration conf) {
    FileSystem fs = null;
    try {
      fs = FileSystem.get(conf);
    } catch (IOException e) {
      LOG.error("Could not retrieve FileSystem object to check for existing path", e);
      throw new CrunchRuntimeException(e);
    }
    
    boolean exists = false;
    try {
      exists = fs.exists(path);
    } catch (IOException e) {
      LOG.error("Exception checking existence of path: " + path, e);
      throw new CrunchRuntimeException(e);
    }
    
    if (exists) {
      switch (strategy) {
      case DEFAULT:
        LOG.error("Path " + path + " already exists!");
        throw new CrunchRuntimeException("Path already exists: " + path);
      case OVERWRITE:
        LOG.info("Removing data at existing path: " + path);
        try {
          fs.delete(path, true);
        } catch (IOException e) {
          LOG.error("Exception thrown removing data at path: " + path, e);
        }
        break;
      case APPEND:
        LOG.info("Adding output files to existing path: " + path);
        break;
      default:
        throw new CrunchRuntimeException("Unknown WriteMode:  " + strategy);
      }
    } else {
      LOG.info("Will write output files to new path: " + path);
    }
  }
}
