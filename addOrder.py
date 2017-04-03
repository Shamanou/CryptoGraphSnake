#! /usr/bin/env python


from pymongo import MongoClient
import ast
import krakenex
import sys
import time
from fractions import Fraction


k = krakenex.API()
k.load_key('apikey')
trajectory = ast.literal_eval(sys.argv[1])
startcurrency = sys.argv[2]
valueType = None
ttype = None

client = MongoClient()
db = client.trade

def wallet(curr):
	balance = k.query_private('Balance')['result'][curr]
	return Fraction(balance)

fit = wallet(startcurrency)
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

	if i == 0:
		if ttype == "quote_base":
			tmp = "base_quote"
		else:
			tmp = ttype
		if trajectory[i][tmp.split("_")[1]] != "XXBT":
			factor = db.trade.find_one({"base": trajectory[i][tmp.split("_")[1]],'quote': trajectory[i+1][tmp.split("_")[0]]})
		else:
			factor = {'bid':1}
		if not factor:
			factor = {'bid':1}
		if ttype == "base_base":
			trtype = "sell"
			fit = wallet(trajectory[i]['quote'])
		elif ttype == "quote_quote":
			trtype = "buy"
			fit = wallet(trajectory[i]['quote'])
		elif ttype == "quote_base":
			trtype = "sell"
			fit = wallet(trajectory[i]['quote'])
		elif ttype == "base_quote":
			trtype = "sell"
			fit = wallet(trajectory[i]['quote'])
	elif i == 1:
		if trajectory[i-1][tmp.split("_")[1]] != "XXBT":
			factor = db.trade.find_one({"base": trajectory[i-1][tmp.split("_")[1]],'quote': trajectory[i][tmp.split("_")[0]]})
		else:
			factor = {'bid':1}
		if ttype == "base_base":
			trtype = "sell"
			fit = wallet(trajectory[i]['quote'])
		elif ttype == "quote_quote":
			trtype = "buy"
			fit = wallet(trajectory[i]['quote'])
		elif ttype == "quote_base":
			trtype = "buy"
			fit = wallet(trajectory[i]['quote'])
		elif ttype == "base_quote":
			trtype = "sell"
			fit = wallet(trajectory[i]['quote'])
	elif i == 2:
		if trajectory[i-1][tmp.split("_")[1]] != "XXBT":
			factor = db.trade.find_one({"base": trajectory[i-1][tmp.split("_")[1]],'quote': trajectory[i][tmp.split("_")[0]]})
		else:
			factor = {'bid':1}
		if ttype == "base_base":
			trtype = "buy"
			fit = wallet(trajectory[i]['quote'])
		elif ttype == "quote_quote":
			trtype = "sell"
			fit = wallet(trajectory[i]['quote'])
		elif ttype == "quote_base":
			trtype = "buy"
			fit = wallet(trajectory[i]['quote'])
		elif ttype == "base_quote":
			trtype = "sell"
			fit = wallet(trajectory[i]['quote'])


	volume = float(fit)
	print trajectory[i]['base']+"_"+trajectory[i]['quote'], trtype, format(float(volume), '.15f'), ttype
	order = k.query_private('AddOrder',\
	 {'pair': trajectory[i]['base']+trajectory[i]['quote'],\
	 'type': trtype, 'ordertype': 'market', 'volume': format(float(volume), '.15f') })
	print order
	time.sleep(15)