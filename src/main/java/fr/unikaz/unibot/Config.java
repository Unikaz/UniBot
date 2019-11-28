package fr.unikaz.unibot;

import java.awt.*;

public class Config extends AConfig {

	// Main config
	public String token = "Place your token here !"; // here in the config file, not in the code ;)
	// Poll command
	public boolean poll = false;
	public Color pollColor = Color.red; // this is not working well, use pattern "rrr,ggg,bbb" in config
	// Party command
	public boolean party = true;
	public Color partyColor = Color.red; // this is not working well, use pattern "rrr,ggg,bbb" in config
	public String partyTextYes = "Yes !";
	public String partyTextNo = "No";
	public String partyTextCounter = "Total";
	public String partyText = "Click on the numbers in reactions to inform how many guests will accompany you.\n You " +
		"have to check for yourself before adding guests.";
	public String partyErrorArguments = "Not enough arguments. Use like `/party some information on the event` minimum.";

	public Config(String filename) { load(filename); }
}
