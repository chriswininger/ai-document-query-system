package com.chriswininger.cli.commands.greet;

import com.chriswininger.cli.commands.greet.dto.GreetResult;
import com.chriswininger.cli.commands.greet.services.GreetService;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "greet", description = "Greet someone by name.", mixinStandardHelpOptions = true)
public class GreetCommand implements Runnable {

    private final GreetService greetService;

    @Option(names = {"-n", "--name"}, description = "Who to greet.", defaultValue = "World")
    String name;

    public GreetCommand(final GreetService greetService) {
        this.greetService = greetService;
    }

    @Override
    public void run() {
        final GreetResult result = greetService.greet(name);
        System.out.println(result.message());
    }
}
