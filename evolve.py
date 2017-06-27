#! /usr/bin/env python

from pymongo import MongoClient
import random
import numpy
from fractions import Fraction

client = MongoClient()
db = client.trade

start = None
volume = None


def setStart_Volume(s,v):
	global start
	start = s
	global volume
	volume = v

def generateIndividual(icls):
	options = list(db.trade.find({"base":start})) + list(db.trade.find({"quote":start}))
	genestart = options[random.randint(0,len(options)-1)]
	while True:
		try:
			genesecond = \
			 list(db.trade.find({"base":genestart[u'quote']})) +\
			 list(db.trade.find({"quote":genestart[u'base']})) +\
			 list(db.trade.find({"base":genestart[u'base']})) +\
			 list(db.trade.find({"quote":genestart[u'quote']}))
			genesecond = genesecond[random.randint(0,len(genesecond)-1)]
			genethird = \
			list(db.trade.find({"base":genesecond[u'quote']})) +\
			list(db.trade.find({"quote":genesecond[u'base']})) +\
			list(db.trade.find({"base":genesecond[u'base']})) +\
			list(db.trade.find({"quote":genesecond[u'quote']}))
			genethird = genethird[random.randint(0,len(genethird)-1)]
			return icls([genestart,genesecond,genethird])
		except Exception as e:
			options = list(db.trade.find({"base":start})) + list(db.trade.find({"quote":start}))
			genestart = options[random.randint(0,len(options)-1)]


from deap import base, creator
creator.create("FitnessMax", base.Fitness, weights=(1.0,))
creator.create("Individual", list, fitness=creator.FitnessMax)

from deap import tools
from deap import algorithms

toolbox = base.Toolbox()
toolbox.register("individual", generateIndividual, creator.Individual)
toolbox.register("population", tools.initRepeat, list, toolbox.individual)

