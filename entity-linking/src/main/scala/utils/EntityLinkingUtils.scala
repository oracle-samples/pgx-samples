package utils

object EntityLinkingUtils {

  /**
    * Obtain the current memory usage of the application.
    * @return the current memory used, in MB
    */
  def memoryConsumedMB = {
    System.gc()
    (Runtime.getRuntime.totalMemory - Runtime.getRuntime.freeMemory) / (1024.toDouble * 1024)
  }
}
