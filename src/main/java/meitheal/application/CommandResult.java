package meitheal.application;

import java.util.Objects;

/**
 * The outcome of a command. Commands that can be refused for a business reason (a grant to a solvent
 * member, an offer in an unknown category, a confirmation below the floor) return a {@code CommandResult}
 * instead of throwing, so the CLI can report the refusal cleanly. Programming errors (a null argument, a
 * state-machine violation) still throw.
 */
public record CommandResult(boolean success, String message) {

    public CommandResult {
        Objects.requireNonNull(message, "message");
    }

    public static CommandResult ok(String message) {
        return new CommandResult(true, message);
    }

    public static CommandResult failure(String message) {
        return new CommandResult(false, message);
    }
}
