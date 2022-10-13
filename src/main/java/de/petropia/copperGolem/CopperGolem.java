package de.petropia.copperGolem;

import de.petropia.copperGolem.audio.WaitingMusic;
import de.petropia.copperGolem.listener.RulesAccept;
import de.petropia.copperGolem.listener.RulesUpdate;
import de.petropia.copperGolem.listener.UserJoinSupportChannel;
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
        if(configPath == null){ //when there is no config, bot will fail to start
            System.err.println("No config provided!");
            return;
        }
        new CopperGolem(configPath);
    }

    public CopperGolem(String configPath) throws IOException, NullPointerException {
        instance = this;    //set current instance cause of Singleton
        properties = new Properties();  //create and load properties out of bot.properties
        properties.load(new FileInputStream(configPath));
        String token = String.valueOf(properties.getProperty("Token"));
        API = new DiscordApiBuilder()
                .setToken(token)
                .setAllIntents()    //Bot requires all perms because to lazy to set all individual
                .setWaitForUsersOnStartup(true) //Wait until all users are recived from the discord api endpoint
                .login().join();
        API.updateActivity("Petropia");
        API.updateStatus(UserStatus.ONLINE);
        this.server = API.getServerById(properties.getProperty("Server")).orElseThrow();
        RulesAccept.reload();
        registerListener();
        new WaitingMusic();
    }

    /**
     * Method for registering all Listener
     */
    private void registerListener(){
        API.addListener(new RulesUpdate());
        API.addReactionAddListener(new RulesAccept());
        API.addServerVoiceChannelMemberJoinListener(new UserJoinSupportChannel());
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
