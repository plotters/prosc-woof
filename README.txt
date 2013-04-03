---Build instructions---
You will need the WeObjects libraries installed in the standard location (/System/Library/Frameworks/) in order to build WooF7.

Just type 'ant' in the WooF7 directory containing the build.xml file (and this README file) and it will run the default target, which does the following:
* Compiles all source code and creates installation files in the build directory (the build directory is created if it does not already exist).
* Installs WooF7 into your /Library/Frameworks/ folder.
* Installs the WooF_EOModelerPlugIn.EOMplugin bundle into /Developer/EOMBundles.
* Installs the 360Works JDBC driver for FileMaker Pro, Fmp360_JDBC.jar, into /Library/Java/Extensions.

You should now be ready to start doing WebObjects and FileMaker development. Read the 'Using WooF7.html' file for information on using WooF7.

You may also want to look at the WooF_Demo folder. This contains a small working application written using WooF. For your convenience, we host the database for this sample application on our server here so that you can immediately start experimenting with WooF.

--Jesse Barnum, President, 360Works
http://www.360works.com
770-234-9293
