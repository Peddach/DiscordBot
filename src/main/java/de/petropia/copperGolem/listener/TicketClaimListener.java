package de.petropia.copperGolem.listener;

import de.petropia.copperGolem.CopperGolem;
import de.petropia.copperGolem.tickets.TicketDatabase;
import org.javacord.api.entity.channel.ServerChannel;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.MessageFlag;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.entity.permission.PermissionsBuilder;
import org.javacord.api.event.interaction.InteractionCreateEvent;
import org.javacord.api.listener.interaction.InteractionCreateListener;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TicketClaimListener implements InteractionCreateListener {

    private static final List<Long> claimedTickets = new ArrayList<>();

    @Override
    public void onInteractionCreate(InteractionCreateEvent event) {
        if (event.getMessageComponentInteraction().isEmpty()) return;
        String id = event.getMessageComponentInteraction().get().getCustomId();
        String[] idSplit = id.split("_");
        if (idSplit.length != 2) {
            return;
        }
        if (!idSplit[0].equalsIgnoreCase("Claim")) {
            return;
        }
        if (claimedTickets.contains(Long.parseLong(idSplit[1]))) {
            event.getInteraction().createImmediateResponder().setContent("Das Ticket ist bereits geclaimed, wird aber grade von der Datenbank bearbeitet!").setFlags(MessageFlag.EPHEMERAL).respond();
            return;
        }
        claimedTickets.add(Long.parseLong(idSplit[1]));
        event.getInteraction().respondLater(true).thenAccept(response -> {
            Optional<ServerChannel> optionalServerChannel = CopperGolem.getInstance().getServer().getChannelById(idSplit[1]);
            if (optionalServerChannel.isEmpty()) {
                response.setContent("Etwas ist schief gelaufen oder das Ticket wurde bereits geclaimed! (Channel not found").setFlags(MessageFlag.EPHEMERAL).update();
                return;
            }
            if (optionalServerChannel.get().asServerTextChannel().isEmpty()) {
                response.setContent("Etwas ist schief gelaufen oder das Ticket wurde schon geclaimed! (Channel not TextChannel)").update();
                return;
            }
            response.setContent("Ticket wird geclaimed!").setFlags(MessageFlag.EPHEMERAL).update();
            ServerTextChannel ticketChannel = optionalServerChannel.get().asServerTextChannel().get();
            String staffName;
            Optional<String> supporterNameOptional = event.getInteraction().getUser().getNickname(CopperGolem.getInstance().getServer());
            staffName = supporterNameOptional.orElseGet(() -> event.getInteraction().getUser().getName());
            ticketChannel.createUpdater().addPermissionOverwrite(event.getInteraction().getUser(),
                    new PermissionsBuilder().setAllowed(PermissionType.VIEW_CHANNEL, PermissionType.SEND_MESSAGES, PermissionType.EMBED_LINKS, PermissionType.ADD_REACTIONS, PermissionType.ATTACH_FILE).build()).update().thenRun(() ->
                    new MessageBuilder().append(event.getInteraction().getUser()).addEmbed(new EmbedBuilder()
                                    .setTitle("Ticket Update")
                                    .setDescription("Das Ticket wird nun vom Teammitglied " + staffName + " bearbeitet!")
                                    .setColor(Color.GREEN)
                                    .setFooter(CopperGolem.getInstance().getAPI().getYourself().getName() + " by Petropia"))
                            .send(ticketChannel));
            event.getMessageComponentInteraction().get().getMessage().delete();
            TicketDatabase.setTicketClaimed(ticketChannel.getIdAsString(), event.getInteraction().getUser().getIdAsString());
        });
        new Thread(() -> {
            try {
                Thread.sleep(10000000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            claimedTickets.remove(Long.parseLong(idSplit[1]));
        }).start();
    }
}
