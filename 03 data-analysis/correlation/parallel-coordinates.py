from pandas.tools.plotting import parallel_coordinates
data = pd.read_csv('data/iris.data')
plt.figure()
parallel_coordinates(data, 'Name')
