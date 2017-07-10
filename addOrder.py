#! /usr/bin/env python


from pymongo import MongoClient
import krakenex
import time
from fractions import Fraction


k = krakenex.API()
k.load_key('apikey')
valueType = None
ttype = None

client = MongoClient()
db = client.trade

def wallet(curr):
	while True:
		try:
			balance = k.query_private('Balance')['result'][curr]
			break
		except:
			pass
	return Fraction(balance)


def trade(trajectory, startcurrency):

	fit = wallet(startcurrency[0])
	trade_types = []
	for i,j in zip(range(0,len(trajectory)-1),range(1,len(trajectory))):
		trade_types.append({\
			"base_base":trajectory[i]['base'] == trajectory[j]['base'],\
			"quote_quote":trajectory[i]['quote'] == trajectory[j]['quote'],\
			"quote_base":trajectory[i]['quote'] == trajectory[j]['base'],\
			"base_quote":trajectory[i]['base'] == trajectory[j]['quote']})
	final=None
	isFirst = True
	for i in range(len(trajectory)):
		try:
			ttype = [ x[0] for x in trade_types[i].items() if x[1] ][0]
		except:
			pass
		hasViqc = False

		if i == 0:
			if ttype == "base_base":
				trtype = "buy"
				fit = wallet(trajectory[i]['quote'])
				hasViqc = True
			elif ttype == "quote_quote":
				trtype = "sell"
				fit = wallet(trajectory[i]['base'])
				# hasViqc = True
			elif ttype == "quote_base":
				trtype = "sell"
				fit = wallet(trajectory[i]['base'])
				# hasViqc = True
			elif ttype == "base_quote":
				trtype = "buy"
				fit = wallet(trajectory[i]['quote'])
				hasViqc = True
		elif i == 1:
			if ttype == "base_base":
				trtype = "buy"
				fit = wallet(trajectory[i]['quote'])
				hasViqc = True
			elif ttype == "quote_quote":
				trtype = "sell"
				fit = wallet(trajectory[i]['base'])
				# hasViqc = True
			elif ttype == "quote_base":
				trtype = "sell"
				fit = wallet(trajectory[i]['base'])
				# hasViqc = True
			elif ttype == "base_quote":
				trtype = "buy"
				fit = wallet(trajectory[i]['quote'])
				hasViqc = True
		elif i == 2:
			if ttype == "base_base":
				trtype = "sell"
				fit = wallet(trajectory[i]['base'])
				#hasViqc = True
			elif ttype == "quote_quote":
				trtype = "buy"
				fit = wallet(trajectory[i]['quote'])
				hasViqc = True
			elif ttype == "quote_base":
				trtype = "sell"
				fit = wallet(trajectory[i]['quote'])
				hasViqc = True
			elif ttype == "base_quote":
				trtype = "sell"
				fit = wallet(trajectory[i]['quote'])
				hasViqc = True

                price = float(db.trade.find_one({ 'quote': trajectory[i]['quote'], 'base': trajectory[i]['base']  })['bid'])
                

		volume = float(fit)

		print trajectory[i]['base']+"_"+trajectory[i]['quote'], trtype, format(float(volume), '.7f'), ttype

		if trtype == "buy":
			out = volume / price
			price = out * volume
		volume -= volume * 0.26


		query = {'pair': trajectory[i]['base']+trajectory[i]['quote'],\
		 'type': trtype,\
                 'ordertype': 'market', \
		 'volume': format(float(volume), '.7f'), \
		}
		if hasViqc:
			query['oflags'] = 'viqc'
		while True:
			try:
				order = k.query_private('AddOrder', query)
				break
			except:
				pass
		print order
		if order['error'] != []:
			break

                while k.query_private('OpenOrders')['result']['open'] != {}:
            		time.sleep(3)
                time.sleep(1)
