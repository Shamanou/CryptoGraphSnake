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
		try:
			reference = {\
				'base_quote' : db.trade.find_one({"base": "XXBT",'quote': x[0]}),\
				'quote_base' : db.trade.find_one({"base":x[0], 'quote': "XXBT"})}.items()
			factor = [ (i,reference[i]) for i in range(len(reference)) if reference[i][1] ][0]
			vol = Fraction(x[1])
			if factor[0] == 0:
				factor[1][1]['bid'] = Fraction(1,vol)
			vol = Fraction(vol, factor[1][1]['bid'])
			balance_conv.append((x[0],float(x[1]),vol))
		except:
			balance_conv.append((x[0],float(x[1]),float(x[1])))
	else:
		balance_conv.append((x[0],float(x[1]),float(x[1])))

balance_conv = sorted(balance_conv, key=operator.itemgetter(2), reverse=True)

print "%s %.4f" %(str(balance_conv[0][0]), float(balance_conv[0][1]))