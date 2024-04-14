package thederpgamer.starbridge.manager;

import api.listener.EventPriority;
import api.listener.Listener;
import api.listener.events.faction.FactionCreateEvent;
import api.listener.events.faction.FactionRelationChangeEvent;
import api.listener.events.player.*;
import api.listener.events.world.SystemNameGetEvent;
import api.mod.StarLoader;
import org.schema.common.util.linAlg.Vector3i;
import thederpgamer.starbridge.StarBridge;

/**
 * [Description]
 *
 * @author Garret Reichenbach
 */
public class EventManager {

	public static void initialize(StarBridge instance) {
		StarLoader.registerListener(SystemNameGetEvent.class, new Listener<SystemNameGetEvent>(EventPriority.LOW) {
			@Override
			public void onEvent(SystemNameGetEvent s) {
				Vector3i pos = s.getPosition();
				pos.add(-64,-64,-64);
				String centerOriginPos = pos.toString();
				String name = ConfigManager.getSystemNamesConfig().getString(centerOriginPos);
				if(name != null) s.setName(name);
			}
		}, instance);

		StarLoader.registerListener(PlayerCustomCommandEvent.class, new Listener<PlayerCustomCommandEvent>() {
			@Override
			public void onEvent(PlayerCustomCommandEvent event) {
				instance.handleEvent(event);
			}
		}, instance);

		StarLoader.registerListener(PlayerChatEvent.class, new Listener<PlayerChatEvent>() {
			@Override
			public void onEvent(PlayerChatEvent event) {
				instance.handleEvent(event);
			}
		}, instance);

		StarLoader.registerListener(PlayerJoinWorldEvent.class, new Listener<PlayerJoinWorldEvent>() {
			@Override
			public void onEvent(PlayerJoinWorldEvent event) {
				instance.handleEvent(event);
			}
		}, instance);

		StarLoader.registerListener(PlayerLeaveWorldEvent.class, new Listener<PlayerLeaveWorldEvent>() {
			@Override
			public void onEvent(PlayerLeaveWorldEvent event) {
				instance.handleEvent(event);
			}
		}, instance);

		StarLoader.registerListener(FactionCreateEvent.class, new Listener<FactionCreateEvent>() {
			@Override
			public void onEvent(FactionCreateEvent event) {
				instance.handleEvent(event);
			}
		}, instance);

		StarLoader.registerListener(PlayerJoinFactionEvent.class, new Listener<PlayerJoinFactionEvent>() {
			@Override
			public void onEvent(PlayerJoinFactionEvent event) {
				instance.handleEvent(event);
			}
		}, instance);

		StarLoader.registerListener(PlayerLeaveFactionEvent.class, new Listener<PlayerLeaveFactionEvent>() {
			@Override
			public void onEvent(PlayerLeaveFactionEvent event) {
				instance.handleEvent(event);
			}
		}, instance);

		StarLoader.registerListener(PlayerDeathEvent.class, new Listener<PlayerDeathEvent>() {
			@Override
			public void onEvent(PlayerDeathEvent event) {
				instance.handleEvent(event);
			}
		}, instance);

		StarLoader.registerListener(FactionRelationChangeEvent.class, new Listener<FactionRelationChangeEvent>() {
			@Override
			public void onEvent(FactionRelationChangeEvent event) {
				instance.handleEvent(event);
			}
		}, instance);
	}
}
