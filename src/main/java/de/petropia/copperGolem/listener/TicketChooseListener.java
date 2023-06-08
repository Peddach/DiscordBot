package de.petropia.copperGolem.listener;

import de.petropia.copperGolem.CopperGolem;
import de.petropia.copperGolem.tickets.TicketDatabase;
import de.petropia.copperGolem.tickets.TicketLabels;
import org.javacord.api.entity.channel.ChannelCategory;
import org.javacord.api.entity.channel.ServerTextChannelBuilder;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.MessageFlag;
import org.javacord.api.entity.message.component.*;
import org.javacord.api.entity.message.component.Button;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.entity.permission.Permissions;
import org.javacord.api.entity.permission.PermissionsBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.interaction.ModalSubmitEvent;
import org.javacord.api.event.interaction.SelectMenuChooseEvent;
import org.javacord.api.listener.interaction.ModalSubmitListener;
import org.javacord.api.listener.interaction.SelectMenuChooseListener;
import org.javacord.api.util.logging.ExceptionLogger;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;

public class TicketChooseListener implements SelectMenuChooseListener, ModalSubmitListener {

    @Override
    public void onSelectMenuChoose(SelectMenuChooseEvent event) {
        if (event.getSelectMenuInteraction().getChosenOptions().size() != 1) {
            event.getInteraction().createImmediateResponder().setContent("Etwas ist schief gelaufen!").setFlags(MessageFlag.EPHEMERAL).respond();
            return;
        }
        SelectMenuOption option = event.getSelectMenuInteraction().getChosenOptions().get(0);
        if (option.getLabel().equalsIgnoreCase(TicketLabels.BUG_REPORT.label())) {
            event.getInteraction().respondWithModal("TicketChooseModal", "Bug Melden",
                    ActionRow.of(TextInput.create(TextInputStyle.SHORT, "TicketChooseModalResponse_Bug", "Beschreibe kurz den Bug")));
            return;
        }
        if (option.getLabel().equalsIgnoreCase(TicketLabels.PLAYER_REPORT.label())) {
            event.getInteraction().respondWithModal("TicketChooseModal", "Spieler Melden",
                    ActionRow.of(TextInput.create(TextInputStyle.SHORT, "TicketChooseModalResponse_Player", "Beschreibe das Fehlverhalten kurz")));
            return;
        }
        if (option.getLabel().equalsIgnoreCase(TicketLabels.OTHER.label())) {
            event.getInteraction().respondWithModal("TicketChooseModal", "Sonstiges",
                    ActionRow.of(TextInput.create(TextInputStyle.SHORT, "TicketChooseModalResponse_Other", "Beschreibe kurz dein Anliegen")));
            return;
        }
        event.getInteraction().createImmediateResponder().setContent("Etwas ist schief gelaufen!").setFlags(MessageFlag.EPHEMERAL).respond();
    }

    @Override
    public void onModalSubmit(ModalSubmitEvent event) {
        if (!event.getModalInteraction().getCustomId().equalsIgnoreCase("TicketChooseModal")) {
            return;
        }
        event.getInteraction().respondLater(true).thenAccept(response -> {
            if (TicketDatabase.hasUserOpenTicket(event.getInteraction().getUser().getIdAsString()).exceptionally(ExceptionLogger.get()).join()) {
                response.setContent("Du hast bereits ein offenes Ticket!").setFlags(MessageFlag.EPHEMERAL).update();
                return;
            }
            response.setContent("Es wird ein Ticket für dich erstellt!").setFlags(MessageFlag.EPHEMERAL).update();
            final var otherInput = event.getModalInteraction().getTextInputValueByCustomId("TicketChooseModalResponse_Other");
            final var bugInput = event.getModalInteraction().getTextInputValueByCustomId("TicketChooseModalResponse_Bug");
            final var playerInput = event.getModalInteraction().getTextInputValueByCustomId("TicketChooseModalResponse_Player");
            var categoryOptional = CopperGolem.getInstance().getServer().getChannelCategoryById(CopperGolem.getInstance().getProperties().getProperty("TicketCategory"));
            if (categoryOptional.isEmpty()) return;
            otherInput.ifPresent(s -> createChannel("Other", s, event.getInteraction().getUser(), categoryOptional.get()));
            bugInput.ifPresent(s -> createChannel("Bug", s, event.getInteraction().getUser(), categoryOptional.get()));
            playerInput.ifPresent(s -> createChannel("Player", s, event.getInteraction().getUser(), categoryOptional.get()));
        });
    }

