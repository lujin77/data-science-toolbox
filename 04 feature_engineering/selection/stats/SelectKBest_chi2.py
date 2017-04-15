from sklearn.datasets import load_iris
from sklearn.feature_selection import SelectKBest
from sklearn.feature_selection import chi2

'''
For regression: f_regression, mutual_info_regression
For classification: chi2, f_classif, mutual_info_classif

(support sparse matrix: chi2, mutual_info_regression, mutual_info_classif)
'''

iris = load_iris()
X, y = iris.data, iris.target
print X.shape

X_new = SelectKBest(chi2, k=2).fit_transform(X, y)
print X_new.shape
