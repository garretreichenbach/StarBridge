package thederpgamer.starbridge.commands;

import api.common.GameServer;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.internal.interactions.CommandDataImpl;
import net.dv8tion.jda.internal.utils.PermissionUtil;
import thederpgamer.starbridge.bot.StarBot;
import thederpgamer.starbridge.data.exception.ExceptionData;

import java.util.Locale;
import java.util.Objects;

/**
 * [Description]
 *
 * @author TheDerpGamer (MrGoose#0027)
 */
public class EditExceptionDataCommand implements DiscordCommand {
	@Override
	public void execute(SlashCommandInteractionEvent event) {
		try {
			if(!PermissionUtil.checkPermission(Objects.requireNonNull(event.getMember()), Permission.ADMINISTRATOR)) event.reply("You do not have permission to use this command!").setEphemeral(true).queue();
			else {
				switch(Objects.requireNonNull(event.getOption("argument")).getAsString().toLowerCase(Locale.ENGLISH)) {
					case "list":
						StringBuilder builder = new StringBuilder();
						for(ExceptionData exceptionData : ExceptionData.getExceptions()) builder.append(exceptionData.getName()).append(" [").append(exceptionData.getSeverity()).append("] - ").append(exceptionData.getDescription()).append("\n");
						event.reply(builder.toString()).setEphemeral(true).queue();
						break;
					case "edit":
						for(ExceptionData exceptionData : ExceptionData.getExceptions()) {
							if(exceptionData.getName().toLowerCase(Locale.ENGLISH).equals(event.getOption("exception").getAsString().toLowerCase(Locale.ENGLISH))) {
								startEditMode(event, exceptionData);
								event.reply("Editing exception data for " + exceptionData.getName()).setEphemeral(true).queue();
								return;
							}
						}
						break;
				}
			}
		} catch(Exception exception) {
			event.reply("Invalid Arguments").setEphemeral(true).queue();
		}
	}

	private void startEditMode(SlashCommandInteractionEvent event, ExceptionData exceptionData) {
		//Todo: Add edit mode
	}

	@Override
	public CommandData getCommandData() {
		CommandDataImpl commandData = new CommandDataImpl("exception", "Command for viewing and editing known exceptions.");
		OptionData optionData = new OptionData(OptionType.STRING, "argument", "The command argument");
		optionData.addChoice("list", "list");
		optionData.addChoice("edit", "edit");

		OptionData optionData1 = new OptionData(OptionType.STRING, "exception", "The exception to edit");
		commandData.addOptions(optionData1);
		return commandData;
	}
}
