package com.shamanou.service;

import org.junit.Test;
import org.knowm.xchange.Exchange;
import org.knowm.xchange.ExchangeFactory;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.kraken.KrakenExchange;
import org.mockito.InjectMocks;


public class DatabaseServiceTest {

    @InjectMocks
    private DatabaseService databaseService;

    @Test
    public void shouldInitialize() {
        ExchangeSpecification exchangeSpecification = new KrakenExchange().getDefaultExchangeSpecification();
        Exchange exchange = ExchangeFactory.INSTANCE.createExchange(KrakenExchange.class.getName());
        exchange.applySpecification(exchangeSpecification);

    }
}
