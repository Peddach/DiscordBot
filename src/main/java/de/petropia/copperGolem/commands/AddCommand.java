package de.petropia.copperGolem.commands;

import de.petropia.copperGolem.CopperGolem;
import org.javacord.api.entity.channel.Categorizable;
import org.javacord.api.entity.channel.ChannelCategory;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.MessageFlag;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.entity.permission.PermissionsBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.interaction.InteractionCreateEvent;
import org.javacord.api.interaction.SlashCommand;
import org.javacord.api.interaction.SlashCommandOption;
import org.javacord.api.interaction.SlashCommandOptionType;
import org.javacord.api.listener.interaction.InteractionCreateListener;

import java.awt.*;

public class AddCommand implements InteractionCreateListener {

    @Override
    public void onInteractionCreate(InteractionCreateEvent interactionCreateEvent) {
        if(interactionCreateEvent.getSlashCommandInteraction().isEmpty()) return;
        if(!interactionCreateEvent.getSlashCommandInteraction().get().getCommandName().equalsIgnoreCase("add")) return;
        ChannelCategory category = CopperGolem.getInstance().getServer().getChannelCategoryById(CopperGolem.getInstance().getProperties().getProperty("TicketCategory")).orElseThrow();
        TextChannel serverChannel = interactionCreateEvent.getInteraction().getChannel().orElseThrow();
        Categorizable categorizable = serverChannel.asCategorizable().orElseThrow();
        ChannelCategory eventCategory = categorizable.getCategory().orElseThrow();
        if(eventCategory.getId() != category.getId()){
            interactionCreateEvent.getInteraction().createImmediateResponder().setContent("Dieser Command ist nur in der Ticket Kategorie verfügbar!").setFlags(MessageFlag.EPHEMERAL).respond();
            return;
        }
        if(interactionCreateEvent.getSlashCommandInteraction().get().getOptionUserValueByName("Nutzer").isEmpty()){
            interactionCreateEvent.getInteraction().createImmediateResponder().setContent("Bitte gib einen nutzer an!").setFlags(MessageFlag.EPHEMERAL).respond();
            return;
        }
        User userToAdd = interactionCreateEvent.getSlashCommandInteraction().get().getOptionUserValueByName("Nutzer").get();
        serverChannel.asServerTextChannel().orElseThrow().createUpdater()
                .addPermissionOverwrite(userToAdd, new PermissionsBuilder().setAllowed(PermissionType.VIEW_CHANNEL, PermissionType.SEND_MESSAGES, PermissionType.EMBED_LINKS, PermissionType.ADD_REACTIONS, PermissionType.ATTACH_FILE).build())
                .update();
        interactionCreateEvent.getInteraction().createImmediateResponder().setFlags(MessageFlag.EPHEMERAL).setContent("Der Nutzer wurde hinzugefügt!").respond();
        String userName = CopperGolem.getInstance().getServer().getDisplayName(userToAdd);
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Support")
                .setDescription("Der Nutzer " + userName + " wurde zum Ticket hinzugefügt.")
                .setFooter(CopperGolem.getInstance().getAPI().getYourself().getName() + " by Petropia")
                .setColor(Color.GREEN);
        new MessageBuilder().append(userToAdd).addEmbed(embed).send(serverChannel);
    }

    public static void init(){
        SlashCommand command = SlashCommand.with("add", "Fügt einen Nutzer zum Ticket hinzu")
                .addOption(SlashCommandOption.create(SlashCommandOptionType.USER, "Nutzer", "Der Nutzer der hinzugefügt werden soll", true))
                .createForServer(CopperGolem.getInstance().getServer()).join();
    }

}
