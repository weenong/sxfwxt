package com.zhexinit.xuelele.config;


import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.zhexinit.xuelele.model.Entities;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataSourceConfig {
    @Autowired
    private MongoProperties mongoProperties;

    private Morphia morphia() {
        final Morphia morphia = new Morphia();
        morphia.mapPackageFromClass(Entities.class);

        return morphia;
    }

    @Bean
    public MongoClientOptions mongoOptions() {
        return MongoClientOptions.builder()
                .connectionsPerHost(500)
                .threadsAllowedToBlockForConnectionMultiplier(50)
                .connectTimeout(10000)
                .maxWaitTime(25000)
                .socketTimeout(0)
                .maxConnectionIdleTime(6000)
                .build();
    }

    @Bean
    public Datastore datastore(MongoClient mongoClient) {
        final Datastore datastore = morphia().createDatastore(mongoClient, mongoProperties.getDatabase());
        datastore.ensureIndexes();
        return datastore;
    }
}
