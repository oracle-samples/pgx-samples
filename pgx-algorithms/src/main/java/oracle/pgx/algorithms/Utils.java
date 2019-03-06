/*
 ** Copyright (c) 2019, Oracle and/or its affiliates.  All rights reserved.
 ** Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

package oracle.pgx.algorithms;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.URL;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.function.Function;

public class Utils {
  public static String getResource(String name) {
    URL resource = Utils.class.getClassLoader().getResource(name);

    if (resource == null) {
      throw new IllegalStateException("Resource '" + name + "' not found.");
    }

    return resource.getFile();
  }

  public static Path createOutputFile(Path path) {
    File file = path.toFile();

    try {
      if (!file.exists()) {
        if (!file.createNewFile()) {
          throw new RuntimeException("Unable to create users file.");
        }
      }

      return path;
    } catch (IOException e) {
      throw new RuntimeException("Unable to create users file.", e);
    }
  }

  public static Writer writer(Path output) throws IOException {
    return new BufferedWriter(new FileWriter(output.toFile()));
  }

  public static void writeln(Writer writer, String line) {
    try {
      writer.write(line + "\n");
    } catch (IOException e) {
      throw new RuntimeException("Cannot write line.", e);
    }
  }

  public static <T>  Iterable<T> limit(Iterable<T> iterable, int limit) {
    return () -> new Iterator<T>() {
      protected Iterator<T> iterator = iterable.iterator();
      protected int i = 0;

      @Override
      public boolean hasNext() {
        return i < limit;
      }

      @Override
      public T next() {
        i++;

        return iterator.next();
      }
    };
  }

  public static <T> Function<T[], T> atIndex(int index) {
    return (T[] ts) -> ts[index];
  }
}
