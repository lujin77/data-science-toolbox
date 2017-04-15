# -*- coding: utf-8 -*-

def chunks(lst, n):
    for i in range(0, len(lst), n):
        yield lst[i:i + n]


def sublists(lst, n):
    return [lst[i:i + n] for i in range(0, len(lst), n)]
