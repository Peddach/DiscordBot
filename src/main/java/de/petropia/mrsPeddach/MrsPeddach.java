package de.petropia.mrsPeddach;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.user.UserStatus;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Properties;

public class MrsPeddach {

    private final DiscordApi API;
    private final Properties properties;

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
            System.err.println("No config provided! Copy default config");
            try(InputStream resource = MrsPeddach.class.getResourceAsStream("bot.properties")){
                if(resource == null){
                    System.err.println("Cant find config in jar!");
                    return;
                }
                Files.copy(resource, Path.of(workdir.getPath()));
            }
            catch(Exception e){
                e.printStackTrace();
            }
            return;
        }
        new MrsPeddach(configPath);
    }

    public MrsPeddach(String configPath) throws IOException {
        properties = new Properties();
        properties.load(new FileInputStream(configPath));
        String token = String.valueOf(properties.getProperty("Token"));
        API = new DiscordApiBuilder()
                .setToken(token)
                .login().join();
        API.updateActivity("Hello World! :-D");
        API.updateStatus(UserStatus.IDLE);
    }
}
