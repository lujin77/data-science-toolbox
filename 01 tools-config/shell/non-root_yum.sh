
https://dl.iuscommunity.org/pub/ius/stable/CentOS/6/x86_64/

rpm -ivh --relocate /=/home/lujin/lib/ python-devel-2.7.12-7.fc25.x86_64.rpm

export LD_LIBRARY_PATH=/home/lujin/lib

1. yumdownloader XXX
2. rpm2cpio XXX | cpio -idv
3. cp ./usr/bin/XXX ~/bin (put the XXX executable in my $PATH)
4. ~/.bashrc: export LD_LIBRARY_PATH=/home/you/lib

rpm2cpio xsnow-1.42-17.fc17.x86_64.rpm | cpio -idv
