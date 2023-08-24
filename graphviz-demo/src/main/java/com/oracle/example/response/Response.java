package com.oracle.example.response;

import java.util.List;

public class Response {
  private List<Result> results;

  public void setResults(List<Result> results) {
    this.results = results;
  }

  public List<Result> getResults() {
    return this.results;
  }
}
