package com.perseus.albionmarket.commands;

import com.perseus.albionmarket.AlbionMarket;
import com.perseus.albionmarket.config.MessageManager;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class MarketCommand implements CommandExecutor {

    private final Map<String, Subcommand> subcommands;

    public MarketCommand() {
        this.subcommands = new HashMap<>();
    }

    public void registerSubcommand(String name, Subcommand subcommand) {
        subcommands.put(name.toLowerCase(), subcommand);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            MessageManager msg = AlbionMarket.getInstance().getMessageManager();
            sender.sendMessage(msg.getPrefixed("<gray>AlbionMarket v" +
                AlbionMarket.getInstance().getDescription().getVersion() + "</gray>"));
            return true;
        }

        String subcommandName = args[0].toLowerCase();
        Subcommand subcommand = subcommands.get(subcommandName);

        if (subcommand == null) {
            MessageManager msg = AlbionMarket.getInstance().getMessageManager();
            sender.sendMessage(msg.getPrefixed("<red>Unknown subcommand. Use /market help.</red>"));
            return true;
        }

        String[] subArgs = new String[args.length - 1];
        System.arraycopy(args, 1, subArgs, 0, subArgs.length);

        return subcommand.execute(sender, subArgs);
    }

    public interface Subcommand {
        boolean execute(CommandSender sender, String[] args);
    }
}
