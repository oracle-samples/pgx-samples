#
# Copyright (c) 2019, Oracle and/or its affiliates.  All rights reserved.
# This software is licensed to you under the Universal Permissive License (UPL).
# See below for license terms.
# ____________________________
# The Universal Permissive License (UPL), Version 1.0

# Subject to the condition set forth below, permission is hereby granted to any person
# obtaining a copy of this software, associated documentation and/or data (collectively the "Software"),
# free of charge and under any and all copyright rights in the Software, and any and all patent rights
# owned or freely licensable by each licensor hereunder covering either (i) the unmodified Software as
# contributed to or provided by such licensor, or (ii) the Larger Works (as defined below), to deal in both

# (a) the Software, and
# (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if one is included with the
# Software (each a "Larger Work" to which the Software is contributed by such licensors),

# without restriction, including without limitation the rights to copy, create derivative works of,
# display, perform, and distribute the Software and make, use, sell, offer for sale, import, export,
# have made, and have sold the Software and the Larger Work(s), and to sublicense the foregoing rights
# on either these or other terms.

# This license is subject to the following condition:

# The above copyright notice and either this complete permission notice or at a minimum a reference
# to the UPL must be included in all copies or substantial portions of the Software.

# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
# NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
# IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
# WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
# SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
#

import pandas as pd
import os
from os.path import dirname
import numpy as np

if __name__ == "__main__":
    # get data paths
    data_dir = os.path.join(dirname(os.getcwd()), "data/")
    input_dir = os.path.join(data_dir, "input/NCI109/")

    # extract the graphlet vectors
    vectors = pd.read_csv(data_dir+"graphlet_vectors.csv", index_col= 0)
    vectors_sorted = vectors.sort_index()

    # obtain the graphlet labels
    labels = pd.read_csv(input_dir+"NCI109_graph_labels.txt", header=None)

    # create train and test datasets
    data = pd.merge(labels, vectors_sorted, left_index=True, right_index=True)

    msk = np.random.rand(len(data)) < 0.8
    train = data[msk]
    test = data[~msk]

    train.to_csv(data_dir+"NCI09_train.csv",index=False, header=False)
    test.to_csv(data_dir+"NCI09_test.csv",index=False, header=False)