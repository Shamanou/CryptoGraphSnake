#! /usr/bin/env python

import requests
from pymongo import MongoClient
import query
import evolve
import addOrder

client = MongoClient()
trade_collection = client.trade.trade

def getCurrencyPairs():
	return requests.get('https://api.kraken.com/0/public/AssetPairs').json()['result'].keys()

def getTickerInformation(pairs):
	ticker = requests.post('https://api.kraken.com/0/public/Ticker',data = {'pair': ','.join(pairs)}).json()['result']
	fees_raw = requests.post("https://api.kraken.com/0/public/AssetPairs", data = {'pair':','.join(pairs)}).json()['result']
	fee_volume_currency = requests.post("https://api.kraken.com/0/public/AssetPairs", data = {'pair':','.join(pairs)}).json()['result']
	out = []
	for pair in pairs:
		if '.' not in pair:
			out.append([ ticker[pair]['a'][0], ticker[pair]['b'][0], fees_raw[pair], fee_volume_currency[pair] ])
	return out 

def insertMarketDataIntoDB(data):
	trade_collection.delete_many({})
	for x in data:
		pair = x[2]
		base = pair['base']
		quote = pair['quote']
		ask = x[0]	
		bid = x[1]	
		fees = x[3]
		trade_collection.insert({'base':base, 'quote':quote, 'ask': ask, 'bid': bid, 'fees': fees})

print ""
print "			Welcome to the CryptocurrencyGraphSnake"
print "			Developed by Shamanou van Leeuwen"
print ""
print ""

while True:
	print "			========================="
	print "			+-----------------------+"
	print "			DOWNLOADING MARKET DATA"
	print "			+-----------------------+"
	print ""

	insertMarketDataIntoDB(getTickerInformation(getCurrencyPairs()))

	print "			+-----------------------+"
	print "			GRABBING TRADE START VALUE"
	print "			+-----------------------+"
	print ""
	start = query.getStart(0)
	print "			+-----------------------+"
	print "			EVOLVING TRADE TRAJECTORY nr. 1"
	print "			+-----------------------+"
	print ""
	evolve.setStart_Volume(start[0],start[1])
	winners = evolve.run()
	print "			+-----------------------+"
	print "			EXECUTING ORDERS"
	print "			+-----------------------+"
	if winners:
		for winner in winners:
			addOrder.trade(winner,start)
	else:
		print "no suitable trajectories" 