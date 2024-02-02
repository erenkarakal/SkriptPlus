package me.eren.skriptplus;

import java.util.List;
import java.util.Locale;

import me.eren.skriptplus.utils2.MessageUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SkriptPlusCommand implements TabExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length > 0) {
            switch (args[0].toLowerCase(Locale.ENGLISH)) {

                case "info" -> {}

                default -> onCommand(commandSender, command, label, new String[0]);

            }
        }

        MessageUtils.sendMessage(commandSender, "<green>&oInsert help message here <3");

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String label, @NotNull String[] argsd) {
        return null;
    }
}
