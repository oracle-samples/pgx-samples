/*
 ** Copyright (c) 2019, Oracle and/or its affiliates.  All rights reserved.
 ** Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

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
