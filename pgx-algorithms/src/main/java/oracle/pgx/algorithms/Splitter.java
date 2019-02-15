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
