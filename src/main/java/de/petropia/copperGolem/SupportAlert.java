package de.petropia.copperGolem;

import org.javacord.api.entity.channel.ServerVoiceChannel;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.user.User;
import org.javacord.api.entity.user.UserStatus;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SupportAlert extends Thread {

    private final long userID;
    private final int delay;

    public SupportAlert(long userID, int delay) {
        this.userID = userID;
        this.delay = delay;
    }

    @Override
    public void run() {
        try {
            sleep((long) delay * 60 * 1000);
        } catch (InterruptedException exception) {
            exception.printStackTrace();
        }
        CopperGolem.getInstance().getAPI().getUserById(userID).thenAccept(user -> {
            Optional<ServerVoiceChannel> channel = user.getConnectedVoiceChannel(CopperGolem.getInstance().getServer());
            if(channel.isEmpty()) return;
            if(!channel.get().getIdAsString().equalsIgnoreCase(CopperGolem.getInstance().getProperties().getProperty("SupportWaitingChannel"))) return;
            Optional<Role> supportPingRole = CopperGolem.getInstance().getServer().getRoleById(CopperGolem.getInstance().getProperties().getProperty("SupporterRole"));
            if(supportPingRole.isEmpty()){
                System.out.println("Supporter role not found on server");
                return;
            }
            List<User> onlineSupporter = new ArrayList<>();
            supportPingRole.get().getUsers().forEach(supporter -> {
                if(supporter.getStatus() != UserStatus.ONLINE){
                    return;
                }
                onlineSupporter.add(supporter);
            });
            EmbedBuilder embedBuilder = new EmbedBuilder()
                    .setTitle("Support")
                    .setDescription("Ein nutzer wartet im Support Warteraum seit " + delay + " Minuten!")
                    .addInlineField("Anzahl Warteraum", String.valueOf(channel.get().getConnectedUserIds().size()))
                    .addInlineField("Supporter online", String.valueOf(onlineSupporter.size()))
                    .setAuthor(user)
                    .setFooter(CopperGolem.getInstance().getAPI().getYourself().getName() + " by Petropia")
                    .setColor(Color.RED);
            onlineSupporter.forEach(supporter -> supporter.sendMessage(embedBuilder));
            if(onlineSupporter.size() != 0) {
                return;
            }
            user.sendMessage(new EmbedBuilder()
                    .setTitle("Support")
                    .setDescription("Leider konnte kein Supporter erreicht werden. Das bedeutet, dass es eventuell etwas länger dauert bis dir geholfen wird.")
                    .setColor(Color.RED)
                    .setFooter(CopperGolem.getInstance().getAPI().getYourself().getName() + " by Petropia"));
        });
    }
}
