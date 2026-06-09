package com.chriswininger.db;

import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

@ApplicationScoped
public class DSLContextProducer {

    private final AgroalDataSource dataSource;

    public DSLContextProducer(final AgroalDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Produces
    @ApplicationScoped
    public DSLContext dslContext() {
        return DSL.using(dataSource, SQLDialect.POSTGRES);
    }
}
