

import logging
from logging.handlers import SMTPHandler
from logging import Formatter


ADMINS = ['yourname@example.com']

mail_handler = SMTPHandler('127.0.0.1',
                           'server-error@example.com',
                           ADMINS, 'YourApplication Failed')
mail_handler.setLevel(logging.ERROR)
mail_handler.setFormatter(Formatter('''
Message type:       %(levelname)s
Location:           %(pathname)s:%(lineno)d
Module:             %(module)s
Function:           %(funcName)s
Time:               %(asctime)s

Message:

%(message)s
'''))

app.logger.addHandler(mail_handler)
