package de.petropia.copperGolem.listener;

import de.petropia.copperGolem.CopperGolem;
import de.petropia.copperGolem.SupportAlert;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.channel.server.voice.ServerVoiceChannelMemberJoinEvent;
import org.javacord.api.listener.channel.server.voice.ServerVoiceChannelMemberJoinListener;

import java.awt.*;

public class UserJoinSupportChannel implements ServerVoiceChannelMemberJoinListener {
    @Override
    public void onServerVoiceChannelMemberJoin(ServerVoiceChannelMemberJoinEvent event) {
        if(!event.getChannel().getIdAsString().equalsIgnoreCase(CopperGolem.getInstance().getProperties().getProperty("SupportWaitingChannel"))){
            return;
        }
        event.getUser().sendMessage(new EmbedBuilder()
                .setTitle("Support")
                .setDescription("Danke, dass du dich an den Support des Petropia.de Netzwerks wendest :star_struck:. Bitte gedulde dich in dem Support Warteraum einen Moment bis der nächste freie Supporter sich um dich kümmert :clock1:")
                .addInlineField("Wie mache ich die Musik aus? :mute:", "Um die Musik stumm zu schalten, mache einen Rechtsklick auf mich und klicke das Kästchen \"Stummschalten\" an")
                .setColor(Color.GREEN)
                .setFooter(CopperGolem.getInstance().getAPI().getYourself().getName() + " by Petropia"));
        if(event.getChannel().getConnectedUsers().size() != 2){
            return;
        }
        new SupportAlert(event.getUser().getId(), Integer.parseInt(CopperGolem.getInstance().getProperties().getProperty("SupportAlertDelay"))).start();
    }
}
