package fr.unikaz.unibot.listeners;


import fr.unikaz.unibot.Emoji;
import fr.unikaz.unibot.Main;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.WeakHashMap;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.api.hooks.SubscribeEvent;

public class Party {
	public static final String KEY = "/party ";

	public static final WeakHashMap<String, Message> HISTORY = new WeakHashMap<>();
	public static final WeakHashMap<String, Object> LOCKS = new WeakHashMap<>();

	@SubscribeEvent
	public void onMessage(MessageReceivedEvent event) {
		if (!event.getMessage().getContentDisplay().startsWith(KEY)) return;
		String value;
		try {
			value = event.getMessage().getContentDisplay().substring(6);
		} catch (Exception e) {
			MessageChannel channel = event.getChannel();
			channel.sendMessage(Main.getConf().partyErrorArguments).queue();
			return;
		}
		event.getMessage().delete().queue();
		EmbedBuilder eb = new EmbedBuilder();
		eb.setTitle(value, null);
		if (Main.getConf().pollColor != null)
			eb.setColor(Main.getConf().pollColor);

		eb.addField(Emoji.WHITE_CHECK_MARK.val + ' ' + Main.getConf().partyTextYes, "", false);
		eb.addField(Emoji.X.val + ' ' + Main.getConf().partyTextNo, "", false);
		eb.addField(Main.getConf().partyTextCounter, "", false);
		eb.addField("", Main.getConf().partyText, false);


		new Thread(() -> {
			MessageEmbed embed = eb.build();
			Message message = event.getChannel().sendMessage(embed).complete();
			message.addReaction(Emoji.WHITE_CHECK_MARK.val).complete();
			message.addReaction(Emoji.X.val).complete();
			message.addReaction(Emoji.ONE.val).complete();
			message.addReaction(Emoji.TWO.val).complete();
			message.addReaction(Emoji.THREE.val).complete();
			// register in history
			HISTORY.put(message.getGuild().getId() + "-" + message.getChannel().getId() + "-" + message.getId(), message);
		}).start();
	}

	@SubscribeEvent
	public void onReactionAdd(MessageReactionAddEvent event) {
		if (event.getReaction().isSelf()) return;
		System.out.println(event.getReaction().getReactionEmote().getEmoji());
		if(true) return;
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
			//todo Check if its V/X or digit
			// if V... ok
			// if digit Check if V is present or remove digit
			// if X check if V and digits and remove them
			// recalculate total
			MessageEmbed embed = message.getEmbeds().get(0);
			EmbedBuilder eb2 = new EmbedBuilder();
			eb2.setTitle(embed.getTitle());
			eb2.setDescription(embed.getDescription());
			eb2.setColor(embed.getColor());
			int vote = Emoji.getDigitValue(event.getReaction().getReactionEmote().getEmoji());
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
		if(true) return;
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
			//todo Check if its V/X or digit
			// if V Check if he comes with someone and remove the digit
			// recalculate total

			MessageEmbed embed = message.getEmbeds().get(0);
			EmbedBuilder eb2 = new EmbedBuilder();
			eb2.setTitle(embed.getTitle());
			eb2.setDescription(embed.getDescription());
			eb2.setColor(embed.getColor());
			int vote = Emoji.getDigitValue(event.getReaction().getReactionEmote().getEmoji());
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

	private int calculateTotal(Message message){
		int total = -7; // to remove bot reactions
		for (MessageReaction reaction : message.getReactions()) {
			if(!reaction.getReactionEmote().isEmoji()) continue;
			String e = reaction.getReactionEmote().getEmoji();
			if(Emoji.WHITE_CHECK_MARK.val.equals(e))
				total++;
			for (int i = 0; i < Emoji.DIGITS.length; i++) {
				if(Emoji.DIGITS[i].val.equals(e))
					total+=i;
			}
		}
		return total;
	}
}