def evaluate(individual):
	vol = Fraction(volume)
	fit = Fraction(vol)
	currency = start
	factor = None
	isFirst = True
	isValid = False
	fitness = None

	if ((individual[0]['base'] != start) and (individual[0]['quote'] != start)):
		return 0,
	if (individual[0]['base'] == individual[1]['base']) and (individual[0]['quote'] == individual[1]['quote']) :
		return 0,
	if (individual[1]['base'] == individual[2]['base']) and (individual[1]['quote'] == individual[2]['quote']) :
		return 0,

	if (individual[0]['base'] == individual[1]['base']) or (individual[0]['base'] == individual[1]['quote']):
		if start == individual[0]['base']:
			return 0,
	elif (individual[0]['quote'] == individual[1]['base']) or (individual[0]['quote'] == individual[1]['quote']):
		if start == individual[0]['quote']:
			return 0,
	else:
		return 0,


	curlist = []
	for i,j in zip(range(0,len(individual)-1),range(1,len(individual))):
		if (individual[i]['base'] == individual[j]['base']):
			curlist.append(( individual[i]['base'], individual[j]['base'] ))
			isValid = True
		if (individual[i]['quote'] == individual[j]['quote']):
			curlist.append(( individual[i]['quote'], individual[j]['quote'] ))
			isValid = True
		if (individual[i]['quote'] == individual[j]['base']):
			curlist.append(( individual[i]['quote'], individual[j]['base'] ))
			isValid = True
		if (individual[i]['base'] == individual[j]['quote']):
			curlist.append(( individual[i]['base'], individual[j]['quote'] ))
			isValid = True

	import itertools
	curlist = list(set(itertools.chain(*curlist)))
	if len(curlist) == 1:
		isValid = False

	if not isValid:
		return 0,

	trade_types = []
	for i,j in zip(range(0,len(individual)-1),range(1,len(individual))):
		trade_types.append({\
			"base_base":individual[i]['base'] == individual[j]['base'],\
			"quote_quote":individual[i]['quote'] == individual[j]['quote'],\
			"quote_base":individual[i]['quote'] == individual[j]['base'],\
			"base_quote":individual[i]['base'] == individual[j]['quote']})
	z = 0
	final=None
	ttype = None
	tmp = 0.0
	for gene in individual:
		if not isFirst:
			try:
				ttype = [ x[0] for x in trade_types[z].items() if x[1] ][0]
			except:
				return 0,
				fit -= db.trade.find_one({'quote':gene['quote'],'bid':gene['bid']})['fees']['fees'][0][1]  * fit
				if not fit:
					return 0,
			fit = Fraction(fit)
			if ttype == "base_base":
				tmp = Fraction(1,Fraction(Fraction(gene['bid']) * fit))
			elif ttype == "quote_quote":
				tmp = fit * Fraction(gene['bid'])
			elif ttype == "quote_base":
				tmp = Fraction(fit,Fraction(gene['bid']))
			elif ttype == "base_quote":
				tmp = Fraction(Fraction(gene['bid']),fit)
			z += 1
		isFirst = False
		# print float(fit), ttype, gene
		final = (gene,ttype)

		if not float(fit):
			return 0,

	#convert trade output to reference value
	REFERENCE = "XXBT"

	if final[1].split('_')[1] == "quote":
		fttype = "base"
	else:
		fttype = "quote"

	if REFERENCE != final[0][fttype]:
		reference = {\
			'base_a' : db.trade.find_one({"base": REFERENCE,'quote': final[0][fttype]}),\
			'quote_a' : db.trade.find_one({"base":final[0][fttype], 'quote': REFERENCE})}.items()
		factor = [ (i,reference[i]) for i in range(len(reference)) if reference[i][1]]
		if factor:
			factor = factor[0]
			if (factor[0] == 0) and (fttype == "quote"):
				fit = Fraction(Fraction(1,Fraction(factor[1][1]['bid'])), fit)
				# print "A:",float(fit)

			elif (factor[0] == 0) and (fttype == "base"):
				fit = Fraction(Fraction(1,Fraction(factor[1][1]['bid'])), fit)
				# print "B:",float(fit)

			elif (factor[0] == 1) and (fttype == "quote"):
				fit = Fraction(Fraction(factor[1][1]['bid']), fit)
				# print "C:",float(fit)

			elif (factor[0] == 1) and (fttype == "base"):
				fit = Fraction(Fraction(factor[1][1]['bid']), fit)
				# print "D:",float(fit)
			else:
				return 0,
		else:
			return 0,
	else:
		return 0,
	#convert input volume to reference
	if start != REFERENCE:
		reference = {\
			'base_quote' : db.trade.find_one({"base": REFERENCE,'quote': start}),\
			'quote_base' : db.trade.find_one({"base":start, 'quote': REFERENCE})}.items()
		factor = [ (i,reference[i]) for i in range(len(reference)) if reference[i][1]]
		if factor:
			factor = factor[0]
			if factor[0] == 0:
				vol = Fraction(Fraction(1,Fraction(factor[1][1]['bid'])) * Fraction(vol))

			elif factor[0] == 1:
				vol = Fraction(vol, Fraction(1,Fraction(factor[1][1]['bid'])))

		else:
			return 0,
	else:
		vol = Fraction(vol)

    #evolution takes place on the profit expected
	return float(fit-vol),

def run():

	toolbox.register("mate", tools.cxOnePoint)
	toolbox.register("mutate", tools.mutShuffleIndexes, indpb=0.1)
	toolbox.register("select", tools.selTournament, tournsize=3)
	toolbox.register("evaluate", evaluate)


	pop = toolbox.population(n=500)
	hof = tools.HallOfFame(1)
	stats = tools.Statistics(lambda ind: ind.fitness.values)
	stats.register("avg", numpy.mean)
	stats.register("std", numpy.std)
	stats.register("min", numpy.min)
	stats.register("max", numpy.max)

	pop, log = algorithms.eaSimple(pop, toolbox, cxpb=0.5, mutpb=0.3, ngen=20, stats=stats, halloffame=hof, verbose=True)
	pop = [ i for i in pop if i.fitness.values[0] > 0 ]
	if len(pop) > 0:	
		pop = sorted(pop, key=lambda ind:ind.fitness.values[0], reverse=True)
		winners = pop[0:5]

		for i in range(len(winners)):
			for z in range(len(winners[i])):
				winners[i][z].pop('_id')

		return winners
		# print json.dumps(winner).replace(" ","")
	else:
		return None
