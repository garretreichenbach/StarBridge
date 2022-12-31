package thederpgamer.starbridge;

import api.ModPlayground;
import api.listener.EventPriority;
import api.listener.Listener;
import api.listener.events.faction.FactionCreateEvent;
import api.listener.events.faction.FactionRelationChangeEvent;
import api.listener.events.player.*;
import api.listener.events.world.SystemNameGetEvent;
import api.mod.StarLoader;
import api.mod.StarMod;
import api.utils.game.chat.CommandInterface;
import api.utils.other.HashList;
import org.schema.common.util.linAlg.Vector3i;
import thederpgamer.starbridge.bot.StarBot;
import thederpgamer.starbridge.commands.*;
import thederpgamer.starbridge.manager.ConfigManager;
import thederpgamer.starbridge.manager.LogManager;

import java.lang.reflect.Field;

/**
 * Main mod class for StarBridge.
 */
public class StarBridge extends StarMod {
	//Instance
	public static void main(String[] args) { }
	private static StarBridge instance;
	public static StarBridge getInstance() {
		return instance;
	}
	public StarBridge() {
		instance = this;
	}

	@Override
	public void onEnable() {
		instance = this;
		ConfigManager.initialize();
		LogManager.initialize();
		doOverwrites();
		registerListeners();
		registerCommands();
		new StarBot();
	}

	private void doOverwrites() {
		try {
			Field commandsField = StarLoader.class.getDeclaredField("commands");
			commandsField.setAccessible(true);
			HashList<StarMod, CommandInterface> commands = (HashList<StarMod, CommandInterface>) commandsField.get(null);
			commands.getList(ModPlayground.inst).remove(StarLoader.getCommand("help"));
			commands.getList(ModPlayground.inst).add(new HelpDiscordCommand());
			commandsField.set(null, commands);
		} catch(NoSuchFieldException | IllegalAccessException | ClassCastException exception) {
			exception.printStackTrace();
		}
	}

	private void registerListeners() {
		StarLoader.registerListener(SystemNameGetEvent.class, new Listener<SystemNameGetEvent>(EventPriority.LOW) {
			@Override
			public void onEvent(SystemNameGetEvent s) {
				Vector3i pos = s.getPosition();
				pos.add(-64,-64,-64);
				String centerOriginPos = pos.toString();
				String name = ConfigManager.getSystemNamesConfig().getString(centerOriginPos);
				if(name != null) s.setName(name);
			}
		}, this);

		StarLoader.registerListener(PlayerCustomCommandEvent.class, new Listener<PlayerCustomCommandEvent>() {
			@Override
			public void onEvent(PlayerCustomCommandEvent event) {
				getBot().handleEvent(event);
			}
		}, this);

		StarLoader.registerListener(PlayerChatEvent.class, new Listener<PlayerChatEvent>() {
			@Override
			public void onEvent(PlayerChatEvent event) {
				getBot().handleEvent(event);
			}
		}, this);

		StarLoader.registerListener(PlayerJoinWorldEvent.class, new Listener<PlayerJoinWorldEvent>() {
			@Override
			public void onEvent(PlayerJoinWorldEvent event) {
				getBot().handleEvent(event);
			}
		}, this);

		StarLoader.registerListener(PlayerLeaveWorldEvent.class, new Listener<PlayerLeaveWorldEvent>() {
			@Override
			public void onEvent(PlayerLeaveWorldEvent event) {
				getBot().handleEvent(event);
			}
		}, this);

		StarLoader.registerListener(FactionCreateEvent.class, new Listener<FactionCreateEvent>() {
			@Override
			public void onEvent(FactionCreateEvent event) {
				getBot().handleEvent(event);
			}
		}, this);

		StarLoader.registerListener(PlayerJoinFactionEvent.class, new Listener<PlayerJoinFactionEvent>() {
			@Override
			public void onEvent(PlayerJoinFactionEvent event) {
				getBot().handleEvent(event);
			}
		}, this);

		StarLoader.registerListener(PlayerLeaveFactionEvent.class, new Listener<PlayerLeaveFactionEvent>() {
			@Override
			public void onEvent(PlayerLeaveFactionEvent event) {
				getBot().handleEvent(event);
			}
		}, this);

		StarLoader.registerListener(PlayerDeathEvent.class, new Listener<PlayerDeathEvent>() {
			@Override
			public void onEvent(PlayerDeathEvent event) {
				getBot().handleEvent(event);
			}
		}, this);

		StarLoader.registerListener(FactionRelationChangeEvent.class, new Listener<FactionRelationChangeEvent>() {
			@Override
			public void onEvent(FactionRelationChangeEvent event) {
				getBot().handleEvent(event);
			}
		}, this);
	}

	private void registerCommands() {
		CommandInterface[] commands = new CommandInterface[] {
				new ListCommand(),
				new LinkCommand(),
				new ClearDataCommand(),
				new InfoPlayerCommand(),
				new InfoFactionCommand(),
				new RenameSystemCommand(),
				new SetExemptCommand()
		};

		for(CommandInterface commandInterface : commands) StarLoader.registerCommand(commandInterface);
	}

	public static StarBot getBot() {
		return StarBot.getInstance();
	}
}
