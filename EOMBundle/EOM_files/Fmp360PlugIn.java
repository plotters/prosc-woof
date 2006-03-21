//
//  FmproPlugIn.java
//  FMPPlugIn
//
//  Created by jesse on Tue Sep 18 2001.
//  Copyright (c) 2001 __CompanyName__. All rights reserved.
//

import com.apple.yellow.eoaccess.*;
import java.sql.SQLException;
import java.sql.ResultSet;

public class Fmp360PlugIn extends JDBCPlugIn {
	public Fmp360PlugIn(JDBCAdaptor adaptor) {super(adaptor);}
	
	public String databaseProductName() {return "FileMaker Pro 7";}
	
	public String defaultDriverName() {return "com.prosc.fmpjdbc.Driver";}
	
	public Class defaultExpressionClass() {return FMPExpression7.class;}
}