package com.shamanou.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.shamanou.config.Configuration;

import javax.enterprise.context.ApplicationScoped;
import java.io.File;
import java.io.IOException;

@ApplicationScoped
public class ConfigurationService {
    private static final String CONFIGURATION_PATH = "src/main/resources/config.yml";

    public Configuration constructConfiguration() throws IOException {
        Configuration configuration;
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.findAndRegisterModules();
        configuration = mapper.readValue(new File(CONFIGURATION_PATH), Configuration.class);
        return configuration;
    }
}
