非root下安装mysql
=====

1. 安装mysql依赖的异步io库：libaio.so
-----

> Optional，如果已经安装过可跳过

1. wget http://libaio.sourcearchive.com/downloads/0.3.104/libaio_0.3.104.orig.tar.gz

2. 解压之后根据需要修改Makefile里面的安装路径(prefix)， 然后运行make && make install 即可。
3. 如果不是用root用户安装，需要将liaio.so的路径加入到动态链接路径， 运行 vim ~/.bash_profile, 在最后添加一行：
export LD_LIBRARY_PATH=/path/to/liaio/lib:$LD_LIBRARY_PATH
- - -

2.设置my.cnf
-----
> 使用 --defaults-file=my.cnf 来指定配置文件运行 （支持多环境共存）

```bash
[client]
port = 3306
socket = /home/lujin/tmp/mysql.sock

[mysqld]
datadir = /home/lujin/data/mysql/data
socket = /home/lujin/tmp/mysql.sock
port = 3306
skip-external-locking
key_buffer_size = 384M
max_allowed_packet = 1024M
table_open_cache = 512
sort_buffer_size = 2M
read_buffer_size = 2M
read_rnd_buffer_size = 8M
myisam_sort_buffer_size = 64M
thread_cache_size = 8
query_cache_size = 32M
# Try number of CPU’s*2 for thread_concurrency
thread_concurrency = 8
performance_schema_max_table_instances=200
table_definition_cache=200
table_open_cache=128

[mysqld_safe]
log-error = /home/lujin/data/mysql/logs/mysqld_safe.log
pid-file = /home/lujin/data/mysql/data/mysqld.pid
```
- - -

3. 执行安装命令，使配置文件生效
-----
```bash
./scripts/mysql_install_db --defaults-file=my.cnf
```
- - -

4. 启动mysql服务
-----
> 执行了步骤3之后，supervisord里面配置这一句即可
```bash
./bin/mysqld_safe &
```
- - -

5.修改root用户密码
-----
```bash
bin/mysqladmin -h '127.0.0.1' -u root password PASSWORD
```
- - -

6.进入mysql数据库，设置密码
-----
```bash
bin/mysql -h '127.0.0.1' -u root -p
```

* 方法1：
```bash
GRANT ALL PRIVILEGES ON *.* TO 'root'@'%' IDENTIFIED BY 'PASSWORD' WITH GRANT OPTION;
```

* 方法2：
```bash
use mysql;
update user set password=PASSWORD(PASSWORD) where User='root';
update user set host = '%' where user = 'root';
```
- - -

7. alias
-----
```bash
alias mysql="/home/lujin/software/mariadb/bin/mysql --defaults-file=/home/lujin/software/mariadb/my.cnf"
```
- - -

8. 授权
-----
```bash
CREATE USER 'name'@'localhost' IDENTIFIED BY 'password';
grant all privileges on *.* to 'root'@'%' identified by 'PASSWORD' with grant option;
GRANT SELECT,INSERT,UPDATE,DELETE ON azkaban.* to 'azkaban'@'%' identified by 'PASSWORD' WITH GRANT OPTION;
FLUSH PRIVILEGES;
```
- - -
