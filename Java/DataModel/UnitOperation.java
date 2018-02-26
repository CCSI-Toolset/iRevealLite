package DataModel;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.lang.Math;
import com.google.gson.*;
import com.google.gson.annotations.Expose;

/**
 * Class representing a unit operation or equipment modeled by CFD and PME
 * @author Jinliang Ma at NETL
 * Usage: ROM builder should read the CFD input file(s) to add gas species to gasSpeciesList, add species to solidSpeciesList for one or multiple solid phases.
 * Then the ROM builder should call updateAllSpeciesList() to update allSpeciesList.
 * ROM builder should then add inlet boundaris to inletBoundaryList and outlet boundaries to outletBoundaryList based on CFD input file
 * ROM builder should then add input parameters to inputParameterList and add output parameters to outputParameterList based on CFD and user input
 * Prepare a list of all possile input parameters for the user to set as fixed or varied by calling getAllAvailableInputParameters()
 * Allow user to pick which input parameter needs to be fixed or varied, set default and limits accordingly.
 * Call updateRomInputVector() to get a parameter list romInputVector for parameters used as ROM input vector
 * Call updateYromOutputVector() to get a parameter list yromOutputVector for parameters used as ROM output vector
 * Do LHS based on romInputVector and yromOutputVector, run and post-process CFD cases (separate executable)
 * To correct CFD or ROM results, set default values in the romInputVector and yromOutputVector and call enforceElementalMassBalance(), the old values of yromOutputVector will be replaced with the correct values
 * To export corrected output vector, call wrtieCorrectedOutputVector() with given file name passed as parameter of the method
 * Prepare input file for regression code (e.g. Kriging executable)
 * Call regression executable and optionally save regression results to the ROM object as a member of UnitOperation class (currently commented out)
 * To export ROM to ACM, call exportACMRom(). Note that regression method specific part not implemented yet
 * To export ROM to CAPE-OPEN input format, call exportCapeOpenRom().  Note that regression method specific part not implemented yet.
 */

public class UnitOperation extends Alias implements Serializable
{
	//enum for regression method
	public enum RegressionMethod {KRIGING, ANN};

	//regression method
	@Expose
	private RegressionMethod regMethod;

	//number of samples or cases of high fidelity model to run
	@Expose
	private int nSample;

	//an coefficient to modify the value of a flow rate if it is negative
	private static final float reflectionCoefficient = -0.01f;

	//lower relative tolerance for fixed feed port variables
	private float lowerTolerance;

	//upper relative tolerance for fixed feed port variables
	private float upperTolerance;

	//a list of gas phase species, from CFD
	@Expose
	private List<Species> gasSpeciesList;

	//a list of solid species in all solid phases, from CFD
	@Expose
	private List<List<Species>> solidSpeciesList;

	//a list of all species from gas and solid phases, calculated by updateAllSpeciesList() method
	private List<Species> allSpeciesList;

	//a list of solid phase names, currently not used to avoid too long port name string in ACM
	@Expose
	private List<String> solidPhaseList;

	//a list of solid phase type, 0=CISOLID, 1=NC, >1 for future use
	@Expose
	private List<Integer> solidPhaseTypeList;

	//index mapping from solid phase species list to all species list, calculated by updateAllSpeciesList() method
	private int[][] iSolidPhase2All;

	//array of indices in the all species list for the species in the inlet flow boundaries, calculated by updateAllSpeciesList() method
	private int[] iInletSpecies;

	//array of indices in the all species list for the species in the outlet flow boundaries, calculated by updateAllSpeciesList() method
	private int[] iOutletSpecies;

	//array of atomic numbers of all elements involved, calculated by updateAllSpeciesList() method
	private int[] iElementAll;

	//array of atomic numbers of element in inlet boundaries, calculated by eliminateProductSpeciesWithElementsUnavailableInFeed() method
	private int[] iElementFeed;

	//a list of inlet boundary conditions, from CFD
	@Expose
	private List<FlowBoundary> inletBoundaryList;

	//a list of outlet boundary conditions, from CFD
	@Expose
	private List<FlowBoundary> outletBoundaryList;

	//a list of other CFD model parameters including wall boundary conditions
	@Expose
	private List<Parameter> inputParameterList;

	//a list of other CFD model results excluding outlet boundary conditions
	//parameters not in CFD input file, requiring post-processing script to calculate
	@Expose
	private List<Parameter> outputParameterList;

	//input vector of ROM regression function, a list of variables chosen by user, calculated after user configuration is done
	private List<Parameter> romInputVector;

	//output vector of ROM regression function, calculated after user configuration is done
	private List<Parameter> yromOutputVector;

	//ROM related data, depending on regression method
	//private Rom romdata;


	public UnitOperation()
	{
		super("rom_model");
		regMethod = RegressionMethod.KRIGING;
		nSample = 10;
		lowerTolerance = 0.99f;
		upperTolerance = 1.01f;
		gasSpeciesList = new ArrayList<Species>();
		solidSpeciesList = new ArrayList<List<Species>>();
		allSpeciesList = new ArrayList<Species>();
		solidPhaseList = new ArrayList<String>();
		solidPhaseTypeList = new ArrayList<Integer>();
		inletBoundaryList = new ArrayList<FlowBoundary>();
		outletBoundaryList = new ArrayList<FlowBoundary>();
		inputParameterList = new ArrayList<Parameter>();
		outputParameterList = new ArrayList<Parameter>();
		romInputVector = new ArrayList<Parameter>();
		yromOutputVector = new ArrayList<Parameter>();
	}

	public RegressionMethod getRegressionMethod()
	{
		return regMethod;
	}

	public void setRegressionMethod(RegressionMethod rm)
	{
		regMethod = rm;
	}

	public int getNumberOfSamples()
	{
		return nSample;
	}

	public void setNumberOfSamples(int ns)
	{
		nSample = ns;
	}

	public float getLowerTolerance()
	{
		return lowerTolerance;
	}

	public void setLowerTolerance(float t)
	{
		lowerTolerance = t;
	}

	public float getUpperTolerance()
	{
		return upperTolerance;
	}

	public void setUpperTolerance(float t)
	{
		upperTolerance = t;
	}

	public List<Species> getGasSpeciesList()
	{
		return gasSpeciesList;
	}

	public void setGasSpeciesList(List<Species> gl)
	{
		gasSpeciesList = gl;
	}

	public List<List<Species>> getSolidSpeciesList()
	{
		return solidSpeciesList;
	}

	public void setSolidSpeciesList(List<List<Species>> sl)
	{
		solidSpeciesList = sl;
	}

	public List<String> getSolidPhaseList()
	{
		return solidPhaseList;
	}

	public void setSolidPhaseList(List<String> spl)
	{
		solidPhaseList = spl;
	}

	public List<Integer> getSolidPhaseTypeList()
	{
		return solidPhaseTypeList;
	}

	public void setSolidPhaseTypeList(List<Integer> sptl)
	{
		solidPhaseTypeList = sptl;
	}

	public List<FlowBoundary> getInletBoundaryList()
	{
		return inletBoundaryList;
	}

	public void setInletBoundaryList(List<FlowBoundary> ibl)
	{
		inletBoundaryList = ibl;
	}

	public List<FlowBoundary> getOutletBoundaryList()
	{
		return outletBoundaryList;
	}

	public void setOutletBoundaryList(List<FlowBoundary> obl)
	{
		outletBoundaryList = obl;
	}

	public List<Parameter> getInputParameterList()
	{
		return inputParameterList;
	}

	public void setInputParameterList(List<Parameter> ipl)
	{
		inputParameterList = ipl;
	}

	public List<Parameter> getOutputParameterList()
	{
		return outputParameterList;
	}

	public void setOutputParameterList(List<Parameter> opl)
	{
		outputParameterList = opl;
	}

	public List<Parameter> getRomInputVector()
	{
		return romInputVector;
	}

	public void setRomInputVector(List<Parameter> iv)
	{
		romInputVector = iv;
	}

	public List<Parameter> getYromOutputVector()
	{
		return yromOutputVector;
	}

	public void setYromOutputVector(List<Parameter> ov)
	{
		yromOutputVector = ov;
	}

	//get a list of all possible variables for a user to include in ROM model
	//a parameter on the list should have at least a default value for ROM model validation
	public List<Parameter> getAllAvailableInputParameters()
	{
		List<Parameter> paramList = new ArrayList<Parameter>();
		for (FlowBoundary fb : inletBoundaryList)
			fb.appendAllInputsToParameterList(paramList);
		for (Parameter p : inputParameterList)
			paramList.add(p);
		return paramList;
	}

	//update the ROM input vector using the varied parameters only based on user's selections
	//a parameter on the list should also have lower and upper limits
	public void updateRomInputVector()
	{
		romInputVector.clear();
		for (FlowBoundary fb : inletBoundaryList)
			fb.appendVariedInputsToParameterList(romInputVector);
		for (Parameter p : inputParameterList)
		{
			if (p.isVaried())
				romInputVector.add(p);
		}
	}

	//update the ROM output vector based on outlet boundary conditons and other output parameters
	public void updateYromOutputVector()
	{
		yromOutputVector.clear();
		for (FlowBoundary fb : outletBoundaryList)
			fb.appendOutputsToParameterList(yromOutputVector);
		for (Parameter p : outputParameterList)
			yromOutputVector.add(p);		//all output parameter should be varied
	}

	//add an outlet flow boundary if there is an outlet boundary condition in CFD input file
	public void addOutletFlowBoundary(int ibc, String name, boolean hasGasPhase, boolean hasSolidPhase)
	{
		//ibc is the boundary index from CFD input file
		//name is a user given boundary name, could be the name in CFD input file, e.g. Fluent model
		//hasGasPhase indicates the existance of gas phase at the outlet
		//hasSolidPhase indicates the existance of solid phase at the outlet
		//For Eulerian particle model, each outlet boundary contains all phases with each phase contains all available species
		//For Langrangian particle model, gas and solid phase are modeled separately
		//at least one phase exists, return if none of them exists
		if (!hasGasPhase && !hasSolidPhase) return;
		int i, j, n, m;
		FlowBoundary fb = new FlowBoundary(ibc, name);
		//for Eulerian multiphase flow model, all phases exit at outlet
		fb.enableGasPhase(hasGasPhase);
		fb.enableSolidPhase(hasSolidPhase);
		if (hasGasPhase)
		{
			GasMixture gm = fb.getGasMixture().get(0);
			gm.enableConstMassFractions(false);
			n = gasSpeciesList.size();
			for (i=0; i<n; i++)
				gm.putSpeciesFlow(i, 0);
		}
		if (hasSolidPhase)
		{
			List<SolidMixture> sms = fb.getSolidMixtures();
			m = solidSpeciesList.size();
			for (i=0; i<m; i++)
			{
				List sl = solidSpeciesList.get(i);
				SolidMixture sm = new SolidMixture();
				sm.setPhaseIndex(i);
				sm.enableConstMassFractions(false);
				n = sl.size();
				for (j=0; j<n; j++)
					sm.putSpeciesFlow(j, 0);
				sms.add(sm);
			}
		}
		outletBoundaryList.add(fb);
	}

	//add a gas species with a name and formula
	public void addGasSpecies(String name, String formula)
	{
		String spName = name.toUpperCase();
		Species s = new Species(spName, formula);
		gasSpeciesList.add(s);
	}

	//add a gas species with a formula
	public void addGasSpecies(String formula)
	{
		//convert to upper case for the name
		String spName = formula.toUpperCase();
		Species s = new Species(spName, formula);
		gasSpeciesList.add(s);
	}

	//set number of solid phase, also allocate species list for each solid phase
	public void setTotalNumberOfSolidPhases(int n)
	{
		int i;
		List<Species> sl;
		solidSpeciesList.clear();
		for (i=0; i<n; i++)
		{
			sl = new ArrayList<Species>();
			solidSpeciesList.add(sl);
		}
	}

	//add a species to a solid species list, assume setTotalNumberOfSolidPhases() already called
	public void addSolidSpecies(int iphase, String name, String formula)
	{
		//iphase is the 0-base solid phase index
		if (iphase<0 || iphase>solidSpeciesList.size()-1)
			return;
		List<Species> sl = solidSpeciesList.get(iphase);
		String spName = name.toUpperCase();
		Species s = new Species(spName,formula);
		sl.add(s);
	}

	//add a species to a solid species list, assume setTotalNumberOfSolidPhases() already called
	public void addSolidSpecies(int iphase, String formula)
	{
		//iphase is the 0-base solid phase index
		if (iphase<0 || iphase>solidSpeciesList.size()-1)
			return;
		List<Species> sl = solidSpeciesList.get(iphase);
		String spName = formula.toUpperCase();
		Species s = new Species(spName,formula);
		sl.add(s);
	}

	//update allSpeciesList based on given gasSpeciesList and solidSpeciesList
	//also create and update iSolidPhase2All 2-D array, iInletSpecies, iOutletSpecies and iElementAll 1-D arrays
	public void updateAllSpeciesList()
	{
		boolean found;
		int i, j, k;
		int iSpecies;
		int iSpeciesAll;
		int nSpecies;
		int nPhase;
		int nElement;
		int nBoundary;
		int nSolidPhase = solidSpeciesList.size();
		int[] iAtom = null;
		allSpeciesList.clear();
		iSolidPhase2All = new int[nSolidPhase][];
		Species sp;
		FlowBoundary fb = null;
		GasMixture gm = null;
		SolidMixture sm = null;
		List<Species> phaseSpeciesList;
		for (i=0; i<nSolidPhase; i++)
			iSolidPhase2All[i] = new int[solidSpeciesList.get(i).size()];
		//assign allSpeciesList and iSolidPhase2All
		allSpeciesList.addAll(gasSpeciesList);
		for (i=0; i<nSolidPhase; i++)
		{
			phaseSpeciesList = solidSpeciesList.get(i);
			nSpecies = phaseSpeciesList.size();
			for (j=0; j<nSpecies; j++)
			{
				sp = phaseSpeciesList.get(j);
				found = false;
				for (k=0; k<allSpeciesList.size(); k++)
				{
					if (sp.equals(allSpeciesList.get(k)))
					{
						iSolidPhase2All[i][j] = k;
						found = true;
						break;
					}
				}
				if (!found)
				{
					allSpeciesList.add(sp);
					iSolidPhase2All[i][j] = k;
				}
			}
		}
		//find all elements involved
		boolean[] bAtomAll = new boolean[Species.maxAtomicNumberPlus1];
		nSpecies = allSpeciesList.size();
		for (i=0; i<nSpecies; i++)
		{
			sp = allSpeciesList.get(i);
			nElement = sp.getNumberOfElements();
			iAtom = sp.getAtomicNumbers();
			for (j=0; j<nElement; j++)
				bAtomAll[iAtom[j]] = true;
		}
		nElement = 0;
		for (i=0; i<Species.maxAtomicNumberPlus1; i++)
		{
			if (bAtomAll[i])
				nElement++;
		}
		iElementAll = new int[nElement];
		j = 0;
		for (i=0; i<Species.maxAtomicNumberPlus1; i++)
		{
			if (bAtomAll[i])
			{
				iElementAll[j] = i;
				j++;
			}
		}
		//find all species in inlet flow boundaries
		boolean[] bInletSpecies = new boolean[nSpecies];
		nBoundary = inletBoundaryList.size();
		for (i=0; i<nBoundary; i++)
		{
			fb = inletBoundaryList.get(i);
			if (fb.hasGasPhase())
			{
				gm = fb.getGasMixture().get(0);
				for(Map.Entry<Integer,Parameter> entry : gm.getSpeciesFlowMap().entrySet())
				{
					iSpeciesAll = entry.getKey().intValue();
					bInletSpecies[iSpeciesAll] = true;
				}
			}
			if (fb.hasSolidPhase())
			{
				nPhase = fb.getSolidMixtures().size();
				for (j=0; j<nPhase; j++)
				{
					sm = fb.getSolidMixtures().get(j);
					for(Map.Entry<Integer,Parameter> entry : sm.getSpeciesFlowMap().entrySet())
					{
						iSpecies = entry.getKey().intValue();
						iSpeciesAll = iSolidPhase2All[sm.getPhaseIndex()][iSpecies];
						bInletSpecies[iSpeciesAll] = true;
					}
				}
			}
		}
		iSpecies = 0;
		for (i=0; i<nSpecies; i++)
		{
			if (bInletSpecies[i])
				iSpecies++;
		}
		iInletSpecies = new int[iSpecies];
		iSpecies = 0;
		for (i=0; i<nSpecies; i++)
		{
			if (bInletSpecies[i])
			{
				iInletSpecies[iSpecies] = i;
				iSpecies++;
			}
		}
		//find all species in outlet flow boundaries
		boolean[] bOutletSpecies = new boolean[nSpecies];
		nBoundary = outletBoundaryList.size();
		for (i=0; i<nBoundary; i++)
		{
			fb = outletBoundaryList.get(i);
			if (fb.hasGasPhase())
			{
				gm = fb.getGasMixture().get(0);
				for(Map.Entry<Integer,Parameter> entry : gm.getSpeciesFlowMap().entrySet())
				{
					iSpeciesAll = entry.getKey().intValue();
					bOutletSpecies[iSpeciesAll] = true;
				}
			}
			if (fb.hasSolidPhase())
			{
				nPhase = fb.getSolidMixtures().size();
				for (j=0; j<nPhase; j++)
				{
					sm = fb.getSolidMixtures().get(j);
					for(Map.Entry<Integer,Parameter> entry : sm.getSpeciesFlowMap().entrySet())
					{
						iSpecies = entry.getKey().intValue();
						iSpeciesAll = iSolidPhase2All[sm.getPhaseIndex()][iSpecies];
						bOutletSpecies[iSpeciesAll] = true;
					}
				}
			}
		}
		iSpecies = 0;
		for (i=0; i<nSpecies; i++)
		{
			if (bOutletSpecies[i])
				iSpecies++;
		}
		iOutletSpecies = new int[iSpecies];
		iSpecies = 0;
		for (i=0; i<nSpecies; i++)
		{
			if (bOutletSpecies[i])
			{
				iOutletSpecies[iSpecies] = i;
				iSpecies++;
			}
		}
	}

	//calculates speceis molar flow rates from given flow boundaries (inlet or outlet)
	//used for mass balance correction
	//returns an array with indices corresponding to the allSpeciesList
	public float[] getSpeciesMoleFlowRate(List<FlowBoundary> flowBoundaryList)
	{
		//assuming updateAllSpeciesList() has been called
		int i, j;
		int iSpecies;
		int iSpeciesAll;
		int nPhase;
		int nSpecies = allSpeciesList.size();
		float[] speciesMoleFlowRate = new float[nSpecies];
		int nBoundary = flowBoundaryList.size();
		FlowBoundary fb = null;
		GasMixture gm = null;
		SolidMixture sm = null;
		Parameter param;
		for (i=0; i<nBoundary; i++)
		{
			fb = flowBoundaryList.get(i);
			if (fb.hasGasPhase())
			{
				gm = fb.getGasMixture().get(0);
				for(Map.Entry<Integer,Parameter> entry : gm.getSpeciesFlowMap().entrySet())
				{
					iSpecies = entry.getKey().intValue();
					iSpeciesAll = iSpecies;
					param = entry.getValue();
					if (gm.hasConstMassFractions())
						speciesMoleFlowRate[iSpeciesAll] += gm.getTotalMassFlow().getDefaultValue()*param.getDefaultValue()/allSpeciesList.get(iSpeciesAll).getMolecularWeight();
					else
						speciesMoleFlowRate[iSpeciesAll] += param.getDefaultValue()/allSpeciesList.get(iSpeciesAll).getMolecularWeight();
				}
			}
			if (fb.hasSolidPhase())
			{
				nPhase = fb.getSolidMixtures().size();
				for (j=0; j<nPhase; j++)
				{
					sm = fb.getSolidMixtures().get(j);
					for(Map.Entry<Integer,Parameter> entry : sm.getSpeciesFlowMap().entrySet())
					{
						iSpecies = entry.getKey().intValue();
						iSpeciesAll = iSolidPhase2All[sm.getPhaseIndex()][iSpecies];
						param = entry.getValue();
						if (sm.hasConstMassFractions())
							speciesMoleFlowRate[iSpeciesAll] += sm.getTotalMassFlow().getDefaultValue()*param.getDefaultValue()/allSpeciesList.get(iSpeciesAll).getMolecularWeight();
						else
							speciesMoleFlowRate[iSpeciesAll] += param.getDefaultValue()/allSpeciesList.get(iSpeciesAll).getMolecularWeight();
					}
				}
			}
		}
		return speciesMoleFlowRate;
	}

