# -*- coding: utf-8 -*-

"""docopt testing

Usage:
  docopt_template.py  --city=<CITY> [--start_date=<START_DATE>] [--end_date=<END_DATE>] [--verbose]
  docopt_template.py (-h | --help)

Examples:
  docopt_template.py --city=bj
  docopt_template.py 1 + 2 '*' 3 / 4 - 5    # note quotes around '*'
  calculator_example.py sum 10 , 20 , 30 , 40

Arguments:
  CITY        input city short code
  START_DATE    job start date
  END_DATE       job end date

Options:
  -h --help     show help information
  --version     show version
  --city=<CITY>       processed city code
  --start_date=<START_DATE>      job start date [default: yesterday]
  --end_date=<END_DATE>       job end date [default: seven_days_ago]
  --quiet          print less text
  --verbose    print more text
"""
from docopt import docopt
import datetime

if __name__ == '__main__':
    arguments = docopt(__doc__, version='Naval Fate 2.0')
    for k, v in arguments.iteritems():
        print k, "->", v

    print "all is done"
