package com.prosc.fmpjdbc;

/**
 * Created by IntelliJ IDEA.
 * User: jesse
 * Date: Aug 25, 2005
 * Time: 4:02:54 PM
 */
public class MiscTests {
	public void memoryTest() {
		StringBuffer sb = new StringBuffer();
		String whatever = "I'm wondering if a StringBuffer will eventually cause the system to die if I continuously reuse it by calling delete().";
		int cycles = 100000000;
		for(int n=0; n<cycles; n++ ) {
			sb.append( whatever );
			sb.delete( 0, sb.length() );
		}
	}

	public static void main(String[] args) {
		new MiscTests().memoryTest();
	}
}
