package com.prosc.fmpjdbc;

import com.prosc.io.IOUtils;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author sbarnum
 */
public class ErrorCodeSpider {
	private static final Logger log = Logger.getLogger(ErrorCodeSpider.class.getName());

	final String lang;

	public ErrorCodeSpider(final String lang) {
		this.lang = lang;
	}

	public static void main(String[] args) throws Exception {
		String[] langs =new String[] {"de",
				"en",
				"es",
				"fr",
				"it",
				"ja",
				"nl"
		};

		final Properties errorMessages = new Properties();
		InputStream stream = FileMakerException.class.getResourceAsStream("ErrorCodes.txt");
		errorMessages.load(stream);
		stream.close();

		for (String eachLang : langs) {
			try {
				Properties properties = new ErrorCodeSpider(eachLang).call();
				boolean isEnglish = false;
				for (Map.Entry<Object, Object> eachOverride : errorMessages.entrySet()) {
					isEnglish = "en".equalsIgnoreCase(eachLang);
					if (!properties.containsKey(eachOverride.getKey().toString()) || isEnglish) {
						properties.setProperty(eachOverride.getKey().toString(), eachOverride.getValue().toString().replace("\"", ""));
					}
				}
				String pathWithCode = "/Users/sbarnum/Projects/woof/Fmp360_JDBC/src/com/prosc/fmpjdbc/errorcodes_" + eachLang + ".properties";
				String pathWithoutCode = "/Users/sbarnum/Projects/woof/Fmp360_JDBC/src/com/prosc/fmpjdbc/errorcodes.properties";
				String pathToWrite = isEnglish ? pathWithoutCode : pathWithCode;
				log.log(Level.INFO, "Writing " + properties.size() + " values to " + pathToWrite);
				properties.store(new FileOutputStream(pathToWrite), "FileMaker Error Codes, Generated " + DateFormat.getDateTimeInstance().format(new Date()));
				if (isEnglish) {
					new Properties().store(new FileOutputStream(pathWithCode), "Empty codes, so we can explicitly ask for english translation, Generated " + DateFormat.getDateTimeInstance().format(new Date()));
				}
			} catch (Exception e) {
				log.log(Level.SEVERE, "Could not get error codes for " + eachLang + ": " + e.getMessage(), e);
			}
		}

	}

	public Properties call() throws Exception {
		String url = "http://www.filemaker.com/help/13/fmp/";
		URLConnection urlConnection = new URL(url + lang + "/html/error_codes.html").openConnection();
		String html = IOUtils.urlConnectionAsString(urlConnection);
		html = html.substring(html.indexOf("<tr"));
		String[] rows = html.split("<tr>");
		Pattern p = Pattern.compile(
				"<a name=\".+\">([\\-0-9]+)</a>" +
				".*" +
				"<a name=\".*\">([^<]+)</a>", Pattern.DOTALL | Pattern.MULTILINE);
		int count = 0;
		final Properties properties = new Properties();
		for (String eachRow : rows) {
			Matcher matcher = p.matcher(eachRow);
			if (matcher.find()) {
				count++;
				String key = matcher.group(1).trim();
				if (!properties.containsKey(key)) {
					properties.setProperty(key, matcher.group(2).trim());
				}
			}
		}
		return properties;
	}
}
