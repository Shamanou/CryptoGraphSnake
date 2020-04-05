package com.shamanou.service;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.shamanou.config.Configuration;
import com.shamanou.domain.TickerDto;
import com.shamanou.domain.TradePair;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.knowm.xchange.Exchange;
import org.knowm.xchange.ExchangeFactory;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.kraken.KrakenExchange;

import javax.enterprise.context.ApplicationScoped;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

@ApplicationScoped
public class InitializeService {

    public Exchange initializeKrakenApiServices(String key, String secret) {
        ExchangeSpecification exchangeSpecification = new KrakenExchange().getDefaultExchangeSpecification();
        exchangeSpecification.setApiKey(key);
        exchangeSpecification.setSecretKey(secret);
        Exchange exchange = ExchangeFactory.INSTANCE.createExchange(KrakenExchange.class.getName());
        exchange.applySpecification(exchangeSpecification);

        return exchange;
    }

    public MongoCollection<TickerDto> initializeDatabase(Configuration configuration) {
        CodecRegistry pojoCodecRegistry = fromRegistries(
                fromProviders(PojoCodecProvider.builder().register(TickerDto.class, TradePair.class).build()),
                MongoClient.getDefaultCodecRegistry());
        MongoClient mongoClient = new MongoClient(configuration.getDatabaseHost() + ":27017", MongoClientOptions.builder().codecRegistry(pojoCodecRegistry).build());
        MongoDatabase db = mongoClient.getDatabase("trade");
        MongoCollection<TickerDto> table = db.getCollection("trade", TickerDto.class);
        try {
            table.drop();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return db.getCollection("trade", TickerDto.class);
    }
}
