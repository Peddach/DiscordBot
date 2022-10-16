package de.petropia.copperGolem.tickets;

import de.petropia.copperGolem.CopperGolem;
import de.petropia.copperGolem.listener.TicketChooseListener;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.component.ActionRow;
import org.javacord.api.entity.message.component.SelectMenu;
import org.javacord.api.entity.message.component.SelectMenuOption;
import org.javacord.api.entity.message.embed.EmbedBuilder;

import java.awt.*;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

public class TicketCreateButton {

    private static String currentTicketCreateID = CopperGolem.getInstance().getProperties().getProperty("CurrentTicketsMessage");

    public static void reload(){
        Optional<ServerTextChannel> channel = CopperGolem.getInstance().getServer().getTextChannelById(CopperGolem.getInstance().getProperties().getProperty("TicketsChannel"));
        if (channel.isEmpty()) return;
        channel.get().getMessageById(currentTicketCreateID).thenAccept(message -> message.addSelectMenuChooseListener(new TicketChooseListener())).exceptionally(throwable -> {
            sendTicketsMessage(channel.get());
            return null;
        });
    }

    private static void sendTicketsMessage(ServerTextChannel channel){
        new MessageBuilder()
                .addEmbed(new EmbedBuilder()
                        .setTitle("Tickets")
                        .setDescription("Hey! Danke, dass du dich an den Support des Petropia.de Netzwerkes wendest :star_struck:. Hier kannst du ein Ticket erstellen und ein Teammitglied wird sich um dein Anliegen schnellst möglich kümmern :clock1: :ticket:.")
                        .setColor(Color.CYAN)
                        .setFooter(CopperGolem.getInstance().getAPI().getYourself().getName() + " by Petropia"))
                .addComponents(ActionRow.of(SelectMenu.create("ticketSelect", "Wähle die Art des Tickets", 1, 1, Arrays.asList(
                        SelectMenuOption.create(TicketLabels.BUG_REPORT.label(), "Bug Report", "Klicke hier, um einen Spielfehler zu melden"),
                        SelectMenuOption.create(TicketLabels.PLAYER_REPORT.label(), "Spieler", "Klicke hier, um das Fehlverhalten eines Spielers zu melden"),
                        SelectMenuOption.create(TicketLabels.OTHER.label(), "Sonstiges", "Klicke hier, wenn keine der Kategorien zutrifft")
                ))))
                .send(channel).thenAccept(message -> {
                    message.addSelectMenuChooseListener(new TicketChooseListener());
                    setCurrentTicketCreateID(message.getIdAsString());
                });
    }

    private static void setCurrentTicketCreateID(String id){
        currentTicketCreateID = id;
        CopperGolem.getInstance().getProperties().setProperty("CurrentTicketsMessage", id);
        try {
            CopperGolem.getInstance().getProperties().store(new FileOutputStream("bot.properties"), null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
