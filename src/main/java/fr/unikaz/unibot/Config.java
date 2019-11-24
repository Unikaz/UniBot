package fr.unikaz.unibot;

import java.awt.*;

public class Config extends AConfig {

	// Members in config
	public String token = "Place your token here !"; // here in the config file, not in the code ;)
	public Color pollColor = Color.red; // this is not working well, use pattern "rrr,ggg,bbb" in config

	public Config(String filename) { load(filename); }
}
