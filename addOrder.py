#! /usr/bin/env python

import ast
import krakenex
import sys
import time

k = krakenex.API()
k.load_key('apikey')
trajectory = ast.literal_eval(sys.argv[1])
startcurrency = sys.argv[2]
volume = float(sys.argv[3])
valueType = None
type = None

def wallet(curr):
	balance = k.query_private('Balance')['result'][curr]
	return float(balance)

for i in range(len(trajectory)):
	transaction = trajectory[i]
	fee = transaction['fees'][0][1]
	# print transaction
	order = None
	volume -= (volume * 0.36)
	if valueType:
		startcurrency = transaction[valueType]

	if startcurrency == transaction['base']:
		type = 'sell'
		if i > 0:
			if trajectory[i-1]['quote'] == transaction['quote']:
				type = 'buy'
				volume = wallet(transaction['quote']) / transaction['bid']
			elif trajectory[i-1]['base'] == transaction['base']:
				type = 'sell'
				volume =  wallet(transaction['base'])
			elif trajectory[i-1]['base'] == transaction['quote']:
				type = "buy"
				volume = wallet(transaction['quote']) / transaction['bid']
			elif trajectory[i-1]['quote'] == transaction['base']:
				type = 'sell'
				volume = wallet(transaction['base'])

	elif startcurrency == transaction['quote']:
		type = 'buy'
		volume = volume / transaction['bid']
		if i > 0:
			if trajectory[i-1]['quote'] == transaction['quote']:
				type = 'buy'
				volume = wallet(transaction['quote']) / transaction['bid']
			elif trajectory[i-1]['base'] == transaction['base']:
				type = 'sell'
				volume =  wallet(transaction['base'])
			elif trajectory[i-1]['base'] == transaction['quote']:
				type = "buy"
				volume =  wallet(transaction['quote']) / transaction['bid']
			elif trajectory[i-1]['quote'] == transaction['base']:
				type = 'sell'
				volume = wallet(transaction['base'])

	print transaction['base']+"_"+transaction['quote'], type, float(volume)

	if type == "sell":
		order = k.query_private('AddOrder',\
		 {'pair': transaction['base']+transaction['quote'],\
		 'type': type,'ordertype': 'market', 'volume': float(volume) })
	elif type == "buy":
		order = k.query_private('AddOrder',\
		 {'pair': transaction['base']+transaction['quote'],\
		 'type': type, 'ordertype': 'market', 'volume': float(volume) })
	print order

	if startcurrency == transaction['base']:
		volume = wallet(transaction['quote'])
		valueType = "quote"
	elif startcurrency == transaction['quote']:
			volume = wallet(transaction['base'])
			valueType = "base"