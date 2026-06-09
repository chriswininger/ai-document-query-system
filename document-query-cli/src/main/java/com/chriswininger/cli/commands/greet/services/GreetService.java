package com.chriswininger.cli.commands.greet.services;

import com.chriswininger.cli.commands.greet.dto.GreetResult;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

@ApplicationScoped
public class GreetService {

    private static final Logger LOG = Logger.getLogger(GreetService.class);

    public GreetResult greet(final String name) {
        LOG.infof("Greeting %s", name);
        return new GreetResult("Hello, %s!".formatted(name));
    }
}
