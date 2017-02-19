#! /usr/bin/python

import krakenex
from pymongo import MongoClient
import operator
from fractions import Fraction


client = MongoClient()
db = client.trade

k = krakenex.API()
k.load_key('apikey')

balance = k.query_private('Balance')['result']
balance = balance.items()
if type(balance) != type([]):
	balance = [balance]

balance_conv = []

for x in balance:
	if x[0] != "XXBT":
		reference = {\
			'base_quote' : db.trade.find_one({"base": "XXBT",'quote':x[0]}),\
			'quote_base' : db.trade.find_one({"base":x[0], 'quote': "XXBT"})}.items()
		try:
			factor = [ (i,reference[i]) for i in range(len(reference)) if reference[i][1] ][0]
		except Exception as e:
			pass
	else:
		factor = ['0',['0',{'0':'','bid':float(x[1]),'bid':Fraction(x[1])}]]

	if float(x[1]) > 0.002:
		balance_conv.append([float(x[1]), x[0], Fraction(factor[1][1]['bid'])])

balance_conv = sorted(balance_conv, key=operator.itemgetter(2,0), reverse=True)

print "%s %.4f %s" %(str(balance_conv[0][1]), float(balance_conv[0][0]), float(balance_conv[0][2]))