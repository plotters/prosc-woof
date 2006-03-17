// Created by Direct to Web's Project Builder Wizard

import com.webobjects.foundation.*;
import com.webobjects.appserver.*;
import com.webobjects.directtoweb.*;
import com.webobjects.eocontrol.*;
import com.webobjects.eoaccess.*;

public class Main extends WOComponent {
    public String username;
    public String password;
    public boolean wantsWebAssistant = false;
    public WODisplayGroup personDisplayGroup;

    /** @TypeInfo Person */
    public EOEnterpriseObject eachPerson;
    public String queryString;
    public Main(WOContext aContext) {
        super(aContext);
		D2W.factory().setWebAssistantEnabled(wantsWebAssistant);
		personDisplayGroup.setSelectedObject(null);
		personDisplayGroup.setSelectsFirstObjectAfterFetch(false);
   }

    public boolean isAssistantCheckboxVisible() {
        String s = System.getProperty("D2WWebAssistantEnabled");
        return s == null || NSPropertyListSerialization.booleanForString(s);
    }
    public String mailtoHref()
    {
        return "mailto:" + eachPerson.valueForKey("email");
    }
	public boolean mailtoIsDisabled() {
		return eachPerson.valueForKey("email") == null;
	}
	
	public WOComponent editPersonAction() {
		personDisplayGroup.setSelectedObject(eachPerson);
		PersonEditPage result = (PersonEditPage)pageWithName("PersonEditPage");
		result.setPerson(eachPerson);
		result.setListPage(this);
		return result;
	}
	
    public WOComponent addPersonAction()
    {
		PersonEditPage result = (PersonEditPage)pageWithName("PersonEditPage");
		personDisplayGroup.insert();
		result.setPerson((EOEnterpriseObject)personDisplayGroup.selectedObject());
		result.setListPage(this);
		return result;
    }
	
	public String cssRowBackground() {
		return eachPerson == personDisplayGroup.selectedObject() ? "#8BF" : "#FFF";
	}

    public WOComponent searchAction()
    {
		queryString = context().request().stringFormValueForKey("q");
		if (queryString != null) {
			personDisplayGroup.queryMatch().takeValueForKey(queryString, "lastName");
		} else {
			personDisplayGroup.queryMatch().removeObjectForKey("lastName");
		}
		personDisplayGroup.qualifyDisplayGroup();
 		personDisplayGroup.setSelectedObject(null);
       return null;
    }

    public WOComponent deleteAction()
    {
		personDisplayGroup.setSelectedObject(eachPerson);
		personDisplayGroup.delete();
		session().defaultEditingContext().saveChanges();
        return null;
    }

}
