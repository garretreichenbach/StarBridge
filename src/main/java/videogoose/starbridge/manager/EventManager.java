package videogoose.starbridge.manager;

import api.event.EventResult;
import api.event.faction.FactionCreateListener;
import api.event.faction.FactionRelationChangeListener;
import api.event.lifecycle.ServerCrashListener;
import api.event.player.*;
import api.event.world.SystemNameGetListener;
import videogoose.starbridge.StarBridge;
import videogoose.starbridge.bot.MessageType;
import videogoose.starbridge.error.ErrorManager;

/**
 * Manager class for handling events.
 */
public class EventManager {

	public static void initialize(StarBridge instance) {
		ServerCrashListener.TYPE.register((throwable, context, fatal, crashReportPath, isServer) -> {
			MessageType.SERVER_CRASHED.sendMessage(throwable.getClass().getSimpleName());
			ErrorManager.reportFatal("The server has crashed! Crash report saved at: " + crashReportPath + "\nContext: " + context, throwable);
		}, instance);

		SystemNameGetListener.TYPE.register((galaxy, name, pos, isServer) -> {
			pos.add(-64, -64, -64);
			String custom = ConfigManager.getSystemNamesConfig().getString(pos.toString());
			return custom != null ? custom : name;
		}, instance);

		// Fires before every command (engine ones included) has run and after its permission check; never vetoes.
		PlayerCustomCommandListener.TYPE.register((command, ctx) -> {
			StarBridge.getBot().onPlayerCommand(command, ctx);
			return true;
		}, instance);

		PlayerChatListener.TYPE.register((message, channelRouter, isServer) -> {
			if(isServer) StarBridge.getBot().onPlayerChat(message);
			return EventResult.CONTINUE;
		}, instance);

		PlayerJoinWorldListener.TYPE.register((client, playerState, isServer) -> {
			if(isServer) StarBridge.getBot().onPlayerJoinWorld(client, playerState);
		}, instance);

		PlayerLeaveWorldListener.TYPE.register((playerName, factionId, sector, isServer) -> {
			if(isServer) MessageType.PLAYER_LEAVE.sendMessage(playerName);
		}, instance);

		FactionCreateListener.TYPE.register((faction, player, isServer) -> {
			if(isServer) StarBridge.getBot().onFactionCreate(faction, player);
			return EventResult.CONTINUE;
		}, instance);

		PlayerJoinFactionListener.TYPE.register((context, isServer) -> {
			if(isServer) StarBridge.getBot().onPlayerJoinFaction(context);
			return EventResult.CONTINUE;
		}, instance);

		PlayerLeaveFactionListener.TYPE.register((faction, player, isServer) -> {
			if(isServer) StarBridge.getBot().onPlayerLeaveFaction(faction, player);
			return EventResult.CONTINUE;
		}, instance);

		PlayerDeathListener.TYPE.register((player, damager, isServer) -> {
			if(isServer) StarBridge.getBot().onPlayerDeath(player, damager);
		}, instance);

		FactionRelationChangeListener.TYPE.register((from, to, oldRelation, newRelation, isServer) -> {
			if(isServer) StarBridge.getBot().onFactionRelationChange(from, to, oldRelation, newRelation);
			return EventResult.CONTINUE;
		}, instance);
	}
}
