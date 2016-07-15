package webby.data;

import java.util.ArrayList;
import java.util.List;

public class AnotherUser {

	private String name;
    private List<String> emails = new ArrayList<String>();

    public void setName(String name) {
    	this.name = name;
    }

    public String getName() {
    	return name;
    }

    public List<String> getEmails() {
    	return emails;
    }

}