	//calculates element molar flow rate from given boundaries
	//returns an array with indices corresponding to the atomic numbers in the periodic table
	//currently only 87 elements are considered
	public float[] getElementMoleFlowRate(List<FlowBoundary> flowBoundaryList)
	{
		int i, j;
		int nSpecies;
		int nElement;
		double[] nAtom = null;
		int[] iAtom = null;
		Species sp = null;
		float[] allSpeciesMoleFlowRate = getSpeciesMoleFlowRate(flowBoundaryList);
		float[] allElementMoleFlowRate = new float[Species.maxAtomicNumberPlus1];
		nSpecies = allSpeciesList.size();
		for (i=0; i<nSpecies; i++)
		{
			if (allSpeciesMoleFlowRate[i]>0)
			{
				sp = allSpeciesList.get(i);
				nElement = sp.getNumberOfElements();
				nAtom = sp.getAtomCounts();
				iAtom = sp.getAtomicNumbers();
				for (j=0; j<nElement; j++)
					allElementMoleFlowRate[iAtom[j]] += allSpeciesMoleFlowRate[i]*nAtom[j];
			}
		}
		return allElementMoleFlowRate;
	}

	//If the predicted product species flow is negative, set to a positive number by multiplying a reflection coefficient
	//This approach makes sure that all elements are represented in the outlet boundaries.
	//If the inlet boundaries does not contain an element, the correction algorithm will make the flow rate of the species to zero
	public void eliminateNegativeProductSpeciesFlow()
	{
		//assuming updateAllSpeciesList() has been called
		boolean bNegative;
		int i, j;
		int nPhase;
		int nBoundary = outletBoundaryList.size();
		float defaultValue;
		FlowBoundary fb = null;
		GasMixture gm = null;
		SolidMixture sm = null;
		Parameter param = null;
		for (i=0; i<nBoundary; i++)
		{
			fb = outletBoundaryList.get(i);
			if (fb.hasGasPhase())
			{
				gm = fb.getGasMixture().get(0);
				bNegative = false;
				for(Map.Entry<Integer,Parameter> entry : gm.getSpeciesFlowMap().entrySet())
				{
					param = entry.getValue();
					defaultValue = param.getDefaultValue();
					if (defaultValue<0)
					{
						param.setDefaultValue(defaultValue*reflectionCoefficient);
						bNegative = true;
					}
				}
				if (bNegative)
				{
					if (gm.hasConstMassFractions())		//usually not constant mass fraction for outlet boundary
						gm.normalizeSpeciesMassFlowAsMassFraction();
					else
						gm.calcTotalMassFlow();
				}
			}
			if (fb.hasSolidPhase())
			{
				nPhase = fb.getSolidMixtures().size();
				for (j=0; j<nPhase; j++)
				{
					sm = fb.getSolidMixtures().get(j);
					bNegative = false;
					for(Map.Entry<Integer,Parameter> entry : sm.getSpeciesFlowMap().entrySet())
					{
						param = entry.getValue();
						defaultValue = param.getDefaultValue();
						if (defaultValue<0)
						{
							param.setDefaultValue(defaultValue*reflectionCoefficient);
							bNegative = true;
						}
					}
					if (bNegative)
					{
						if (gm.hasConstMassFractions())		//usually not constant mass fraction for outlet boundary
							gm.normalizeSpeciesMassFlowAsMassFraction();
						else
							gm.calcTotalMassFlow();
					}
				}
			}
		}
	}

	//set the product species flow rate to zero if it contains a given element that is not in inlet boundary
	//usually this is not needed since a valid CFD model would not create element at outlet boundary
	//even if a specific inlet condition may have some element missing, the correction algorithm should get a correction factor of -1
	//which makes the outlet flow of the species to zero.  ACM code exported does not have this type of check.
	public void eliminateProductSpeciesContainingElement(int iAtomicNumber)
	{
		//assuming updateAllSpeciesList() has been called
		boolean bContainsElement;
		int i, j;
		int nPhase;
		int iSpecies;
		int iSpeciesAll;
		int nBoundary = outletBoundaryList.size();
		FlowBoundary fb = null;
		GasMixture gm = null;
		SolidMixture sm = null;
		Parameter param = null;
		for (i=0; i<nBoundary; i++)
		{
			fb = outletBoundaryList.get(i);
			if (fb.hasGasPhase())
			{
				gm = fb.getGasMixture().get(0);
				bContainsElement = false;
				for(Map.Entry<Integer,Parameter> entry : gm.getSpeciesFlowMap().entrySet())
				{
					iSpecies = entry.getKey().intValue();
					iSpeciesAll = iSpecies;
					param = entry.getValue();
					if (allSpeciesList.get(iSpeciesAll).containsElement(iAtomicNumber))
					{
						param.setDefaultValue(0);
						bContainsElement = true;
					}
				}
				if (bContainsElement)
				{
					if (gm.hasConstMassFractions())
						gm.normalizeSpeciesMassFlowAsMassFraction();
					else
						gm.calcTotalMassFlow();
				}
			}
			if (fb.hasSolidPhase())
			{
				nPhase = fb.getSolidMixtures().size();
				for (j=0; j<nPhase; j++)
				{
					sm = fb.getSolidMixtures().get(j);
					bContainsElement = false;
					for(Map.Entry<Integer,Parameter> entry : sm.getSpeciesFlowMap().entrySet())
					{
						iSpecies = entry.getKey().intValue();
						iSpeciesAll = iSolidPhase2All[sm.getPhaseIndex()][iSpecies];
						param = entry.getValue();
						if (allSpeciesList.get(iSpeciesAll).containsElement(iAtomicNumber))
						{
							param.setDefaultValue(0);
							bContainsElement = true;
						}
					}
					if (bContainsElement)
					{
						if (gm.hasConstMassFractions())
							gm.normalizeSpeciesMassFlowAsMassFraction();
						else
							gm.calcTotalMassFlow();
					}
				}
			}
		}
	}

	//If an element has zero molar flow in the feed stream, set the flow rate of a product species that containing the element to zero
	//also create and update iElementFeed[] array
	//This may not needed since the correction algorithm will correct the product species flow and set it to zero
	public void eliminateProductSpeciesWithElementsUnavailableInFeed()
	{
		//assuming updateAllSpeciesList() has been called
		int i, j;
		int nElementAll = iElementAll.length;
		int nElementFeed = 0;
		float[] elementMoleFlowIn = getElementMoleFlowRate(inletBoundaryList);
		for (i=0; i<nElementAll; i++)
		{
			j = iElementAll[i];
			if (elementMoleFlowIn[j]<=0)
				eliminateProductSpeciesContainingElement(j);
			else
				nElementFeed++;
		}
		iElementFeed = new int[nElementFeed];
		nElementFeed = 0;
		for (i=0; i<nElementAll; i++)
		{
			j = iElementAll[i];
			if (elementMoleFlowIn[j]>0)
				iElementFeed[nElementFeed++] = j;
		}
	}

	//checks if all elements in the feed boundaries are also found in the product boundary
	//this should be true for a valid CFD model and if negative outlet flow is reflected to a small positive value
	public boolean areAllFeedElementsInProductBoundaries()
	{
		//assuming eliminateProductSpeciesWithElementsUnavailableInFeed() has been called
		int i;
		int nElementFeed = iElementFeed.length;
		float[] elementMoleFlowOut = getElementMoleFlowRate(outletBoundaryList);
		for (i=0; i<nElementFeed; i++)
		{
			if (elementMoleFlowOut[iElementFeed[i]]<=0)
				return false;
		}
		return true;
	}

	//algorithm to solve a set of linear equations by Gaussian elimination
	public int GaussianEliminationWithRowPivoting(int nrow, int ncol, double[][] ppa, double[] pb, double[] px)
	{
		boolean brank = false;			//true if rank<nrow
		int i, j, k;
		int imax;
		double aijabs;
		double aijmax;
		double aswap;
		double fac;
		for (i=0; i<nrow; i++)
		{
			imax = i;
			aijmax = Math.abs(ppa[i][i]);
			for (k=i+1; k<nrow; k++)
			{
				aijabs = Math.abs(ppa[k][i]);
				if (aijabs > aijmax)
				{
					imax = k;
					aijmax = aijabs;
				}
			}
			if (aijmax==0)		//rank < nrow
			{
				brank = true;
				break;
			}
			if (imax!=i)	//swap
			{
				for (j=i; j<ncol; j++)
				{
					aswap = ppa[i][j];
					ppa[i][j] = ppa[imax][j];
					ppa[imax][j] = aswap;
				}
				aswap = pb[i];
				pb[i] = pb[imax];
				pb[imax] = aswap;
			}
			//elimimation
			for (k=i+1; k<nrow; k++)
			{
				fac = ppa[k][i]/ppa[i][i];
				for (j=i; j<ncol; j++)
					ppa[k][j] -= fac*ppa[i][j];
				pb[k] -= fac*pb[i];
			}
		}
		//set px[] to zero from i to ncol
		for (k=i; k<ncol; k++)
			px[k] = 0;
		//back substitution
		for (k=i-1; k>=0; k--)
		{
			aswap = 0;
			for (j=k+1; j<ncol; j++)
			{
				aswap += ppa[k][j]*px[j];
			}
			px[k] = (pb[k]-aswap)/ppa[k][k];
		}
		if (brank)
			return 1;
		return 0;
	}

	//method to enforce elemental mass balance for current set of input vector and output vector predicted by CFD or ROM
	//all inlet and outlet flow boundary data have to be assigned before calling this method including unvaried parameters
	//since one UnitOperation class contains one set of input and output, the method enforces the mass balance for one set of data only
	public int enforceElementalMassBalance()
	{
		//return non-zero value if failed
		//assuming updateAllSpeciesList() has been called
		int i, j, k, n;
		Species sp;
		eliminateNegativeProductSpeciesFlow();
		eliminateProductSpeciesWithElementsUnavailableInFeed();	//this will update iElementFeed array
		if (!areAllFeedElementsInProductBoundaries())
		{
			System.out.println("Mass balance failed");
			return 1;
		}
		float[] speciesMoleFlowRate = getSpeciesMoleFlowRate(outletBoundaryList);
		float[] elementMoleFlowFeed = getElementMoleFlowRate(inletBoundaryList);
		float[] elementMoleFlowProduct = getElementMoleFlowRate(outletBoundaryList);
		int nElementFeed = iElementFeed.length;
		int nSpecies = allSpeciesList.size();
		int nSpeciesProduct = 0;
		for (i=0; i<nSpecies; i++)
		{
			if (speciesMoleFlowRate[i]>0)
				nSpeciesProduct++;
		}
		double[] speciesMoleFlowRateNonZero = new double [nSpeciesProduct];
		int[] iAll2Product = new int[nSpecies];
		List<Species> productSpeciesList = new ArrayList<Species>();
		j = 0;
		for (i=0; i<nSpecies; i++)
		{
			if (speciesMoleFlowRate[i]>0)
			{
				speciesMoleFlowRateNonZero[j] = speciesMoleFlowRate[i];
				productSpeciesList.add(allSpeciesList.get(i));
				iAll2Product[i] = j;
				j++;
			}
		}
		double[] correctionFactor;
		if (nElementFeed>nSpeciesProduct)	//use regression equations
		{
			double[][] aij = new double[nElementFeed][];
			for (i=0; i<nElementFeed; i++)
				aij[i] = new double[nSpeciesProduct];
			double[][] aij_reg = new double[nSpeciesProduct][];
			for (i=0; i<nSpeciesProduct; i++)
				aij_reg[i] = new double[nSpeciesProduct];
			double[] b = new double[nElementFeed];
			double[] b_reg = new double[nSpeciesProduct];
			correctionFactor = new double [nSpeciesProduct];	//correction factor + Langrangian coefficients
			//calculate equation for each element
			for (j=0; j<nElementFeed; j++)
			{
				k = iElementFeed[j];
				b[j] = elementMoleFlowFeed[k] - elementMoleFlowProduct[k];
				for (i=0; i<nSpeciesProduct; i++)
				{
					sp = productSpeciesList.get(i);
					aij[j][i] = speciesMoleFlowRateNonZero[i]*sp.getNumberOfAtoms(k);
				}
			}
			//calculate regression equations
			for (i=0; i<nSpeciesProduct; i++)
			{
				for (j=0; j<nSpeciesProduct; j++)
				{
					aij_reg[i][j] = 0;
					for (k=0; k<nElementFeed; k++)
						aij_reg[i][j] += aij[k][i]*aij[k][j];
				}
				b_reg[i] = 0;
				for (k=0; k<nElementFeed; k++)
					b_reg[i] += aij[k][i]*b[k];
			}
			if (GaussianEliminationWithRowPivoting(nSpeciesProduct, nSpeciesProduct, aij_reg, b_reg, correctionFactor)!=0)
				return 3;
		}
		else		//use Lagrangian multiplier method
		{
			//prepare lagrangian minimization matrix
			int nx = nElementFeed + nSpeciesProduct;
			double[] b = new double[nx];
			correctionFactor = new double [nx];	//correction factor + Langrangian coefficients
			double[][] aij = new double[nx][];
			for (i=0; i<nx; i++)
				aij[i] = new double[nx];
			//calculate b vector and matrix coefficient
			for (i=0; i<nSpeciesProduct; i++)
			{
				b[i] = 0;
				for (j=0; j<nSpeciesProduct; j++)
					aij[i][j] = 0;
				aij[i][i] = 2;
				sp = productSpeciesList.get(i);
				for (j=0; j<nElementFeed; j++)
					aij[i][j+nSpeciesProduct] = speciesMoleFlowRateNonZero[i]*sp.getNumberOfAtoms(iElementFeed[j]);
			}
			for (i=0; i<nElementFeed; i++)
			{
				n = i + nSpeciesProduct;
				for (j=0; j<nSpeciesProduct; j++)
				{
					sp = productSpeciesList.get(j);
					aij[n][j] = speciesMoleFlowRateNonZero[j]*sp.getNumberOfAtoms(iElementFeed[i]);
				}
				for (j=0; j<nElementFeed; j++)
					aij[n][nSpeciesProduct+j] = 0;
				k = iElementFeed[i];
				b[n] = elementMoleFlowFeed[k] - elementMoleFlowProduct[k];
				//debug
				//float err = Math.abs((float)(b[n]/elementMoleFlowFeed[k]));
				//System.out.print(err + " " + (float)elementMoleFlowFeed[k] + " ");

			}
			//debug
			//System.out.println();
			if (GaussianEliminationWithRowPivoting(nx, nx, aij, b, correctionFactor)!=0)
				return 3;
		}
		//now do correction
		int iSpecies;
		int iSpeciesAll;
		int nPhase;
		int nBoundary = outletBoundaryList.size();
		float xfloat;
		FlowBoundary fb = null;
		GasMixture gm = null;
		SolidMixture sm = null;
		Parameter param;
		for (i=0; i<nBoundary; i++)
		{
			fb = outletBoundaryList.get(i);
			if (fb.hasGasPhase())
			{
				gm = fb.getGasMixture().get(0);
				for(Map.Entry<Integer,Parameter> entry : gm.getSpeciesFlowMap().entrySet())
				{
					iSpecies = entry.getKey().intValue();
					iSpeciesAll = iSpecies;
					param = entry.getValue();
					//if flow is zero, apply correctionFactor[0], the value is still zero
					xfloat = (float)(param.getDefaultValue()*(1+correctionFactor[iAll2Product[iSpeciesAll]]));
					param.setDefaultValue(xfloat);
				}
			}
			if (fb.hasSolidPhase())
			{
				nPhase = fb.getSolidMixtures().size();
				for (j=0; j<nPhase; j++)
				{
					sm = fb.getSolidMixtures().get(j);
					for(Map.Entry<Integer,Parameter> entry : sm.getSpeciesFlowMap().entrySet())
					{
						iSpecies = entry.getKey().intValue();
						iSpeciesAll = iSolidPhase2All[sm.getPhaseIndex()][iSpecies];
						param = entry.getValue();
						//if flow is zero, apply correctionFactor[0], the value is still zero
						xfloat = (float)(param.getDefaultValue()*(1+correctionFactor[iAll2Product[iSpeciesAll]]));
						param.setDefaultValue(xfloat);
					}
				}
			}
		}
		return 0;
	}

