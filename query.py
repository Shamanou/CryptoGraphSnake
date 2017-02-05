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
	reference = {\
		'base_quote' : db.trade.find_one({"base": "XXBT",'quote':x[0]}),\
		'quote_base' : db.trade.find_one({"base":x[0], 'quote': "XXBT"})}.items()
	try:
		factor = [ (i,reference[i]) for i in range(len(reference)) if reference[i][1] ][0]
	except Exception as e:
		continue

	if float(x[1]) > 0.002:
		if factor[0] == 0:
			value = factor[1][1]['bid'] / float(x[1])
		else:
			value = float(x[1]) / factor[1][1]['ask']

		balance_conv.append([float(x[1]), x[0], value])

balance_conv = sorted(balance_conv, key=operator.itemgetter(2), reverse=True)

print "%s %.4f" %(str(balance_conv[0][1]), float(balance_conv[0][0]))