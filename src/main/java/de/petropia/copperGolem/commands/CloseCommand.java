package de.petropia.copperGolem.commands;

import de.petropia.copperGolem.CopperGolem;
import de.petropia.copperGolem.tickets.TicketDatabase;
import org.javacord.api.entity.channel.*;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.interaction.InteractionCreateEvent;
import org.javacord.api.interaction.SlashCommand;
import org.javacord.api.listener.interaction.InteractionCreateListener;

import java.awt.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;

public class CloseCommand implements InteractionCreateListener {

    @Override
    public void onInteractionCreate(InteractionCreateEvent event) {
        if (event.getSlashCommandInteraction().isEmpty()) return;
        if (!event.getSlashCommandInteraction().get().getCommandName().equalsIgnoreCase("close")) return;
        event.getInteraction().respondLater(true).thenAccept(response -> {
            TextChannel channel = event.getInteraction().getChannel().orElseThrow();
            ServerTextChannel serverTextChannel = channel.asServerTextChannel().orElseThrow();
            Categorizable categorizable = channel.asCategorizable().orElseThrow();
            ChannelCategory channelCategory = categorizable.getCategory().orElseThrow();
            ChannelCategory ticketCategory = CopperGolem.getInstance().getServer().getChannelCategoryById(CopperGolem.getInstance().getProperties().getProperty("TicketCategory")).orElseThrow();
            if(channelCategory.getId() != ticketCategory.getId()){
                response.setContent("Falscher Channel!").update();
                return;
            }
            User executer = event.getInteraction().getUser();
            TicketDatabase.getClaimedBy(channel.getIdAsString()).thenAccept(claimed -> {
                if (!executer.getIdAsString().equalsIgnoreCase(claimed)) {
                    response.setContent("Nur der Nutzer der das Ticket geclaimed hat, kann dieses schließen!").update();
                    return;
                }
                response.setContent("Ticket wird gelschlossen!").update();
                TicketDatabase.deleteTicket(channel.getIdAsString());
                new MessageBuilder().addEmbed(new EmbedBuilder()
                        .setTitle("Ticket Update")
                        .setDescription("Das Ticket wurde geschlossen")
                        .setColor(Color.RED)
                        .setFooter(CopperGolem.getInstance().getAPI().getYourself().getName() + " by Petropia")).send(channel);
                collectMessage(serverTextChannel, CopperGolem.getInstance().getServer().getDisplayName(executer));
            }).exceptionally(e -> {
                response.setContent("Ticket konnte nicht geschlossen werden!").update();
                e.printStackTrace();
                return null;
            });
        });
    }

    private void collectMessage(ServerTextChannel channel, String claimedBy) {
        File file = new File(channel.getName() + "-" + channel.getIdAsString());
        try (BufferedWriter output = new BufferedWriter(new FileWriter(file, StandardCharsets.UTF_8, true))) {
            output.write("-x-x-x-x-x-x-x-x-x-x-x-x-x-x-x-x-x-x-x");
            output.newLine();
            output.write("Ticket Name: " + channel.getName());
            output.newLine();
            output.write("Ticket Topic: " + channel.getTopic());
            output.newLine();
            output.write("Claimed by: " + claimedBy);
            output.newLine();
            output.write("-x-x-x-x-x-x-x-x-x-x-x-x-x-x-x-x-x-x-x");
            output.newLine();
            ArrayList<String> messages = new ArrayList<>();
            channel.getMessagesAsStream().forEach(message -> {
                if (message.getAuthor().getIdAsString().equalsIgnoreCase(CopperGolem.getInstance().getAPI().getYourself().getIdAsString())) {
                    return;
                }
                String content = message.getContent();
                String date = formatDate(message.getCreationTimestamp());
                String author = "Unbekannt";
                if (message.getUserAuthor().isPresent()) {
                    author = CopperGolem.getInstance().getServer().getDisplayName(message.getUserAuthor().get());
                }
                String formatString = "[" + date + "] " + "(" + author + ") " + content;
                messages.add(formatString);
            });
            Collections.reverse(messages);
            for(String message : messages){
                output.write(message);
                output.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        Optional<ServerChannel> serverChannel = CopperGolem.getInstance().getServer().getChannelById(CopperGolem.getInstance().getProperties().getProperty("TicketLog"));
        if (serverChannel.isEmpty()) {
            System.out.println("Ticket Log Channel not set!");
            return;
        }
        ServerTextChannel logChannel = serverChannel.get().asServerTextChannel().orElseThrow();
        new MessageBuilder().setContent(channel.getName() + " - Log").addAttachment(file).send(logChannel).thenAccept(message -> {
            if (file.delete()) {
                channel.delete();
                return;
            }
            message.addReaction("👎");
            channel.delete();
        });
    }

    private String formatDate(Instant instant) {
        return new SimpleDateFormat("dd.MM.yyyyy HH:mm").format(Date.from(instant));
    }

    public static void init() {
        SlashCommand.with("close", "Schließt das Ticket").createForServer(CopperGolem.getInstance().getServer()).join();
    }
}
