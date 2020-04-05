package com.shamanou.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.shamanou.service.ConfigurationService;
import junit.framework.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;
import java.io.File;
import java.io.IOException;


@RunWith(MockitoJUnitRunner.class)
public class ConfigTest {

    @InjectMocks
    private ConfigurationService configurationService;

    @Test
    public void shouldParseWithValidValues() throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.findAndRegisterModules();
        Configuration configuration = mapper.readValue(new File("src/test/resources/config.yml"), Configuration.class);

        Assert.assertEquals(configuration.getDatabaseHost(), "localhost");
        Assert.assertEquals(configuration.getGenomeSize(), 3);
        Assert.assertEquals(configuration.getReferenceCurrency(), "XBT");
        Assert.assertEquals(configuration.getCurrencyPairLimits().size(), 2);
        Assert.assertTrue(configuration.getCurrencyPairLimits().containsKey("ADA/CAD"));
        Assert.assertTrue(configuration.getCurrencyPairLimits().containsKey("ADA/ETH"));
        Assert.assertEquals(configuration.getCurrencyLimits().size(), 2);
        Assert.assertTrue(configuration.getCurrencyLimits().containsKey("ALGO"));
        Assert.assertTrue(configuration.getCurrencyLimits().containsKey("REP"));
    }

    @Test
    public void shouldReturnValidValues() throws IOException {
        Configuration configuration = configurationService.constructConfiguration();

        Assert.assertEquals(configuration.getDatabaseHost(), "localhost");
        Assert.assertEquals(configuration.getGenomeSize(), 3);
        Assert.assertEquals(configuration.getReferenceCurrency(), "XBT");
        Assert.assertEquals(configuration.getCurrencyPairLimits().size(), 148);
        Assert.assertTrue(configuration.getCurrencyPairLimits().containsKey("ADA/CAD"));
        Assert.assertTrue(configuration.getCurrencyPairLimits().containsKey("ADA/ETH"));
        Assert.assertEquals(configuration.getCurrencyLimits().size(), 36);
        Assert.assertTrue(configuration.getCurrencyLimits().containsKey("ALGO"));
        Assert.assertTrue(configuration.getCurrencyLimits().containsKey("REP"));
    }
}
