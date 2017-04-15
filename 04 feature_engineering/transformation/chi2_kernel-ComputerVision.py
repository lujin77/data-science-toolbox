from sklearn.svm import SVC
from sklearn.metrics.pairwise import chi2_kernel

'''
The chi-squared kernel is a very popular choice for
training non-linear SVMs in computer vision applications.
'''

X = [[0, 1], [1, 0], [.2, .8], [.7, .3]]
y = [0, 1, 0, 1]
K = chi2_kernel(X, gamma=.5)
# Usage 1. passed to an sklearn.svm.SVC with kernel="precomputed":
svm = SVC(kernel='precomputed').fit(K, y)
svm.predict(K)

# Usage 2. directly used as the kernel argument:
svm = SVC(kernel=chi2_kernel).fit(X, y)
svm.predict(X)
