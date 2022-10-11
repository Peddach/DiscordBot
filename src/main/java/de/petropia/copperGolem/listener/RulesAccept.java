package de.petropia.copperGolem.listener;

import de.petropia.copperGolem.CopperGolem;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.Reaction;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.reaction.ReactionAddEvent;
import org.javacord.api.listener.message.reaction.ReactionAddListener;
import org.javacord.api.util.logging.ExceptionLogger;

import java.awt.*;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Optional;

public class RulesAccept implements ReactionAddListener {

    private static String reationMessageId = CopperGolem.getInstance().getProperties().getProperty("CurrentAcceptMessage");

    @Override
    public void onReactionAdd(ReactionAddEvent event) {
        if (!event.getChannel().getIdAsString().equalsIgnoreCase(CopperGolem.getInstance().getProperties().getProperty("RulesChannel"))) {
            return;
        }
        if(event.getUserId() == CopperGolem.getInstance().getAPI().getYourself().getId()) return;
        event.removeReaction();
        Optional<Message> reactedMessage = event.getMessage();
        Optional<User> user = event.getUser();
        Optional<Reaction> reaction = event.getReaction();
        Optional<Role> acceptRole = CopperGolem.getInstance().getServer().getRoleById(CopperGolem.getInstance().getProperties().getProperty("RuleAcceptRole"));
        if(reactedMessage.isEmpty() || reaction.isEmpty() || user.isEmpty() || acceptRole.isEmpty()) return;
        if (!reactedMessage.get().getIdAsString().equalsIgnoreCase(reationMessageId)) return;
        if (!reaction.get().getEmoji().equalsEmoji("✅")) return;
        user.get().addRole(acceptRole.get()).exceptionally(ExceptionLogger.get());
    }

    /**
     * This Method should be called, when the messages gets updated or the Bot restarts
     */
    public static void reload() {
        Optional<ServerTextChannel> channel = CopperGolem.getInstance().getServer().getTextChannelById(CopperGolem.getInstance().getProperties().getProperty("RulesChannel"));
        if (channel.isEmpty()) return;
        channel.get().getMessageById(reationMessageId).thenAccept(Message::delete).whenComplete((unused, throwable) -> channel.get().sendMessage(new EmbedBuilder()
                .setTitle("Regeln")
                .setColor(Color.GREEN)
                .setDescription("Bitte akzeptiere die Regeln, indem du mit :white_check_mark: regierst")).thenAccept(message -> {
            reationMessageId = message.getIdAsString();
            CopperGolem.getInstance().getProperties().setProperty("CurrentAcceptMessage", message.getIdAsString());
            try {
                CopperGolem.getInstance().getProperties().store(new FileOutputStream("bot.properties"), null);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            message.addReaction("✅").exceptionally(ExceptionLogger.get());
        }));
    }
}
