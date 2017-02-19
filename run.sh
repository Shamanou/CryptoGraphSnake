#! /bin/bash

getCurrencyPairs () {
    curl  --silent https://api.kraken.com/0/public/AssetPairs | jq ' .result | keys' | tr -d '"' | tr -d "," | tr -d "[" | tr -d "]" | tr -d " "
}

getTickerInformation () {
	raw=`curl  --silent --data "pair=$1" https://api.kraken.com/0/public/Ticker`
	raw_fees=`curl --silent --data "pair=$1" https://api.kraken.com/0/public/AssetPairs`
 	fees=`echo "$raw_fees" | jq ' .result | .[] | .fees' | tr -d '\n' | tr -d ' ' | sed 's/\]\[/\]\n\[/g'`
 	fee_volume_currency=`echo "$raw_fees" | jq ' .result | .[] | .fee_volume_currency' | tr -d '"'`
	keys=`echo "$raw" | jq ' .result | keys[] ' | tr -d '"'`
	values=`echo "$raw" | jq ' .result | .[] | .a[0] + "\t" + .b[0] ' | tr -d '"'`
	out=`paste <(echo -ne "$keys") <(echo -ne "$values")`
	out=`paste <(echo -ne "$out") <(echo -ne "$fees")`
	paste <(echo -ne "$out") <(echo -ne "$fee_volume_currency")
 }

insertMarketDataIntoDB () {
	x=`mongo localhost/trade --eval "db.trade.drop()"  --quiet `
	data=(`echo -ne "$1" | tr '\t' '_'`)
	for x in "${data[@]}"; do
		pair=`echo "$x" | cut -d'_' -f1`
		base=`echo "$pair" | cut -c1-4`
	    quote=`echo "$pair" | cut -c5-8`
		ask=`echo "$x" | cut -d'_' -f2`
		bid=`echo "$x" | cut -d'_' -f3`
		reference=`echo "$x" | cut -d'_' -f5`
		fees=`echo "$x" | cut -d'_' -f4`
		mong=`echo "db.trade.insert({\"base\":\"$base\", \"quote\":\"$quote\", \"ask\":$ask, \"bid\":$bid, \"fees\":$fees,\"reference\":\"$reference\"})"`
		x=`mongo localhost/trade --eval "$mong" --quiet `
	done
}
echo
echo "			Welcome to the CryptocurrencyGraphSnake"
echo "			Developed by Shamanou van Leeuwen"
echo
echo

while true; do
	echo "			========================="
	echo "			+-----------------------+"
	echo "			DOWNLOADING MARKET DATA"
	echo "			+-----------------------+"
	echo
	pairs=`getCurrencyPairs | getCurrencyPairs | sed  '/^$/d' |  tr '\n' ',' | sed  's/,$//' | tr -d '.' | tr -d 'd'`
	marketData=`getTickerInformation "$pairs"`
	insertMarketDataIntoDB "$marketData"

	echo "			+-----------------------+"
	echo "			GRABBING TRADE START VALUE"
	echo "			+-----------------------+"
	echo
	# ./query.py
	start=`./query.py`
	# ./evolve.py $start
	echo "			+-----------------------+"
	echo "			EVOLVING TRADE TRAJECTORY"
	echo "			+-----------------------+"
	echo
	winner=`./evolve.py $start | tail -n 1`
	echo "			+-----------------------+"
	echo "			EXECUTING ORDER"
	echo "			+-----------------------+"
	if [ "$winner" != "NO SUITABLE INDIVIDUALS" ]; then
		./addOrder.py "$winner" $start
	else
		echo "			$winner"
	fi
done