package de.petropia.mrsPeddach;

import de.petropia.mrsPeddach.listener.RulesUpdate;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.UserStatus;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Properties;

public class MrsPeddach {

    private static MrsPeddach instance;
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
        new MrsPeddach(configPath);
    }

    public MrsPeddach(String configPath) throws IOException, NullPointerException {
        instance = this;
        properties = new Properties();
        properties.load(new FileInputStream(configPath));
        String token = String.valueOf(properties.getProperty("Token"));
        API = new DiscordApiBuilder()
                .setToken(token)
                .login().join();
        API.updateActivity("Petropia");
        API.updateStatus(UserStatus.ONLINE);
        this.server = API.getServerById(properties.getProperty("Server")).orElseThrow();
        registerListener();
    }

    private void registerListener(){
        API.addListener(new RulesUpdate());
    }

    public static MrsPeddach getInstance(){
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
