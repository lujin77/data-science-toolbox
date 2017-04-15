from sklearn.feature_selection import VarianceThreshold

'''
suppose that we have a dataset with boolean features,
and we want to remove all features that are either one or zero (on or off) in more than 80% of the samples.
Boolean features are Bernoulli random variables, and the variance of such variables is given by

Var[X] = p(1 - p)
'''

X = [[0, 0, 1], [0, 1, 0], [1, 0, 0], [0, 1, 1], [0, 1, 0], [0, 1, 1]]
sel = VarianceThreshold(threshold=(.8 * (1 - .8)))
print sel.fit_transform(X)
