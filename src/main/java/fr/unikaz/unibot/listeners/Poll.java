package fr.unikaz.unibot.listeners;


import fr.unikaz.unibot.Main;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.WeakHashMap;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.api.hooks.SubscribeEvent;

public class Poll {
	public static final String KEY = "/poll ";

	public static final WeakHashMap<String, Message> HISTORY = new WeakHashMap<>();
	public static final WeakHashMap<String, Object> LOCKS = new WeakHashMap<>();

	public enum EMOJI {
		ZERO("\u0030\u20e3"),
		ONE("\u0031\u20e3"),
		TWO("\u0032\u20e3"),
		THREE("\u0033\u20e3"),
		FOUR("\u0034\u20e3"),
		FIVE("\u0035\u20e3"),
		SIX("\u0036\u20e3"),
		SEVEN("\u0037\u20e3"),
		EIGHT("\u0038\u20e3"),
		NINE("\u0039\u20e3"),
		;
		final String val;

		EMOJI(String val) {
			this.val = val;
		}

		static String get(int ordinal) {
			return EMOJI.values()[ordinal].val;
		}

		static int get(String val) {
			for (EMOJI value : values()) {
				if (value.val.equals(val))
					return value.ordinal();
			}
			return -1;
		}
	}

	@SubscribeEvent
	public void onMessage(MessageReceivedEvent event) {
		if (!event.getMessage().getContentDisplay().startsWith(KEY)) return;
		List<String> values = new ArrayList<>(Arrays.asList(event
			.getMessage()
			.getContentDisplay()
			.substring(6)
			.split("\"")));
		event.getMessage().delete().queue();
		values.removeIf(s -> s.trim().isEmpty());
		if (values.size() < 2) {
			MessageChannel channel = event.getChannel();
			channel.sendMessage("Not enough arguments. Use like `/poll \"question\" \"something 1\"` minimum.").queue();
			return;
		}
		EmbedBuilder eb = new EmbedBuilder();
		eb.setTitle(values.remove(0), null);
		if (Main.getConf().pollColor != null)
			eb.setColor(Main.getConf().pollColor);
		for (int i = 0; i < values.size(); i++) {
			eb.addField(EMOJI.get(i) + ' ' + values.get(i), "", false);
		}
		new Thread(() -> {
			MessageEmbed embed = eb.build();
			Message message = event.getChannel().sendMessage(embed).complete();
			for (int i = 0; i < values.size(); i++) {
				message.addReaction(EMOJI.get(i)).complete();
			}
			// register in history
			HISTORY.put(message.getGuild().getId() + "-" + message.getChannel().getId() + "-" + message.getId(), message);
		}).start();
	}

	@SubscribeEvent
	public void onReactionAdd(MessageReactionAddEvent event) {
		if (event.getReaction().isSelf()) return;
		String hash = event.getGuild().getId() + '-' + event.getChannel().getId() + '-' + event.getMessageId();
		// Lock to avoid errors
		Object lock = LOCKS.computeIfAbsent(hash, k -> new Object());
		synchronized (lock) {
			// We try to get the message from the local history to avoid calling the API everytime
			Message message = HISTORY.get(hash);
			if (message == null)
				event.getChannel().getHistory().getMessageById(event.getMessageId());
			if (message == null) { // In the worst case, we call the API ^^'
				message = event.getChannel().retrieveMessageById(event.getMessageId()).complete();
				// store in the history
				HISTORY.put(hash, message);
			}
			if (!Main.getJda().getSelfUser().equals(message.getAuthor())) return; // if the bot is not the author
			MessageEmbed embed = message.getEmbeds().get(0);
			EmbedBuilder eb2 = new EmbedBuilder();
			eb2.setTitle(embed.getTitle());
			eb2.setDescription(embed.getDescription());
			eb2.setColor(embed.getColor());
			int vote = EMOJI.get(event.getReaction().getReactionEmote().getEmoji());
			for (int i = 0; i < embed.getFields().size(); i++) {
				MessageEmbed.Field field = embed.getFields().get(i);
				String value = field.getValue();
				if (i == vote)
					value += " " + event.getUser().getAsMention();
				eb2.addField(field.getName(), value, false);
			}
			embed = eb2.build();
			Message m = event.getChannel().editMessageById(message.getId(), embed).complete();
			HISTORY.put(hash, m);
		}
	}

	@SubscribeEvent
	public void onReactionRemove(MessageReactionRemoveEvent event) {
		if (event.getReaction().isSelf()) return;
		String hash = event.getGuild().getId() + '-' + event.getChannel().getId() + '-' + event.getMessageId();
		// Lock to avoid errors
		Object lock = LOCKS.computeIfAbsent(hash, k -> new Object());
		synchronized (lock) {
			// We try to get the message from the local history to avoid calling the API everytime
			Message message = HISTORY.get(hash);
			if (message == null)
				event.getChannel().getHistory().getMessageById(event.getMessageId());
			if (message == null) { // In the worst case, we call the API ^^'
				message = event.getChannel().retrieveMessageById(event.getMessageId()).complete();
				// store in the history
				HISTORY.put(hash, message);
			}
			if (!Main.getJda().getSelfUser().equals(message.getAuthor())) return; // if the bot is not the author
			MessageEmbed embed = message.getEmbeds().get(0);
			EmbedBuilder eb2 = new EmbedBuilder();
			eb2.setTitle(embed.getTitle());
			eb2.setDescription(embed.getDescription());
			eb2.setColor(embed.getColor());
			int vote = EMOJI.get(event.getReaction().getReactionEmote().getEmoji());
			for (int i = 0; i < embed.getFields().size(); i++) {
				MessageEmbed.Field field = embed.getFields().get(i);
				String value = field.getValue();
				if (i == vote) {
					List<String> mentions = new ArrayList<>(Arrays.asList(value.split(" ")));
					mentions.removeIf(v -> v.trim().isEmpty());
					mentions.removeIf(v -> event.getUser().getAsMention().equals(v));
					value = mentions.isEmpty() ? "" : String.join(" ", mentions);
				}
				eb2.addField(field.getName(), value, false);
			}
			embed = eb2.build();
			Message m = event.getChannel().editMessageById(message.getId(), embed).complete();
			HISTORY.put(hash, m);
		}
	}
}
