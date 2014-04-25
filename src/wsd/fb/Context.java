package wsd.fb;

import java.util.ArrayList;
import java.util.List;

public class Context {

	private List<String> context = new ArrayList<String>();

	public List<String> getContext() {
		return context;
	}

	public void setContext(List<String> context) {
		this.context = context;
	}

	public Context(List<String> context) {
		super();
		this.context = context;
	}
	
}
