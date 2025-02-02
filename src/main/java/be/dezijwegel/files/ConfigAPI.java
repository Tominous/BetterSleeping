package be.dezijwegel.files;

import be.dezijwegel.bettersleeping.BetterSleeping;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.LinkedList;


public class ConfigAPI {

    private FileConfiguration configuration;
    private File file;
    public FileType type;
    private BetterSleeping plugin;

    String fileName;

    private FileConfiguration defaultConfig;

    public enum FileType {
        CONFIG,
        LANG,
        BUFFS
    }

    /**
     * @param type of file
     * @param plugin
     */
    public ConfigAPI(FileType type, BetterSleeping plugin) {
        this.plugin = plugin;

        this.type = type;

        switch (type)
        {
            case CONFIG :
                fileName = "config.yml";
                break;
            case LANG:
                fileName = "lang.yml";
                break;
            case BUFFS:
                fileName = "buffs.yml";
        }

        this.file = new File(plugin.getDataFolder(), fileName);
        this.configuration = YamlConfiguration.loadConfiguration(file);

        //Copy contents of internal config file
        Reader defaultStream = null;
        try {
            defaultStream = new InputStreamReader(plugin.getResource(fileName), "UTF8");
            if (defaultStream != null) {
                this.defaultConfig = YamlConfiguration.loadConfiguration(defaultStream);
            }
        } catch (Exception ex) {}

        saveDefaultConfig();
    }

    /**
     * Get an Object from a file
     * @param path
     * @return Object
     */
    public Object get(String path) {
        if (configuration.contains(path))
            return configuration.get(path);
        else return defaultConfig.get(path);
    }

    /**
     * Get the default Object from file in jar
     * @param path
     * @return
     */
    @Deprecated
    public Object getDefault(String path) {return defaultConfig.get(path); }

    /**
     * Get a String from a file
     * @param path
     * @return String
     */
    public String getString(String path) {

        String string;

        if (configuration.contains(path))
            string = configuration.getString(path);
        else string = defaultConfig.getString(path);

        if (string != null)
            if (string.contains("&")) string = string.replaceAll("&", "§");

        return string;
    }

    /**
     * Get an integer from a file
     * @param path
     * @return int
     */
    public int getInt(String path)
    {
        if (configuration.contains(path))
            return configuration.getInt(path);
        else return defaultConfig.getInt(path);
    }

    /**
     * Get a boolean from a file
     * @param path
     * @return boolean
     */
    public boolean getBoolean(String path)
    {
        if (configuration.contains(path))
            return configuration.getBoolean(path);
        else return defaultConfig.getBoolean(path);
    }

    /**
     * Get a long from a file
     * @param path
     * @return
     */
    public long getLong(String path)
    {
        if (configuration.contains(path))
            return configuration.getLong(path);
        return defaultConfig.getLong(path);
    }

    /**
     * Check if the file contains a specific path
     * Does not check the default configuration file
     * @param path
     * @return
     */
    public boolean contains(String path)
    {
        return configuration.contains(path);
    }

    /**
     * Check if the file contains a specific path
     * And choose to ignore the default file
     * @param path
     * @return
     */
    public boolean containsIgnoreDefault(String path) { return configuration.contains(path, true); }

    /**
     * This method will force the file back to its default
     * This is helpful when new options are added and comments are needed
     */
    public void forceDefaultConfig()
    {
        plugin.saveResource(fileName, true);
    }

    /**
     * Reload the config file
     */
    public void reloadFile() {
        if (configuration == null) {
            file = new File(plugin.getDataFolder(), fileName);
        }
        configuration = YamlConfiguration.loadConfiguration(file);

        // Look for defaults in the jar
        Reader defConfigStream = null;
        try {
                defConfigStream = new InputStreamReader(plugin.getResource(fileName), "UTF8");
        } catch (UnsupportedEncodingException ex) {}

        if (defConfigStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
            configuration.setDefaults(defConfig);
        }
    }

    public void saveDefaultConfig() {
        if (!file.exists())
        {
            ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
            console.sendMessage("[BetterSleeping] " + ChatColor.GREEN + "Copying a new " + fileName + " ...");
            plugin.saveResource(fileName, false);

            //Make sure the Configuration is up to date. Otherwise missing options may be reported!
            this.configuration = YamlConfiguration.loadConfiguration(file);
        }

        if (file == null)
        {
            file = new File(plugin.getDataFolder(), fileName);
        }
    }

