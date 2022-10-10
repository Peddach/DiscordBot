package de.petropia.mrsPeddach.listener;

import de.petropia.mrsPeddach.MrsPeddach;
import org.javacord.api.entity.channel.PrivateChannel;
import org.javacord.api.entity.channel.ServerChannel;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.MessageAttachment;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class RulesUpdate implements MessageCreateListener {

    @Override
    public void onMessageCreate(MessageCreateEvent event) {
        if (!event.isPrivateMessage()) {
            return;
        }
        Optional<PrivateChannel> privateChannelOptional = event.getPrivateChannel();
        if (privateChannelOptional.isEmpty()) {
            return;
        }
        PrivateChannel privateChannel = privateChannelOptional.get();
        Optional<User> userOptional = privateChannel.getRecipient();
        if (userOptional.isEmpty()) {
            return;
        }
        if (!event.getMessage().getContent().equalsIgnoreCase("!rules")) {
            return;
        }
        User user = userOptional.get();
        List<String> allowedRoles = Arrays.asList(MrsPeddach.getInstance().getProperties().getProperty("RulesUpdateRoles").split(","));
        AtomicBoolean isPermitted = new AtomicBoolean(false);
        for (Role role : MrsPeddach.getInstance().getServer().getRoles(user)) {
            allowedRoles.forEach(id -> {
                if (id.equalsIgnoreCase(role.getIdAsString())) {
                    isPermitted.set(true);
                }
            });
        }
        if (!isPermitted.get()) {
            user.sendMessage("Keine Rechte!");
            return;
        }
        if (event.getMessageAttachments().size() == 0) {
            user.sendMessage("Bitte hänge eine markdown Datei an!");
            return;
        }
        String rulesChannelID = MrsPeddach.getInstance().getProperties().getProperty("RulesChannel");
        Optional<ServerChannel> optionalChannel = MrsPeddach.getInstance().getServer().getChannelById(rulesChannelID);
        if (optionalChannel.isEmpty()) {
            event.getChannel().sendMessage("Der Regel Channel wurde nicht gesetzt in der Config oder existiert nicht (mehr)! Bitte informiere einen Dev");
            return;
        }
        ServerChannel genericChannel = optionalChannel.get();
        Optional<ServerTextChannel> rulesChannelOptional = genericChannel.asServerTextChannel();
        if (rulesChannelOptional.isEmpty()) {
            event.getChannel().sendMessage("Der RegelChannel ist kein ServerTextChannel!");
            return;
        }
        ServerTextChannel rulesChannel = rulesChannelOptional.get();
        rulesChannel.getMessages(100).thenAccept(messages -> messages.forEach(msg -> msg.delete().join())).whenComplete((result, exception) -> {
            if (exception != null) {
                exception.printStackTrace();
                user.sendMessage("Es ist ein Fehler aufgetreten: " + exception.getMessage());
                return;
            }
            for (MessageAttachment attachment : event.getMessageAttachments()) {
                try {
                    InputStream inputStream = attachment.downloadAsInputStream();
                    String readString = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)).lines().collect(Collectors.joining("\n"));
                    List<String> messages = splitText(readString);
                    for(String message : messages){
                        rulesChannel.sendMessage(message).exceptionally(e -> {
                            e.printStackTrace();
                            user.sendMessage("Es ist ein Fehler aufgetreten: " + e.getMessage());
                            return null;
                        });
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    user.sendMessage("Es ist ein Fehler aufgetreten: " + e.getMessage());
                    return;
                }
            }
            user.sendMessage("Die Regeln wurden erfolgreich geupdated!");
        });
    }

    private List<String> splitText(String input) {
        String[] splitStringBySpace = input.split(" ");
        List<String> messages = new ArrayList<>();
        StringBuilder currentBuffer = new StringBuilder();
        for (String string : splitStringBySpace) {
            if (currentBuffer.capacity() < 1950) {
                currentBuffer.append(string).append(" ");
                continue;
            }
            messages.add(currentBuffer.toString());
            currentBuffer = new StringBuilder();
            currentBuffer.append(string).append(" ");
        }
        messages.add(currentBuffer.toString());
        return messages;
    }
}
