package de.petropia.copperGolem;

import de.petropia.copperGolem.listener.RulesAccept;
import de.petropia.copperGolem.listener.RulesUpdate;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.UserStatus;

import java.io.*;
import java.util.Objects;
import java.util.Properties;

public class CopperGolem {

    private static CopperGolem instance;
    private final DiscordApi API;
    private final Properties properties;
    private final Server server;

    public static void main(String[] args) throws IOException {
        File workdir = new File(".");   //Get current dir
        if(!workdir.isDirectory()){
            return;
        }
        String configPath = null;
        for(File file : Objects.requireNonNull(workdir.listFiles())){
            if(!file.getName().equalsIgnoreCase("bot.properties")){ //Check if there is a config
                continue;
            }
            configPath = file.getPath();
        }
        if(configPath == null){ //when there is no config, one will be created from the resources
            System.err.println("No config provided!");
            return;
        }
        new CopperGolem(configPath);
    }

    public CopperGolem(String configPath) throws IOException, NullPointerException {
        instance = this;
        properties = new Properties();
        properties.load(new FileInputStream(configPath));
        String token = String.valueOf(properties.getProperty("Token"));
        API = new DiscordApiBuilder()
                .setToken(token)
                .setAllIntents()
                .setWaitForUsersOnStartup(true)
                .login().join();
        API.updateActivity("Petropia");
        API.updateStatus(UserStatus.ONLINE);
        this.server = API.getServerById(properties.getProperty("Server")).orElseThrow();
        RulesAccept.reload();
        registerListener();
    }

    private void registerListener(){
        API.addListener(new RulesUpdate());
        API.addReactionAddListener(new RulesAccept());
    }

    public static CopperGolem getInstance(){
        return instance;
    }

    public Server getServer(){
        return server;
    }

    public DiscordApi getAPI(){
        return API;
    }

    public Properties getProperties(){
        return properties;
    }
}
