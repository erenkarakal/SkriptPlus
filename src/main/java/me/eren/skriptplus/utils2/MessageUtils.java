package me.eren.skriptplus.utils2;

import me.eren.skriptplus.SkriptPlus;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Nullable;

public class MessageUtils {

    /**
     * @param who who to send
     * @param message the message
     */
    public static void sendMessage(CommandSender who, String message) {
        sendMessage(who, message, SkriptPlus.PREFIX);
    }

    /**
     * @param who who to send
     * @param message the message
     * @param prefix the prefix for this message
     */
    public static void sendMessage(CommandSender who, String message, @Nullable String prefix) {
        if (prefix != null)
            message = prefix + " " + message;
        who.sendMessage(MiniMessage.miniMessage().deserialize(message));
    }

}
