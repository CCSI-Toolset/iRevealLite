package DataModel;

import java.io.*;
import com.google.gson.*;
import com.google.gson.annotations.Expose;

/**
 * Class for assigning an alias to the parameters.
 * @author port091, modified by Jinliang Ma at NETL
 */
public class Parameter extends Alias implements Serializable {

	private static final long serialVersionUID = -7548951555700258244L;

	//flag to check if the input parameter is varied for ROM generation
	@Expose
	private boolean isVaried;

	//default value used in baseline CFD case, also as the fixed value if isVaried is false
	@Expose
	private float defaultValue;

	//lower limit value
	@Expose
	private float minValue;

	//upper limit value
	@Expose
	private float maxValue;

	public boolean isVaried() {
		return isVaried;
	}

	public void enableVaried(boolean b) {
		isVaried = b;
	}

	public Parameter(String parameterName) {
		super(parameterName);
	}

	public Parameter(String parameterName, String alias) {
		super(parameterName, alias);
	}

	public float getDefaultValue() {
		return defaultValue;
	}

	public void setDefaultValue(float defaultValue) {
		this.defaultValue = defaultValue;
	}

	public float getMinValue() {
		return minValue;
	}

	public void setMinValue(float minValue) {
		this.minValue = minValue;
	}

	public float getMaxValue() {
		return maxValue;
	}

	public void setMaxValue(float maxValue) {
		this.maxValue = maxValue;
	}

	public void setAllValues(float value) {
		defaultValue = value;
		minValue = value;
		maxValue = value;
	}

	//print a variable that is related to a port
	public void printACMVariable(PrintWriter out, String variableName)
	{
		out.println(variableName + " as RealVariable;");
		if (isVaried)
		{
			out.println(variableName + ".lower : " + minValue + ";");
			out.println(variableName + ".upper : " + maxValue + ";");
		}
		else
		{
			out.println(variableName + ".lower : " + defaultValue + "*LowerTolerance;");
			out.println(variableName + ".upper : " + defaultValue + "*UpperTolerance;");
		}
	}

	//print a parameter that is not related to port data, called only if the parameter is varied
	public void printACMParameter(PrintWriter out, String variableName)
	{
		out.println(variableName + " as RealParameter(" + defaultValue + ");");
		out.println(variableName + ".lower : " + minValue + ";");
		out.println(variableName + ".upper : " + maxValue + ";");
	}

	//print a variable or parameter, either related to port or not
	public void printCapeOpen(PrintWriter out, String variableName)
	{
		if (isVaried)
			out.println("1\t" + defaultValue + "\t" + minValue + "\t" + maxValue + "\t//" + variableName);
		else
			out.println("0\t" + defaultValue + "\t//" + variableName);
	}
}