//See http://developer.apple.com/technotes/tn/tn2027.html for how to write a plugin

//
//  FMPExpression.java
//  FMPPlugIn
//
//  Created by jesse on Tue Sep 18 2001.
//  Copyright (c) 2001 __CompanyName__. All rights reserved.
//
import com.apple.yellow.eoaccess.*;

public class FMPExpression7 extends JDBCExpression {
	//Only in bridged version
	public FMPExpression7() {
		super();
	}
	//End bridge code
	
	public FMPExpression7(EOEntity theEntity) {super(theEntity);}
	
	public boolean useAliases() {return false;}
	
	public String sqlStringForAttribute(EOAttribute anAttribute) {
		return ("\"" + anAttribute.columnName() + "\"");
	}
	
	public String tableListWithRootEntity(EOEntity entity)  {
		return ("\"" + entity.externalName() + "\"");
	}
}