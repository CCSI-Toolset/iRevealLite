package DataModel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import com.google.gson.*;
import com.google.gson.annotations.Expose;

/**
 * Class representing an inlet or outlet boundary condition
 * @author Jinliang Ma at NETL
 */
public class FlowBoundary implements Serializable
{

	//flag to indicate if gas phase exits at the boundary
	@Expose
	private boolean hasGasPhase;

	//flag to indicate if solid phases exit at the boundary
	@Expose
	private boolean hasSolidPhase;

	//boundary index used by CFD code. MFIX is 1-based
	@Expose
	private int boundaryIndex;

	//boundary name, user might need to specify. converted to port name for PME
	@Expose
	private String boundaryName;

	//gas phase mixture
	@Expose
	private List<GasMixture> gasMixture;

	//a list of solid phase mixtures
	@Expose
	private List<SolidMixture> solidMixtures;

	//constructor with default boundary index and boundary name
	public FlowBoundary()
	{
		hasGasPhase = true;
		hasSolidPhase = false;
		boundaryIndex = 1;
		boundaryName = "Boundary";
		gasMixture = new ArrayList<GasMixture>();
		solidMixtures = new ArrayList<SolidMixture>();
	}

	//constructor with given boundary index and boundary name
	public FlowBoundary(int i, String name)
	{
		hasGasPhase = true;
		hasSolidPhase = false;
		boundaryIndex = i;
		boundaryName = name;
		gasMixture = new ArrayList<GasMixture>();
		solidMixtures = new ArrayList<SolidMixture>();
	}

	public boolean hasGasPhase()
	{
		return hasGasPhase;
	}

	public void enableGasPhase(boolean b)
	{
		hasGasPhase = b;
	}

	public boolean hasSolidPhase()
	{
		return hasSolidPhase;
	}

	public void enableSolidPhase(boolean b)
	{
		hasSolidPhase = b;
	}

	public int getBoundaryIndex()
	{
		return boundaryIndex;
	}

	public void setBoundaryIndex(int i)
	{
		boundaryIndex = i;
	}

	public String getBoundaryName()
	{
		return boundaryName;
	}

	public void setBoundaryName(String name)
	{
		boundaryName = name;
	}

	public List<GasMixture> getGasMixture()
	{
		return gasMixture;
	}

	public void setGasMixture(List<GasMixture> gm)
	{
		gasMixture = gm;
	}

	public List<SolidMixture> getSolidMixtures()
	{
		return solidMixtures;
	}

	public void setSolidMixtures(List<SolidMixture> sms)
	{
		solidMixtures = sms;
	}

	//append all available input parameters to a list
	public void appendAllInputsToParameterList(List<Parameter> paramList)
	{
		if (hasGasPhase)
			gasMixture.get(0).appendAllInputsToParameterList(paramList);
		if (hasSolidPhase)
		{
			for (SolidMixture sm : solidMixtures)
				sm.appendAllInputsToParameterList(paramList);
		}
	}

	//append varied input parameters to a list as ROM input vector
	public void appendVariedInputsToParameterList(List<Parameter> paramList)
	{
		if (hasGasPhase)
			gasMixture.get(0).appendVariedInputsToParameterList(paramList);
		if (hasSolidPhase)
		{
			for (SolidMixture sm : solidMixtures)
				sm.appendVariedInputsToParameterList(paramList);
		}
	}

	//append all output parameters to a list as ROM output vector
	public void appendOutputsToParameterList(List<Parameter> paramList)
	{
		if (hasGasPhase)
			gasMixture.get(0).appendOutputsToParameterList(paramList);
		if (hasSolidPhase)
		{
			for (SolidMixture sm : solidMixtures)
				sm.appendOutputsToParameterList(paramList);
		}
	}
}