package fr.unikaz.unibot;

import java.awt.*;
import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public abstract class AConfig {

	public final void load(String filename) {
		Properties prop = new Properties();
		boolean[] missingFields = {false};
		try {
			prop.load(new FileInputStream(new File(filename)));
			getFields().forEach(f -> {
				boolean acc = f.isAccessible();
				if (!acc)
					f.setAccessible(true);
				try {
					Object value = prop.getProperty(f.getName());
					Class targetType = f.getType();
					if (value == null) {
						missingFields[0] = true;
					}

					if (targetType == Boolean.TYPE || targetType == Boolean.class) {
						if (value == null || "".equals(value))
							f.set(this, false);
						else
							f.set(this, Boolean.valueOf(String.valueOf(value)));
						return;
					}
					if (targetType == Character.TYPE || targetType == Character.class) {
						if (value == null || "".equals(value))
							f.set(this, '\0');
						else
							f.set(this, String.valueOf(value).charAt(0));
						return;
					}
					if ("".equals(value)) {
						f.set(this, null);
						return;
					}
					if(value == null){
						return;
					}

					PropertyEditor editor = PropertyEditorManager.findEditor(f.getType());
					editor.setAsText(prop.getProperty(f.getName()));
					f.set(this, editor.getValue());

				} catch (Exception e) {
					System.err.println(e.getMessage());
					e.printStackTrace();
				}finally {
					f.setAccessible(acc);
				}

			});
		} catch (IOException e) {
			System.err.println("Cannot find config file named " + filename);
			System.err.println("Creation of " + filename);
			save(filename);
		}
		if (missingFields[0]) {
			save(filename);
		}
	}

	public final boolean save(String filename) {
		Properties prop = new Properties();
		getFields().forEach(f -> {
			try {
				boolean acc = f.isAccessible();
				f.setAccessible(true);
				if (f.get(this) == null)
					prop.setProperty(f.getName(), "");
				else {
					if (f.getType() == Color.class)
						prop.setProperty(f.getName(),
							((Color) f.get(this)).getRed() + "," + ((Color) f.get(this)).getGreen() + "," + ((Color) f.get(this)).getBlue());
					else
						prop.setProperty(f.getName(), String.valueOf(f.get(this)));
				}
				f.setAccessible(acc);
			} catch (IllegalAccessException e) {
				System.err.println(e.getMessage());
			}
		});
		try {
			prop.store(new FileOutputStream(filename), null);
			return true;
		} catch (IOException e) {
			System.err.println("Cannot store newly created config file. Check your configuration please");
			e.printStackTrace();
			return false;
		}
	}

	private List<Field> getFields() {
		return Arrays.stream(this.getClass().getDeclaredFields())
			.filter(f -> !Modifier.isTransient(f.getModifiers()))
			.collect(Collectors.toList());
	}
}
