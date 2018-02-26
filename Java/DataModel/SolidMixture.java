package DataModel;

import java.io.Serializable;
import java.util.List;
import com.google.gson.*;
import com.google.gson.annotations.Expose;

/**
 * Class representing a solid mixture of multiple speices
 * @author Jinliang Ma at NETL
 */
public class SolidMixture extends Mixture implements Serializable
{
	//0-based solid phase index in the solid phase list
	@Expose
	private int phaseIndex;

	//solid type defined by Aspen Plus or ACM, 0=CISOLID, 1=NC (non-conventional solid such as coal), 2=CIPSD, 3=NCPSD
	//currently only inplemented CISOLID and NC since PSD requires more input/output data
	@Expose
	private int solidType;

	//flag to indicate if granular energy PDE is solved
	@Expose
	private boolean isGranularEnergySolved;

	//particle pressure, use the gas phase pressure
	@Expose
	private Parameter pressure;

	//particle temperature
	@Expose
	private Parameter temperature;

	//particle volume fraction or void factor
	@Expose
	private Parameter volumeFraction;

	//particle diameter
	@Expose
	private Parameter diameter;

	//particle density
	@Expose
	private Parameter density;

	//granular temperature
	@Expose
	private Parameter granularTemperature;

	//dry based volatile matter mass percentage, for NC solid only
	@Expose
	private Parameter vm;

	//constructor
	public SolidMixture()
	{
		super();
		phaseIndex = 0;
		solidType = 0;
		isGranularEnergySolved = false;
		granularTemperature = new Parameter("GranularTemperature");
		pressure = new Parameter("Pressure");
		pressure.setAllValues(101325.0f);		//ambient pressure
		temperature = new Parameter("Temperature");
		temperature.setAllValues(298.15f);		//ambient temperature
		volumeFraction = new Parameter("VolumeFraction");
		volumeFraction.setAllValues(0.1f);
		diameter = new Parameter("Diameter");
		diameter.setAllValues(0.0001f);			//100 micron
		density = new Parameter("Density");
		density.setAllValues(1000f);			//same as water density
		vm = new Parameter("VM");
		vm.setAllValues(40f);
	}

	public int getPhaseIndex()
	{
		return phaseIndex;
	}

	public void setPhaseIndex(int i)
	{
		phaseIndex = i;
	}

	public int getSolidType()
	{
		return solidType;
	}

	public void setSolidType(int i)
	{
		solidType = i;
	}

	public void setSolidType(Integer i)
	{
		solidType = i.intValue();
	}

	public boolean isGranularEnergySolved()
	{
		return isGranularEnergySolved;
	}

	public void enableGranularEnergySolved(boolean b)
	{
		isGranularEnergySolved = b;
		//if disabled, set isVaried to false for granularTemperature
		if (!b)
			granularTemperature.enableVaried(false);
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

	public Parameter getDiameter()
	{
		return diameter;
	}

	public void setDiameter(Parameter d)
	{
		diameter = d;
	}

	public Parameter getDensity()
	{
		return density;
	}

	public void setDensity(Parameter d)
	{
		density = d;
	}

	public Parameter getGranularTemperature()
	{
		return granularTemperature;
	}

	public void setGranularTemperature(Parameter t)
	{
		granularTemperature = t;
	}

	public Parameter getVm()
	{
		return vm;
	}

	public void setVm(Parameter v)
	{
		vm = v;
	}

	//append all available input parameters to a list, valid for inlet boundary only
	public void appendAllInputsToParameterList(List<Parameter> paramList)
	{
		paramList.add(pressure);
		paramList.add(temperature);
		paramList.add(volumeFraction);
		paramList.add(diameter);
		paramList.add(density);
		if (isGranularEnergySolved)
			paramList.add(granularTemperature);
		if (solidType==1)
			paramList.add(vm);
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
		if (diameter.isVaried())
			paramList.add(diameter);
		if (density.isVaried())
			paramList.add(density);
		if (isGranularEnergySolved)
		{
			if (granularTemperature.isVaried())
				paramList.add(granularTemperature);
		}
		if (solidType==1)
		{
			if (vm.isVaried())
				paramList.add(vm);
		}
		super.appendVariedInputsToParameterList(paramList);
	}

	//append output parameters to a list, valid for outlet boundary only
	public void appendOutputsToParameterList(List<Parameter> paramList)
	{
		//some outlet boundary conditions could be fixed such as back pressure, still check isVaried flag
		if (pressure.isVaried())
			paramList.add(pressure);
		if (temperature.isVaried())
			paramList.add(temperature);
		if (volumeFraction.isVaried())
			paramList.add(volumeFraction);
		if (diameter.isVaried())
			paramList.add(diameter);
		if (density.isVaried())
			paramList.add(density);
		if (isGranularEnergySolved)
		{
			if (granularTemperature.isVaried())
				paramList.add(granularTemperature);
		}
		if (solidType==1)
		{
			if (vm.isVaried())
				paramList.add(vm);
		}
		super.appendOutputsToParameterList(paramList);
	}

	public void setAllParemeterAliasAs(String str)
	{
		super.setAllParemeterAliasAs(str);
		pressure.setAlias(str);
		temperature.setAlias(str);
		volumeFraction.setAlias(str);
		diameter.setAlias(str);
		density.setAlias(str);
		granularTemperature.setAlias(str);
		vm.setAlias(str);
	}
}