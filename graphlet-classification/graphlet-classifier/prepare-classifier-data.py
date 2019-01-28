"""
This file prepares the NCI109 dataset for the classifier

@author: rpatra
"""

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