    private void createChannel(String title, String description, User user, ChannelCategory category) {
        final var builder = new ServerTextChannelBuilder(CopperGolem.getInstance().getServer())
                .setName(title + "-" + user.getName().replace(" ", ""))
                .setTopic(description)
                .setAuditLogReason("Ticket created by " + user.getName())
                .setCategory(category);
        for (var role : CopperGolem.getInstance().getServer().getRoles()) {
            builder.addPermissionOverwrite(role, new PermissionsBuilder().setDenied(PermissionType.VIEW_CHANNEL).build());
        }
        builder.create().thenAccept(channel -> {
            Permissions permissions = new PermissionsBuilder().setAllowed(PermissionType.VIEW_CHANNEL, PermissionType.SEND_MESSAGES, PermissionType.EMBED_LINKS, PermissionType.ADD_REACTIONS, PermissionType.ATTACH_FILE).build();
            channel.createUpdater().addPermissionOverwrite(user, permissions).update();
            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("Support")
                    .setDescription("Hey " + CopperGolem.getInstance().getServer().getNickname(user).orElse(user.getName()) + ":wave:. Danke, dass du dich an uns wendest! Bitte beschreibe in diesem Channel möglichst detailliert dein Anliegen.")
                    .setTimestampToNow()
                    .setColor(Color.ORANGE)
                    .setFooter(CopperGolem.getInstance().getAPI().getYourself().getName() + " by Petropia");
            new MessageBuilder().append(user).addEmbed(embed).send(channel);
            EmbedBuilder embed2 = new EmbedBuilder()
                    .setTitle("Anliegen von " + CopperGolem.getInstance().getServer().getNickname(user).orElse(user.getName()) + ":")
                    .setDescription(description)
                    .setTimestampToNow()
                    .setColor(Color.ORANGE)
                    .setFooter(CopperGolem.getInstance().getAPI().getYourself().getName() + " by Petropia");
            new MessageBuilder().addEmbed(embed2).send(channel);
            TicketDatabase.createTicket(user.getIdAsString(), channel.getIdAsString(), description, title);
            sendMessageToSupporter(user, description, title, channel.getIdAsString());
        });
    }

    private void sendMessageToSupporter(User user, String description, String ticketType, String ticketChannelID) {
        var claimChannel = CopperGolem.getInstance().getServer().getChannelById(CopperGolem.getInstance().getProperties().getProperty("TicketClaimChannel"));
        if (claimChannel.isEmpty()) return;
        var claimChannelAsServerTextChannel = claimChannel.get().asServerTextChannel();
        if (claimChannelAsServerTextChannel.isEmpty()) return;
        new MessageBuilder().addEmbed(new EmbedBuilder()
                        .setColor(Color.RED)
                        .setFooter(CopperGolem.getInstance().getAPI().getYourself().getName() + " by Petropia")
                        .setDescription("Der Nutzer " + CopperGolem.getInstance().getServer().getNickname(user).orElse(user.getName()) + " (" + user.getDiscriminatedName() + ") hat ein neues Ticket erstellt.")
                        .addField("Ticket Typ", ticketType)
                        .addField("Beschreibung", description)
                        .addField("Datum", date())
                        .addField("Nutzer", user.getDisplayName(CopperGolem.getInstance().getServer())))
                .addComponents(ActionRow.of(Button.create("Claim_" + ticketChannelID, ButtonStyle.SUCCESS, "Claimen")))
                .send(claimChannelAsServerTextChannel.get());
    }

    private String date() {
        Date date = Date.from(Instant.now());
        return new SimpleDateFormat("dd.MM.yyyyy HH:mm").format(date);
    }
}
