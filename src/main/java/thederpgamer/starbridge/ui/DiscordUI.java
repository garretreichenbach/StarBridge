package thederpgamer.starbridge.ui;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonInteraction;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import thederpgamer.starbridge.StarBridge;

import java.util.HashMap;
import java.util.Objects;

public abstract class DiscordUI {

	protected final ActionRow row;
	private final HashMap<ItemComponent, InteractionCallback> components = new HashMap<>();

	protected DiscordUI(Member member, Channel channel) {
		createUI(member, channel);
		row = ActionRow.of(components.keySet());
		StarBridge.getBot().registerUI(this);
	}

	protected abstract void createUI(Member member, Channel channel);

	public abstract MessageCreateBuilder toMessage();

	public void addComponent(ItemComponent component, InteractionCallback callback) {
		components.put(component, callback);
	}

	public HashMap<ItemComponent, InteractionCallback> getComponents() {
		return components;
	}

	public boolean hasComponent(String componentId) {
		return getComponent(componentId) != null;
	}

	public ItemComponent getComponent(String componentId) {
		for(ItemComponent component : components.keySet()) {
			if(component instanceof Button) {
				if(Objects.equals(((Button) component).getId(), componentId)) {
					return component;
				}
			} else if(component instanceof SelectMenu) {
				if(Objects.equals(((SelectMenu) component).getId(), componentId)) {
					return component;
				}
			}
		}
		return null;
	}

	public void handleInteraction(String componentId, ButtonInteraction interaction) {
		ItemComponent component = getComponent(componentId);
		if(component != null) {
			InteractionCallback callback = components.get(component);
			if(callback != null) callback.onInteraction(interaction);
		}
	}
}
