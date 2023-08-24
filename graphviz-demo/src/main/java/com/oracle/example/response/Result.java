package com.oracle.example.response;

public class Result {
  private String pgqlStatement;
  private String result;
  private boolean success;
  private Object error;
  private String started;
  private String ended;

  public void setPgqlStatement(String pgqlStatement) {
    this.pgqlStatement = pgqlStatement;
  }

  public void setResult(String result) {
    this.result = result;
  }

  public void setSuccess(boolean success) {
    this.success = success;
  }

  public void setError(Object error) {
    this.error = error;
  }

  public void setStarted(String started) {
    this.started = started;
  }

  public void setEnded(String ended) {
    this.ended = ended;
  }

  public String getPgqlStatement() {
    return this.pgqlStatement;
  }

  public String getResult() {
    return this.result;
  }

  public boolean getSuccess() {
    return this.success;
  }

  public Object getError() {
    return this.error;
  }

  public String getStarted() {
    return this.started;
  }

  public String getEnded() {
    return this.ended;
  }
}
