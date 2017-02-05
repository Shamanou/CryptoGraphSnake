#! /usr/bin/env python

from pymongo import MongoClient
import sys
import random
import numpy

client = MongoClient()
db = client.trade

start=sys.argv[1]
volume=float(sys.argv[2])

def generateIndividual(icls):
	options = list(db.trade.find({"base":start})) + list(db.trade.find({"quote":start}))
	genestart = options[random.randint(0,len(options)-1)]
	while True:
		try:
			genesecond = list(db.trade.find({"base":genestart[u'quote']})) + list(db.trade.find({"quote":genestart[u'base']}))
			genesecond = genesecond[random.randint(0,len(genesecond)-1)]
			genethird = list(db.trade.find({"base":genesecond[u'quote']})) + list(db.trade.find({"quote":genesecond[u'base']}))
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
	vol = volume
	fitness  = vol
	fit = vol
	currency = start
	factor = None
	isFirst = True
	if ((individual[0]['base'] != start) and (individual[0]['quote'] != start)):
		return 0,
	if (individual[0]['base'] == individual[1]['base']) and (individual[0]['quote'] == individual[1]['quote']) :
		return 0,
	if (individual[1]['base'] == individual[2]['base']) and (individual[1]['quote'] == individual[2]['quote']) :
		return 0,

	trade_types = []
	for i in range(len(individual)):
		try:
			trade_types.append({\
				"base_base":individual[i-1]['base'] == individual[i]['base'],\
				"quote_quote":individual[i-1]['quote'] == individual[i]['quote'],\
				"quote_base":individual[i-1]['quote'] == individual[i]['base'],\
				"base_quote":individual[i-1]['base'] == individual[i]['quote']})
		except:
			pass

	z = 0
	for gene in individual:
		if not isFirst:
			ttype = None
			try:
				ttype = [ x[0] for x in trade_types[z].items() if x[1] ][0]
			except:
				return 0,
			if ttype == "base_base":
				fit =  (1/gene['bid']) * (1/fit)
			elif ttype == "quote_quote":
				fit *= gene['bid']
			elif ttype == "quote_base":
				fit /= gene['ask']
			elif ttype == "base_quote":
				fit = (1/fit) *  gene['bid']
			z += 1
		isFirst = False

		# fee_raw = 0.36 * fitness
		# fitness -= fee_raws

	reference = {\
		'base_quote' : db.trade.find_one({"base": "ZUSD",'quote': start}),\
		'quote_base' : db.trade.find_one({"base":start, 'quote': "ZUSD"}),\
		'base_base' : db.trade.find_one({"base":start, 'base': "ZUSD"}),\
		'quote_quote' : db.trade.find_one({"quote":start, 'quote': "ZUSD"}),}.items()
	factor = [ (i,reference[i]) for i in range(len(reference)) if reference[i][1] ][0]
	if factor[0] == 0:
		factor = factor[1][1]['ask']
		vol = (1/vol) * factor
	elif factor[0] == 1:
		factor = factor[1][1]['ask']
		vol /= factor
	elif factor[0] == 2:
		vol = (1/ factor[1][1]['ask'] ) * (1/vol)
	else:
		vol *= factor[1][1]['ask']
	return fitness - vol,


toolbox.register("mate", tools.cxOnePoint)
toolbox.register("mutate", tools.mutShuffleIndexes, indpb=0.1)
toolbox.register("select", tools.selTournament, tournsize=3)
toolbox.register("evaluate", evaluate)


pop = toolbox.population(n=300)
hof = tools.HallOfFame(1)
stats = tools.Statistics(lambda ind: ind.fitness.values)
stats.register("avg", numpy.mean)
stats.register("std", numpy.std)
stats.register("min", numpy.min)
stats.register("max", numpy.max)

pop, log = algorithms.eaSimple(pop, toolbox, cxpb=0.5, mutpb=0.2, ngen=40, stats=stats, halloffame=hof, verbose=True)
pop = [ i for i in pop if i.fitness.values[0] > 0 ]
if len(pop) > 0:	
	pop = sorted(pop, key=lambda ind:ind.fitness.values[0], reverse=True)
	winner = pop[0]

	for i in range(len(winner)):
		winner[i].pop('_id',None)

	import json

	print json.dumps(winner).replace(" ","")
else:
	print "NO SUITABLE INDIVIDUALS"