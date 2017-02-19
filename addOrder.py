#! /usr/bin/env python

import ast
import krakenex
import sys
import time
from fractions import Fraction


k = krakenex.API()
k.load_key('apikey')
trajectory = ast.literal_eval(sys.argv[1])
startcurrency = sys.argv[2]
volume = float(sys.argv[3])
valueType = None
ttype = None

def wallet(curr):
	balance = k.query_private('Balance')['result'][curr]
	return float(balance)

for i in range(len(trajectory)):
	transaction = trajectory[i]
	fee = transaction['fees'][0][1]
	# print transaction
	order = None
	volume -= (volume * 0.36)
	volume = Fraction(volume)
	if valueType:
		startcurrency = transaction[valueType]

	if startcurrency == transaction['base']:
		ttype = 'sell'
		if i > 0:
			if trajectory[i-1]['quote'] == transaction['quote']:
				ttype = 'buy'
				volume = Fraction(Fraction(wallet(transaction['quote'])),Fraction(transaction['bid']))
			elif trajectory[i-1]['base'] == transaction['base']:
				ttype = 'sell'
				volume =  wallet(transaction['base'])
			elif trajectory[i-1]['base'] == transaction['quote']:
				ttype = "buy"
				volume = Fraction(Fraction(wallet(transaction['quote'])),Fraction(transaction['bid']))
			elif trajectory[i-1]['quote'] == transaction['base']:
				ttype = 'sell'
				volume = Fraction(wallet(transaction['base']))

	elif startcurrency == transaction['quote']:
		ttype = 'buy'
		volume = Fraction(Fraction(volume), Fraction(transaction['bid']))
		if i > 0:
			if trajectory[i-1]['quote'] == transaction['quote']:
				ttype = 'buy'
				volume = Fraction(Fraction(wallet(transaction['quote'])), Fraction(transaction['bid']))
			elif trajectory[i-1]['base'] == transaction['base']:
				ttype = 'sell'
				volume =  wallet(transaction['base'])
			elif trajectory[i-1]['base'] == transaction['quote']:
				ttype = "buy"
				volume =  Fraction(Fraction(wallet(transaction['quote'])), Fraction(transaction['bid']))
			elif trajectory[i-1]['quote'] == transaction['base']:
				ttype = 'sell'
				volume = Fraction(wallet(transaction['base']))

	if type(volume) == type(Fraction(1,1)):
		volume = volume.limit_denominator()
	else:
		volume = volume
	print transaction['base']+"_"+transaction['quote'], ttype, float(volume)

	if ttype == "sell":
		order = k.query_private('AddOrder',\
		 {'pair': transaction['base']+transaction['quote'],\
		 'type': ttype,'ordertype': 'market', 'volume': float(volume) })
	elif ttype == "buy":
		order = k.query_private('AddOrder',\
		 {'pair': transaction['base']+transaction['quote'],\
		 'type': ttype, 'ordertype': 'market', 'volume': float(volume) })
	print order

	if startcurrency == transaction['base']:
		volume = wallet(transaction['quote'])
		valueType = "quote"
	elif startcurrency == transaction['quote']:
			volume = wallet(transaction['base'])
			valueType = "base"