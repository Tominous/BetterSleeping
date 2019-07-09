package be.dezijwegel.files;

import be.dezijwegel.bettersleeping.BetterSleeping;
import be.dezijwegel.bettersleeping.Reloadable;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Lang implements Reloadable {

    private BetterSleeping plugin;
    private ConfigAPI configAPI;

    public Lang (BetterSleeping plugin)
    {
        this.plugin = plugin;

        configAPI = new ConfigAPI(ConfigAPI.FileType.LANG, plugin);
        configAPI.reportMissingOptions();
    }

    /**
     * Sends a raw message to the receiver but substitutes <receiver> by the receiver's name
     * @param message
     * @param receiver
     */
    private void sendRaw(String message, CommandSender receiver)
    {
        if (message.equals(""))
        {
            return;
        }

        if (message.contains("<receiver>"))
            receiver.sendMessage(message.replace("<receiver>", receiver.getName()));
        else receiver.sendMessage(message);
    }

    /**
     * (Attempt to) send a message to the given receiver
     * The message must exist in the default lang.yml or lang.yml on disk
     * @param messagePath
     * @param receiver
     */
    public void sendMessage(String messagePath, CommandSender receiver)
    {
        sendMessage(messagePath,receiver,new LinkedHashMap<>());
    }

    /**
     * (Attempt to) send a message to the given receiver
     * The message must exist in the default lang.yml or lang.yml on disk
     * The placeholders (keys of replacings) will be replaced by their respective values
     * @param messagePath
     * @param receiver
     * @param replacings
     */
    public void sendMessage(String messagePath, CommandSender receiver, Map<String, String> replacings)
    {
        sendMessage(messagePath,receiver,replacings,false);
    }

    /**
     * (Attempt to) send a message to the given receiver
     * The message must exist in the default lang.yml or lang.yml on disk
     * The placeholders (keys of replacings) will be replaced by their respective values
     * If a [singular.plural] part exists, it will be corrected based on the given boolean
     * @param messagePath
     * @param receiver
     * @param replacings
     */
    public void sendMessage(String messagePath, CommandSender receiver, Map<String, String> replacings, boolean singular)
    {
        String msg = composeMessage(messagePath, replacings, singular);
        if (msg != "") sendRaw(msg, receiver);
    }

    /**
     * Send a given String to a group of receivers
     * @param messagePath
     * @param receivers
     */
    public void sendMessageToGroup(String messagePath, List<Player> receivers)
    {
        sendMessageToGroup(messagePath, receivers, new LinkedHashMap<>());
    }

    /**
     * (Attempt to) send a message to the given receivers
     * The message must exist in the default lang.yml or lang.yml on disk
     * The placeholders (keys of replacings) will be replaced by their respective values
     * @param messagePath
     * @param receivers
     * @param replacings
     */
    public void sendMessageToGroup(String messagePath, List<Player> receivers, Map<String,String> replacings)
    {
        sendMessageToGroup(messagePath, receivers, replacings, false);
    }

    /**
     * (Attempt to) send a message to the given receivers
     * The message must exist in the default lang.yml or lang.yml on disk
     * The placeholders (keys of replacings) will be replaced by their respective values
     * If a [singular.plural] part exists, it will be corrected based on the given boolean
     * @param messagePath
     * @param receivers
     * @param replacings
     */
    public void sendMessageToGroup(String messagePath, List<Player> receivers, Map<String,String> replacings, boolean singular)
    {
        String message = composeMessage(messagePath, replacings, singular);

        for (Player player : receivers) {
            sendRaw(message, player);
        }
    }

    /**
     * Returns a String which is the composed version of the message (ready for sending to a player)
     * @param messagePath
     * @return
     */
    public String composeMessage(String messagePath, Map<String, String> replacings, boolean isSingular)
    {
        String message = "";

        if (configAPI.getString(messagePath) != null)
            message = configAPI.getString(messagePath);

        if (isMessageDisabled(message))
        {
            return "";
        }

        message = substitute(message,replacings);
        message = correctSingularPlural(message, isSingular);
        message = addPrefix(message);

        return message;
    }

    /**
     * Creates a String that combines the prefix with the given string
     * @param message
     * @return
     */
    public String addPrefix(String message)
    {
        String fullMessage = "";

        if (configAPI.getString("prefix") != null)
        {
            String prefix = configAPI.getString("prefix");
            fullMessage += prefix;
        }

        fullMessage += message;

        return fullMessage;
    }

    /**
     * Replace certain Strings within the given message
     * @return
     */
    public String substitute(String message, Map<String, String> replacings)
    {
            for (Map.Entry<String, String> entry : replacings.entrySet()) {
                if (message.contains(entry.getKey())) {
                    message = message.replaceAll(entry.getKey(), entry.getValue());
                }
            }
        return message;
    }

    public boolean isMessageDisabled(String message)
    {
        switch (message)
        {
            case "":        return true;
            case "ignored": return true;
            default:        return false;
        }
    }

    /**
     * Replaces [singular.plural] to the correct one (singular or plural), based on a given amount. The corrected String is returned.
     * @param str The String that will be checked for singular/plural nouns in the form of [singular.plural]
     * @param singular set to true if the String should be corrected to be singular, or plural if false
     * @return the corrected String
     */
    public String correctSingularPlural(String str, boolean singular)
    {
        String string = str;
        boolean bracketsOpen = false;
        int startIndex = 0;
        for (int ind = 0; ind < string.length()-1; ind++)
        {
            if (bracketsOpen)
            {
                if (string.charAt(ind) == ']')
                {
                    String temp = string.substring(startIndex+1,ind);
                    bracketsOpen = false;

                    if (str.contains("."))
                    {
                        String[] strings = temp.split("\\.");

                        if (strings.length > 1)
                        {
                            if (singular) str = str.replace("[" + temp + "]", strings[0]);
                            else str = str.replace("[" + temp + "]", strings[1]);
                        }
                    } else str = str.replace("[" + temp + "]", temp);
                }
            }
            else
            {
                if (string.charAt(ind) == '[')
                {
                    startIndex = ind;
                    bracketsOpen = true;
                }
            }
        }

        return str;
    }

    @Override
    public void reload() {
        configAPI = new ConfigAPI(ConfigAPI.FileType.LANG, plugin);
        configAPI.reportMissingOptions();
        lang = new HashMap<String, Object>();

        configAPI.loadTypesFromFile(String.class, lang);
    }
}
