from pandas.tools.plotting import radviz

data = pd.read_csv('data/iris.data')
plt.figure()
radviz(data, 'Name')
