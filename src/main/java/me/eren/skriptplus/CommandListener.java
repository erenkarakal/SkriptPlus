package me.eren.skriptplus;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;

import java.util.List;

public class CommandListener implements Listener {

    /**
     * List of Skript subcommands to overwrite.
     */
    private static final List<String> skpCommands = List.of("info", "addon", "check", "recover");

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerCommand(PlayerCommandPreprocessEvent e) {
        String[] splitCommand = e.getMessage().split(" ");
        if (splitCommand[0].equalsIgnoreCase("/sk") || splitCommand[0].equalsIgnoreCase("/skript")) {
            if (splitCommand.length > 1 && skpCommands.contains(splitCommand[1])) {
                splitCommand[0] = "/skp";
                e.setMessage(String.join(" ", splitCommand));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onConsoleCommand(ServerCommandEvent e) {
        String[] splitCommand = e.getCommand().split(" ");
        if (splitCommand[0].equalsIgnoreCase("sk") || splitCommand[0].equalsIgnoreCase("skript")) {
            if (splitCommand.length > 1 && skpCommands.contains(splitCommand[1])) {
                splitCommand[0] = "skp";
                e.setCommand(String.join(" ", splitCommand));
            }
        }
    }

}
