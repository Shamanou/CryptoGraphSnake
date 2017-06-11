#! /usr/bin/python

import krakenex
from pymongo import MongoClient
import operator
from fractions import Fraction

client = MongoClient()
db = client.trade

k = krakenex.API()
k.load_key('apikey')

def getStart(pos):
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
				    factor[1][1]['bid'] = Fraction(1,factor[1][1]['bid'])
                                else:
                                    factor[1][1]['bid'] = Fraction(factor[1][1]['bid'])
				vol = Fraction(vol, factor[1][1]['bid'])
				#if float(vol) > 0.01:
				balance_conv.append((x[0],float(x[1]),float(vol)))
			except:
				#if float(x[1]) > 0.01:
			    balance_conv.append((x[0],float(x[1]),float(x[1])))
		else:
			#if float(x[1]) > 0.1:
		    balance_conv.append((x[0],float(x[1]),float(x[1])))

	balance_conv = sorted(balance_conv, key=operator.itemgetter(2), reverse=True)

	if pos < len(balance_conv):
		return (str(balance_conv[pos][0]), float(balance_conv[pos][1]))
	else:
		return (str(balance_conv[0][0]), float(balance_conv[0][1]))
