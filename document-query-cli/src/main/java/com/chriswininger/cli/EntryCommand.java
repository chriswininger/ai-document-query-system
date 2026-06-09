package com.chriswininger.cli;

import com.chriswininger.cli.commands.greet.GreetCommand;
import io.quarkus.picocli.runtime.annotations.TopCommand;
import picocli.CommandLine.Command;

@TopCommand
@Command(
        name = "document-query-cli",
        mixinStandardHelpOptions = true,
        version = "v1",
        subcommands = {GreetCommand.class}
)
public class EntryCommand {
}
