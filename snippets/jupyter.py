import subprocess
import time

cmd = "ls -a"

proc = subprocess.Popen(cmd, shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
while proc.poll() is None:
    print proc.stdout.readline()
    time.sleep(1)

print 'returen code:', proc.returncode
