免密登录
1. A机器上生成密钥：
ssh-keygen -t rsa
scp ~/.ssh/id_rsa.pub lujin@XX.XX.XX.XX:~/id_rsa.pub
2. B机器上储存密钥
mkdir .ssh && chmod 0700 .ssh && cat  ~/id_rsa.pub  >> /home/lujin/.ssh/authorized_keys

无授权scp
sshpass -p Password  scp tmp/ods_order_comment_20161004/a.txt user@XX.XX.XX.XX:/home/user/
