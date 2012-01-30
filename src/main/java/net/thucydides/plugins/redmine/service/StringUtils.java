package net.thucydides.plugins.redmine.service;

public class StringUtils {

	private StringUtils() {
		super();
	}
	
	public static boolean hasText(String text) {
		return text != null && !"".equals(text.trim());
	}
	
}
