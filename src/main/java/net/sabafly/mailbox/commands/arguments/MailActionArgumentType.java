package net.sabafly.mailbox.commands.arguments;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;
import net.sabafly.mailbox.type.MailAction;
import org.jetbrains.annotations.NotNull;

public class MailActionArgumentType implements CustomArgumentType<MailAction, String> {

    public static MailActionArgumentType mailAction() {
        return new MailActionArgumentType();
    }

    private MailActionArgumentType() {}

    public static final DynamicCommandExceptionType ERROR_INVALID_JSON = new DynamicCommandExceptionType(
            text -> new LiteralMessage("Invalid JSON: " + text)
    );

    @Override
    public @NotNull MailAction parse(@NotNull StringReader reader) throws CommandSyntaxException {
        try {
            return MailAction.deserialize(reader.readString());
        } catch (Exception e) {
            String string = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            throw ERROR_INVALID_JSON.createWithContext(reader, string);
        }
    }

    @Override
    public @NotNull ArgumentType<String> getNativeType() {
        return StringArgumentType.string();
    }
}
