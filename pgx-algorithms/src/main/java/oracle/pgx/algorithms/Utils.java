/*
 ** Copyright (c) 2019, Oracle and/or its affiliates.  All rights reserved.
 ** This software is licensed to you under the Universal Permissive License (UPL).
 ** See below for license terms.
 **  ____________________________
 ** The Universal Permissive License (UPL), Version 1.0

 ** Subject to the condition set forth below, permission is hereby granted to any person
 ** obtaining a copy of this software, associated documentation and/or data (collectively the "Software"),
 ** free of charge and under any and all copyright rights in the Software, and any and all patent rights
 ** owned or freely licensable by each licensor hereunder covering either (i) the unmodified Software as
 ** contributed to or provided by such licensor, or (ii) the Larger Works (as defined below), to deal in both

 ** (a) the Software, and
 ** (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if one is included with the
 ** Software (each a "Larger Work" to which the Software is contributed by such licensors),

 ** without restriction, including without limitation the rights to copy, create derivative works of,
 ** display, perform, and distribute the Software and make, use, sell, offer for sale, import, export,
 ** have made, and have sold the Software and the Larger Work(s), and to sublicense the foregoing rights
 ** on either these or other terms.

 ** This license is subject to the following condition:

 ** The above copyright notice and either this complete permission notice or at a minimum a reference
 ** to the UPL must be included in all copies or substantial portions of the Software.

 ** THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 ** NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 ** IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 ** WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 ** SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
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