    /**
     * Gets the ConfigurationSection that equals the path
     * @param path
     */
    public ConfigurationSection getConfigurationSection(String path)
    {
        return configuration.getConfigurationSection(path);
    }

    /**
     * Compare the default file with the one on the server and report every missing option
     */
    public void reportMissingOptions()
    {
        Reader defConfigStream = null;
        try {
            defConfigStream = new InputStreamReader(plugin.getResource(fileName), "UTF8");
        } catch (UnsupportedEncodingException ex) {}

        if (defConfigStream != null) {
            LinkedList<String> missingOptions = new LinkedList<>();

            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
            for (String path : defConfig.getKeys(true))
            {
                if ( ! configuration.contains(path))
                {
                    missingOptions.add(path);
                }
            }

            if (missingOptions.size() > 0)
            {
                ConsoleCommandSender console = Bukkit.getConsoleSender();

                if (missingOptions.size() == 1)
                     console.sendMessage("[BetterSleeping] " + ChatColor.RED + "A missing option has been found in " + fileName + "!");
                else console.sendMessage("[BetterSleeping] " + ChatColor.RED + missingOptions.size() + " Missing options have been found in " + fileName + "!");

                console.sendMessage("[BetterSleeping] " + ChatColor.RED + "Please add the missing option(s) manually or delete this file and restart the server");
                console.sendMessage("[BetterSleeping] " + ChatColor.RED + "The default values will be used until then");

                for (String path : missingOptions)
                {
                    Object value = defaultConfig.get(path);

                    //Change formatting if the setting is a String
                    if (value instanceof String)
                        value = "\"" + value + "\"";

                    console.sendMessage("[BetterSleeping] " + ChatColor.DARK_RED + "Missing option: " + path + " with default value: " + value);
                }
            }
        }
    }

//    /**
//     * Load all values that are an instance of a given type into the given list
//     * @param type
//     * @param map
//     */
//    @Deprecated
//    public void loadTypesFromFile(Class type, Map<String, Object> map)
//    {
//        File file = null;
//        file = new File(fileName);
//        YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(file);
//
//        for (String path : configuration.getKeys(true))
//        {
//            boolean found = false;
//
//            if (!configuration.isConfigurationSection(path)) {
//                if (configuration.contains(path)) {
//                    if (configuration.get(path) != null) {
//                        if (type.isInstance(configuration.get(path))) {
//                            map.put(path, configuration.get(path));
//                            found = true;
//
//                            //Give the console messages if needed (regarding configuration options)
//                            performPathCheck(path);
//                        }
//                    }
//                }
//            }
//
//            if (!found) {
//                if (!defaultConfig.isConfigurationSection(path)) {
//                    if (defaultConfig.contains(path)) {
//                        if (defaultConfig.get(path) != null) {
//                            if (type.isInstance(defaultConfig.get(path))) {
//                                ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
//                                map.put(path, defaultConfig.get(path));
//                                console.sendMessage("[BetterSleeping] " + Color.RED + "A missing config option (" + path + ") has been found in " + fileName + ". Now using default value: " + defaultConfig.get(path));
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    }
//
//    /**
//     * Checks a path and send specific messages to the console regarding to the configuration
//     * @param path
//     */
//    @Deprecated
//    public void performPathCheck(String path)
//    {
//        ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
//        if (type == FileType.CONFIG) {
//            if (path.equalsIgnoreCase("world_specific_behavior")) {
//                String append = "";
//                if (configuration.get(path) != null) {
//                    append = " Your preferred value will still be used: " + configuration.get(path);
//                } else {
//                    append = " The default value will be used: true";
//                }
//                console.sendMessage("[BetterSleeping] " + Color.RED + "Your config file contains \'world_specific_behavior\', please replace this with \'multiworld_support\'." + append);
//            }
//        } else if (type == FileType.LANG)
//        {
//            if (path.equalsIgnoreCase("prefix"))
//            {
//                if (!configuration.getString(path).toLowerCase().contains("bettersleeping"))
//                {
//                    console.sendMessage("[BetterSleeping] Please consider keeping \'BetterSleeping\' in the prefix, as it would support my plugin.");
//                    console.sendMessage("[BetterSleeping] Of course you are not obliged to do so, since I gave you the option but it would be greatly appreciated! :-)");
//                }
//            }
//        }
//    }
}