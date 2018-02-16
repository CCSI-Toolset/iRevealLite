package DataModel;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import com.google.gson.*;
import com.google.gson.annotations.Expose;

/**
 * Class representing a mixture of multiple speices
 * @author Jinliang Ma at NETL
 */
public class Mixture implements Serializable
{
	//option for constant mass fractions
	@Expose
	private boolean hasConstMassFractions;

	//total mass flow rate of all species
	@Expose
	private Parameter totalMassFlow;

	//a map between 0-based species index and mass flow rate or mass fraction of a species
	//the species index corresponding to the array index in the species list for each phase in UnitOperation class
	//For MFIX CFD, species index in mfix.dat file is 1-based. Fluent CFD could be 0-based
	//If hasConstMassFraction is true, speciesFlow in the map is actually the mass fraction of the species, otherwise the mass flow rate
	@Expose
	private Map<Integer,Parameter> speciesFlowMap;

	public Mixture()
	{
		hasConstMassFractions = true;
		totalMassFlow = new Parameter("TotalMassFlow");
		speciesFlowMap = new HashMap<Integer,Parameter>();
	}

	public boolean hasConstMassFractions()
	{
		return hasConstMassFractions;
	}

	public void enableConstMassFractions(boolean b)
	{
		hasConstMassFractions = b;
		//if true, set isVared in the flow map to false
		if (b)
		{
			Collection<Parameter> speciesFlowValues = speciesFlowMap.values();
			for (Parameter p : speciesFlowValues)
				p.enableVaried(false);
		}
	}

	public void putSpeciesFlow(int i, Parameter p)
	{
		Integer a = new Integer(i);
		speciesFlowMap.put(a,p);
	}

	//insert or update all mass flow values of Species i
	public void putSpeciesFlow(int i, float x)
	{
		Integer a = new Integer(i);
		String paraName = "Species_" + i + "_MassFlow";
		Parameter p = new Parameter(paraName);	//default constructor will set p.isVaried to false
		p.setAllValues(x);
		speciesFlowMap.put(a,p);
	}

	//insert or update default, min and max mass flow of Species i, set isVaried to true
	public void putSpeciesFlow(int i, float x, float xmin, float xmax)
	{
		Integer a = new Integer(i);
		String paraName = "Species_" + i + "_MassFlow";
		Parameter p = new Parameter(paraName);
		p.enableVaried(true);
		p.setDefaultValue(x);
		p.setMinValue(xmin);
		p.setMaxValue(xmax);
		speciesFlowMap.put(a,p);
	}

	public Parameter getTotalMassFlow()
	{
		return totalMassFlow;
	}

	//calculate total mass flow rate based on default value of individual species mass flow
	public void calcTotalMassFlow()
	{
		float total = 0;
		Object mf[] = speciesFlowMap.values().toArray();
		for (int i=0; i<mf.length; i++)
		{
			total += ((Parameter)mf[i]).getDefaultValue();
		}
		totalMassFlow.setDefaultValue(total);
	}

	//normalize the mass fractions in the map, usually hasConstMassFractions is true
	public void normalizeSpeciesMassFlowAsMassFraction()
	{
		float sum = 0;
		Parameter param = null;
		for(Map.Entry<Integer,Parameter> entry : speciesFlowMap.entrySet())
		{
			param = entry.getValue();
			sum += param.getDefaultValue();
		}
		for(Map.Entry<Integer,Parameter> entry : speciesFlowMap.entrySet())
		{
			param = entry.getValue();
			param.setDefaultValue(param.getDefaultValue()/sum);
		}
	}

	public Map<Integer,Parameter> getSpeciesFlowMap()
	{
		return speciesFlowMap;
	}

	public void setSpeciesFlowMap(Map<Integer,Parameter> m)
	{
		speciesFlowMap = m;
	}

	//append all available input parameters to a list, valid for inlet boundary only
	public void appendAllInputsToParameterList(List<Parameter> paramList)
	{
		Collection<Parameter> speciesFlowValues = speciesFlowMap.values();
		if (hasConstMassFractions)
			paramList.add(totalMassFlow);
		//If hasConstMassFractions is true, allow user to modify the mass fractions
		//However, GUI should not allow user to vary it or included in the ROM input vector
		paramList.addAll(speciesFlowValues);
	}

	//append varied input parameters to a list, valid for inlet boundary only
	public void appendVariedInputsToParameterList(List<Parameter> paramList)
	{
		if (hasConstMassFractions)
		{
			if (totalMassFlow.isVaried())
				paramList.add(totalMassFlow);
		}
		else
		{
			Collection<Parameter> speciesFlowValues = speciesFlowMap.values();
			for (Parameter p : speciesFlowValues)
			{
				if (p.isVaried())
					paramList.add(p);
			}
		}
	}

	//append all output parameters to a list, valid for outlet boundary only
	public void appendOutputsToParameterList(List<Parameter> paramList)
	{
		Collection<Parameter> speciesFlowValues = speciesFlowMap.values();
		//all of them should have isVaried true, hasConstMassFractions should be false for outlet boundaries
		paramList.addAll(speciesFlowValues);
	}

	public void setAllParemeterAliasAs(String str)
	{
		totalMassFlow.setAlias(str);
		for (Parameter value : speciesFlowMap.values())
			value.setAlias(str);
	}
}