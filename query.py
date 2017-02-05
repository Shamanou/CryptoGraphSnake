#! /usr/bin/python

import krakenex
from pymongo import MongoClient
import operator

client = MongoClient()
db = client.trade

k = krakenex.API()
k.load_key('apikey')

balance = k.query_private('Balance')['result']
balance = balance.items()
balance_conv = []

for x in balance:
	try:
		reference = {\
			'base_quote' : db.trade.find_one({"base": "ZUSD",'quote':str(x[0])}),\
			'quote_base' : db.trade.find_one({"base":str(x[0]), 'quote': "ZUSD"}),\
			'base_base' : db.trade.find_one({"base":  "ZUSD",'quote':str(x[0])}),\
			'quote_quote': db.trade.find_one({"base":str(x[0]), 'quote': "ZUSD"})}.items()
		factor = [ (i,reference[i]) for i in range(len(reference)) if reference[i][1] ][0]
	except:
		continue
	if (factor[0] % 2) == 0:
		factor = factor[1][1]['bid']
	else:
		factor = 1/factor[1][1]['ask']

	balance_conv.append([float(x[1]), x[0], float(x[1])*factor])

balance_conv = sorted(balance_conv, key=operator.itemgetter(2), reverse=True)

print "%s %.4f" %(str(balance_conv[0][1]), float(balance_conv[0][0]))