	//this method uses an input file to configure the ROM to be built.
	//the input file can be prepared by GUI
	//this method is not needed if the configuration is handled by GUI directly
	//this method also update two parameter lists: romInputVector and yromOutputVector
	public void readUserSetupFile(String fileName)
	{
		Scanner s = null;
		boolean b;
		int i, j, k;
		int nSpecies;			//number of species
		int iSpecies;			//species index
		int nBoundary;			//number of boundaries
		int iBoundary;			//boundary index
		int nPhase;				//number of solid phases
		int iPhase;				//phase index
		int iSolidType;			//solid type
		int nParameter;			//number of parameters
		float xfloat = 0;
		float xmin = 0;
		float xmax = 0;
		String str;
		String str1;
		FlowBoundary fb = null;
		GasMixture gm = null;
		SolidMixture sm = null;
		Parameter param = null;
		gasSpeciesList.clear();
		solidSpeciesList.clear();
		solidPhaseList.clear();
		solidPhaseTypeList.clear();
		inletBoundaryList.clear();
		outletBoundaryList.clear();
		inputParameterList.clear();
		outputParameterList.clear();
		try
		{
			s = new Scanner(new BufferedReader(new FileReader(fileName)));
			//read gas species list, corresponding to mixed substream in Aspen Plus
			nSpecies = s.nextInt();
			s.nextLine();
			for (i=0; i<nSpecies; i++)
			{
				str = s.next();		//name, corresponding to Aspen's Component ID, all upper cases
				str1 = s.next();	//formula, corresponding to Aspen's Alias, Two letter element has to have the second letter in lower case
				s.nextLine();
				addGasSpecies(str,str1);
			}
			//read solid species lists
			nPhase = s.nextInt();
			s.nextLine();
			setTotalNumberOfSolidPhases(nPhase);
			for (i=0; i<nPhase; i++)
			{
				//name of solid phase
				str = s.next();
				s.nextLine();
				solidPhaseList.add(str);
				//type of solid phase
				iSolidType = s.nextInt();
				s.nextLine();
				solidPhaseTypeList.add(new Integer(iSolidType));
				//number of species in solid phase
				nSpecies = s.nextInt();
				s.nextLine();
				for (j=0; j<nSpecies; j++)
				{
					str = s.next();
					str1 = s.next();
					s.nextLine();
					addSolidSpecies(i,str,str1);
				}
				if (iSolidType==1)		//NC solid, species are fixed as C H O N S Cl Ah H2O
				{
					//checking if valid NC species list
					if (nSpecies!=8)
						System.out.println("Number of species of NC solid is not 8.");
					if (!solidSpeciesList.get(i).get(0).getFormula().equalsIgnoreCase("C"))
						System.out.println("C is not the first Species in NC solid.");
					if (!solidSpeciesList.get(i).get(1).getFormula().equalsIgnoreCase("H"))
						System.out.println("H is not the second Species in NC solid.");
					if (!solidSpeciesList.get(i).get(2).getFormula().equalsIgnoreCase("N"))
						System.out.println("N is not the third Species in NC solid.");
					if (!solidSpeciesList.get(i).get(3).getFormula().equalsIgnoreCase("Cl"))
						System.out.println("Cl is not the fourth Species in NC solid.");
					if (!solidSpeciesList.get(i).get(4).getFormula().equalsIgnoreCase("S"))
						System.out.println("S is not the fifth Species in NC solid.");
					if (!solidSpeciesList.get(i).get(5).getFormula().equalsIgnoreCase("O"))
						System.out.println("O is not the sixth Species in NC solid.");
					if (!solidSpeciesList.get(i).get(6).getFormula().equalsIgnoreCase("Ah"))
						System.out.println("Ah is not the seventh Species in NC solid.");
					if (!solidSpeciesList.get(i).get(7).getFormula().equalsIgnoreCase("H2O"))
						System.out.println("H2O is not the eighth Species in NC solid.");
				}
			}
			//read inlet boundary inputs
			nBoundary = s.nextInt();
			s.nextLine();
			//for each inlet boundary
			for (i=0; i<nBoundary; i++)
			{
				str = s.next();
				s.nextLine();
				//remove '_' or '-'
				str = str.replaceAll("[_-]","");
				//truncate to 15 characters
				if (str.length()>15)
					str = str.substring(0,15);
				iBoundary = s.nextInt();
				s.nextLine();
				fb = new FlowBoundary(iBoundary,str);
				inletBoundaryList.add(fb);
				b = s.nextBoolean();
				s.nextLine();
				fb.enableGasPhase(b);
				b = s.nextBoolean();
				s.nextLine();
				fb.enableSolidPhase(b);
				if (fb.hasGasPhase())
				{
					gm = new GasMixture();
					fb.getGasMixture().add(gm);
					//pressure
					param = gm.getPressure();
					xfloat = s.nextFloat();
					param.setDefaultValue(xfloat);
					b = s.nextBoolean();
					param.enableVaried(b);
					if (b)
					{
						xmin = s.nextFloat();
						param.setMinValue(xmin);
						xmax = s.nextFloat();
						param.setMaxValue(xmax);
					}
					s.nextLine();
					//temperature
					param = gm.getTemperature();
					xfloat = s.nextFloat();
					param.setDefaultValue(xfloat);
					b = s.nextBoolean();
					param.enableVaried(b);
					if (b)
					{
						xmin = s.nextFloat();
						param.setMinValue(xmin);
						xmax = s.nextFloat();
						param.setMaxValue(xmax);
					}
					s.nextLine();
					//volume fraction
					param = gm.getVolumeFraction();
					xfloat = s.nextFloat();
					param.setDefaultValue(xfloat);
					b = s.nextBoolean();
					param.enableVaried(b);
					if (b)
					{
						xmin = s.nextFloat();
						param.setMinValue(xmin);
						xmax = s.nextFloat();
						param.setMaxValue(xmax);
					}
					s.nextLine();
					//has constant mass fractoin?
					b = s.nextBoolean();
					s.nextLine();
					gm.enableConstMassFractions(b);
					if (gm.hasConstMassFractions())
					{
						//total mass flow rate
						param = gm.getTotalMassFlow();
						xfloat = s.nextFloat();
						param.setDefaultValue(xfloat);
						b = s.nextBoolean();
						param.enableVaried(b);
						if (b)
						{
							xmin = s.nextFloat();
							param.setMinValue(xmin);
							xmax = s.nextFloat();
							param.setMaxValue(xmax);
						}
						s.nextLine();
						nSpecies = s.nextInt();
						s.nextLine();
						for (j=0; j<nSpecies; j++)
						{
							iSpecies = s.nextInt();
							xfloat = s.nextFloat();
							s.nextLine();
							//putSpeciesFlow() make Parameter.isVaried false
							gm.putSpeciesFlow(iSpecies,xfloat);
						}
					}
					else
					{
						nSpecies = s.nextInt();
						s.nextLine();
						for (j=0; j<nSpecies; j++)
						{
							iSpecies = s.nextInt();
							xfloat = s.nextFloat();
							b = s.nextBoolean();
							if (b)
							{
								xmin = s.nextFloat();
								xmax = s.nextFloat();
								gm.putSpeciesFlow(iSpecies,xfloat,xmin,xmax);	//Parameter.isVaried is true
							}
							else
								gm.putSpeciesFlow(iSpecies,xfloat);				//Parameter.isVaried is false
							s.nextLine();
						}
					}
					gm.setAllParemeterAliasAs(fb.getBoundaryName() + "GP");
				}
				if (fb.hasSolidPhase())
				{
					nPhase = s.nextInt();
					s.nextLine();
					for (j=0; j<nPhase; j++)
					{
						sm = new SolidMixture();
						fb.getSolidMixtures().add(sm);
						b = s.nextBoolean();
						s.nextLine();
						sm.enableGranularEnergySolved(b);
						if (sm.isGranularEnergySolved())
						{
							param = sm.getGranularTemperature();
							xfloat = s.nextFloat();
							param.setDefaultValue(xfloat);
							b = s.nextBoolean();
							param.enableVaried(b);
							if (b)
							{
								xmin = s.nextFloat();
								param.setMinValue(xmin);
								xmax = s.nextFloat();
								param.setMaxValue(xmax);
							}
							s.nextLine();
						}
						//phase index
						iPhase = s.nextInt();
						s.nextLine();
						sm.setPhaseIndex(iPhase);
						//set solid type based on solidPhaseTypeList
						sm.setSolidType(solidPhaseTypeList.get(iPhase));
						//pressure
						param = sm.getPressure();
						xfloat = s.nextFloat();
						param.setDefaultValue(xfloat);
						b = s.nextBoolean();
						param.enableVaried(b);
						if (b)
						{
							xmin = s.nextFloat();
							param.setMinValue(xmin);
							xmax = s.nextFloat();
							param.setMaxValue(xmax);
						}
						s.nextLine();
						//temperature
						param = sm.getTemperature();
						xfloat = s.nextFloat();
						param.setDefaultValue(xfloat);
						b = s.nextBoolean();
						param.enableVaried(b);
						if (b)
						{
							xmin = s.nextFloat();
							param.setMinValue(xmin);
							xmax = s.nextFloat();
							param.setMaxValue(xmax);
						}
						s.nextLine();
						//volume fraction
						param = sm.getVolumeFraction();
						xfloat = s.nextFloat();
						param.setDefaultValue(xfloat);
						b = s.nextBoolean();
						param.enableVaried(b);
						if (b)
						{
							xmin = s.nextFloat();
							param.setMinValue(xmin);
							xmax = s.nextFloat();
							param.setMaxValue(xmax);
						}
						s.nextLine();
						//diameter
						param = sm.getDiameter();
						xfloat = s.nextFloat();
						param.setDefaultValue(xfloat);
						b = s.nextBoolean();
						param.enableVaried(b);
						if (b)
						{
							xmin = s.nextFloat();
							param.setMinValue(xmin);
							xmax = s.nextFloat();
							param.setMaxValue(xmax);
						}
						s.nextLine();
						//density
						param = sm.getDensity();
						xfloat = s.nextFloat();
						param.setDefaultValue(xfloat);
						b = s.nextBoolean();
						param.enableVaried(b);
						if (b)
						{
							xmin = s.nextFloat();
							param.setMinValue(xmin);
							xmax = s.nextFloat();
							param.setMaxValue(xmax);
						}
						s.nextLine();
						if (sm.getSolidType()==1)	//NC solid
						{
							//volatile matter
							param = sm.getVm();
							xfloat = s.nextFloat();
							param.setDefaultValue(xfloat);
							b = s.nextBoolean();
							param.enableVaried(b);
							if (b)
							{
								xmin = s.nextFloat();
								param.setMinValue(xmin);
								xmax = s.nextFloat();
								param.setMaxValue(xmax);
							}
							s.nextLine();
						}
						//constant mass fractions?
						b = s.nextBoolean();
						s.nextLine();
						sm.enableConstMassFractions(b);
						if (sm.hasConstMassFractions())
						{
							//total mass flow
							param = sm.getTotalMassFlow();
							xfloat = s.nextFloat();
							param.setDefaultValue(xfloat);
							b = s.nextBoolean();
							param.enableVaried(b);
							if (b)
							{
								xmin = s.nextFloat();
								param.setMinValue(xmin);
								xmax = s.nextFloat();
								param.setMaxValue(xmax);
							}
							s.nextLine();
							nSpecies = s.nextInt();
							s.nextLine();
							for (k=0; k<nSpecies; k++)
							{
								iSpecies = s.nextInt();
								xfloat = s.nextFloat();
								s.nextLine();
								//putSpeciesFlow() make Parameter.isVaried false
								sm.putSpeciesFlow(iSpecies,xfloat);
							}
						}
						else
						{
							nSpecies = s.nextInt();
							s.nextLine();
							for (k=0; k<nSpecies; k++)
							{
								iSpecies = s.nextInt();
								xfloat = s.nextFloat();
								b = s.nextBoolean();
								if (b)
								{
									xmin = s.nextFloat();
									xmax = s.nextFloat();
									sm.putSpeciesFlow(iSpecies,xfloat,xmin,xmax);	//Parameter.isVaried is true
								}
								else
									sm.putSpeciesFlow(iSpecies,xfloat);				//Parameter.isVaried is false
								s.nextLine();
							}
						}
						sm.setAllParemeterAliasAs(fb.getBoundaryName() + "SP" + iPhase);
					}
				}
			}//end of inlet boundary for loop
			//read input parameters
			nParameter = s.nextInt();
			s.nextLine();
			for (i=0; i<nParameter; i++)
			{
				str = s.next();
				param = new Parameter(str,"");
				inputParameterList.add(param);
				xfloat = s.nextFloat();
				param.setDefaultValue(xfloat);
				b = s.nextBoolean();
				param.enableVaried(b);
				if (b)
				{
					xmin = s.nextFloat();
					param.setMinValue(xmin);
					xmax = s.nextFloat();
					param.setMaxValue(xmax);
				}
				s.nextLine();
			}
			//read outlet boundary results
			nBoundary = s.nextInt();
			s.nextLine();
			for (i=0; i<nBoundary; i++)
			{
				str = s.next();
				s.nextLine();
				//remove '_' or '-'
				str = str.replaceAll("[_-]","");
				if (str.length()>15)
					str = str.substring(0,15);
				iBoundary = s.nextInt();
				s.nextLine();
				fb = new FlowBoundary(iBoundary,str);
				outletBoundaryList.add(fb);
				b = s.nextBoolean();
				s.nextLine();
				fb.enableGasPhase(b);
				b = s.nextBoolean();
				s.nextLine();
				fb.enableSolidPhase(b);
				if (fb.hasGasPhase())
				{
					gm = new GasMixture();
					fb.getGasMixture().add(gm);
					//always disable constant mass fraction flag
					gm.enableConstMassFractions(false);
					//pressure
					param = gm.getPressure();
					b = s.nextBoolean();
					param.enableVaried(b);
					if (!b)
					{
						xfloat = s.nextFloat();
						param.setDefaultValue(xfloat);
					}
					s.nextLine();
					//temperature
					param = gm.getTemperature();
					b = s.nextBoolean();
					param.enableVaried(b);
					if (!b)
					{
						xfloat = s.nextFloat();
						param.setDefaultValue(xfloat);
					}
					s.nextLine();
					//volume fraction
					param = gm.getVolumeFraction();
					b = s.nextBoolean();
					param.enableVaried(b);
					if (!b)
					{
						xfloat = s.nextFloat();
						param.setDefaultValue(xfloat);
					}
					s.nextLine();
					//read number of species in the gas phase
					nSpecies = s.nextInt();
					s.nextLine();
					//read species indices on the gas species list
					for (k=0; k<nSpecies; k++)
					{
						iSpecies = s.nextInt();
						s.nextLine();
						gm.putSpeciesFlow(iSpecies, 0, 0, 0);
					}
					gm.setAllParemeterAliasAs(fb.getBoundaryName() + "GP");
				}
				if (fb.hasSolidPhase())
				{
					nPhase = s.nextInt();
					s.nextLine();
					for (j=0; j<nPhase; j++)
					{
						sm = new SolidMixture();
						//always disable constant mass fraction flag
						sm.enableConstMassFractions(false);
						fb.getSolidMixtures().add(sm);
						b = s.nextBoolean();
						s.nextLine();
						sm.enableGranularEnergySolved(b);
						if (sm.isGranularEnergySolved())
						{
							//granular temperature
							param = sm.getGranularTemperature();
							b = s.nextBoolean();
							param.enableVaried(b);
							if (!b)
							{
								xfloat = s.nextFloat();
								param.setDefaultValue(xfloat);
							}
							s.nextLine();
						}
						iPhase = s.nextInt();
						s.nextLine();
						sm.setPhaseIndex(iPhase);
						//set solid type based on solidPhaseTypeList
						sm.setSolidType(solidPhaseTypeList.get(iPhase));
						//pressure
						param = sm.getPressure();
						b = s.nextBoolean();
						param.enableVaried(b);
						if (!b)
						{
							xfloat = s.nextFloat();
							param.setDefaultValue(xfloat);
						}
						s.nextLine();
						//temperature
						param = sm.getTemperature();
						b = s.nextBoolean();
						param.enableVaried(b);
						if (!b)
						{
							xfloat = s.nextFloat();
							param.setDefaultValue(xfloat);
						}
						s.nextLine();
						//volume fraction
						param = sm.getVolumeFraction();
						b = s.nextBoolean();
						param.enableVaried(b);
						if (!b)
						{
							xfloat = s.nextFloat();
							param.setDefaultValue(xfloat);
						}
						s.nextLine();
						//diameter
						param = sm.getDiameter();
						b = s.nextBoolean();
						param.enableVaried(b);
						if (!b)
						{
							xfloat = s.nextFloat();
							param.setDefaultValue(xfloat);
						}
						s.nextLine();
						//density
						param = sm.getDensity();
						b = s.nextBoolean();
						param.enableVaried(b);
						if (!b)
						{
							xfloat = s.nextFloat();
							param.setDefaultValue(xfloat);
						}
						s.nextLine();
						if (sm.getSolidType()==1)	//NC solid
						{
							//volatile matter
							param = sm.getVm();
							b = s.nextBoolean();
							param.enableVaried(b);
							if (!b)
							{
								xfloat = s.nextFloat();
								param.setDefaultValue(xfloat);
							}
							s.nextLine();
						}
						//read number of species in the solid phase
						nSpecies = s.nextInt();
						s.nextLine();
						for (k=0; k<nSpecies; k++)
						{
							iSpecies = s.nextInt();
							s.nextLine();
							sm.putSpeciesFlow(iSpecies, 0, 0, 0);
						}
						sm.setAllParemeterAliasAs(fb.getBoundaryName() + "SP" + iPhase);
					}
				}
			}//end of outlet boundary for loop
			//read output parameters
			nParameter = s.nextInt();
			s.nextLine();
			for (i=0; i<nParameter; i++)
			{
				str = s.next();
				s.nextLine();
				param = new Parameter(str,"");
				//always set output parameter and varied
				param.enableVaried(true);
				outputParameterList.add(param);
			}
			//read number of samples or cases
			nSample = s.nextInt();
			s.nextLine();
			//read regression method
			str = s.next();
			if (str.equalsIgnoreCase("Kriging"))
				regMethod = RegressionMethod.KRIGING;
			else if (str.equalsIgnoreCase("ANN"))
				regMethod = RegressionMethod.ANN;

		}//end of try
		catch(IOException e)
		{
			System.out.println(e);
		}
		finally
		{
			s.close();
		}
		//update input and output vector (of parameters)
		updateRomInputVector();
		updateYromOutputVector();
	}

