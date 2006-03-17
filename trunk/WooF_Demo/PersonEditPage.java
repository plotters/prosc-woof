//
// PersonEditPage.java: Class file for WO Component 'PersonEditPage'
// Project WooF_Demo
//
// Created by sbarnum on 9/28/05
//

import com.webobjects.foundation.*;
import com.webobjects.appserver.*;
import com.webobjects.eocontrol.*;
import com.webobjects.eoaccess.*;

public class PersonEditPage extends WOComponent {
	
	private EOEnterpriseObject person;
	private WOComponent listPage;

    /** @TypeInfo Phone */
    public EOEnterpriseObject eachPhone;
    public NSArray phoneLabelOptions = new NSArray(new String[] {"work", "home", "mobile", "fax", "pager", "other"});

    /** @TypeInfo Address */
    public EOEnterpriseObject eachAddress;
    public NSArray addressOptions = new NSArray(new String[] {"work", "home", "other"});;
    public PersonEditPage(WOContext context) {
        super(context);
    }
	
	public void setListPage(Main listPage) {
		this.listPage = listPage;
	}
	
	public void setPerson(EOEnterpriseObject person) {
		this.person = person;
	}
	
    /** @TypeInfo Person */
	public EOEnterpriseObject getPerson() {
		return person;
	}

    public WOComponent deletePhoneAction()
    {
		person.removeObjectFromBothSidesOfRelationshipWithKey(eachPhone, "phones");
		session().defaultEditingContext().deleteObject(eachPhone);
        return null;
    }

    public WOComponent addPhoneAction()
    {
		EOEnterpriseObject newPhone = EOUtilities.createAndInsertInstance(session().defaultEditingContext(), "Phone");
		newPhone.addObjectToBothSidesOfRelationshipWithKey(person, "person");
        return null;
    }

    public WOComponent deleteAddressAction()
    {
		person.removeObjectFromBothSidesOfRelationshipWithKey(eachAddress, "addresses");
		session().defaultEditingContext().deleteObject(eachAddress);
        return null;
    }
	
    public WOComponent addAddressAction()
    {
		EOEnterpriseObject newAddress = EOUtilities.createAndInsertInstance(session().defaultEditingContext(), "Address");
		newAddress.addObjectToBothSidesOfRelationshipWithKey(person, "person");
        return null;
    }
	
	public WOComponent saveAction() {
		session().defaultEditingContext().saveChanges();
		return listPage;
	}
	
	public WOComponent deleteAction() {
		session().defaultEditingContext().deleteObject(person);
		return listPage;
	}
    public WOComponent cancelAction()
    {
		session().defaultEditingContext().revert();
		return listPage;
    }

}
