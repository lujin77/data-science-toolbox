import subprocess
import time

def sh(cmd, verbose=False):
    if verbose:
        startTime = time.time()
        print cmd

    proc = subprocess.Popen(cmd, shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    while proc.poll() is None:
        print proc.stdout.readline()
        time.sleep(1)

    if verbose:
        print 'returen code:', proc.returncode
        print "elapsed:", (time.time() - start_time) / 60.0, "mins"

    return proc.returncode
