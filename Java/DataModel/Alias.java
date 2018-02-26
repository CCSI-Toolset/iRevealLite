package DataModel;

import java.io.Serializable;
import com.google.gson.*;
import com.google.gson.annotations.Expose;

/**
 * Class for assigning an alias
 * @author port091
 *
 */
public class Alias implements Serializable {

	private static final long serialVersionUID = 7248170918564092331L;

	@Expose
	private String name;

	private String alias;

	/**
	 * Creates an easy way to alias names
	 * @param name
	 * @param alias
	 */
	Alias(String name, String alias) {
		this.setName(name);
		this.setAlias(alias);
	}

	/**
	 * No Alias provided, use real name
	 * @param name
	 */
	Alias(String name) {
		this(name, name);
	}

	/**
	 * No Name or Alias provided
	 * @param name
	 */
	Alias() {
		this.setName("");
		this.setAlias("");
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}

	@Override
	public String toString() {
		return alias;
	}

}
