/*
 ** Copyright (c) 2019, Oracle and/or its affiliates.  All rights reserved.
 ** Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

package oracle.pgx.algorithms;

import java.util.function.Function;

public enum Splitter implements Function<String, String[]> {
  tab {
    @Override
    public String[] apply(String s) {
      return s.split("\t");
    }
  },

  comma {
    @Override
    public String[] apply(String s) {
      return s.split(",");
    }
  }
}
