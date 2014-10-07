package com.prosc.fmpjdbc.util;

import org.jetbrains.annotations.NotNull;

/**
 * Created by IntelliJ IDEA. User: jesse Date: 4/19/13 Time: 4:50 PM
 */
public enum Platform {	
	mac(true), windows(false), linux(true), unknown(true);

	/** Contains the current platform, ie. mac / windows / linux */
	public static final Platform current;
	
	public final boolean unixLike;

	Platform( boolean unixLike ) {
		this.unixLike = unixLike;
	}

	static {
		final String osName = System.getProperty( "os.name" );
		if( osName.startsWith( "Mac" ) ) current = mac;
		else if( osName.startsWith( "Windows" ) ) current = windows;
		else if( osName.startsWith( "Linux" ) ) current = linux;
		else current = unknown;
	}

	public static boolean isMac() { return current == mac; }
	public static boolean isWin() { return current == windows; }

	/**
	 * Takes a path from some unknown <code>Platform</code> and translates it to this <code>Platform</code>'s path style.
	 * @param pathFromOtherPlatform some sort of path
	 * @return a nice path suitable for this <code>Platform</code>.
	 */
	public String pathTranslated(@NotNull String pathFromOtherPlatform) {
		Platform origin = Platform.analyzePath(pathFromOtherPlatform);
		if (origin == this) return pathFromOtherPlatform; // same platform
		switch (origin) {
			case unknown:
				return pathFromOtherPlatform;
			case mac:
			case linux:
				if (this == windows) {
					// mac->win, convert backslashes to colons, then forward slashes to backslashes
					final String backslashed = pathFromOtherPlatform.replace("\\", ":").replace("/", "\\");
					return backslashed.substring(1).replaceFirst("\\\\", ":\\\\");
				} else {
					return pathFromOtherPlatform;
				}

			case windows:
				// win->mac, convert forward slashes to colons, then backslashes to forward slashes
				return "/" +  pathFromOtherPlatform.replace("/", ":").replace("\\", "/").replaceFirst("\\:/", "/");
		}
		return pathFromOtherPlatform;
	}

	/**
	 * @param pathFromOtherPlatform
	 * @return mac, windows, or unknown
	 */
	@NotNull
	private static Platform analyzePath(@NotNull final String pathFromOtherPlatform) {
		if (pathFromOtherPlatform.startsWith("/")) return mac;
		if (pathFromOtherPlatform.contains(":\\")) return windows;
		return unknown;
	}
}
