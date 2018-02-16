package DataModel;

import java.io.Serializable;
import java.util.List;
import com.google.gson.*;
import com.google.gson.annotations.Expose;


/**
 * Class representing a gas mixture of multiple speices
 * @author Jinliang Ma at NETL
 */
public class GasMixture extends Mixture implements Serializable
{
	//pressure
	@Expose
	private Parameter pressure;

	//temperature
	@Expose
	private Parameter temperature;

	//volume fraction or void factor
	@Expose
	private Parameter volumeFraction;


	public GasMixture()
	{
		super();
		pressure = new Parameter("Pressure");
		pressure.setAllValues(101325.0f);		//ambient pressure
		temperature = new Parameter("Temperature");
		temperature.setAllValues(298.15f);		//ambient temperature
		volumeFraction = new Parameter("VolumeFraction");
		volumeFraction.setDefaultValue(1.0f);	//gas phase only
	}

	public Parameter getPressure()
	{
		return pressure;
	}

	public void setPressure(Parameter p)
	{
		pressure = p;
	}

	public Parameter getTemperature()
	{
		return temperature;
	}

	public void setTemperature(Parameter t)
	{
		temperature = t;
	}

	public Parameter getVolumeFraction()
	{
		return volumeFraction;
	}

	public void setVolumeFraction(Parameter vf)
	{
		volumeFraction = vf;
	}

	//append all available input parameters to a list, valid for inlet boundary only
	public void appendAllInputsToParameterList(List<Parameter> paramList)
	{
		paramList.add(pressure);
		paramList.add(temperature);
		paramList.add(volumeFraction);
		super.appendAllInputsToParameterList(paramList);
	}

	//append varied input parameters to a list, valid for inlet boundary only
	public void appendVariedInputsToParameterList(List<Parameter> paramList)
	{
		if (pressure.isVaried())
			paramList.add(pressure);
		if (temperature.isVaried())
			paramList.add(temperature);
		if (volumeFraction.isVaried())
			paramList.add(volumeFraction);
		super.appendVariedInputsToParameterList(paramList);
	}

	//append all output parameters to a list, valid for outlet boundary only
	public void appendOutputsToParameterList(List<Parameter> paramList)
	{
		//some outlet boundary conditions could be fixed such as back pressure, still check isVaried flag
		if (pressure.isVaried())
			paramList.add(pressure);
		if (temperature.isVaried())
			paramList.add(temperature);
		if (volumeFraction.isVaried())
			paramList.add(volumeFraction);
		super.appendOutputsToParameterList(paramList);
	}

	public void setAllParemeterAliasAs(String str)
	{
		super.setAllParemeterAliasAs(str);
		pressure.setAlias(str);
		temperature.setAlias(str);
		volumeFraction.setAlias(str);
	}
}