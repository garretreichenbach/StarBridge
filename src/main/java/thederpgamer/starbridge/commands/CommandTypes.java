package thederpgamer.starbridge.commands;

import api.common.GameServer;
import api.mod.StarLoader;
import api.mod.StarMod;
import api.mod.config.PersistentObjectUtil;
import api.utils.game.PlayerUtils;
import api.utils.game.chat.CommandInterface;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.internal.interactions.CommandDataImpl;
import org.jetbrains.annotations.Nullable;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.game.common.data.world.StellarSystem;
import thederpgamer.starbridge.StarBridge;
import thederpgamer.starbridge.bot.DiscordBot;
import thederpgamer.starbridge.data.other.Pair;
import thederpgamer.starbridge.data.permissions.IPermissibleAction;
import thederpgamer.starbridge.data.player.PlayerData;
import thederpgamer.starbridge.manager.ConfigManager;
import thederpgamer.starbridge.server.ServerDatabase;
import thederpgamer.starbridge.ui.SettingsUI;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public enum CommandTypes {
	INFO("info", "Displays information about a player or faction.", new String[]{"/info <player_name>", "/info <faction_name>"}, () -> new Pair<>("starbridge.view_info", true), new ICommandExecutor() {
		@Override
		public boolean executeGame(PlayerState sender, String[] args) {
			if(args.length == 1) {
				try {
					PlayerState playerState = GameServer.getServerState().getPlayerFromNameIgnoreCaseWOException(args[0]);
					if(playerState != null) {
						PlayerData playerData = ServerDatabase.getPlayerDataOrCreateIfNull(playerState.getName());
						PlayerUtils.sendMessage(sender, playerData.toString());
						return true;
					} else {
						List<Faction> factions = new ArrayList<>(GameServer.getServerState().getFactionManager().getFactionCollection());
						Faction faction = factions.stream().filter(f -> f.getName().equalsIgnoreCase(args[0])).findFirst().orElse(null);
						if(faction != null) {
							StringBuilder builder = new StringBuilder();
							builder.append(faction.getName()).append(":\n");
							builder.append("\tMembers: ").append(faction.getMembersUID().size()).append(":\n");
							builder.append("\tHomeBase: ");
							if(faction.getHomeSector() != null) {
								builder.append("(").append(faction.getHomeSector().x).append(", ").append(faction.getHomeSector().y).append(", ").append(faction.getHomeSector().z).append(")");
							} else {
								builder.append("NONE");
							}
							builder.append("\n");
							builder.append("\tDescription: ").append(faction.getDescription());
							PlayerUtils.sendMessage(sender, builder.toString());
							return true;
						} else PlayerUtils.sendMessage(sender, "Player or faction not found: " + args[0]);
						return true;
					}
				} catch(Exception exception) {
					StarBridge.getInstance().logException("Error executing info command", exception);
				}
			}
			return false;
		}

		@Override
		public void executeDiscord(SlashCommandInteractionEvent event) {
			String name = event.getOption("name").getAsString();
			PlayerState playerState = GameServer.getServerState().getPlayerFromNameIgnoreCaseWOException(name);
			if(playerState != null) {
				PlayerData playerData = ServerDatabase.getPlayerDataOrCreateIfNull(playerState.getName());
				event.reply(playerData.toString()).queue();
			} else {
				List<Faction> factions = new ArrayList<>(GameServer.getServerState().getFactionManager().getFactionCollection());
				Faction faction = factions.stream().filter(f -> f.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
				if(faction != null) {
					StringBuilder builder = new StringBuilder();
					builder.append(faction.getName()).append(":\n");
					builder.append("\tMembers: ").append(faction.getMembersUID().size()).append(":\n");
					builder.append("\tHomeBase: ");
					if(faction.getHomeSector() != null) {
						builder.append("(").append(faction.getHomeSector().x).append(", ").append(faction.getHomeSector().y).append(", ").append(faction.getHomeSector().z).append(")");
					} else {
						builder.append("NONE");
					}
					builder.append("\n");
					builder.append("\tDescription: ").append(faction.getDescription());
					event.reply(builder.toString()).queue();
				} else event.reply("Player or faction not found: " + name).queue();
			}
		}

		@Override
		public CommandData getDiscordCommandData() {
			CommandDataImpl commandData = new CommandDataImpl("info", "Displays information about a player or faction.");
			commandData.addOption(OptionType.STRING, "name", "The name of the player or faction", true);
			return commandData;
		}
	}),
	LINK("link", "Links a player's StarMade account with their Discord account.", new String[]{"/link <discord_id>"}, () -> new Pair<>("starbridge.link_account", true), new ICommandExecutor() {
		@Override
		public boolean executeGame(PlayerState sender, String[] args) {
			StarBridge.getBot().addLinkRequest(sender);
			return true;
		}

		@Override
		public void executeDiscord(SlashCommandInteractionEvent event) {
			int linkCode = Objects.requireNonNull(event.getOption("link_code")).getAsInt();
			PlayerData playerData = StarBridge.getBot().getLinkRequest(linkCode);
			if(playerData != null) {
				playerData.setDiscordId(event.getUser().getIdLong());
				ServerDatabase.updatePlayerData(playerData);
				PersistentObjectUtil.save(StarBridge.getInstance().getSkeleton());
				String logMessage = "Successfully linked user " + event.getUser().getName() + " to " + playerData.getPlayerName();
				StarBridge.getBot().removeLinkRequest(playerData);
				event.reply(logMessage).queue();
				StarBridge.getInstance().logInfo(logMessage);
				event.getHook().deleteOriginal().queue();
			}
		}

		@Override
		public CommandData getDiscordCommandData() {
			CommandDataImpl commandData = new CommandDataImpl("link", "Links your Discord and StarMade accounts using the provided link code");
			commandData.addOption(OptionType.INTEGER, "link_code", "The link code to use", true);
			return commandData;
		}
	}),
	LIST("list", "Lists the specified server info", new String[]{"/list players", "/list staff"}, () -> new Pair<>("starbridge.list_info", true), new ICommandExecutor() {
		@Override
		public boolean executeGame(PlayerState sender, String[] args) {
			String command = null;
			if(args == null || args.length == 0) {
				command = "list players";
			} else if("players".equalsIgnoreCase(args[0]) || "p".equalsIgnoreCase(args[0])) {
				command = "list players";
			} else if("staff".equalsIgnoreCase(args[0]) || "s".equalsIgnoreCase(args[0])) {
				command = "list staff";
			}
			if(command != null) {
				switch(command) {
					case "list players":
						StringBuilder playerBuilder = new StringBuilder();
						for(PlayerState playerState : GameServer.getServerState().getPlayerStatesByName().values()) {
							playerBuilder.append(playerState.getName()).append(" [").append(playerState.getFactionName()).append("]\n");
						}
						PlayerUtils.sendMessage(sender, "Current Online Players:\n" + playerBuilder.toString().trim());
						return true;
					case "list staff":
						StringBuilder staffBuilder = new StringBuilder();
						for(PlayerState playerState : GameServer.getServerState().getPlayerStatesByName().values()) {
							if(playerState.isAdmin()) {
								staffBuilder.append(playerState.getName()).append(" [").append(playerState.getFactionName()).append("]\n");
							}
						}
						PlayerUtils.sendMessage(sender, "Current Online Staff:\n" + staffBuilder.toString().trim());
						return true;
				}
			}
			return false;
		}

		@Override
		public void executeDiscord(SlashCommandInteractionEvent event) {
			String message = event.getCommandString().trim().replace("/", " ").toLowerCase();
			String type = "players";
			if(event.getOption("list") != null) {
				type = Objects.requireNonNull(event.getOption("list")).getAsString();
			}
			StringBuilder builder = new StringBuilder();
			if("players".equalsIgnoreCase(type)) {
				try {
					builder.append("Current Online Players:\n");
					for(PlayerState playerState : GameServer.getServerState().getPlayerStatesByName().values()) {
						builder.append(playerState.getName()).append(" [").append(playerState.getFactionName()).append("]\n");
					}
				} catch(NullPointerException exception) {
					StarBridge.getInstance().logWarning("Encountered a NullPointerException while trying to fetch list of online players! This is most likely due to there being no players currently online.");
					builder = new StringBuilder();
					builder.append("There are no players currently online.");
				}
				event.reply(builder.toString().trim()).queue();
			} else if("staff".equalsIgnoreCase(type) || "admins".equalsIgnoreCase(type)) {
				try {
					builder.append("Current Online Staff:\n");
					for(PlayerState playerState : GameServer.getServerState().getPlayerStatesByName().values()) {
						if(playerState.isAdmin()) {
							builder.append(playerState.getName()).append(" [").append(playerState.getFactionName()).append("]\n");
						}
					}
				} catch(NullPointerException exception) {
					StarBridge.getInstance().logWarning("Encountered a NullPointerException while trying to fetch list of online staff! This is most likely due to there being no staff currently online.");
					builder = new StringBuilder();
					builder.append("There are no staff currently online.");
				}
				event.reply(builder.toString().trim()).queue();
			} else event.reply("Incorrect usage \"/" + message + "\".\nUsage: /list players or /list staff").queue();
		}

		@Override
		public CommandData getDiscordCommandData() {
			CommandDataImpl commandData = new CommandDataImpl("list", "Lists the specified server info");
			OptionData optionData = new OptionData(OptionType.STRING, "list", "The data to list");
			optionData.addChoice("players", "players");
			optionData.addChoice("staff", "staff");
			commandData.addOptions(optionData);
			return commandData;
		}
	}),
	RENAME_SYSTEM("rename_system", "Renames the specified System", new String[]{"/rename_system"}, () -> new Pair<>("rename_system", true), new ICommandExecutor() {
		@Override
		public boolean executeGame(PlayerState sender, String[] args) {
			try {
				StellarSystem stellarSystem = GameServer.getServerState().getUniverse().getStellarSystemFromStellarPos(sender.getCurrentSystem());
				Vector3i pos = stellarSystem.getPos();
				pos.add(-64, -64, -64);
				String centerOriginPos = pos.toString();
				PlayerData playerData = PlayerData.getFromName(sender.getName());
				if(playerData != null) {
					boolean hasPermission = (boolean) playerData.getPermission("rename_system");
					if(stellarSystem.getOwnerFaction() != sender.getFactionId() && !sender.isAdmin()) {
						api.utils.game.PlayerUtils.sendMessage(sender, "You do not have permission to rename this system!");
						return false;
					}
					if(!hasPermission && !ConfigManager.getMainConfig().getBoolean("debug-mode")) {
						api.utils.game.PlayerUtils.sendMessage(sender, "You do not have permission to rename systems!");
						return false;
					}
					if(args[0].length() > 32) {
						api.utils.game.PlayerUtils.sendMessage(sender, "System name cannot be longer than 32 characters!");
						return false;
					}
					ConfigManager.getSystemNamesConfig().set(centerOriginPos, args[0]);
					api.utils.game.PlayerUtils.sendMessage(sender, "System name has been changed to: " + args[0] + ", will take effect on next server restart.");
				}
			} catch(Exception exception) {
				StarBridge.getInstance().logException("Error executing rename_system command", exception);
				return false;
			}
			return true;
		}

		@Override
		public void executeDiscord(SlashCommandInteractionEvent event) {
			throw new UnsupportedOperationException("This command can only be executed in-game!");
		}

		@Override
		public CommandData getDiscordCommandData() {
			return null;
		}
	}),
	SETTINGS_COMMAND("settings", "Opens the settings menu", new String[]{"/settings"}, () -> new Pair<>("starbridge.admin.settings", true), new ICommandExecutor() {
		@Override
		public boolean executeGame(PlayerState sender, String[] args) {
			return false;
		}

		@Override
		public void executeDiscord(SlashCommandInteractionEvent event) {
			event.reply(new SettingsUI(event.getMember(), event.getChannel()).toMessage().build()).setEphemeral(true).queue();
		}

		@Override
		public CommandData getDiscordCommandData() {
			return new CommandDataImpl("settings", "Opens the settings menu");
		}
	});

	public final IPermissibleAction permission;
	private final String command;
	private final String[] aliases;
	private final String description;
	private final String[] usages;
	private final ICommandExecutor executor;
	private final CommandInterface commandInterface;

	CommandTypes(String command, String[] aliases, String description, String[] usages, IPermissibleAction permission, ICommandExecutor executor) {
		this.command = command;
		this.aliases = aliases;
		this.description = description;
		this.usages = usages;
		this.permission = permission;
		this.executor = executor;
		commandInterface = new CommandInterface() {
			@Override
			public String getCommand() {
				return command;
			}

			@Override
			public String[] getAliases() {
				return aliases;
			}

			@Override
			public String getDescription() {
				return description;
			}

			@Override
			public boolean isAdminOnly() {
				return permission.isAdminOnly();
			}

			@Override
			public boolean onCommand(PlayerState playerState, String[] strings) {
				return executor.executeGame(playerState, strings);
			}

			@Override
			public void serverAction(@Nullable PlayerState playerState, String[] strings) {
				executor.executeGame(playerState, strings);
			}

			@Override
			public StarMod getMod() {
				return StarBridge.getInstance();
			}
		};
	}

	CommandTypes(String command, String description, String[] usages, IPermissibleAction permission, ICommandExecutor executor) {
		this(command, new String[]{command}, description, usages, permission, executor);
	}

	public static CommandTypes getFromName(String name) {
		for(CommandTypes commandType : values()) {
			if(commandType.command.equalsIgnoreCase(name) || commandType.aliases != null && commandType.aliases.length > 0 && commandType.aliases[0].equalsIgnoreCase(name)) {
				return commandType;
			}
		}
		return null;
	}

	public static void registerGameCommands() {
		for(CommandTypes commandType : values()) {
			CommandInterface command = commandType.commandInterface;
			if(command != null) {
				StarLoader.registerCommand(command);
				StarBridge.getInstance().logInfo("Registered Game command: " + commandType.command);
			}
		}
	}

	public static void registerDiscordCommands(DiscordBot bot) {
		for(CommandTypes commandType : values()) {
			CommandData commandData = commandType.executor.getDiscordCommandData();
			if(commandData != null) {
				bot.getGuild().upsertCommand(commandType.executor.getDiscordCommandData()).queue();
				StarBridge.getInstance().logInfo("Registered Discord command: " + commandType.command);
			}
		}
	}

	public String getCommand() {
		return command;
	}

	public String getDescription() {
		return description;
	}

	public String[] getAliases() {
		return aliases;
	}

	public String[] getUsages() {
		return usages;
	}

	public Pair<String, Object> getPermission() {
		return permission.getRequiredPermission();
	}

	public void execute(PlayerState sender, String[] args) {
		executor.executeGame(sender, args);
	}

	public CommandInterface getGameCommand() {
		return commandInterface;
	}
}