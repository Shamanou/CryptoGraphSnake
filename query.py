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
	while True:
		try:
			balance = k.query_private('Balance')['result']
			print balance
			break
		except:
			pass
	balance = balance.items()
	if type(balance) != type([]):
		balance = [balance]

	REFERENCE = u"XXBT"
	balance_conv = []
	for x in balance:
		if x[0] != REFERENCE:
			try:
				reference = {\
					'base_quote' : db.trade.find_one({"base": REFERENCE,'quote': x[0]}),\
					'quote_base' : db.trade.find_one({"base":x[0], 'quote': REFERENCE})}.items()
				factor = [ (i,reference[i]) for i in range(len(reference)) if reference[i][1] ][0]
				vol = Fraction(x[1])
				if factor[0] == 0:
					vol = Faction(vol ,Fraction(1,factor[1][1]['bid']) )
				else:
					vol = Fraction(vol,Fraction(factor[1][1]['bid']))
					if vol >= 0.1:
						balance_conv.append((x[0],float(x[1]),float(vol), float(factor[1][1]['bid'])))
			except:
				 	balance_conv.append((x[0],float(x[1]),float(x[1]), float(factor[1][1]['bid'])))
		else:
			balance_conv.append((x[0],float(x[1]),float(x[1]), float(factor[1][1]['bid'])))

	balance_conv = sorted(balance_conv, key=operator.itemgetter(2,1,3), reverse=True)
	# print balance_conv

	if pos < len(balance_conv):
		return (str(balance_conv[pos][0]), float(balance_conv[pos][1]))
	else:
		return (str(balance_conv[0][0]), float(balance_conv[0][1]))
