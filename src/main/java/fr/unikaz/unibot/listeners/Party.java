package fr.unikaz.unibot.listeners;


import fr.unikaz.unibot.Emoji;
import fr.unikaz.unibot.Main;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.stream.Collectors;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.IMentionable;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.GenericMessageReactionEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.api.hooks.SubscribeEvent;
import net.dv8tion.jda.api.requests.restaction.pagination.ReactionPaginationAction;

public class Party {
	public static final String KEY = "/party ";

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
		new Thread(() -> {
			MessageEmbed embed = rebuildEmbed(value, " ", " ", 0);
			Message message = event.getChannel().sendMessage(embed).complete();
			message.addReaction(Emoji.WHITE_CHECK_MARK.val).complete();
			message.addReaction(Emoji.X.val).complete();
			message.addReaction(Emoji.ONE.val).complete();
			message.addReaction(Emoji.TWO.val).complete();
			message.addReaction(Emoji.THREE.val).complete();
		}).start();
	}

	@SubscribeEvent
	public void onReactionAdd(MessageReactionAddEvent event) {
		handleReactionEvent(event);
	}

	@SubscribeEvent
	public void onReactionRemove(MessageReactionRemoveEvent event) {
		handleReactionEvent(event);
	}

	private void handleReactionEvent(GenericMessageReactionEvent event) {
		if (event.getReaction().isSelf()) return;
		String hash = event.getGuild().getId() + '-' + event.getChannel().getId() + '-' + event.getMessageId();
		// Lock to avoid errors
		Object lock = LOCKS.computeIfAbsent(hash, k -> new Object());
		synchronized (lock) {
			// We try to get the message from the local history to avoid calling the API everytime
			Message message = event.getChannel().getHistory().getMessageById(event.getMessageId());
			if (message == null) { // In the worst case, we call the API ^^'
				message = event.getChannel().retrieveMessageById(event.getMessageId()).complete();
			}
			if (!Main.getJda().getSelfUser().equals(message.getAuthor())) return; // if the bot is not the author
			MessageReaction reaction = event.getReaction();
			if (!reaction.getReactionEmote().isEmoji()) return;
			String e = reaction.getReactionEmote().getEmoji();
			if (!Emoji.WHITE_CHECK_MARK.val.equals(e) &&
				!Emoji.X.val.equals(e) &&
				Arrays.stream(Emoji.DIGITS).noneMatch(emoji -> emoji.val.equals(e))) {
				// random emoji... don't care
				return;
			}
			Message finalMessage = message;
			new Thread(() -> update(finalMessage)).start();
		}
	}

	private void update(Message message) {
		int total = 0; // to remove bot reactions
		Map<String, Integer> yesMentions = new HashMap<>();
		List<String> noMentions = new ArrayList<>();
		int anonymous = 0;
		for (MessageReaction reaction : message.getReactions()) {
			if (!reaction.getReactionEmote().isEmoji()) continue;
			if (reaction.getCount() == 1) continue; // dodge useless call
			String e = reaction.getReactionEmote().getEmoji();
			if (Emoji.WHITE_CHECK_MARK.val.equals(e)) {
				total += reaction.getCount() - 1;
				yesMentions = reaction.retrieveUsers().stream()
					.filter(u -> !u.isBot())
					.collect(Collectors.toMap(IMentionable::getAsMention, u -> 0));
			} else if (Emoji.X.val.equals(e)) {
				noMentions = reaction.retrieveUsers().stream()
					.filter(u -> !u.isBot())
					.map(IMentionable::getAsMention).collect(Collectors.toList());
			} else {
				for (int i = 0; i < Emoji.DIGITS.length; i++) {
					if (Emoji.DIGITS[i].val.equals(e)) {
						total += i * (reaction.getCount() - 1);
						ReactionPaginationAction users = reaction.retrieveUsers();
						for (User user : users) {
							if (user.isBot()) continue;
							if (yesMentions.containsKey(user.getAsMention())) {
								yesMentions.put(user.getAsMention(), yesMentions.get(user.getAsMention()) + i);
							} else {
								anonymous += i;
							}
						}
					}
				}
			}
		}
		String yes = "";
		if (!yesMentions.isEmpty())
			yes = yesMentions.entrySet().stream()
				.map(e -> e.getKey() + (e.getValue() > 0 ? "(+" + e.getValue() + ")" : ""))
				.collect(Collectors.joining(", "));
		if (anonymous > 0)
			yes += " +" + anonymous;
		MessageEmbed embed = rebuildEmbed(message.getEmbeds().get(0).getTitle(),
			yes,
			!noMentions.isEmpty() ? String.join(", ", noMentions) : " ",
			total);
		message.getChannel().editMessageById(message.getId(), embed).complete();
	}

	private MessageEmbed rebuildEmbed(String title, String yesStr, String noStr, int total) {
		EmbedBuilder eb = new EmbedBuilder();
		eb.setTitle(title, null);
		eb.setColor(Main.getConf().partyColor);
		if (Main.getConf().pollColor != null)
			eb.setColor(Main.getConf().pollColor);
		eb.addField(Emoji.WHITE_CHECK_MARK.val + ' ' + Main.getConf().partyTextYes, yesStr, false);
		eb.addField(Emoji.X.val + ' ' + Main.getConf().partyTextNo, noStr, false);
		eb.addField(Main.getConf().partyTextCounter, total + "", false);
		eb.addField("", Main.getConf().partyText, false);
		return eb.build();
	}
}
