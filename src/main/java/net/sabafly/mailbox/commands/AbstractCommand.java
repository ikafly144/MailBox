package net.sabafly.mailbox.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.sabafly.mailbox.library.LibNMSAccessor;
import org.bukkit.entity.Player;

public abstract class AbstractCommand {

    private static boolean enabled = false;

    public static void enable() {
        enabled = true;
    }

    public abstract LiteralCommandNode<CommandSourceStack> command();

    protected AbstractCommand() {
    }

    public static <S extends CommandSourceStack> Command<S> executor(Command<S> command) {
        return executor(command, true);
    }

    public static <S extends CommandSourceStack> Command<S> executor(Command<S> command, boolean requirePlayerExecutor) {
        return new TempCommand<>(command, requirePlayerExecutor);
    }

    private record TempCommand<S extends CommandSourceStack>(Command<S> command, boolean requirePlayerExecutor) implements Command<S> {
        @Override
        public int run(CommandContext<S> context) throws CommandSyntaxException {
            if (!enabled)
                throw new SimpleCommandExceptionType(() -> "Command is disabled").create();
            if (requirePlayerExecutor && !(context.getSource().getExecutor() instanceof Player))
                throw LibNMSAccessor.get().notPlayerException();
            return command.run(context);
        }
    }

}