	public void readUserJsonFile(String fileName)
	{
		Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().setPrettyPrinting().create();
		try
		{
			int i, j;
			int nSpecies;
			int nPhase;
			Species sp;
			List<Species> splist;
			BufferedReader br = new BufferedReader(new FileReader(fileName));
			UnitOperation obj = gson.fromJson(br,UnitOperation.class);
			br.close();
			setName(obj.getName());
			setRegressionMethod(obj.getRegressionMethod());
			setNumberOfSamples(obj.getNumberOfSamples());
			//set gas species list data
			splist = obj.getGasSpeciesList();
			nSpecies = splist.size();
			for (i=0; i<nSpecies; i++)
			{
				sp = splist.get(i);
				addGasSpecies(sp.getName(),sp.getFormula());
			}
			nPhase = obj.getSolidSpeciesList().size();
			setTotalNumberOfSolidPhases(nPhase);
			for (i=0; i<nPhase; i++)
			{
				splist = obj.getSolidSpeciesList().get(i);
				nSpecies = splist.size();
				for (j=0; j<nSpecies; j++)
					addSolidSpecies(i,splist.get(j).getName(),splist.get(j).getFormula());
			}
			setSolidPhaseList(obj.getSolidPhaseList());
			setSolidPhaseTypeList(obj.getSolidPhaseTypeList());
			setInletBoundaryList(obj.getInletBoundaryList());
			setOutletBoundaryList(obj.getOutletBoundaryList());
			setInputParameterList(obj.getInputParameterList());
			setOutputParameterList(obj.getOutputParameterList());
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		//update alias
		for (FlowBoundary fb : inletBoundaryList)
		{
			for (GasMixture gm : fb.getGasMixture())
			   gm.setAllParemeterAliasAs(fb.getBoundaryName() + "GP");
			for (SolidMixture sm : fb.getSolidMixtures())
			{
				int iPhase = sm.getPhaseIndex();
				sm.setAllParemeterAliasAs(fb.getBoundaryName() + "SP" + iPhase);
			}
		}
		for (FlowBoundary fb : outletBoundaryList)
		{
			for (GasMixture gm : fb.getGasMixture())
			   gm.setAllParemeterAliasAs(fb.getBoundaryName() + "GP");
			for (SolidMixture sm : fb.getSolidMixtures())
			{
				int iPhase = sm.getPhaseIndex();
				sm.setAllParemeterAliasAs(fb.getBoundaryName() + "SP" + iPhase);
			}
		}
		updateRomInputVector();
		updateYromOutputVector();
	}

	//read a ROM input vector from a text file
	public void readAnInputVector(String fileName)
	{
		int i;
		int nParam;
		float xfloat;
		Scanner s = null;
		Parameter param;
		try
		{
			s = new Scanner(new BufferedReader(new FileReader(fileName)));
			nParam = romInputVector.size();
			for (i=0; i<nParam; i++)
			{
				param = romInputVector.get(i);
				xfloat = s.nextFloat();
				param.setDefaultValue(xfloat);
			}
		}//end of try
		catch(IOException e)
		{
			System.out.println(e);
		}
		finally
		{
			s.close();
		}
	}

	//set a ROM input vector by passing in an array
	public void setAnInputVector(float[] v)
	{
		int i;
		int nParam = romInputVector.size();
		Parameter param;
		for (i=0; i<nParam; i++)
		{
			param = romInputVector.get(i);
			param.setDefaultValue(v[i]);
		}
	}

	//read a ROM output vector from a text file
	public void readAnOutputVector(String fileName)
	{
		int i;
		int nParam;
		float xfloat;
		Scanner s = null;
		Parameter param;
		try
		{
			s = new Scanner(new BufferedReader(new FileReader(fileName)));
			nParam = yromOutputVector.size();
			for (i=0; i<nParam; i++)
			{
				param = yromOutputVector.get(i);
				xfloat = s.nextFloat();
				param.setDefaultValue(xfloat);
			}
		}//end of try
		catch(IOException e)
		{
			System.out.println(e);
		}
		finally
		{
			s.close();
		}
	}

	//set a ROM Output vector by passing in an array
	public void setAnOutputVector(float[] v)
	{
		int i;
		int nParam = yromOutputVector.size();
		Parameter param;
		for (i=0; i<nParam; i++)
		{
			param = yromOutputVector.get(i);
			param.setDefaultValue(v[i]);
		}
	}

	//save corrected ROM output vector to a file
	public void wrtieCorrectedOutputVector(String fileName)
	{
		int i;
		int nParam;
		float xfloat;
		Parameter param;
		try
		{
			FileWriter outFile = new FileWriter(fileName);
			PrintWriter out = new PrintWriter(outFile);
			nParam = yromOutputVector.size();
			for (i=0; i<nParam; i++)
			{
				param = yromOutputVector.get(i);
				xfloat = param.getDefaultValue();
				out.print(xfloat);
				out.print("\t");
			}
			out.close();
		}//end of try
		catch(IOException e)
		{
			System.out.println(e);
		}
	}

	//export ROM to ACM source code
	public void exportACMRom()
	{
		String fileName = getName();
		String fileNameWithExtension;
		//make sure the fileName contains extension ".acmf"
		int iPoint = fileName.lastIndexOf(".");
		if (iPoint>=0)
			fileNameWithExtension = fileName.substring(0,iPoint) + ".acmf";
		else
			fileNameWithExtension = fileName + ".acmf";
		try
		{
			FileWriter outFile = new FileWriter(fileNameWithExtension);
			PrintWriter out = new PrintWriter(outFile);
			boolean bFirst;
			int i, i1, j, j1, k;
			int index;
			int iElement;
			int nElement;
			int iSpecies;
			int nSpecies;
			int iSpeciesAll;
			int nPhase;
			int nBoundary;
			int nParameter;
			int nLinearEqn;
			float defaultValue;
			String strMolarFlowAll;
			String boundaryName;
			String portName;
			String variableName;
			String massFractionName;
			String speciesName;
			String speciesNameMod;	//modified by replacing the + or - sign
			String totalMassInEqn = "TotalMassIn = ";
			String totalEnthalpyInEqn = "TotalEnthalpyIn = ";
			String totalEnthalpyOutEqn = "TotalEnthalpyOut = ";
			String subStreamNames = "SubStreamNames: ";
			String subStreamType = "CISOLID";
			String gasFeedPortName = null;				//used to set component list and set component attributes of product port based on feed port
			String solidFeedPortNames[] = new String[solidPhaseList.size()];		//used to set component list and set component attributes of product port based on feed port
			String aspenSubStreamNames[] = new String[4];
			String connectionName = null;		//portName + ".connection(" + portName + "Name+i)"
			String ncName = null;		//solidPhaseList.get(sm.getPhaseIndex()), NC species name in Aspen Plus
			StringBuffer strEqns = new StringBuffer(10000);
			aspenSubStreamNames[0] = "\"CISOLID\"";
			aspenSubStreamNames[1] = "\"NC\"";
			aspenSubStreamNames[2] = "\"CIPSD\"";
			aspenSubStreamNames[3] = "\"NCPSD\"";
			FlowBoundary fb;
			GasMixture gm;
			SolidMixture sm;
			Parameter param;
			Parameter param_heatloss = null;
			Species sp;
			Map<Integer,Parameter> flowMap;
			out.println("//ACM source file exported from iREVEAL");
			out.println("MODEL " + getName());
			//set tolerance as ACM RealParameter
			out.println("//Tolerance for fixed feed port variables used by ROM");
			out.println("LowerTolerance as RealParameter(" + lowerTolerance + ");");
			out.println("UpperTolerance as RealParameter(" + upperTolerance + ");");
			//allow user to set energy balance correction options
			out.println("//Energy balance correcton options. 0: correct heat loss, 1: correct temperature");
			out.println("EnergyCorrectionOption as IntegerParameter(0);");
			out.println("TotalMassIn as RealVariable;");
			out.println("TotalEnthalpyIn as RealVariable;");
			out.println("TotalEnthalpyOut as RealVariable;");
			out.println("Heat_Loss_Corrected as RealVariable;");
			out.println("H_correction as RealVariable;");
			//heat loss term
			out.println("Qout as OUTPUT HeatPort;");
			//port declaration section
			out.println("//Feed and product ports");
			bFirst = true;
			//declare feed ports, also collect the port name for gas and each solid phase
			//it is assumed that all the solid phases are used in feed streams so we can copy the component attributes to the product streams
			nBoundary = inletBoundaryList.size();
			for (i=0; i<nBoundary; i++)
			{
				fb = inletBoundaryList.get(i);
				boundaryName = fb.getBoundaryName();
				if (fb.hasGasPhase())
				{
					portName = boundaryName + "GP";
					if (gasFeedPortName==null)		//use the gas phase of the first feed port and assign component attributes to product port
						gasFeedPortName = portName;
					out.println(portName + " as INPUT MultiPort of SolidPort;");
					out.println(portName + "Name as StringParameter(\"In" + i + "GP\");");
					out.println(portName + "SSNs as hidden stringset;");
					out.println(portName + "SSNs: Union(" + portName + ".Connection.SubStreamName)-\"\";");
					if (bFirst)
					{
						subStreamNames += portName + "SSNs";
						bFirst = false;
					}
					else
						subStreamNames += "*" + portName + "SSNs";
				}
				if (fb.hasSolidPhase())
				{
					nPhase = fb.getSolidMixtures().size();
					for (j=0; j<nPhase; j++)
					{
						sm = fb.getSolidMixtures().get(j);
						k = sm.getPhaseIndex();
						portName = boundaryName + "SP" + k;
						if (solidFeedPortNames[k]==null)
							solidFeedPortNames[k] = portName;
						out.println(portName + " as INPUT MultiPort of SolidPort;");
						out.println(portName + "Name as StringParameter(\"In" + i + "SP" + j + "\");");
						out.println(portName + "SSNs as hidden stringset;");
						out.println(portName + "SSNs: Union(" + portName + ".Connection.SubStreamName)-\"\";");
						if (bFirst)
						{
							subStreamNames += portName + "SSNs";
							bFirst = false;
						}
						else
							subStreamNames += "*" + portName + "SSNs";
					}
				}
			}
			//declare product ports
			nBoundary = outletBoundaryList.size();
			for (i=0; i<nBoundary; i++)
			{
				fb = outletBoundaryList.get(i);
				boundaryName = fb.getBoundaryName();
				if (fb.hasGasPhase())
				{
					portName = boundaryName + "GP";
					out.println(portName + " as OUTPUT MultiPort of SolidPort;");
					out.println(portName + "Name as StringParameter(\"Out" + i + "GP\");");
					out.println(portName + "SSNs as hidden stringset;");
					out.println(portName + "SSNs: Union(" + portName + ".Connection.SubStreamName)-\"\";");
					subStreamNames += "*" + portName + "SSNs";
				}
				if (fb.hasSolidPhase())
				{
					nPhase = fb.getSolidMixtures().size();
					for (j=0; j<nPhase; j++)
					{
						sm = fb.getSolidMixtures().get(j);
						portName = boundaryName + "SP" + sm.getPhaseIndex();
						out.println(portName + " as OUTPUT MultiPort of SolidPort;");
						out.println(portName + "Name as StringParameter(\"Out" + i + "SP" + j + "\");");
						out.println(portName + "SSNs as hidden stringset;");
						out.println(portName + "SSNs: Union(" + portName + ".Connection.SubStreamName)-\"\";");
						subStreamNames += "*" + portName + "SSNs";
					}
				}
			}
			out.println("SubStreamNames as hidden stringset(Description:\"Set of names of the substreams that are connected to the model\");");
			out.println(subStreamNames + ";");
			//check if all solid phase names are attached to the unit operation
			//note that when Aspen Plus open a saved flowsheet, the solid phase variables are not loaded, which makes "containsAllSolidNames" FALSE
			out.println("containsAllSolidNames as LogicalParameter;");
			out.print("containsAllSolidNames : ");
			nPhase = solidPhaseTypeList.size();
			if (nPhase==0)
				out.println("TRUE;");
			else
			{
				bFirst = true;
				for (j=0; j<4; j++)
				{
					for (i=0; i<nPhase; i++)
					{
						if (solidPhaseTypeList.get(i).intValue()==j)
						{
							if (bFirst)
								bFirst = false;
							else
								out.print(" and ");
							out.print(aspenSubStreamNames[j] + " in SubStreamNames");
							break;
						}
					}
				}
				out.println(";");
			}
			//check if all species involved are in the component list
			//first check gas phase species
			out.println("containsGasSpecies as LogicalParameter;");
			out.print("containsGasSpecies : ");
			nSpecies = gasSpeciesList.size();
			if (nSpecies==0)
				out.println("TRUE;");
			else
			{
				for (i=0; i<nSpecies; i++)
				{
					if (i>0)
						out.print(" and ");
					sp = gasSpeciesList.get(i);
					out.print("\"" + sp.getName() + "\" in " + gasFeedPortName + ".connection(" + gasFeedPortName + "Name + \"MIXED\").Componentlist");
				}
				out.println(";");
			}
			//then check the solid species
			out.println("containsSolidSpecies as LogicalParameter;");
			out.println("if (not containsAllSolidNames) then");
			out.println("  containsSolidSpecies : FALSE;");
			out.println("else");
			out.print("  containsSolidSpecies : ");
			if (nPhase==0)
				out.println("TRUE;");
			else
			{
				for (i=0; i<nPhase; i++)
				{
					if (solidPhaseTypeList.get(i).intValue()==0)	//CISOLID
					{
						nSpecies = solidSpeciesList.get(i).size();
						for (j=0; j<nSpecies; j++)
						{
							if (i>0 || j>0)
								out.print(" and ");
							sp = solidSpeciesList.get(i).get(j);
							out.print("\"" + sp.getName() + "\" in " + solidFeedPortNames[i] + ".connection(" + solidFeedPortNames[i] + "Name + " + aspenSubStreamNames[solidPhaseTypeList.get(i).intValue()] + ").Componentlist");
						}
					}
					else		//NC
					{
						if (i>0)
							out.print(" and ");
						out.print("\"" + solidPhaseList.get(i) + "\" in " + solidFeedPortNames[i] + ".connection(" + solidFeedPortNames[i] + "Name + " + aspenSubStreamNames[solidPhaseTypeList.get(i).intValue()] + ").Componentlist");
					}
				}
				out.println(";");
			}
			out.println("endif");
			//set product ports' component list, component attribute and others, do this only valid solid phases and valid species are available
			out.println("IF containsAllSolidNames AND containsGasSpecies AND containsSolidSpecies THEN");
			out.println("for i in SubStreamNames do");
			nBoundary = outletBoundaryList.size();
			for (i=0; i<nBoundary; i++)
			{
				fb = outletBoundaryList.get(i);
				boundaryName = fb.getBoundaryName();
				if (fb.hasGasPhase())
				{
					portName = boundaryName + "GP";
					connectionName = portName + ".connection(" + portName + "Name+i)";
					out.println("  if (" + portName + "Name+i) in " + portName + ".ConnectionSet and (" + gasFeedPortName + "Name+i) in " + gasFeedPortName + ".ConnectionSet then");
					out.println("    " + connectionName + ".SubStreamType : " + gasFeedPortName + ".connection(" + gasFeedPortName + "Name+i).SubStreamType;");
					out.println("    " + connectionName + ".componentlist : " + gasFeedPortName + ".connection(" + gasFeedPortName + "Name+i).componentlist;");
					out.println("    if (" + gasFeedPortName + ".connection(" + gasFeedPortName + "Name+i).SubStreamType in [\"CISOLID\",\"CIPSD\"]) then");
					out.println("      " + connectionName + ".F = 0;");
					out.println("      " + connectionName + ".T = " + gasFeedPortName + ".connection(" + gasFeedPortName + "Name+i).T;");
					out.println("      " + connectionName + ".P = " + portName + ".connection(" + portName + "Name+\"MIXED\").P;");
					out.println("    endif");
					out.println("    if (" + gasFeedPortName + ".connection(" + gasFeedPortName + "Name+i).SubStreamType in [\"NC\",\"NCPSD\"]) then");
					out.println("      " + connectionName + ".Fm = 0;");
					out.println("      " + connectionName + ".T = " + gasFeedPortName + ".connection(" + gasFeedPortName + "Name+i).T;");
					out.println("      " + connectionName + ".P = " + portName + ".connection(" + portName + "Name+\"MIXED\").P;");
					out.println("    endif");
					out.println("    if (" + gasFeedPortName + ".connection(" + gasFeedPortName + "Name+i).SubStreamType in [\"CISOLID\",\"NC\"]) then");
					out.println("      if (" + gasFeedPortName + ".connection(" + gasFeedPortName + "Name+i).NIntervals > 0 ) then");
					out.println("        " + connectionName + ".NIntervals : " + gasFeedPortName + ".connection(" + gasFeedPortName + "Name+i).NIntervals;");
					out.println("        " + connectionName + ".PSDZw = " + gasFeedPortName + ".connection(" + gasFeedPortName + "Name+i).PSDZw;");
					out.println("      endif");
					out.println("    endif");
					out.println("    if (" + gasFeedPortName + ".connection(" + gasFeedPortName + "Name+i).SubStreamType in [\"MIXED\",\"CISOLID\"]) then");
					out.println("      " + connectionName + ".CAUSR1 = " +gasFeedPortName + ".connection(" + gasFeedPortName + "Name+i).CAUSR1;");
					out.println("      " + connectionName + ".CAUSR2 = " +gasFeedPortName + ".connection(" + gasFeedPortName + "Name+i).CAUSR2;");
					out.println("      " + connectionName + ".CAUSR3 = " +gasFeedPortName + ".connection(" + gasFeedPortName + "Name+i).CAUSR3;");
					out.println("      " + connectionName + ".CAUSR4 = " +gasFeedPortName + ".connection(" + gasFeedPortName + "Name+i).CAUSR4;");
					out.println("      " + connectionName + ".CAUSR5 = " +gasFeedPortName + ".connection(" + gasFeedPortName + "Name+i).CAUSR5;");
					out.println("      " + connectionName + ".CAUSRA = " +gasFeedPortName + ".connection(" + gasFeedPortName + "Name+i).CAUSRA;");
					out.println("      " + connectionName + ".CAUSRB = " +gasFeedPortName + ".connection(" + gasFeedPortName + "Name+i).CAUSRB;");
					out.println("      " + connectionName + ".CAUSRC = " +gasFeedPortName + ".connection(" + gasFeedPortName + "Name+i).CAUSRC;");
					out.println("      " + connectionName + ".CAUSRD = " +gasFeedPortName + ".connection(" + gasFeedPortName + "Name+i).CAUSRD;");
					out.println("      " + connectionName + ".CAUSRE = " +gasFeedPortName + ".connection(" + gasFeedPortName + "Name+i).CAUSRE;");
					out.println("      " + connectionName + ".CACLASS0 = " +gasFeedPortName + ".connection(" + gasFeedPortName + "Name+i).CACLASS0;");
					out.println("    endif");
					out.println("    if (" + gasFeedPortName + ".connection(" + gasFeedPortName + "Name+i).SubStreamType in [\"NC\"]) then");
					out.println("      " + connectionName + ".PROXANAL = " +gasFeedPortName + ".connection(" + gasFeedPortName + "Name+i).PROXANAL;");
					out.println("      " + connectionName + ".ULTANAL = " +gasFeedPortName + ".connection(" + gasFeedPortName + "Name+i).ULTANAL;");
					out.println("      " + connectionName + ".SULFANAL = " +gasFeedPortName + ".connection(" + gasFeedPortName + "Name+i).SULFANAL;");
					out.println("      " + connectionName + ".GENANAL = " +gasFeedPortName + ".connection(" + gasFeedPortName + "Name+i).GENANAL;");
					out.println("    endif");
					out.println("  endif");
				}
				if (fb.hasSolidPhase())
				{
					nPhase = fb.getSolidMixtures().size();
					for (j=0; j<nPhase; j++)
					{
						sm = fb.getSolidMixtures().get(j);
						k = sm.getPhaseIndex();
						i1 = solidPhaseTypeList.get(k).intValue();
						portName = boundaryName + "SP" + k;
						connectionName = portName + ".connection(" + portName + "Name+i)";
						out.println("  if (" + portName + "Name+i) in " + portName + ".ConnectionSet and (" + solidFeedPortNames[k] + "Name+i) in " + solidFeedPortNames[k] + ".ConnectionSet then");
						out.println("    " + connectionName + ".SubStreamType : " + solidFeedPortNames[k] + ".connection(" + solidFeedPortNames[k] + "Name+i).SubStreamType;");
						out.println("    " + connectionName + ".componentlist : " + solidFeedPortNames[k] + ".connection(" + solidFeedPortNames[k] + "Name+i).componentlist;");
						if (solidPhaseTypeList.get(k).intValue()==0 || solidPhaseTypeList.get(k).intValue()==2)		//CISOLID or CIPSD for current solid port
						{
							out.println("    if (" + solidFeedPortNames[k] + ".connection(" + solidFeedPortNames[k] + "Name+i).SubStreamType in [\"MIXED\"]) then");
							out.println("      " + connectionName + ".F = 0;");
							out.println("      " + connectionName + ".T = " + solidFeedPortNames[k] + ".connection(" + solidFeedPortNames[k] + "Name+i).T;");
							out.println("      " + connectionName + ".P = " + portName + ".connection(" + portName + "Name+" + aspenSubStreamNames[i1] + ").P;");
							out.println("    endif");
							out.println("    if (" + solidFeedPortNames[k] + ".connection(" + solidFeedPortNames[k] + "Name+i).SubStreamType in [\"NC\",\"NCPSD\"]) then");
							out.println("      " + connectionName + ".Fm = 0;");
							out.println("      " + connectionName + ".T = " + solidFeedPortNames[k] + ".connection(" + solidFeedPortNames[k] + "Name+i).T;");
							out.println("      " + connectionName + ".P = " + portName + ".connection(" + portName + "Name+" + aspenSubStreamNames[i1] + ").P;");
							out.println("    endif");
							out.println("    if (" + solidFeedPortNames[k] + ".connection(" + solidFeedPortNames[k] + "Name+i).SubStreamType in [\"NC\"]) then");
							out.println("      " + connectionName + ".PROXANAL = " + solidFeedPortNames[k] + ".connection(" + solidFeedPortNames[k] + "Name+i).PROXANAL;");
							out.println("      " + connectionName + ".ULTANAL = " + solidFeedPortNames[k] + ".connection(" + solidFeedPortNames[k] + "Name+i).ULTANAL;");
							out.println("      " + connectionName + ".SULFANAL = " + solidFeedPortNames[k] + ".connection(" + solidFeedPortNames[k] + "Name+i).SULFANAL;");
							out.println("      " + connectionName + ".GENANAL = " + solidFeedPortNames[k] + ".connection(" + solidFeedPortNames[k] + "Name+i).GENANAL;");
							out.println("    endif");
						}
						else	//NC or NCPSD for current solid port
						{
							out.println("    if (" + solidFeedPortNames[k] + ".connection(" + solidFeedPortNames[k] + "Name+i).SubStreamType in [\"MIXED\",\"CISOLID\",\"CIPSD\"]) then");
							out.println("      " + connectionName + ".F = 0;");
							out.println("      " + connectionName + ".T = " + solidFeedPortNames[k] + ".connection(" + solidFeedPortNames[k] + "Name+i).T;");
							out.println("      " + connectionName + ".P = " + portName + ".connection(" + portName + "Name+" + aspenSubStreamNames[i1] + ").P;");
							out.println("    endif");
							//for NC, we need to specify PROXANAL ULTANAL ane SULANAL should be obtained from model output parameter
							out.println("    if (" + solidFeedPortNames[k] + ".connection(" + solidFeedPortNames[k] + "Name+i).SubStreamType in [\"NC\",\"NCPSD\"]) then");
							out.println("      " + connectionName + ".Zm(\"" + solidPhaseList.get(sm.getPhaseIndex()) + "\") = 1;");
							out.println("      for j in " + connectionName + ".Componentlist - [\"" + solidPhaseList.get(sm.getPhaseIndex()) + "\"] DO");
							out.println("        " + connectionName + ".Zm(j) = 0;");
							out.println("      endfor");
							//initialize NC arrays, revised on 2/22/2018
							out.println("      " + connectionName + ".PROXANAL:0.0;");
							out.println("      " + connectionName + ".ULTANAL:0.0;");
							out.println("      " + connectionName + ".SULFANAL:0.0;");
							out.println("      " + connectionName + ".GENANAL:0.0;");
							//end of revision on 2/22/2018
							out.println("      " + connectionName + ".GENANAL = " + solidFeedPortNames[k] + ".connection(" + solidFeedPortNames[k] + "Name+i).GENANAL;");
							out.println("    endif");
						}
						out.println("    if (" + solidFeedPortNames[k] + ".connection(" + solidFeedPortNames[k] + "Name+i).SubStreamType in [\"CISOLID\",\"NC\"]) then");
						out.println("      if (" + solidFeedPortNames[k] + ".connection(" + solidFeedPortNames[k] + "Name+i).NIntervals > 0 ) then");
						out.println("        " + connectionName + ".NIntervals : " + solidFeedPortNames[k] + ".connection(" + solidFeedPortNames[k] + "Name+i).NIntervals;");
						out.println("        " + connectionName + ".PSDZw = " + solidFeedPortNames[k] + ".connection(" + solidFeedPortNames[k] + "Name+i).PSDZw;");
						out.println("      endif");
						out.println("    endif");
						out.println("    if (" + solidFeedPortNames[k] + ".connection(" + solidFeedPortNames[k] + "Name+i).SubStreamType in [\"MIXED\",\"CISOLID\"]) then");
						out.println("      " + connectionName + ".CAUSR1 = " +solidFeedPortNames[k] + ".connection(" + solidFeedPortNames[k] + "Name+i).CAUSR1;");
						out.println("      " + connectionName + ".CAUSR2 = " +solidFeedPortNames[k] + ".connection(" + solidFeedPortNames[k] + "Name+i).CAUSR2;");
						out.println("      " + connectionName + ".CAUSR3 = " +solidFeedPortNames[k] + ".connection(" + solidFeedPortNames[k] + "Name+i).CAUSR3;");
						out.println("      " + connectionName + ".CAUSR4 = " +solidFeedPortNames[k] + ".connection(" + solidFeedPortNames[k] + "Name+i).CAUSR4;");
						out.println("      " + connectionName + ".CAUSR5 = " +solidFeedPortNames[k] + ".connection(" + solidFeedPortNames[k] + "Name+i).CAUSR5;");
						out.println("      " + connectionName + ".CAUSRA = " +solidFeedPortNames[k] + ".connection(" + solidFeedPortNames[k] + "Name+i).CAUSRA;");
						out.println("      " + connectionName + ".CAUSRB = " +solidFeedPortNames[k] + ".connection(" + solidFeedPortNames[k] + "Name+i).CAUSRB;");
						out.println("      " + connectionName + ".CAUSRC = " +solidFeedPortNames[k] + ".connection(" + solidFeedPortNames[k] + "Name+i).CAUSRC;");
						out.println("      " + connectionName + ".CAUSRD = " +solidFeedPortNames[k] + ".connection(" + solidFeedPortNames[k] + "Name+i).CAUSRD;");
						out.println("      " + connectionName + ".CAUSRE = " +solidFeedPortNames[k] + ".connection(" + solidFeedPortNames[k] + "Name+i).CAUSRE;");
						out.println("      " + connectionName + ".CACLASS0 = " +solidFeedPortNames[k] + ".connection(" + solidFeedPortNames[k] + "Name+i).CACLASS0;");
						out.println("    endif");
						out.println("  endif");
					}
				}
			}
			out.println("endfor;");
			//end of IF containsAllSolidNames AND containsGasSpecies AND containsSolidSpecies THEN
			out.println("ENDIF");

			//species list section
			out.println("//Speceis at individual ports");
			//declare species set for each feed port
			nBoundary = inletBoundaryList.size();
			for (i=0; i<nBoundary; i++)
			{
				fb = inletBoundaryList.get(i);
				boundaryName = fb.getBoundaryName();
				if (fb.hasGasPhase())
				{
					gm = fb.getGasMixture().get(0);
					portName = boundaryName + "GP";
					flowMap = gm.getSpeciesFlowMap();
					k = 1;
					nSpecies = flowMap.size();
					out.print(portName + "_Species as StringSet([\"");
					for(Map.Entry<Integer,Parameter> entry : flowMap.entrySet())
					{
						iSpeciesAll = entry.getKey().intValue();
						out.print(allSpeciesList.get(iSpeciesAll).getName() + "\"");
						if (k<nSpecies)
							out.print(",\"");
						k++;
					}
					out.println("]);");
				}
				if (fb.hasSolidPhase())
				{
					nPhase = fb.getSolidMixtures().size();
					for (j=0; j<nPhase; j++)
					{
						sm = fb.getSolidMixtures().get(j);
						portName = boundaryName + "SP" + sm.getPhaseIndex();
						flowMap = sm.getSpeciesFlowMap();
						k = 1;
						nSpecies = flowMap.size();
						out.print(portName + "_Species as StringSet([\"");
						for(Map.Entry<Integer,Parameter> entry : flowMap.entrySet())
						{
							iSpecies = entry.getKey().intValue();
							iSpeciesAll = iSolidPhase2All[sm.getPhaseIndex()][iSpecies];
							out.print(allSpeciesList.get(iSpeciesAll).getName() + "\"");
							if (k<nSpecies)
								out.print(",\"");
							k++;
						}
						out.println("]);");
					}
				}
			}
			//declare species set for each product port
			nBoundary = outletBoundaryList.size();
			for (i=0; i<nBoundary; i++)
			{
				fb = outletBoundaryList.get(i);
				boundaryName = fb.getBoundaryName();
				if (fb.hasGasPhase())
				{
					gm = fb.getGasMixture().get(0);
					portName = boundaryName + "GP";
					flowMap = gm.getSpeciesFlowMap();
					k = 1;
					nSpecies = flowMap.size();
					out.print(portName + "_Species as StringSet([\"");
					for(Map.Entry<Integer,Parameter> entry : flowMap.entrySet())
					{
						iSpeciesAll = entry.getKey().intValue();
						out.print(allSpeciesList.get(iSpeciesAll).getName() + "\"");
						if (k<nSpecies)
							out.print(",\"");
						k++;
					}
					out.println("]);");
				}
				if (fb.hasSolidPhase())
				{
					nPhase = fb.getSolidMixtures().size();
					for (j=0; j<nPhase; j++)
					{
						sm = fb.getSolidMixtures().get(j);
						portName = boundaryName + "SP" + sm.getPhaseIndex();
						flowMap = sm.getSpeciesFlowMap();
						k = 1;
						nSpecies = flowMap.size();
						out.print(portName + "_Species as StringSet([\"");
						for(Map.Entry<Integer,Parameter> entry : flowMap.entrySet())
						{
							iSpecies = entry.getKey().intValue();
							iSpeciesAll = iSolidPhase2All[sm.getPhaseIndex()][iSpecies];
							out.print(allSpeciesList.get(iSpeciesAll).getName() + "\"");
							if (k<nSpecies)
								out.print(",\"");
							k++;
						}
						out.println("]);");
					}
				}
			}
			//all species set and molecular weight
			out.println("//All species related to the unit operation");
			out.print("AllSpecies as StringSet([\"");
			nSpecies = allSpeciesList.size();
			for (i=0; i<nSpecies-1; i++)
				out.print(allSpeciesList.get(i).getName() + "\",\"");
			out.println(allSpeciesList.get(nSpecies-1).getName() + "\"]);");
			out.println("//molecular weights of all species");
			out.println("vMW([1:" + nSpecies + "]) as MolWeight(Fixed);");
			for (i=0; i<nSpecies; i++)
			{
				i1 = i + 1;
				//assign to a float number to avoid too many digits
				defaultValue = (float)allSpeciesList.get(i).getMolecularWeight();
				out.println("vMW(" + i1 + "):" + defaultValue + ";");
			}
			//declare feed port molecular weight and mass enthalpy
			out.println("//Mixture molecular weights at individual feed ports");
			nBoundary = inletBoundaryList.size();
			for (i=0; i<nBoundary; i++)
			{
				fb = inletBoundaryList.get(i);
				boundaryName = fb.getBoundaryName();
				if (fb.hasGasPhase())
				{
					portName = boundaryName + "GP";
					out.println(portName + "_MW as MolWeight;");
					strEqns.append("Call (" + portName + "_MW) = pMolWeight(" + portName + ".connection(" + portName + "Name+\"MIXED\").Z);\n");
					//commented out for converting ACM code to DLL with correct degree of freedom
					//out.println("Call (" + portName + ".h) = pEnth_Mol(" + portName + ".T, " + portName + ".P, " + portName + ".Z);");
					totalMassInEqn += portName + ".connection(" + portName + "Name+\"MIXED\").F/3600*" + portName + "_MW + ";
					totalEnthalpyInEqn += portName + ".connection(" + portName + "Name+\"MIXED\").F/3600*" + portName + ".connection(" + portName + "Name+\"MIXED\").h*1e9 + ";
				}
				if (fb.hasSolidPhase())
				{
					nPhase = fb.getSolidMixtures().size();
					for (j=0; j<nPhase; j++)
					{
						sm = fb.getSolidMixtures().get(j);
						portName = boundaryName + "SP" + sm.getPhaseIndex();
						k = sm.getSolidType();
						if (k==0 || k==2)	//CISOLID, CIPSD
						{
							out.println(portName + "_MW as MolWeight;");
							strEqns.append("Call (" + portName + "_MW) = pMolWeight(" + portName + ".connection(" + portName + "Name+" + aspenSubStreamNames[k] + ").Z)" + " " + portName + ".connection(" + portName + "Name+" + aspenSubStreamNames[k] + ").componentlist;\n");
							//commented out for converting ACM code to DLL with correct degree of freedom
							//out.println("Call (" + portName + ".h) = pEnth_Mol(" + portName + ".T, " + portName + ".P, " + portName + ".Z);");
							totalMassInEqn += portName + ".connection(" + portName + "Name+" + aspenSubStreamNames[k] + ").F/3600*" + portName + "_MW + ";
							totalEnthalpyInEqn += portName + ".connection(" + portName + "Name+" + aspenSubStreamNames[k] + ").F/3600*" + portName + ".connection(" + portName + "Name+" + aspenSubStreamNames[k] + ").h*1e9 + ";
						}
						if (k==1 || k==3)	//NC, NCPSD, molecular weight variable not needed
						{
							totalMassInEqn += portName + ".connection(" + portName + "Name+" + aspenSubStreamNames[k] + ").Fm/3600 + ";
							totalEnthalpyInEqn += portName + ".connection(" + portName + "Name+" + aspenSubStreamNames[k] + ").Fm/3600*" + portName + ".connection(" + portName + "Name+" + aspenSubStreamNames[k] + ").hm*1e6 + ";
						}
					}
				}
			}
			strEqns.append(totalMassInEqn.substring(0,totalMassInEqn.length()-3) + ";\n");
			strEqns.append(totalEnthalpyInEqn.substring(0,totalEnthalpyInEqn.length()-3) + ";\n");
			//declare ROM input and output vectors
			out.println("//ROM input and output vectors");
			out.println("vRomInput([1:" + romInputVector.size() + "]) as RealVariable;");
			out.println("vRomOutput([1:" + yromOutputVector.size() + "]) as RealVariable;");
			//feed port related input parameters
			out.println("//Variables and parameters related to feed ports");
			index = 1;
			nBoundary = inletBoundaryList.size();
			for (i=0; i<nBoundary; i++)
			{
				fb = inletBoundaryList.get(i);
				boundaryName = fb.getBoundaryName();
				if (fb.hasGasPhase())
				{
					gm = fb.getGasMixture().get(0);
					portName = boundaryName + "GP";
					connectionName = portName + ".connection(" + portName + "Name+\"MIXED\")";
					//pressure
					param = gm.getPressure();
					variableName = portName + "_P";
					param.printACMVariable(out,variableName);
					strEqns.append(variableName + " = " + connectionName + ".P*1e5;\n");
					if (param.isVaried())
					{
						strEqns.append("vRomInput(" + index + ") = " + variableName + ";\n");
						index++;
					}
					//temperature
					param = gm.getTemperature();
					variableName = portName + "_T";
					param.printACMVariable(out,variableName);
					strEqns.append(variableName + " = " + connectionName + ".T + 273.15;\n");
					if (param.isVaried())
					{
						strEqns.append("vRomInput(" + index + ") = " + variableName + ";\n");
						index++;
					}
					//volume fraction, not port variable, skip if fixed
					param = gm.getVolumeFraction();
					if (param.isVaried())
					{
						variableName = portName + "_VolumeFraction";
						param.printACMParameter(out, variableName);
						strEqns.append("vRomInput(" + index + ") = " + variableName + ";\n");
						index++;
					}
					//species list
					flowMap = gm.getSpeciesFlowMap();
					if (gm.hasConstMassFractions())
					{
						//total mass flow rate
						param = gm.getTotalMassFlow();
						variableName = portName + "_Total_Fm";
						param.printACMVariable(out,variableName);
						strEqns.append(variableName + " = " + connectionName + ".F/3600*" + portName + "_MW;\n");
						if (param.isVaried())
						{
							strEqns.append("vRomInput(" + index + ") = " + variableName + ";\n");
							index++;
						}
						//check species mass fraction
						variableName = portName + "_Zm";
						for(Map.Entry<Integer,Parameter> entry : flowMap.entrySet())
						{
							iSpeciesAll = entry.getKey().intValue();
							i1 = iSpeciesAll + 1;
							speciesName = allSpeciesList.get(iSpeciesAll).getName();
							speciesNameMod = speciesName.replace('+','p');
							speciesNameMod = speciesNameMod.replace('-','m');
							param = entry.getValue();
							massFractionName = variableName + "_" + speciesNameMod;
							param.printACMVariable(out,massFractionName);
							strEqns.append(massFractionName + " = " + connectionName + ".Z(\"" + speciesName + "\")*vMW(" + i1 + ")/" + portName + "_MW;\n");
						}
					}
					else
					{
						variableName = portName + "_MassFlow";
						for(Map.Entry<Integer,Parameter> entry : flowMap.entrySet())
						{
							iSpeciesAll = entry.getKey().intValue();
							i1 = iSpeciesAll + 1;
							speciesName = allSpeciesList.get(iSpeciesAll).getName();
							speciesNameMod = speciesName.replace('+','p');
							speciesNameMod = speciesNameMod.replace('-','m');
							param = entry.getValue();
							massFractionName = variableName + "_" + speciesNameMod;
							param.printACMVariable(out,massFractionName);
							strEqns.append(massFractionName + " = " + connectionName + ".F/3600*" + connectionName + ".Z(\"" + speciesName + "\")*vMW(" + i1 + ");\n");
							if (param.isVaried())
							{
								strEqns.append("vRomInput(" + index + ") = " + massFractionName + ";\n");
								index++;
							}
						}
					}
					//check the sum of total mole fraction in inlet port
					variableName = portName + "_SumZ";
					out.println(variableName + " as RealVariable;");
					out.println(variableName + ".lower : LowerTolerance;");
					out.println(variableName + ".upper : UpperTolerance;");
					strEqns.append(variableName + " = sigma(foreach (i in " + portName + "_Species) " + connectionName + ".Z(i));\n");
				}
				if (fb.hasSolidPhase())
				{
					nPhase = fb.getSolidMixtures().size();
					for (j=0; j<nPhase; j++)
					{
						sm = fb.getSolidMixtures().get(j);
						portName = boundaryName + "SP" + sm.getPhaseIndex();
						subStreamType = aspenSubStreamNames[sm.getSolidType()];
						connectionName = portName + ".connection(" + portName + "Name+" + subStreamType + ")";
						//pressure
						param = sm.getPressure();
						variableName = portName + "_P";
						param.printACMVariable(out,variableName);
						strEqns.append(variableName + " = " + connectionName + ".P*1e5;\n");
						if (param.isVaried())
						{
							strEqns.append("vRomInput(" + index + ") = " + variableName + ";\n");
							index++;
						}
						//temperature
						param = sm.getTemperature();
						variableName = portName + "_T";
						param.printACMVariable(out,variableName);
						strEqns.append(variableName + " = " + connectionName + ".T + 273.15;\n");
						if (param.isVaried())
						{
							strEqns.append("vRomInput(" + index + ") = " + variableName + ";\n");
							index++;
						}
						//volume fraction, not port variable, skip if fixed
						param = sm.getVolumeFraction();
						if (param.isVaried())
						{
							variableName = portName + "_VolumeFraction";
							param.printACMParameter(out, variableName);
							strEqns.append("vRomInput(" + index + ") = " + variableName + ";\n");
							index++;
						}
						//particle diameter, not port variable, skip if fixed
						param = sm.getDiameter();
						if (param.isVaried())
						{
							variableName = portName + "_Diameter";
							param.printACMParameter(out, variableName);
							strEqns.append("vRomInput(" + index + ") = " + variableName + ";\n");
							index++;
						}
						//particle density, not port variable, skip if fixed
						param = sm.getDensity();
						if (param.isVaried())
						{
							variableName = portName + "_Density";
							param.printACMParameter(out, variableName);
							strEqns.append("vRomInput(" + index + ") = " + variableName + ";\n");
							index++;
						}
						if (sm.isGranularEnergySolved())
						{
							//granular temperature, not port variable, skip if fixed
							param = sm.getGranularTemperature();
							if (param.isVaried())
							{
								variableName = portName + "_GranularTemperature";
								param.printACMParameter(out, variableName);
								strEqns.append("vRomInput(" + index + ") = " + variableName + ";\n");
								index++;
							}
						}
						if (sm.getSolidType()==1 || sm.getSolidType()==3)	//NC or NCPSD
						{
							//volatile matter percentage
							param = sm.getVm();
							variableName = portName + "_VM";
							param.printACMVariable(out, variableName);
							strEqns.append(variableName + " = " + connectionName + ".PROXANAL(\"" + solidPhaseList.get(sm.getPhaseIndex()) + "\",\"VM\");\n");
							if (param.isVaried())
							{
								strEqns.append("vRomInput(" + index + ") = " + variableName + ";\n");
								index++;
							}
							//check is the NC substream contains one solid component only
							variableName = portName + "_Zm_" + solidPhaseList.get(sm.getPhaseIndex());
							out.println(variableName + " as RealVariable;");
							out.println(variableName + ".lower : LowerTolerance;");
							out.println(variableName + ".upper : UpperTolerance;");
							strEqns.append(variableName + " = " + connectionName + ".Zm(\"" + solidPhaseList.get(sm.getPhaseIndex()) + "\");\n");
						}

						//species list
						flowMap = sm.getSpeciesFlowMap();
						if (sm.hasConstMassFractions())
						{
							//total mass flow rate
							param = sm.getTotalMassFlow();
							variableName = portName + "_Total_Fm";
							param.printACMVariable(out,variableName);
							if (sm.getSolidType()==0 || sm.getSolidType()==2)		//CISOLID, CIPSD
								strEqns.append(variableName + " = " + connectionName + ".F/3600*" + portName + "_MW;\n");
							if (sm.getSolidType()==1 || sm.getSolidType()==3)		//NC, NCPSD
								strEqns.append(variableName + " = " + connectionName + ".Fm/3600;\n");
							if (param.isVaried())
							{
								strEqns.append("vRomInput(" + index + ") = " + variableName + ";\n");
								index++;
							}
							//check species mass fraction
							if (sm.getSolidType()==0 || sm.getSolidType()==2)	//CISOLID, CIPSD
							{
								variableName = portName + "_Zm";
								for(Map.Entry<Integer,Parameter> entry : flowMap.entrySet())
								{
									iSpecies = entry.getKey().intValue();
									iSpeciesAll = iSolidPhase2All[sm.getPhaseIndex()][iSpecies];
									i1 = iSpeciesAll + 1;
									speciesName = allSpeciesList.get(iSpeciesAll).getName();
									speciesNameMod = speciesName.replace('+','p');
									speciesNameMod = speciesNameMod.replace('-','m');
									param = entry.getValue();
									massFractionName = variableName + "_" + speciesNameMod;
									param.printACMVariable(out,massFractionName);
									strEqns.append(massFractionName + " = " + connectionName + ".Z(\"" + speciesName + "\")*vMW(" + i1 + ")/" + portName + "_MW;\n");
								}
							}
							else		//NC, NCPSD
							{
								ncName = solidPhaseList.get(sm.getPhaseIndex());
								massFractionName = portName + "_Zm_C";
								param = flowMap.get(new Integer(0));
								param.printACMVariable(out,massFractionName);
								strEqns.append(massFractionName + " = (1 - " + connectionName + ".PROXANAL(\"" + ncName + "\",\"Moisture\")/100)*" + connectionName + ".ULTANAL(\"" + ncName + "\",\"CARBON\")/100;\n");
								massFractionName = portName + "_Zm_H";
								param = flowMap.get(new Integer(1));
								param.printACMVariable(out,massFractionName);
								strEqns.append(massFractionName + " = (1 - " + connectionName + ".PROXANAL(\"" + ncName + "\",\"Moisture\")/100)*" + connectionName + ".ULTANAL(\"" + ncName + "\",\"HYDROGEN\")/100;\n");
								massFractionName = portName + "_Zm_N";
								param = flowMap.get(new Integer(2));
								param.printACMVariable(out,massFractionName);
								strEqns.append(massFractionName + " = (1 - " + connectionName + ".PROXANAL(\"" + ncName + "\",\"Moisture\")/100)*" + connectionName + ".ULTANAL(\"" + ncName + "\",\"NITROGEN\")/100;\n");
								massFractionName = portName + "_Zm_Cl";
								param = flowMap.get(new Integer(3));
								param.printACMVariable(out,massFractionName);
								strEqns.append(massFractionName + " = (1 - " + connectionName + ".PROXANAL(\"" + ncName + "\",\"Moisture\")/100)*" + connectionName + ".ULTANAL(\"" + ncName + "\",\"CHLORINE\")/100;\n");
								massFractionName = portName + "_Zm_S";
								param = flowMap.get(new Integer(4));
								param.printACMVariable(out,massFractionName);
								strEqns.append(massFractionName + " = (1 - " + connectionName + ".PROXANAL(\"" + ncName + "\",\"Moisture\")/100)*" + connectionName + ".ULTANAL(\"" + ncName + "\",\"SULFUR\")/100;\n");
								massFractionName = portName + "_Zm_O";
								param = flowMap.get(new Integer(5));
								param.printACMVariable(out,massFractionName);
								strEqns.append(massFractionName + " = (1 - " + connectionName + ".PROXANAL(\"" + ncName + "\",\"Moisture\")/100)*" + connectionName + ".ULTANAL(\"" + ncName + "\",\"OXYGEN\")/100;\n");
								massFractionName = portName + "_Zm_Ah";
								param = flowMap.get(new Integer(6));
								param.printACMVariable(out,massFractionName);
								strEqns.append(massFractionName + " = (1 - " + connectionName + ".PROXANAL(\"" + ncName + "\",\"Moisture\")/100)*" + connectionName + ".ULTANAL(\"" + ncName + "\",\"ASH\")/100;\n");
								massFractionName = portName + "_Zm_H2O";
								param = flowMap.get(new Integer(7));
								param.printACMVariable(out,massFractionName);
								strEqns.append(massFractionName + " = " + connectionName + ".PROXANAL(\"" + ncName + "\",\"Moisture\")/100;\n");
							}
						}
						else		//not constant
						{
							if (sm.getSolidType()==0 || sm.getSolidType()==2)	//CISOLID, CIPSD
							{
								variableName = portName + "_MassFlow";
								for(Map.Entry<Integer,Parameter> entry : flowMap.entrySet())
								{
									iSpecies = entry.getKey().intValue();
									iSpeciesAll = iSolidPhase2All[sm.getPhaseIndex()][iSpecies];
									i1 = iSpeciesAll + 1;
									speciesName = allSpeciesList.get(iSpeciesAll).getName();
									speciesNameMod = speciesName.replace('+','p');
									speciesNameMod = speciesNameMod.replace('-','m');
									param = entry.getValue();
									massFractionName = variableName + "_" + speciesNameMod;
									param.printACMVariable(out,massFractionName);
									strEqns.append(massFractionName + " = " + connectionName + ".F/3600*" + connectionName + ".Z(\"" + speciesName + "\")*vMW(" + i1 + ");\n");
									if (param.isVaried())
									{
										strEqns.append("vRomInput(" + index + ") = " + massFractionName + ";\n");
										index++;
									}
								}
							}
							else		//NC, NCPSD
							{
								ncName = solidPhaseList.get(sm.getPhaseIndex());
								massFractionName = portName + "_MassFlow_C";
								param = flowMap.get(new Integer(0));
								param.printACMVariable(out,massFractionName);
								strEqns.append(massFractionName + " = " + connectionName + ".Fm/3600*(1 - " + connectionName + ".PROXANAL(\"" + ncName + "\",\"Moisture\")/100)*" + connectionName + ".ULTANAL(\"" + ncName + "\",\"CARBON\")/100;\n");
								if (param.isVaried())
								{
									strEqns.append("vRomInput(" + index + ") = " + massFractionName + ";\n");
									index++;
								}
								massFractionName = portName + "_MassFlow_H";
								param = flowMap.get(new Integer(1));
								param.printACMVariable(out,massFractionName);
								strEqns.append(massFractionName + " = " + connectionName + ".Fm/3600*(1 - " + connectionName + ".PROXANAL(\"" + ncName + "\",\"Moisture\")/100)*" + connectionName + ".ULTANAL(\"" + ncName + "\",\"HYDROGEN\")/100;\n");
								if (param.isVaried())
								{
									strEqns.append("vRomInput(" + index + ") = " + massFractionName + ";\n");
									index++;
								}
								massFractionName = portName + "_MassFlow_N";
								param = flowMap.get(new Integer(2));
								param.printACMVariable(out,massFractionName);
								strEqns.append(massFractionName + " = " + connectionName + ".Fm/3600*(1 - " + connectionName + ".PROXANAL(\"" + ncName + "\",\"Moisture\")/100)*" + connectionName + ".ULTANAL(\"" + ncName + "\",\"NITROGEN\")/100;\n");
								if (param.isVaried())
								{
									strEqns.append("vRomInput(" + index + ") = " + massFractionName + ";\n");
									index++;
								}
								massFractionName = portName + "_MassFlow_Cl";
								param = flowMap.get(new Integer(3));
								param.printACMVariable(out,massFractionName);
								strEqns.append(massFractionName + " = " + connectionName + ".Fm/3600*(1 - " + connectionName + ".PROXANAL(\"" + ncName + "\",\"Moisture\")/100)*" + connectionName + ".ULTANAL(\"" + ncName + "\",\"CHLORINE\")/100;\n");
								if (param.isVaried())
								{
									strEqns.append("vRomInput(" + index + ") = " + massFractionName + ";\n");
									index++;
								}
								massFractionName = portName + "_MassFlow_S";
								param = flowMap.get(new Integer(4));
								param.printACMVariable(out,massFractionName);
								strEqns.append(massFractionName + " = " + connectionName + ".Fm/3600*(1 - " + connectionName + ".PROXANAL(\"" + ncName + "\",\"Moisture\")/100)*" + connectionName + ".ULTANAL(\"" + ncName + "\",\"SULFUR\")/100;\n");
								if (param.isVaried())
								{
									strEqns.append("vRomInput(" + index + ") = " + massFractionName + ";\n");
									index++;
								}
								massFractionName = portName + "_MassFlow_O";
								param = flowMap.get(new Integer(5));
								param.printACMVariable(out,massFractionName);
								strEqns.append(massFractionName + " = " + connectionName + ".Fm/3600*(1 - " + connectionName + ".PROXANAL(\"" + ncName + "\",\"Moisture\")/100)*" + connectionName + ".ULTANAL(\"" + ncName + "\",\"OXYGEN\")/100;\n");
								if (param.isVaried())
								{
									strEqns.append("vRomInput(" + index + ") = " + massFractionName + ";\n");
									index++;
								}
								massFractionName = portName + "_MassFlow_Ah";
								param = flowMap.get(new Integer(6));
								param.printACMVariable(out,massFractionName);
								strEqns.append(massFractionName + " = " + connectionName + ".Fm/3600*(1 - " + connectionName + ".PROXANAL(\"" + ncName + "\",\"Moisture\")/100)*" + connectionName + ".ULTANAL(\"" + ncName + "\",\"ASH\")/100;\n");
								if (param.isVaried())
								{
									strEqns.append("vRomInput(" + index + ") = " + massFractionName + ";\n");
									index++;
								}
								massFractionName = portName + "_MassFlow_H2O";
								param = flowMap.get(new Integer(7));
								param.printACMVariable(out,massFractionName);
								strEqns.append(massFractionName + " = " + connectionName + ".Fm/3600*" + connectionName + ".PROXANAL(\"" + ncName + "\",\"Moisture\")/100;\n");
								if (param.isVaried())
								{
									strEqns.append("vRomInput(" + index + ") = " + massFractionName + ";\n");
									index++;
								}
							}
						}
						//check the sum of total mole or mass fraction in inlet port, for CISOLID only
						if (sm.getSolidType()==0 || sm.getSolidType()==2)		//CISOLID, CIPSD
						{
							variableName = portName + "_SumZ";
							out.println(variableName + " as RealVariable;");
							out.println(variableName + ".lower : LowerTolerance;");
							out.println(variableName + ".upper : UpperTolerance;");
							strEqns.append(variableName + " = sigma(foreach (i in " + portName + "_Species) " + portName + ".connection(" + portName + "Name+\"CISOLID\").Z(i));\n");
						}
					}
				}
			}
			//model input parameters
			out.println("//Model specific input parameters");
			nParameter = inputParameterList.size();
			for (i=0; i<nParameter; i++)
			{
				param = inputParameterList.get(i);
				if (param.isVaried())
				{
					variableName = param.getName();
					param.printACMParameter(out, variableName);
					strEqns.append("vRomInput(" + index + ") = " + variableName + ";\n");
					index++;
				}
			}
			//product port related output variables
			out.println("//Variables related to product ports");
			index = 1;
			nBoundary = outletBoundaryList.size();
			for (i=0; i<nBoundary; i++)
			{
				fb = outletBoundaryList.get(i);
				boundaryName = fb.getBoundaryName();
				if (fb.hasGasPhase())
				{
					portName = boundaryName + "GP";
					connectionName = portName + ".connection(" + portName + "Name+\"MIXED\")";
					//molecular weight and enthalpy for energy balance
					out.println(portName + "_MW as MolWeight;");
					out.println(portName + "_H_rom as Enth_Mol;");
					gm = fb.getGasMixture().get(0);
					//pressure
					param = gm.getPressure();
					if (param.isVaried())
					{
						strEqns.append(connectionName + ".P = vRomOutput(" + index + ")/1e5;\n");
						index++;
					}
					else
						strEqns.append(connectionName + ".P = " + param.getDefaultValue() + "/1e5;\n");
					//temperature
					param = gm.getTemperature();
					//temperature from ROM model before energy balance correction
					out.println(portName + "_T_rom as Temperature;");
					if (param.isVaried())
					{
						strEqns.append(portName + "_T_rom = vRomOutput(" + index + ") -273.15;\n");
						index++;
					}
					else
						strEqns.append(portName + "_T_rom = " + param.getDefaultValue() + "-273.15;\n");
					//volume fraction, not a port variable, skip if fixed
					param = gm.getVolumeFraction();
					if (param.isVaried())
					{
						variableName = portName + "_VolumeFraction";
						out.println(variableName + " as RealVariable;");
						strEqns.append(variableName + " = vRomOutput(" + index + ");\n");
						index++;
					}
					//species list
					flowMap = gm.getSpeciesFlowMap();
					//always non-constant speceis mass fractions
					variableName = portName + "_F";
					for(Map.Entry<Integer,Parameter> entry : flowMap.entrySet())
					{
						iSpeciesAll = entry.getKey().intValue();
						i1 = iSpeciesAll + 1;
						speciesName = allSpeciesList.get(iSpeciesAll).getName();
						//replace + and - if any
						speciesNameMod = speciesName.replace('+','p');
						speciesNameMod = speciesNameMod.replace('-','m');
						massFractionName = variableName + "_" + speciesNameMod;
						out.println(massFractionName + " as RealVariable;");
						strEqns.append("IF vRomOutput(" + index + ") < 0 THEN\n");
						strEqns.append("\t" + massFractionName + " = " + "vRomOutput(" + index + ")*(" + reflectionCoefficient + ")/vMW(" + i1 + ");\n");
						strEqns.append("ELSE\n");
						strEqns.append("\t" + massFractionName + " = " + "vRomOutput(" + index + ")/vMW(" + i1 + ");\n");
						strEqns.append("ENDIF\n");
						//assignment to product port will be after correction
						index++;
					}
				}
				if (fb.hasSolidPhase())
				{
					nPhase = fb.getSolidMixtures().size();
					for (j=0; j<nPhase; j++)
					{
						sm = fb.getSolidMixtures().get(j);
						portName = boundaryName + "SP" + sm.getPhaseIndex();
						//molecular weight and enthalpy for energy balance
						subStreamType = aspenSubStreamNames[sm.getSolidType()];
						connectionName = portName + ".connection(" + portName + "Name+" + subStreamType + ")";
						if (sm.getSolidType()==0 || sm.getSolidType()==2)		//CISOLID, CIPSD
						{
							out.println(portName + "_MW as MolWeight;");
							out.println(portName + "_H_rom as Enth_Mol;");
						}
						else		//NC, NCPSD, no molecular weight, mass enthalpy
							out.println(portName + "_H_rom as Enth_Mass;");
						//pressure
						param = sm.getPressure();
						if (param.isVaried())
						{
							strEqns.append(connectionName + ".P = vRomOutput(" + index + ")/1e5;\n");
							index++;
						}
						else
							strEqns.append(connectionName + ".P = " + param.getDefaultValue() + "/1e5;\n");
						//temperature
						param = sm.getTemperature();
						//temperature from ROM model before energy balance correction
						out.println(portName + "_T_rom as Temperature;");
						if (param.isVaried())
						{
							strEqns.append(portName + "_T_rom = vRomOutput(" + index + ") -273.15;\n");
							index++;
						}
						else
							strEqns.append(portName + "_T_rom = " + param.getDefaultValue() + "-273.15;\n");
						//volume fraction, not port related data
						param = sm.getVolumeFraction();
						if (param.isVaried())
						{
							variableName = portName + "_VolumeFraction";
							out.println(variableName + " as RealVariable;");
							strEqns.append(variableName + " = vRomOutput(" + index + ");\n");
							index++;
						}
						//particle diameter
						param = sm.getDiameter();
						if (param.isVaried())
						{
							variableName = portName + "_Diameter";
							out.println(variableName + " as RealVariable;");
							strEqns.append(variableName + " = vRomOutput(" + index + ");\n");
							index++;
						}
						//particle density
						param = sm.getDensity();
						if (param.isVaried())
						{
							variableName = portName + "_Density";
							out.println(variableName + " as RealVariable;");
							strEqns.append(variableName + " = vRomOutput(" + index + ");\n");
							index++;
						}
						if (sm.isGranularEnergySolved())
						{
							//granular temperature, not port related data
							param = sm.getGranularTemperature();
							if (param.isVaried())
							{
								variableName = portName + "_GranularTemperature";
								out.println(variableName + " as RealVariable;");
								strEqns.append(variableName + " = vRomOutput(" + index + ");\n");
								index++;
							}
						}
						if (sm.getSolidType()==1 || sm.getSolidType()==3)	//NC, NCPSD
						{
							param = sm.getVm();
							variableName = portName + "_VM";
							out.println(variableName + " as RealVariable;");
							if (param.isVaried())
							{
								strEqns.append(portName + "_VM = vRomOutput(" + index + ");\n");
								index++;
							}
							else
								strEqns.append(portName + "_VM = " + param.getDefaultValue() + ";\n");
							//variables for total mass flow rate, dry mass flow rate
							out.println(portName + "_Fm as RealVariable;");
							out.println(portName + "_Fm_dry as RealVariable;");
						}
						//species list
						flowMap = sm.getSpeciesFlowMap();
						//always non-constant speceis mass fractions
						variableName = portName + "_F";
						for(Map.Entry<Integer,Parameter> entry : flowMap.entrySet())
						{
							iSpecies = entry.getKey().intValue();
							iSpeciesAll = iSolidPhase2All[sm.getPhaseIndex()][iSpecies];
							i1 = iSpeciesAll + 1;
							speciesName = allSpeciesList.get(iSpeciesAll).getName();
							speciesNameMod = speciesName.replace('+','p');
							speciesNameMod = speciesNameMod.replace('-','m');
							massFractionName = variableName + "_" + speciesNameMod;
							out.println(massFractionName + " as RealVariable;");
							if (sm.getSolidType()==1 || sm.getSolidType()==3)	//NC, NCPSD
							{
								out.println(massFractionName + "_cor as RealVariable;");		//mass balance corrected molar flow
								out.println(portName + "_Fm_" + speciesName + " as RealVariable;");
							}
							strEqns.append("IF vRomOutput(" + index + ") < 0 THEN\n");
							strEqns.append("\t" + massFractionName + " = " + "vRomOutput(" + index + ")*(" + reflectionCoefficient + ")/vMW(" + i1 + ");\n");
							strEqns.append("ELSE\n");
							strEqns.append("\t" + massFractionName + " = " + "vRomOutput(" + index + ")/vMW(" + i1 + ");\n");
							strEqns.append("ENDIF\n");
							//assignment to product port will be after correction
							index++;
						}
					}
				}
			}
			//model output parameters,varied parameters only
			out.println("//Model specific output parameters");
			nParameter = outputParameterList.size();
			for (i=0; i<nParameter; i++)
			{
				param = outputParameterList.get(i);
				if (param.isVaried())
				{
					out.println(param.getName() + " as RealVariable;");
					strEqns.append("vRomOutput(" + index + ") = " + param.getName() + ";\n");
					index++;
					if (param.getName().equalsIgnoreCase("heatloss") || param.getName().equalsIgnoreCase("heat_loss"))
						param_heatloss = param;
				}
			}
			//equations for feed speceis molar flow rates
			out.println("//Variables and equations related to feed species molar flow rates");
			nSpecies = iInletSpecies.length;
			out.println("vSpeciesMolarFlowIn([1:" + nSpecies + "]) as RealVariable;");
			for (k=0; k<nSpecies; k++)
			{
				bFirst = true;
				i1 = k + 1;
				strEqns.append("vSpeciesMolarFlowIn(" + i1 + ") = ");
				nBoundary = inletBoundaryList.size();
				for (i=0; i<nBoundary; i++)
				{
					fb = inletBoundaryList.get(i);
					boundaryName = fb.getBoundaryName();
					if (fb.hasGasPhase())
					{
						portName = boundaryName + "GP";
						connectionName = portName + ".connection(" + portName + "Name+\"MIXED\")";
						gm = fb.getGasMixture().get(0);
						flowMap = gm.getSpeciesFlowMap();
						for(Map.Entry<Integer,Parameter> entry : flowMap.entrySet())
						{
							iSpeciesAll = entry.getKey().intValue();
							if (iInletSpecies[k]==iSpeciesAll)
							{
								speciesName = allSpeciesList.get(iSpeciesAll).getName();
								if (bFirst)
									bFirst = false;
								else
									strEqns.append(" + ");
								strEqns.append(connectionName + ".F/3600*" + connectionName + ".Z(\"" + speciesName + "\")");
							}
						}
					}
					if (fb.hasSolidPhase())
					{
						nPhase = fb.getSolidMixtures().size();
						for (j=0; j<nPhase; j++)
						{
							sm = fb.getSolidMixtures().get(j);
							portName = boundaryName + "SP" + sm.getPhaseIndex();
							subStreamType = aspenSubStreamNames[sm.getSolidType()];
							connectionName = portName + ".connection(" + portName + "Name+" + subStreamType + ")";
							if (sm.getSolidType()==0 || sm.getSolidType()==2)	//CISOLID, CIPSD
							{
								flowMap = sm.getSpeciesFlowMap();
								for(Map.Entry<Integer,Parameter> entry : flowMap.entrySet())
								{
									iSpecies = entry.getKey().intValue();
									iSpeciesAll = iSolidPhase2All[sm.getPhaseIndex()][iSpecies];
									if (iInletSpecies[k]==iSpeciesAll)
									{
										speciesName = allSpeciesList.get(iSpeciesAll).getName();
										if (bFirst)
											bFirst = false;
										else
											strEqns.append(" + ");
										strEqns.append(connectionName + ".F/3600*" + connectionName + ".Z(\"" + speciesName + "\")");
									}
								}
							}
							else	//NC, NCPSD
							{
								ncName = solidPhaseList.get(sm.getPhaseIndex());
								flowMap = sm.getSpeciesFlowMap();
								for(Map.Entry<Integer,Parameter> entry : flowMap.entrySet())
								{
									iSpecies = entry.getKey().intValue();
									iSpeciesAll = iSolidPhase2All[sm.getPhaseIndex()][iSpecies];
									i1 = iSpeciesAll + 1;
									if (iInletSpecies[k]==iSpeciesAll)
									{
										speciesName = allSpeciesList.get(iSpeciesAll).getName();
										if (bFirst)
											bFirst = false;
										else
											strEqns.append(" + ");
										if (speciesName.equalsIgnoreCase("C"))
											strEqns.append(connectionName + ".Fm/3600/vMW(" + i1 + ")*(1-" + connectionName + ".PROXANAL(\"" + ncName + "\",\"MOISTURE\")/100)*" + connectionName + ".ULTANAL(\"" + ncName + "\",\"CARBON\")/100");
										if (speciesName.equalsIgnoreCase("H"))
											strEqns.append(connectionName + ".Fm/3600/vMW(" + i1 + ")*(1-" + connectionName + ".PROXANAL(\"" + ncName + "\",\"MOISTURE\")/100)*" + connectionName + ".ULTANAL(\"" + ncName + "\",\"HYDROGEN\")/100");
										if (speciesName.equalsIgnoreCase("N"))
											strEqns.append(connectionName + ".Fm/3600/vMW(" + i1 + ")*(1-" + connectionName + ".PROXANAL(\"" + ncName + "\",\"MOISTURE\")/100)*" + connectionName + ".ULTANAL(\"" + ncName + "\",\"NITROGEN\")/100");
										if (speciesName.equalsIgnoreCase("Cl"))
											strEqns.append(connectionName + ".Fm/3600/vMW(" + i1 + ")*(1-" + connectionName + ".PROXANAL(\"" + ncName + "\",\"MOISTURE\")/100)*" + connectionName + ".ULTANAL(\"" + ncName + "\",\"CHLORINE\")/100");
										if (speciesName.equalsIgnoreCase("S"))
											strEqns.append(connectionName + ".Fm/3600/vMW(" + i1 + ")*(1-" + connectionName + ".PROXANAL(\"" + ncName + "\",\"MOISTURE\")/100)*" + connectionName + ".ULTANAL(\"" + ncName + "\",\"SULFUR\")/100");
										if (speciesName.equalsIgnoreCase("O"))
											strEqns.append(connectionName + ".Fm/3600/vMW(" + i1 + ")*(1-" + connectionName + ".PROXANAL(\"" + ncName + "\",\"MOISTURE\")/100)*" + connectionName + ".ULTANAL(\"" + ncName + "\",\"OXYGEN\")/100");
										if (speciesName.equalsIgnoreCase("Ah"))
											strEqns.append(connectionName + ".Fm/3600/vMW(" + i1 + ")*(1-" + connectionName + ".PROXANAL(\"" + ncName + "\",\"MOISTURE\")/100)*" + connectionName + ".ULTANAL(\"" + ncName + "\",\"ASH\")/100");
										if (speciesName.equalsIgnoreCase("H2O"))
											strEqns.append(connectionName + ".Fm/3600/vMW(" + i1 + ")*" + connectionName + ".PROXANAL(\"" + ncName + "\",\"MOISTURE\")/100");
									}
								}
							}
						}
					}
				}
				strEqns.append(";\n");
			}
			//equations for feed stream elemental molar flow rates
			out.println("//Variables and equations related to feed elemental molar flow rates");
			nElement = iElementAll.length;
			out.println("vElementMolarFlowIn([1:" + nElement + "]) as RealVariable;");
			for (i=0; i<nElement; i++)
			{
				i1 = i + 1;
				bFirst = true;
				strEqns.append("vElementMolarFlowIn(" + i1 + ") = ");
				iElement = iElementAll[i];
				for (j=0; j<nSpecies; j++)
				{
					iSpecies = iInletSpecies[j];
					k = j + 1;
					sp = allSpeciesList.get(iSpecies);
					if (sp.containsElement(iElement))
					{
						if (bFirst)
							bFirst = false;
						else
							strEqns.append(" + ");
						strEqns.append("vSpeciesMolarFlowIn(" + k + ")*" + sp.getNumberOfAtoms(iElement));
					}
				}
				strEqns.append(";\n");
			}
			//equations for product speceis molar flow rates
			out.println("//Variables and equations related to product species molar flow rates");
			nSpecies = iOutletSpecies.length;
			out.println("vSpeciesMolarFlowOut([1:" + nSpecies + "]) as RealVariable;");
			for (k=0; k<nSpecies; k++)
			{
				bFirst = true;
				i1 = k + 1;
				strEqns.append("vSpeciesMolarFlowOut(" + i1 + ") = ");
				nBoundary = outletBoundaryList.size();
				for (i=0; i<nBoundary; i++)
				{
					fb = outletBoundaryList.get(i);
					boundaryName = fb.getBoundaryName();
					if (fb.hasGasPhase())
					{
						portName = boundaryName + "GP";
						gm = fb.getGasMixture().get(0);
						flowMap = gm.getSpeciesFlowMap();
						variableName = portName + "_F";
						for(Map.Entry<Integer,Parameter> entry : flowMap.entrySet())
						{
							iSpeciesAll = entry.getKey().intValue();
							if (iOutletSpecies[k]==iSpeciesAll)
							{
								speciesName = allSpeciesList.get(iSpeciesAll).getName();
								speciesNameMod = speciesName.replace('+','p');
								speciesNameMod = speciesNameMod.replace('-','m');
								massFractionName = variableName + "_" + speciesNameMod;
								if (bFirst)
									bFirst = false;
								else
									strEqns.append(" + ");
								strEqns.append(massFractionName);
							}
						}
					}
					if (fb.hasSolidPhase())
					{
						nPhase = fb.getSolidMixtures().size();
						for (j=0; j<nPhase; j++)
						{
							sm = fb.getSolidMixtures().get(j);
							portName = boundaryName + "SP" + sm.getPhaseIndex();
							flowMap = sm.getSpeciesFlowMap();
							variableName = portName + "_F";
							for(Map.Entry<Integer,Parameter> entry : flowMap.entrySet())
							{
								iSpecies = entry.getKey().intValue();
								iSpeciesAll = iSolidPhase2All[sm.getPhaseIndex()][iSpecies];
								if (iOutletSpecies[k]==iSpeciesAll)
								{
									speciesName = allSpeciesList.get(iSpeciesAll).getName();
									speciesNameMod = speciesName.replace('+','p');
									speciesNameMod = speciesNameMod.replace('-','m');
									massFractionName = variableName + "_" + speciesNameMod;
									if (bFirst)
										bFirst = false;
									else
										strEqns.append(" + ");
									strEqns.append(massFractionName);
								}
							}
						}
					}
				}
				strEqns.append(";\n");
			}
			//equations for product stream elemental molar flow rates
			out.println("//Variables and equations related to product elemental molar flow rates");
			nElement = iElementAll.length;
			out.println("vElementMolarFlowOut([1:" + nElement + "]) as RealVariable;");
			for (i=0; i<nElement; i++)
			{
				i1 = i + 1;
				bFirst = true;
				strEqns.append("vElementMolarFlowOut(" + i1 + ") = ");
				iElement = iElementAll[i];
				for (j=0; j<nSpecies; j++)
				{
					iSpecies = iOutletSpecies[j];
					k = j + 1;
					sp = allSpeciesList.get(iSpecies);
					if (sp.containsElement(iElement))
					{
						if (bFirst)
							bFirst = false;
						else
							strEqns.append(" + ");
						strEqns.append("vSpeciesMolarFlowOut(" + k + ")*" + sp.getNumberOfAtoms(iElement));
					}
				}
				strEqns.append(";\n");
			}
			//equations for imbalance of elemental molar flows
			out.println("//Variables and equations related to the correction for mass conservation");
			out.println("vDeltaElementMolarFlow([1:" + nElement + "]) as RealVariable;");
			strEqns.append("FOR i IN [1:" + nElement + "] DO\n");
			strEqns.append("\tvDeltaElementMolarFlow(i) = vElementMolarFlowIn(i) - vElementMolarFlowOut(i);\n");
			strEqns.append("ENDFOR\n");
			if (nSpecies<nElement)		//use regression method
			{
				out.println("vCorrection([1:" + nSpecies + "]) as RealVariable;");
				Species sp2;
				boolean bfirst;
				boolean bij[][] = new boolean[nSpecies][];
				String sij[][] = new String[nSpecies][];
				String sb[] = new String[nSpecies];
				for (i=0; i<nSpecies; i++)
				{
					bij[i] = new boolean[nSpecies];
					sij[i] = new String[nSpecies];
				}
				for (i=0; i<nSpecies; i++)
				{
					i1 = i + 1;
					sp = allSpeciesList.get(iOutletSpecies[i]);
					for (j=0; j<nSpecies; j++)
					{
						j1 = j + 1;
						sp2 = allSpeciesList.get(iOutletSpecies[j]);
						bij[i][j] = false;
						sij[i][j] = "(";
						for (k=0; k<nElement; k++)
						{
							iElement = iElementAll[k];
							if (sp.containsElement(iElement) && sp2.containsElement(iElement))
							{
								if (bij[i][j])
									sij[i][j] += "+";
								sij[i][j] += sp.getNumberOfAtoms(iElement) + "*" + sp2.getNumberOfAtoms(iElement) + "*vSpeciesMolarFlowOut(" + i1 + ")*vSpeciesMolarFlowOut(" + j1 + ")";
								bij[i][j] = true;
							}
						}
						sij[i][j] += ")";
					}
					sb[i] = "";
					bfirst = true;
					for (j=0; j<nElement; j++)
					{
						j1 = j + 1;
						iElement = iElementAll[j];
						if (sp.containsElement(iElement))
						{
							if (!bfirst)
								sb[i] += " + ";
							bfirst = false;
							sb[i] += sp.getNumberOfAtoms(iElement) + "*vSpeciesMolarFlowOut(" + i1 + ")*vDeltaElementMolarFlow(" + j1 + ")";
						}
					}
				}
				//regression equations
				for (i=0; i<nSpecies; i++)
				{
					bfirst = true;
					for (j=0; j<nSpecies; j++)
					{
						j1 = j + 1;
						if (bij[i][j])
						{
							if (!bfirst)
								out.print(" + ");
							bfirst = false;
							strEqns.append(sij[i][j] + "*vCorrection(" + j1 + ")");
						}

					}
					strEqns.append(" = " + sb[i]);
					strEqns.append(";\n");
				}
			}
			else		//use Lagrangian multiplier method
			{
				//declare correction factors and Lagrangian multipliers
				nLinearEqn = nSpecies + nElement;
				out.println("vCorrection([1:" + nLinearEqn + "]) as RealVariable;");
				//equations for Langrangian minimization
				for (i=0; i<nSpecies; i++)
				{
					i1 = i + 1;
					iSpecies = iOutletSpecies[i];
					sp = allSpeciesList.get(iSpecies);
					strEqns.append("2*vCorrection(" + i1 + ")");
					for (j=0; j<nElement; j++)
					{
						k = nSpecies + j + 1;
						iElement = iElementAll[j];
						if (sp.containsElement(iElement))
							strEqns.append(" + " + sp.getNumberOfAtoms(iElement) + "*vSpeciesMolarFlowOut(" + i1 + ")*vCorrection(" + k + ")");
					}
					strEqns.append(" = 0;\n");
				}
				for (j=0; j<nElement; j++)
				{
					k = j + 1;
					iElement = iElementAll[j];
					bFirst = true;
					for (i=0; i<nSpecies; i++)
					{
						i1 = i + 1;
						iSpecies = iOutletSpecies[i];
						sp = allSpeciesList.get(iSpecies);
						if (sp.containsElement(iElement))
						{
							if (bFirst)
								bFirst = false;
							else
								strEqns.append(" + ");
							strEqns.append(sp.getNumberOfAtoms(iElement) + "*vSpeciesMolarFlowOut(" + i1 + ")*vCorrection(" + i1 + ")");
						}
					}
					strEqns.append(" = vDeltaElementMolarFlow(" + k + ");\n");
				}
			}
			//perform correction and assign molar flow to product ports
			nBoundary = outletBoundaryList.size();
			for (i=0; i<nBoundary; i++)
			{
				fb = outletBoundaryList.get(i);
				boundaryName = fb.getBoundaryName();
				if (fb.hasGasPhase())
				{
					portName = boundaryName + "GP";
					connectionName = portName + ".connection(" + portName + "Name+\"MIXED\")";
					gm = fb.getGasMixture().get(0);
					flowMap = gm.getSpeciesFlowMap();
					variableName = portName + "_F";
					strMolarFlowAll = "";
					for(Map.Entry<Integer,Parameter> entry : flowMap.entrySet())
					{
						iSpeciesAll = entry.getKey().intValue();
						for (k=0; k<nSpecies; k++)
						{
							i1 = k + 1;
							if (iOutletSpecies[k]==iSpeciesAll)
							{
								speciesName = allSpeciesList.get(iSpeciesAll).getName();
								speciesNameMod = speciesName.replace('+','p');
								speciesNameMod = speciesNameMod.replace('-','m');
								if (!strMolarFlowAll.isEmpty())
								  strMolarFlowAll += " + ";
								strMolarFlowAll += variableName + "_" + speciesNameMod + "*(1 + vCorrection(" + i1 + "))";
								strEqns.append(connectionName + ".F/3600*" + connectionName + ".Z(\"" + speciesName + "\") = " + variableName + "_" + speciesNameMod + "*(1 + vCorrection(" + i1 + "));\n");
								break;
							}
						}
					}
					strEqns.append("FOR i in " + connectionName + ".Componentlist - " + portName + "_Species DO\n");
					strEqns.append("\t" + connectionName+ ".Z(i) = 0;\n");
					strEqns.append("ENDFOR\n");
					//strEqns.append("SIGMA(FOREACH (i in " + portName + "_Species) " + connectionName + ".Z(i)) = 1;\n");
					//replace SIGMA equation with Molar flow explicitly calculated could avoid mass imbalance in some cases with low molar balance. Revised 12/15/2015
					strEqns.append(connectionName + ".F/3600 = " + strMolarFlowAll + ";\n");
				}
				if (fb.hasSolidPhase())
				{
					nPhase = fb.getSolidMixtures().size();
					for (j=0; j<nPhase; j++)
					{
						sm = fb.getSolidMixtures().get(j);
						portName = boundaryName + "SP" + sm.getPhaseIndex();
						flowMap = sm.getSpeciesFlowMap();
						variableName = portName + "_F";
						subStreamType = aspenSubStreamNames[sm.getSolidType()];
						connectionName = portName + ".connection(" + portName + "Name+" + subStreamType + ")";
						for(Map.Entry<Integer,Parameter> entry : flowMap.entrySet())
						{
							iSpecies = entry.getKey().intValue();
							iSpeciesAll = iSolidPhase2All[sm.getPhaseIndex()][iSpecies];
							for (k=0; k<nSpecies; k++)
							{
								i1 = k + 1;
								if (iOutletSpecies[k]==iSpeciesAll)
								{
									speciesName = allSpeciesList.get(iSpeciesAll).getName();
									speciesNameMod = speciesName.replace('+','p');
									speciesNameMod = speciesNameMod.replace('-','m');
									if (sm.getSolidType()==0 || sm.getSolidType()==2)		//CISOLID, CIPSD
										strEqns.append(connectionName + ".F/3600*" + connectionName + ".Z(\"" + speciesName + "\") = " + variableName + "_" + speciesNameMod + "*(1 + vCorrection(" + i1 + "));\n");
									else	//NC, NCPSD
										strEqns.append(variableName + "_" + speciesNameMod + "_cor = " + variableName + "_" + speciesNameMod + "*(1 + vCorrection(" + i1 + "));\n");
									break;
								}
							}
						}
						if (sm.getSolidType()==0 || sm.getSolidType()==2)		//CISOLID, CIPSD
						{
							strEqns.append("FOR i in "  + connectionName + ".Componentlist - " + portName + "_Species DO\n");
							strEqns.append("\t" + connectionName + ".Z(i) = 0;\n");
							strEqns.append("ENDFOR\n");
							strEqns.append("SIGMA(FOREACH (i in " + portName + "_Species) " + connectionName + ".Z(i)) = 1;\n");
						}
						else		//NC, NCPSD
						{
							//assing corrected molar flow rate to PROXANAL, ULTANAL, SULFANAL
							ncName = solidPhaseList.get(sm.getPhaseIndex());
							double mw_c = 12.01115;
							double mw_h = 1.0079;
							double mw_n = 14.0067;
							double mw_cl = 35.453;
							double mw_s = 32.064;
							double mw_o = 15.9994;
							double mw_ah = 1;
							double mw_h2o = 18.0152;
							//calculate mass flow of individual NC species
							strEqns.append("//convert mole flow to mass flow\n");
							strEqns.append(portName + "_Fm_C = " + portName + "_F_C_cor*" + mw_c + ";\n");
							strEqns.append(portName + "_Fm_H = " + portName + "_F_H_cor*" + mw_h + ";\n");
							strEqns.append(portName + "_Fm_N = " + portName + "_F_N_cor*" + mw_n + ";\n");
							strEqns.append(portName + "_Fm_CL = " + portName + "_F_CL_cor*" + mw_cl + ";\n");
							strEqns.append(portName + "_Fm_S = " + portName + "_F_S_cor*" + mw_s + ";\n");
							strEqns.append(portName + "_Fm_O = " + portName + "_F_O_cor*" + mw_o + ";\n");
							strEqns.append(portName + "_Fm_AH = " + portName + "_F_AH_cor*" + mw_ah + ";\n");
							strEqns.append(portName + "_Fm_H2O = " + portName + "_F_H2O_cor*" + mw_h2o + ";\n");
							strEqns.append(portName + "_Fm_dry = " + portName + "_Fm_C + " + portName + "_Fm_H + " + portName + "_Fm_N + " + portName + "_Fm_CL + " + portName + "_Fm_S + " + portName + "_Fm_O + " + portName + "_Fm_AH;\n");
							strEqns.append(portName + "_Fm = " + portName + "_Fm_dry + " + portName + "_Fm_H2O;\n");
							//assign total mass flow rate, proximate, ultimate and sulfur analysis arrays
							strEqns.append("//assign mass flow rate, proximate, ultimate, and sulfur analysis arrays\n");
							strEqns.append(connectionName + ".Fm = " + portName + "_Fm*3600.0;\n");
							strEqns.append(connectionName + ".PROXANAL(\"" + ncName + "\",\"MOISTURE\")/100.0*" + portName + "_Fm = " + portName + "_Fm_H2O;\n");
							strEqns.append(connectionName + ".PROXANAL(\"" + ncName + "\",\"VM\") = " + portName + "_VM;\n");
							strEqns.append(connectionName + ".PROXANAL(\"" + ncName + "\",\"ASH\")/100.0*" + portName + "_Fm_dry = " + portName + "_Fm_AH;\n");
							strEqns.append(connectionName + ".PROXANAL(\"" + ncName + "\",\"FC\") + " + connectionName + ".PROXANAL(\"" + ncName + "\",\"VM\") + " + connectionName + ".PROXANAL(\"" + ncName + "\",\"ASH\") = 100.0;\n");
							strEqns.append(connectionName + ".ULTANAL(\"" + ncName + "\",\"CARBON\")/100.0*" + portName + "_Fm_dry = " + portName + "_Fm_C;\n");
							strEqns.append(connectionName + ".ULTANAL(\"" + ncName + "\",\"HYDROGEN\")/100.0*" + portName + "_Fm_dry = " + portName + "_Fm_H;\n");
							strEqns.append(connectionName + ".ULTANAL(\"" + ncName + "\",\"NITROGEN\")/100.0*" + portName + "_Fm_dry = " + portName + "_Fm_N;\n");
							strEqns.append(connectionName + ".ULTANAL(\"" + ncName + "\",\"CHLORINE\")/100.0*" + portName + "_Fm_dry = " + portName + "_Fm_CL;\n");
							strEqns.append(connectionName + ".ULTANAL(\"" + ncName + "\",\"SULFUR\")/100.0*" + portName + "_Fm_dry = " + portName + "_Fm_S;\n");
							strEqns.append(connectionName + ".ULTANAL(\"" + ncName + "\",\"OXYGEN\")/100.0*" + portName + "_Fm_dry = " + portName + "_Fm_O;\n");
							strEqns.append(connectionName + ".ULTANAL(\"" + ncName + "\",\"ASH\")/100.0*" + portName + "_Fm_dry = " + portName + "_Fm_AH;\n");
							strEqns.append(connectionName + ".SULFANAL(\"" + ncName + "\",\"SULFATE\")/100.0*" + portName + "_Fm_dry = " + portName + "_Fm_S;\n");
							strEqns.append(connectionName + ".SULFANAL(\"" + ncName + "\",\"PYRITIC\") = 0.0;\n");
							strEqns.append(connectionName + ".SULFANAL(\"" + ncName + "\",\"ORGANIC\") = 0.0;\n");
						}
					}
				}
			}
			//energy balance correction
			//EnergyCorrectionOption == 0, set temperatures same as the ROM predicted temperatures
			strEqns.append("IF EnergyCorrectionOption == 0 THEN\n");
			nBoundary = outletBoundaryList.size();
			for (i=0; i<nBoundary; i++)
			{
				fb = outletBoundaryList.get(i);
				boundaryName = fb.getBoundaryName();
				if (fb.hasGasPhase())
				{
					portName = boundaryName + "GP";
					connectionName = portName + ".connection(" + portName + "Name+\"MIXED\")";
					strEqns.append("\tCall (" + portName + "_MW) = pMolWeight(" + connectionName + ".Z);\n");
					strEqns.append("\t" + connectionName + ".T = " + portName + "_T_rom;\n");
					strEqns.append("\tCall (" + connectionName + ".h) = pEnth_Mol(" + connectionName + ".T, " + connectionName + ".P, " + connectionName + ".Z);\n");
					strEqns.append("\t" + portName + "_H_rom = " + connectionName + ".h;\n");
					totalEnthalpyOutEqn += connectionName + ".F/3600*" + connectionName + ".h*1e9 + ";
				}
				if (fb.hasSolidPhase())
				{
					nPhase = fb.getSolidMixtures().size();
					for (j=0; j<nPhase; j++)
					{
						sm = fb.getSolidMixtures().get(j);
						subStreamType = aspenSubStreamNames[sm.getSolidType()];
						portName = boundaryName + "SP" + sm.getPhaseIndex();
						connectionName = portName + ".connection(" + portName + "Name+" + subStreamType + ")";
						if (sm.getSolidType()==0 || sm.getSolidType()==2)		//CISOLID, CIPSD
						{
							strEqns.append("\tCall (" + portName + "_MW) = pMolWeight(" + connectionName + ".Z) " + connectionName + ".componentlist;\n");
							strEqns.append("\t" + connectionName + ".T = " + portName + "_T_rom;\n");
							strEqns.append("\tCall (" + connectionName + ".h) = pEnth_Mol_Sol(" + connectionName + ".T, " + connectionName + ".P, " + connectionName + ".Z) " + connectionName + ".componentlist;\n");
							strEqns.append("\t" + portName + "_H_rom = " + connectionName + ".h;\n");
							totalEnthalpyOutEqn += connectionName + ".F/3600*" + connectionName + ".h*1e9 + ";
						}
						if (sm.getSolidType()==1 || sm.getSolidType()==3)		//NC, NCPSD
						{
							//molecular weight is always 1 for NC, NCPSD
							strEqns.append("\t" + connectionName + ".T = " + portName + "_T_rom;\n");
							strEqns.append("\tCall (" + connectionName + ".hm) = pEnth_Mass_NCSolid(" + connectionName + ".T, " + connectionName + ".P, " + connectionName + ".Zm, " + connectionName + ".PROXANAL, " + connectionName + ".ULTANAL, " + connectionName + ".SULFANAL, " + connectionName + ".GENANAL) " + connectionName + ".componentlist;\n");
							strEqns.append("\t" + portName + "_H_rom = " + connectionName + ".hm;\n");
							totalEnthalpyOutEqn += connectionName + ".Fm/3600*" + connectionName + ".hm*1e6 + ";
						}
					}
				}
			}
			strEqns.append("\t" + totalEnthalpyOutEqn.substring(0,totalEnthalpyOutEqn.length()-3) + ";\n");
			strEqns.append("\tH_correction = 0;\n");
			strEqns.append("\tHeat_Loss_Corrected = TotalEnthalpyIn - TotalEnthalpyOut;\n");
			strEqns.append("\tQout.Q = Heat_Loss_Corrected/1e9*3600;\n");
			strEqns.append("ELSE\n");
			//EnergyCorrectionOption == 1, correct temperature
			totalEnthalpyOutEqn = "TotalEnthalpyOut = ";
			for (i=0; i<nBoundary; i++)
			{
				fb = outletBoundaryList.get(i);
				boundaryName = fb.getBoundaryName();
				if (fb.hasGasPhase())
				{
					portName = boundaryName + "GP";
					connectionName = portName + ".connection(" + portName + "Name+\"MIXED\")";
					strEqns.append("\tCall (" + portName + "_MW) = pMolWeight(" + connectionName + ".Z);\n");
					strEqns.append("\tCall (" + portName + "_H_rom) = pEnth_Mol(" + portName + "_T_rom, " + connectionName + ".P, " + connectionName + ".Z);\n");
					totalEnthalpyOutEqn += connectionName + ".F/3600*" + portName + "_H_rom*1e9 + ";
				}
				if (fb.hasSolidPhase())
				{
					nPhase = fb.getSolidMixtures().size();
					for (j=0; j<nPhase; j++)
					{
						sm = fb.getSolidMixtures().get(j);
						subStreamType = aspenSubStreamNames[sm.getSolidType()];
						portName = boundaryName + "SP" + sm.getPhaseIndex();
						connectionName = portName + ".connection(" + portName + "Name+" + subStreamType + ")";
						if (sm.getSolidType()==0 || sm.getSolidType()==2)		//CISOLID, CIPSD
						{
							strEqns.append("\tCall (" + portName + "_MW) = pMolWeight(" + connectionName + ".Z) " + connectionName + ".componentlist;\n");
							strEqns.append("\tCall (" + portName + "_H_rom) = pEnth_Mol_Sol(" + portName + "_T_rom, " + connectionName + ".P, " + connectionName + ".Z) " + connectionName + ".componentlist;\n");
							totalEnthalpyOutEqn += connectionName + ".F/3600*" + portName + "_H_rom*1e9 + ";
						}
						if (sm.getSolidType()==1 || sm.getSolidType()==3)		//NC, NCPSD
						{
							//MW is 1 for NC
							strEqns.append("\tCall (" + portName + "_H_rom) = pEnth_Mass_NCSolid(" + portName + "_T_rom, " + connectionName + ".P, " + connectionName + ".Zm, " + connectionName + ".PROXANAL, " + connectionName + ".ULTANAL, " + connectionName + ".SULFANAL, " + connectionName + ".GENANAL) " + connectionName + ".componentlist;\n");
							totalEnthalpyOutEqn += connectionName + ".Fm/3600*" + portName + "_H_rom*1e6 + ";
						}
					}
				}
			}
			strEqns.append("\t" + totalEnthalpyOutEqn.substring(0,totalEnthalpyOutEqn.length()-3) + ";\n");
			if (param_heatloss==null)
				strEqns.append("\tH_correction = (TotalEnthalpyIn - TotalEnthalpyOut)/TotalMassIn;\n");
			else
				strEqns.append("\tH_correction = (TotalEnthalpyIn - TotalEnthalpyOut - " + param_heatloss.getName() + ")/TotalMassIn;\n");
			for (i=0; i<nBoundary; i++)
			{
				fb = outletBoundaryList.get(i);
				boundaryName = fb.getBoundaryName();
				if (fb.hasGasPhase())
				{
					portName = boundaryName + "GP";
					connectionName = portName + ".connection(" + portName + "Name+\"MIXED\")";
					strEqns.append("\t" + connectionName + ".h = " + portName + "_H_rom + H_correction*" + portName + "_MW/1e9;\n");
					strEqns.append("\tCall (" + connectionName + ".h) = pEnth_Mol(" + connectionName + ".T, " + connectionName + ".P, " + connectionName + ".Z);\n");
				}
				if (fb.hasSolidPhase())
				{
					nPhase = fb.getSolidMixtures().size();
					for (j=0; j<nPhase; j++)
					{
						sm = fb.getSolidMixtures().get(j);
						subStreamType = aspenSubStreamNames[sm.getSolidType()];
						portName = boundaryName + "SP" + sm.getPhaseIndex();
						connectionName = portName + ".connection(" + portName + "Name+" + subStreamType + ")";
						if (sm.getSolidType()==0 || sm.getSolidType()==2)		//CISOLID, CIPSD
						{
							strEqns.append("\t" + connectionName + ".h = " + portName + "_H_rom + H_correction*" + portName + "_MW/1e9;\n");
							strEqns.append("\tCall (" + connectionName + ".h) = pEnth_Mol_Sol(" + connectionName + ".T, " + connectionName + ".P, " + connectionName + ".Z) " + connectionName + ".componentlist;\n");
						}
						if (sm.getSolidType()==1 || sm.getSolidType()==3)		//NC, NCPSD
						{
							strEqns.append("\t" + connectionName + ".hm = " + portName + "_H_rom + H_correction/1e6;\n");
							strEqns.append("\tCall (" + connectionName + ".hm) = pEnth_Mass_NCSolid(" + connectionName + ".T, " + connectionName + ".P, " + connectionName + ".Zm, " + connectionName + ".PROXANAL, " + connectionName + ".ULTANAL, " + connectionName + ".SULFANAL, " + connectionName + ".GENANAL) " + connectionName + ".componentlist;\n");
						}
					}
				}
			}
			if (param_heatloss!=null)
				strEqns.append("\tHeat_Loss_Corrected = " + param_heatloss.getName() + ";\n");
			else
				strEqns.append("\tHeat_Loss_Corrected = 0;\n");
			strEqns.append("\tQout.Q = Heat_Loss_Corrected/1e9*3600;\n");
			strEqns.append("ENDIF\n");
			//add the equations if valid substreams and species
			out.println("IF containsAllSolidNames AND containsGasSpecies AND containsSolidSpecies THEN");
			out.print(strEqns.toString());
			out.println("ENDIF");
			out.println("//Regression variables and equations need to be appended");
			//final section or End statement in ROM file so the ROM file can be append to this file
			out.close();
		}
		catch(IOException e)
		{
			System.out.println(e);
		}
		//need to append source code related to regression
		//need to add mass balance correction algorithm in ACM language
	}

	//export ROM to CapeOpen readable file
	public void exportCapeOpenRom()
	{
		int i, j, k;
		String fileName = getName();
		String fileNameWithExtension;
		//make sure the fileName contains extension ".rom"
		int iPoint = fileName.lastIndexOf(".");
		if (iPoint>=0)
			fileNameWithExtension = fileName.substring(0,iPoint) + ".rom";
		else
			fileNameWithExtension = fileName + ".rom";
		try
		{
			FileWriter outFile = new FileWriter(fileNameWithExtension);
			PrintWriter out = new PrintWriter(outFile);
			int iSpecies;
			int nSpecies;
			int iSpeciesAll;
			int nPort;
			int nPhase;
			int nBoundary;
			int nParameter;
			float defaultValue;
			String portName;
			String boundaryName;
			Species sp;
			FlowBoundary fb;
			GasMixture gm;
			SolidMixture sm;
			Parameter param;
			Map<Integer,Parameter> flowMap;
			//comment line
			out.println("//ROM for unit operation: " + getName());
			//version number
			out.println("0\t//version number");
			//regression method
			out.println(regMethod.ordinal() + "\t//" + regMethod + " regression method");
			//all species list
			nSpecies = allSpeciesList.size();
			out.println(nSpecies + "\t//number of species");
			out.println("//list of species name, formula, molecular weight, number of elements, iatom, natom");
			for (i=0; i<nSpecies; i++)
			{
				sp = allSpeciesList.get(i);
				defaultValue = (float)sp.getMolecularWeight();
				k = sp.getNumberOfElements();
				out.print(sp.getName() + "\t" + sp.getFormula() + "\t" + defaultValue + "\t" + k);
				for (j=0; j<k; j++)
					out.print("\t" + sp.getAtomicNumbers()[j] + "\t" + sp.getAtomCounts()[j]);
				out.println();
			}
			//translate inlet boundaries to inlet ports, split gas and solid phases
			//first calculate number of port and assign port names
			nPort = 0;
			nBoundary = inletBoundaryList.size();
			for (i=0; i<nBoundary; i++)
			{
				fb = inletBoundaryList.get(i);
				if (fb.hasGasPhase())
					nPort++;
				if (fb.hasSolidPhase())
					nPort += fb.getSolidMixtures().size();
			}
			out.println(nPort + "\t//number of feed ports");
			for (i=0; i<nBoundary; i++)
			{
				fb = inletBoundaryList.get(i);
				boundaryName = fb.getBoundaryName();
				if (fb.hasGasPhase())
				{
					portName = boundaryName + "GP";
					out.println(portName + "\t//port name");
					out.println("0\t//gas phase port");
					gm = fb.getGasMixture().get(0);
					//pressure
					param = gm.getPressure();
					param.printCapeOpen(out, "pressure");
					//temperature
					param = gm.getTemperature();
					param.printCapeOpen(out, "temperature");
					//volume fraction
					param = gm.getVolumeFraction();
					param.printCapeOpen(out, "gas volume fraction");
					//species list
					flowMap = gm.getSpeciesFlowMap();
					out.println(flowMap.size() + "\t//number of species");
					if (gm.hasConstMassFractions())
					{
						//constant mass fraction flag
						out.println("1\t//constant mass fractions");
						//total mass flow rate
						param = gm.getTotalMassFlow();
						param.printCapeOpen(out, "total mass flow");
						for(Map.Entry<Integer,Parameter> entry : flowMap.entrySet())
						{
							iSpeciesAll = entry.getKey().intValue();
							param = entry.getValue();
							out.println(iSpeciesAll + "\t" + param.getDefaultValue() + "\t//mass fraction of " + allSpeciesList.get(iSpeciesAll).getName());
						}
					}
					else
					{
						//constant mass fraction flag
						out.println("0\t//not constant mass fractions");
						for(Map.Entry<Integer,Parameter> entry : flowMap.entrySet())
						{
							iSpeciesAll = entry.getKey().intValue();
							param = entry.getValue();
							if (param.isVaried())
								out.println(iSpeciesAll + "\t1\t" + param.getDefaultValue() + "\t" + param.getMinValue() + "\t" + param.getMaxValue() + "\t//mass flow of " + allSpeciesList.get(iSpeciesAll).getName());
							else
								out.println(iSpeciesAll + "\t0\t" + param.getDefaultValue() + "\t//mass flow of " + allSpeciesList.get(iSpeciesAll).getName());
						}
					}
				}
				if (fb.hasSolidPhase())
				{
					nPhase = fb.getSolidMixtures().size();
					for (j=0; j<nPhase; j++)
					{
						sm = fb.getSolidMixtures().get(j);
						portName = boundaryName + "SP" + sm.getPhaseIndex();
						out.println(portName + "\t//port name");
						out.println("1\t//discrete phase port");
						//pressure
						param = sm.getPressure();
						param.printCapeOpen(out, "pressure");
						//temperature
						param = sm.getTemperature();
						param.printCapeOpen(out, "temperature");
						//volume fraction
						param = sm.getVolumeFraction();
						param.printCapeOpen(out, "volume fraction");
						//particle diameter
						param = sm.getDiameter();
						param.printCapeOpen(out, "particle diameter");
						//particle density
						param = sm.getDensity();
						param.printCapeOpen(out, "particle density");
						if (sm.isGranularEnergySolved())
						{
							out.println("1\t//granular energy solved");
							//granular temperature
							param = sm.getGranularTemperature();
							param.printCapeOpen(out, "granular temperature");
						}
						else
							out.println("0\t//granular energy not solved");
						//species list
						flowMap = sm.getSpeciesFlowMap();
						out.println(flowMap.size() + "\t//number of species");
						if (sm.hasConstMassFractions())
						{
							//constant mass fraction flag
							out.println("1\t//constant mass fractions");
							//total mass flow rate
							param = sm.getTotalMassFlow();
							param.printCapeOpen(out, "total mass flow");
							for(Map.Entry<Integer,Parameter> entry : flowMap.entrySet())
							{
								iSpecies = entry.getKey().intValue();
								iSpeciesAll = iSolidPhase2All[sm.getPhaseIndex()][iSpecies];
								param = entry.getValue();
								out.println(iSpeciesAll + "\t" + param.getDefaultValue() + "\t//mass fraction of " + allSpeciesList.get(iSpeciesAll).getName());
							}
						}
						else
						{
							//constant mass fraction flag
							out.println("0\t//not constant mass fractions");
							for(Map.Entry<Integer,Parameter> entry : flowMap.entrySet())
							{
								iSpecies = entry.getKey().intValue();
								iSpeciesAll = iSolidPhase2All[sm.getPhaseIndex()][iSpecies];
								param = entry.getValue();
								if (param.isVaried())
									out.println(iSpeciesAll + "\t1\t" + param.getDefaultValue() + "\t" + param.getMinValue() + "\t" + param.getMaxValue() + "\t//mass flow of " + allSpeciesList.get(iSpeciesAll).getName());
								else
									out.println(iSpeciesAll + "\t0\t" + param.getDefaultValue() + "\t//mass flow of " + allSpeciesList.get(iSpeciesAll).getName());
							}
						}
					}
				}
			}
			//model input parameters, varied parameters only
			nParameter = inputParameterList.size();
			j = 0;
			for (i=0; i<nParameter; i++)
			{
				param = inputParameterList.get(i);
				if (param.isVaried())
					j++;
			}
			out.println(j + "\t//number of varied input parameters");
			for (i=0; i<nParameter; i++)
			{
				param = inputParameterList.get(i);
				if (param.isVaried())
				{
					out.print(param.getName() + "\t");		//parameter name added so that it is consistent with CAPE-OPEN DLL
					param.printCapeOpen(out, param.getName());
				}
			}
			//outlet boundaries
			nPort = 0;
			nBoundary = outletBoundaryList.size();
			for (i=0; i<nBoundary; i++)
			{
				fb = outletBoundaryList.get(i);
				if (fb.hasGasPhase())
					nPort++;
				if (fb.hasSolidPhase())
					nPort += fb.getSolidMixtures().size();
			}
			out.println(nPort + "\t//number of product ports");
			for (i=0; i<nBoundary; i++)
			{
				fb = outletBoundaryList.get(i);
				boundaryName = fb.getBoundaryName();
				if (fb.hasGasPhase())
				{
					portName = boundaryName + "GP";
					out.println(portName + "\t//port name");
					out.println("0\t//gas phase port");
					gm = fb.getGasMixture().get(0);
					//pressure
					param = gm.getPressure();
					if (param.isVaried())
						out.print("1");
					else
						out.print("0\t" + param.getDefaultValue());
					out.println("\t//pressure");
					//temperature
					param = gm.getTemperature();
					if (param.isVaried())
						out.print("1");
					else
						out.print("0\t" + param.getDefaultValue());
					out.println("\t//temperature");
					//volume fraction
					param = gm.getVolumeFraction();
					if (param.isVaried())
						out.print("1");
					else
						out.print("0\t" + param.getDefaultValue());
					out.println("\t//gas volume fraction");
					//species list
					flowMap = gm.getSpeciesFlowMap();
					out.println(flowMap.size() + "\t//number of species");
					//always non-constant speceis mass fractions
					for(Map.Entry<Integer,Parameter> entry : flowMap.entrySet())
					{
						iSpeciesAll = entry.getKey().intValue();
						param = entry.getValue();
						//always varied species mass flow rate
						out.println(iSpeciesAll + "\t//species index of " + allSpeciesList.get(iSpeciesAll).getName());
					}
				}
				if (fb.hasSolidPhase())
				{
					nPhase = fb.getSolidMixtures().size();
					for (j=0; j<nPhase; j++)
					{
						sm = fb.getSolidMixtures().get(j);
						portName = boundaryName + "SP" + sm.getPhaseIndex();
						out.println(portName + "\t//port name");
						out.println("1\t//discrete phase port");
						//pressure
						param = sm.getPressure();
						if (param.isVaried())
							out.print("1");
						else
							out.print("0\t" + param.getDefaultValue());
						out.println("\t//pressure");
						//temperature
						param = sm.getTemperature();
						if (param.isVaried())
							out.print("1");
						else
							out.print("0\t" + param.getDefaultValue());
						out.println("\t//temperature");
						//volume fraction
						param = sm.getVolumeFraction();
						if (param.isVaried())
							out.print("1");
						else
							out.print("0\t" + param.getDefaultValue());
						out.println("\t//particle volume fraction");
						//particle diameter
						param = sm.getDiameter();
						if (param.isVaried())
							out.print("1");
						else
							out.print("0\t" + param.getDefaultValue());
						out.println("\t//particle diameter");
						//particle density
						param = sm.getDensity();
						if (param.isVaried())
							out.print("1");
						else
							out.print("0\t" + param.getDefaultValue());
						out.println("\t//particle density");
						if (sm.isGranularEnergySolved())
						{
							out.println("1\t//granular energy solved");
							//granular temperature
							param = sm.getGranularTemperature();
							if (param.isVaried())
								out.print("1");
							else
								out.print("0\t" + param.getDefaultValue());
							out.println("\t//granular temperature");
						}
						else
							out.println("0\t//granular energy not solved");
						//species list
						flowMap = sm.getSpeciesFlowMap();
						out.println(flowMap.size() + "\t//number of species");
						//always non-constant speceis mass fractions
						for(Map.Entry<Integer,Parameter> entry : flowMap.entrySet())
						{
							iSpecies = entry.getKey().intValue();
							iSpeciesAll = iSolidPhase2All[sm.getPhaseIndex()][iSpecies];
							param = entry.getValue();
							//always varied species mass flow rate
							out.println(iSpeciesAll + "\t//species index of " + allSpeciesList.get(iSpeciesAll).getName());
						}
					}
				}
			}
			//model output parameters,varied parameters only
			nParameter = outputParameterList.size();
			j = 0;
			for (i=0; i<nParameter; i++)
			{
				param = outputParameterList.get(i);
				if (param.isVaried())
					j++;
			}
			out.println(j + "\t//number of varied output parameters");
			for (i=0; i<nParameter; i++)
			{
				param = outputParameterList.get(i);
				if (param.isVaried())
					out.println(param.getName() + "\t//output parameter");
			}
			out.close();
		}
		catch(IOException e)
		{
			System.out.println(e);
		}
		//need to append data realted to regression
	}

	//export file for sampling
	public void exportFileForSampling(String fileName)
	{
		//assuming updateRomInputVector() and updateYromOutputVector() have been called
		String fileNameWithExtension;
		//make sure the fileName contains extension ".io"
		int iPoint = fileName.lastIndexOf(".");
		if (iPoint>=0)
			fileNameWithExtension = fileName.substring(0,iPoint) + ".io";
		else
			fileNameWithExtension = fileName + ".in";
		try
		{
			FileWriter outFile = new FileWriter(fileNameWithExtension);
			PrintWriter out = new PrintWriter(outFile);
			int i;
			int ninput = romInputVector.size();
			int noutput = yromOutputVector.size();
			Parameter param;
			out.println(getName() + "\t//name of reduced order model");
			out.println(nSample + "\t//number of cases to run");
			out.println(ninput + "\t//number of input variables");
			for (i=0; i<ninput; i++)
			{
				param = romInputVector.get(i);
				if (param.getAlias()!=null && !param.getAlias().isEmpty())
					out.println(param.getName() + "_" + param.getAlias() + "\t" + param.getMinValue() + "\t" + param.getMaxValue());
				else
					out.println(param.getName() + "\t" + param.getMinValue() + "\t" + param.getMaxValue());
			}
			out.println(noutput + "\t//number of output variables");
			for (i=0; i<noutput; i++)
			{
				param = yromOutputVector.get(i);
				if (param.getAlias()!=null && !param.getAlias().isEmpty())
					out.println(param.getName() + "_" + param.getAlias());
				else
					out.println(param.getName());
			}
			out.close();
		}
		catch(IOException e)
		{
			System.out.println(e);
		}
	}

	//main method for command line executable
	//Command example: java DataModel.UnitOperation -b user_input.json row_vectors.input corrected_vectors.output
	//Command example: java DataModel.UnitOperation -a user_input.json
	//Command example: java DataModel.UnitOperation -c user_input.json
	public static void main(String argu[])
	{
		//argu[0]: option, -b for mass/energy balance correction, -a for exporting ACM code, -c for exporting CapeOpen code
		//argu[1]: name of JSON file for user inputs
		//argu[2]: name of ACM or CapeOpen file for output or the name of raw (uncorrected) ROM input and output vectors
		//argu[3]: name of file for input vector and output vector to be used by sampling code (rom.in) or name of corrected output file
		//Note: for -b option, input and output vectors for each case are in a single line, vecters can be in multiple groups separated by empty lines comparible to 2-D GNUPLOT format
		int i, j, k;
		int nGroup;		//number of groups
		int nCase;		//number of cases in each group
		float xfloat;
		i = argu.length;
		if (i<2)
		{
			System.out.println("Number of argument < 2!");
			return;
		}
		UnitOperation uo = new UnitOperation();
		//uo.readUserSetupFile(argu[1]);	//original version with setup input file
		uo.readUserJsonFile(argu[1]);
		uo.updateAllSpeciesList();
		if (argu[0].equals("-a"))
		{
			uo.exportACMRom();
			uo.exportFileForSampling("iReveal.io");
			return;
		}
		if (argu[0].equals("-c"))
		{
			uo.exportCapeOpenRom();
			uo.exportFileForSampling("iReveal.io");
			return;
		}
		//assume any other option is "-b" option
		if (i<4)
		{
			System.out.println("Number of argument < 4! Unable to perform mass/energy balance.");
			return;
		}
		int nInputParam = uo.romInputVector.size();
		int nOutputParam = uo.yromOutputVector.size();
		Scanner s = null;
		Parameter param;
		try
		{
			s = new Scanner(new BufferedReader(new FileReader(argu[2])));
			FileWriter outFile = new FileWriter(argu[3]);
			PrintWriter out = new PrintWriter(outFile);
			nGroup = s.nextInt();
			nCase = s.nextInt();
			s.nextLine();
			for (k=0; k<nGroup; k++)
			{
				for (j=0; j<nCase; j++)
				{
					for (i=0; i<nInputParam; i++)
					{
						param = uo.romInputVector.get(i);
						xfloat = s.nextFloat();
						param.setDefaultValue(xfloat);
					}
					for (i=0; i<nOutputParam; i++)
					{
						param = uo.yromOutputVector.get(i);
						xfloat = s.nextFloat();
						param.setDefaultValue(xfloat);
					}
					s.nextLine();
					//perform elemental balance correction
					uo.enforceElementalMassBalance();
					for (i=0; i<nInputParam; i++)
					{
						param = uo.romInputVector.get(i);
						xfloat = param.getDefaultValue();
						out.print(xfloat);
						out.print("\t");
					}
					for (i=0; i<nOutputParam; i++)
					{
						param = uo.yromOutputVector.get(i);
						xfloat = param.getDefaultValue();
						out.print(xfloat);
						out.print("\t");
					}
					out.println();
				}
				if (k<nGroup-1)
				{
					s.nextLine();
					out.println();
				}
			}
			out.close();
		}//end of try
		catch(IOException e)
		{
			System.out.println(e);
		}
		finally
		{
			s.close();
		}
	}
}