package com.wininger.spring_ai_demo;

import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DataSourceConnectionProvider;
import org.jooq.impl.DefaultConfiguration;
import org.jooq.impl.DefaultDSLContext;
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
}
