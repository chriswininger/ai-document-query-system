package com.wininger.spring_ai_demo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.wininger.spring_ai_demo.config.jooq.JooqJsonSerializer;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DataSourceConnectionProvider;
import org.jooq.impl.DefaultConfiguration;
import org.jooq.impl.DefaultDSLContext;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class JooqConfig {
  @Bean
  public DSLContext dslContext(DataSource dataSource) {
    DefaultConfiguration config = new DefaultConfiguration();
    config.set(new DataSourceConnectionProvider(dataSource));
    config.set(SQLDialect.POSTGRES);

    return new DefaultDSLContext(config);
  }

  // https://stackoverflow.com/questions/71570788/no-serializer-found-for-class-org-jooq-json-and-no-properties-discovered-to-crea
  // retrieve json columns
  @Bean
  public Jackson2ObjectMapperBuilderCustomizer jsonCustomizer() {
    return builder ->
        builder.serializationInclusion(JsonInclude.Include.USE_DEFAULTS)
            .serializers(new JooqJsonSerializer());
  }
}
