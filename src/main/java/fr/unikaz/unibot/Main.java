package fr.unikaz.unibot;

import fr.unikaz.unibot.listeners.Party;
import fr.unikaz.unibot.listeners.Poll;

import javax.security.auth.login.LoginException;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.hooks.AnnotatedEventManager;

public class Main {
	private static JDA jda;
	private static Config conf = new Config("conf.cfg");

	public static void main(String[] args) throws LoginException {
		jda = new JDABuilder(conf.token)
			.setEventManager(new AnnotatedEventManager())
			.build();
		if (conf.poll)
			jda.addEventListener(new Poll());
		if (conf.party)
			jda.addEventListener(new Party());
	}

	public static JDA getJda() {
		return jda;
	}

	public static Config getConf() {
		return conf;
	}
}
