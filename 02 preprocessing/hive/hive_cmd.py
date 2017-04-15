import commands

'''
using shell to get hive data
'''

cmd = "hive -S -e 'SELECT * FROM yc_mds_letv.hz_bidding LIMIT 1;' "
status, output = commands.getstatusoutput(cmd)
if status == 0:
    print output
else:
    print "error"
