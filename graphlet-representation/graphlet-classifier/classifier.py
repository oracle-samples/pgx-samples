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

from __future__ import print_function

import tensorflow as tf
from numpy import genfromtxt
import numpy as np
import os
from os.path import dirname

# set logging parameters
os.environ['TF_CPP_MIN_LOG_LEVEL'] = '2'
tf.logging.set_verbosity(tf.logging.ERROR)

def oneHotEncoder(data, n_classes):
    y = np.array([int(i[0]) for i in data])
    y_onehot = [0]*len(y)
    for i,j in enumerate(y):
        y_onehot[i] = [0]*n_classes
        y_onehot[i][j]= 1
    return (y,y_onehot)

# model parameters
n_input = 200
n_classes = 2

# tunable hyper-parameters
shape1 = 10
shape2 = 20
init_learning_rate = 0.004
training_iters = 40000
batch_size = 256
dropout = 0.5 # probability to keep units (for dropout)
seed = 0

# display test metrics
test_step = 10

# set random seed for reproducability
tf.set_random_seed(seed)


#NCI109 dataset
data_dir = os.path.join(dirname(os.getcwd()), "data/")
train_data = genfromtxt(data_dir+"NCI09_train.csv", delimiter=',')  # Training data
test_data = genfromtxt(data_dir+"NCI09_test.csv", delimiter=',')  # Training data


x_train = np.array([ i[1::] for i in train_data])
y_train, y_train_onehot = oneHotEncoder(train_data, n_classes)

x_test = np.array([ i[1::] for i in test_data])
y_test, y_test_onehot = oneHotEncoder(test_data, n_classes)

# tf Graph input
x = tf.placeholder(tf.float32, [None, n_input])
y = tf.placeholder(tf.float32, [None, n_classes])
keep_prob = tf.placeholder(tf.float32)

# Conv2D wrapper, with bias and relu activation
def conv2d(x, W, b, strides=1):
    x = tf.nn.conv2d(x, W, strides=[1, strides, strides, 1], padding='SAME')
    x = tf.nn.bias_add(x, b)
    return tf.nn.relu(x)


# MaxPool2D wrapper
def pool2d(x, k=2):
    return tf.nn.max_pool(x, ksize=[1, k, k, 1], strides=[1, k, k, 1], padding='SAME')


# convnet model
def conv_net(x, keep_prob):
    # Store layers weight & bias
    weights = {
        'conv_layer1': tf.Variable(tf.random_normal([5, 5, 1, 32], seed=seed)),
        'conv_layer2': tf.Variable(tf.random_normal([5, 5, 32, 64], seed=seed)),
        'dense_layer': tf.Variable(tf.random_normal([shape1*shape2*64, 1024], seed=seed)),
        'output_layer': tf.Variable(tf.random_normal([1024, n_classes], seed=seed))
    }

    biases = {
        'conv_layer1': tf.Variable(tf.random_normal([32], seed=seed)),
        'conv_layer2': tf.Variable(tf.random_normal([64], seed=seed)),
        'dense_layer': tf.Variable(tf.random_normal([1024], seed=seed)),
        'output_layer': tf.Variable(tf.random_normal([n_classes], seed=seed))
    }

    # Reshape input picture
    x = tf.reshape(x, shape=[-1, shape1, shape2, 1])

    # Convolution Layer
    conv1 = conv2d(x, weights['conv_layer1'], biases['conv_layer1'])
    # Max Pooling (down-sampling)
    conv1 = pool2d(conv1, k=1)

    # Convolution Layer
    conv2 = conv2d(conv1, weights['conv_layer2'], biases['conv_layer2'])
    # Max Pooling (down-sampling)
    conv2 = pool2d(conv2, k=1)

    fc1 = tf.reshape(conv2, [-1, weights['dense_layer'].get_shape().as_list()[0]])
    fc1 = tf.add(tf.matmul(fc1, weights['dense_layer']), biases['dense_layer'])
    fc1 = tf.nn.relu(fc1)
    # Apply Dropout
    fc1 = tf.nn.dropout(fc1, keep_prob, seed=seed)

    # Output, class prediction
    out = tf.add(tf.matmul(fc1, weights['output_layer']), biases['output_layer'])
    return out


global_step = tf.Variable(0, trainable=False)
learning_rate = tf.train.exponential_decay(init_learning_rate, global_step, 10000, 0.96, staircase=True)

# construct model with input data
pred = conv_net(x, keep_prob)

# define loss and optimizer
cost = tf.reduce_mean(tf.nn.softmax_cross_entropy_with_logits(logits=pred, labels=y))
optimizer = tf.train.AdamOptimizer(learning_rate=learning_rate).minimize(cost,global_step=global_step)

# evaluate model
correct_pred = tf.equal(tf.argmax(pred, 1), tf.argmax(y, 1))
accuracy = tf.reduce_mean(tf.cast(correct_pred, tf.float32))

# initializing the variables
init = tf.global_variables_initializer()

# Launch the graph
with tf.Session() as sess:
    sess.run(init)
    step = 1
    # keep training until reach max iterations
    while step * batch_size < training_iters:
        np.random.seed(step+seed)
        idx = np.random.randint(len(x_train), size=batch_size)
        batch_x = x_train[idx,:]
        batch_y = np.asarray(y_train_onehot)[idx,:]
        # run optimization op (backprop)
        sess.run(optimizer, feed_dict={x: batch_x, y: batch_y, keep_prob: dropout})
        if step % test_step == 0:
            # calculate accuracy for test data
            loss, acc = sess.run([cost, accuracy], feed_dict={x: x_test, y: np.asarray(y_test_onehot), keep_prob: 1.})
            print("Iterations: %s, Test Accuracy: %f" % (str(step*batch_size),acc))
        step += 1

print("Complete!")