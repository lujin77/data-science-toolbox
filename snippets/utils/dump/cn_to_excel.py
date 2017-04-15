import pandas as pd
import numpy as np
import string
import xlsxwriter
import chardet

'''
pandas导出在中文excel
'''


def toCn(str):
    if type(str).__name__ != "unicode":
        str = unicode(str, "utf-8")
    return str

# f = open('test.txt','r')
# result = chardet.detect(f.read())

df1 = pd.DataFrame(np.random.randn(2, 3), columns=list('abc'))
df2 = pd.DataFrame(np.random.randn(5, 2), columns=[
                   "id", unicode("测试", "utf-8")])

df3 = pd.DataFrame(
    {"test": [unicode('中文测试', "utf-8"), unicode('中文测试2', "utf-8")]})

with pd.ExcelWriter('test.xlsx', options={'encoding': 'utf-8', 'engine': 'xlsxwriter'}) as writer:
    df1.to_excel(writer, sheet_name=unicode('测试1', "utf-8"), index=False)
    df2.to_excel(writer, sheet_name=unicode('中文标题', "utf-8"), index=False)
    df3.to_excel(writer, sheet_name=unicode('中文内容', "utf-8"), index=False)

print "all is done"
