# 监控工具（版本过高不行）
pip install glance==2.7.1

# 查看文件有多少列
head XXX.tsv | head -n1 | awk -F '\t' '{print NF}'

# 查物理CPU
cat /proc/cpuinfo | grep "physical id" | uniq | wc -l

# 查CPU核数
cat /proc/cpuinfo | grep "cpu cores" | uniq

# 查逻辑核数（超线程）
cat /proc/cpuinfo |grep "processor"|wc -l

# 查内存
cat /proc/meminfo | grep MemTotal

# 查OS
cat /etc/redhat-release

# 查看端口占用
netstat   -anp   |   grep  portno

# 批量删除进程
ps -ef | grep xxx | grep -v root | awk '{print $2}' | xargs kill -9

# CPU占用最多的前10个进程：
ps auxw|head -1;ps auxw|sort -rn -k3|head -10

# 内存消耗最多的前10个进程
ps auxw|head -1;ps auxw|sort -rn -k4|head -10

# 虚拟内存使用最多的前10个进程 
ps auxw|head -1;ps auxw|sort -rn -k5|head -10
