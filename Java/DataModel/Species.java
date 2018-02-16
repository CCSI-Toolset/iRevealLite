package DataModel;

import java.io.Serializable;
import com.google.gson.*;
import com.google.gson.annotations.Expose;

/**
 * Class representing a chemical species
 * @author Jinliang Ma at NETL
 */
public class Species implements Serializable {

	//private static final long serialVersionUID = -7548951555700258244L;
	//atomic mass of 87 atoms from periodic table, array index corresponding to atomic number, 0th element is Ah as coal ash with atomic mass of 1
	private static final double[] atomicMass = {1,1.0079,4.0026,6.939,9.0122,10.81,12.01115,14.0067,15.9994,18.994,20.183,22.9898,24.312,26.9815,28.086,30.9738,32.064,35.453,39.948,
		39.098,40.08,44.956,47.9,50.942,51.996,54.938,55.847,58.933,58.71,63.546,65.38,69.72,72.59,74.922,78.96,79.904,83.8,
		85.47,87.62,88.905,91.22,92.906,95.94,98.0,101.07,102.905,106.4,107.868,112.4,114.82,118.69,121.75,127.6,126.904,131.3,
		132.905,137.34,138.91,140.12,140.907,144.24,147.0,150.35,151.96,157.25,158.924,162.5,164.93,167.26,168.934,173.04,174.97,
		178.49,180.948,183.85,186.2,190.2,192.2,195.09,196.967,200.59,204.37,207.19,208.98,210.0,210.0,222.0};

	//maximum atomic number from periodic table plus 0th element ash
	public static final int maxAtomicNumberPlus1 = 88;

	//maximum number of elements in a species, currently set to 10
	private static final int nElementMax = 10;

	//species name, corresponding to Aspen's component ID. It could contain + or -
	//when exporting to ACM, + and - is converted to p and m
	@Expose
	private String name;

	//species formula, for two-letter element, the second letter should be lower case to avoid confusion (e.g. SI is S + I not Si), allow + or - for cation and anion
	//formula is parsed to calculate nAtom, iAtom, and nElement
	@Expose
	private String formula;

	//atom counts for each element in formula, use double in case not the whole number
	private double[] nAtom;

	//atomic number in periodic table for each element in formula
	private int[] iAtom;

	//number of elements in species
	private int nElement;

	//molecular weight of species
	private double molecularWeight;

	//constructor with default species of N2
	public Species() {
		name = new String("N2");
		formula = new String("N2");
		nAtom = new double[nElementMax];
		iAtom = new int[nElementMax];
		parseFormula();
	}

	//constructor with default species of N2 with a given commom name
	public Species(String speciesName) {
		name = speciesName;
		formula = new String("N2");
		nAtom = new double[nElementMax];
		iAtom = new int[nElementMax];
		parseFormula();
	}

	//constructor with given common name and formula
	public Species(String speciesName, String speciesFormula) {
		name = speciesName;
		formula = speciesFormula;
		nAtom = new double[nElementMax];
		iAtom = new int[nElementMax];
		parseFormula();
	}

	public int getNumberOfElements() {
		return nElement;
	}

	public double getMolecularWeight() {
		return molecularWeight;
	}

	public int[] getAtomicNumbers() {
		return iAtom;
	}

	public double[] getAtomCounts() {
		return nAtom;
	}

	public String getName() {
		return name;
	}

	public void setName(String speciesName) {
		name = speciesName;
	}

	public String getFormula() {
		return formula;
	}

	//set species formula, member data is calculated based on the formula given
	public void setFormula(String speciesFormula) {
		formula = speciesFormula;
		parseFormula();
	}

	//calculate index based on letter symbol (1-based)
	//return 0 if failed
	private int getElementIndex(String ele)
	{
		//up to atomic number 86 (excluding 7th period and Actinoids)
		//first check the length of string
		char ch;
		char ch2;
		int nlen = ele.length();
		if (nlen<1)
			return 0;
		if (nlen>2)
			return 0;
		ch = ele.charAt(0);
		if (nlen==1)
		{
			switch (ch)
			{
			case 'H':
				return 1;
			case 'B':
				return 5;
			case 'C':
				return 6;
			case 'N':
				return 7;
			case 'O':
				return 8;
			case 'F':
				return 9;
			case 'P':
				return 15;
			case 'S':
				return 16;
			case 'K':
				return 19;
			case 'V':
				return 23;
			case 'Y':
				return 39;
			case 'I':
				return 53;
			case 'W':
				return 74;
			default:
				return 0;
			}
		}
		else
		{
			ch2 = ele.charAt(1);
			switch (ch)
			{
			case 'A':
				switch (ch2)
				{
				case 'l':
					return 13;
				case 'r':
					return 18;
				case 's':
					return 33;
				case 'g':
					return 47;
				case 'u':
					return 79;
				case 't':
					return 85;
				case 'h':		//use Ah for coal ash as the 0th emement with atomic weight of 1
					return 0;
				}
				break;
			case 'B':
				switch (ch2)
				{
				case 'e':
					return 4;
				case 'r':
					return 35;
				case 'a':
					return 56;
				case 'i':
					return 83;
				}
				break;
			case 'C':
				switch (ch2)
				{
				case 'l':
					return 17;
				case 'a':
					return 20;
				case 'r':
					return 24;
				case 'o':
					return 27;
				case 'u':
					return 29;
				case 'd':
					return 48;
				case 's':
					return 55;
				case 'e':
					return 58;
				}
				break;
			case 'D':
				switch (ch2)
				{
				case 'y':
					return 66;
				}
				break;
			case 'E':
				switch (ch2)
				{
				case 'u':
					return 63;
				case 'r':
					return 68;
				}
				break;
			case 'F':
				switch (ch2)
				{
				case 'e':
					return 26;
				}
				break;
			case 'G':
				switch (ch2)
				{
				case 'a':
					return 31;
				case 'e':
					return 32;
				case 'd':
					return 64;
				}
				break;
			case 'H':
				switch (ch2)
				{
				case 'e':
					return 2;
				case 'o':
					return 67;
				case 'f':
					return 72;
				case 'g':
					return 80;
				}
				break;
			case 'I':
				switch (ch2)
				{
				case 'n':
					return 49;
				case 'r':
					return 77;
				}
				break;
			case 'K':
				switch (ch2)
				{
				case 'r':
					return 36;
				}
				break;
			case 'L':
				switch (ch2)
				{
				case 'i':
					return 3;
				case 'a':
					return 57;
				case 'u':
					return 71;
				}
				break;
			case 'M':
				switch (ch2)
				{
				case 'g':
					return 12;
				case 'n':
					return 25;
				case 'o':
					return 42;
				}
				break;
			case 'N':
				switch (ch2)
				{
				case 'e':
					return 10;
				case 'a':
					return 11;
				case 'i':
					return 28;
				case 'b':
					return 41;
				case 'd':
					return 60;
				}
				break;
			case 'O':
				switch (ch2)
				{
				case 's':
					return 76;
				}
				break;
			case 'P':
				switch (ch2)
				{
				case 'd':
					return 46;
				case 'r':
					return 59;
				case 'm':
					return 61;
				case 't':
					return 78;
				case 'b':
					return 82;
				case 'o':
					return 84;
				}
				break;
			case 'R':
				switch (ch2)
				{
				case 'b':
					return 37;
				case 'u':
					return 44;
				case 'h':
					return 45;
				case 'e':
					return 75;
				case 'n':
					return 86;
				}
				break;
			case 'S':
				switch (ch2)
				{
				case 'i':
					return 14;
				case 'c':
					return 21;
				case 'e':
					return 34;
				case 'r':
					return 38;
				case 'n':
					return 50;
				case 'b':
					return 51;
				case 'm':
					return 62;
				}
				break;
			case 'T':
				switch (ch2)
				{
				case 'i':
					return 22;
				case 'c':
					return 43;
				case 'e':
					return 52;
				case 'b':
					return 65;
				case 'm':
					return 69;
				case 'a':
					return 73;
				case 'l':
					return 81;
				}
				break;
			case 'X':
				switch (ch2)
				{
				case 'e':
					return 54;
				}
				break;
			case 'Y':
				switch (ch2)
				{
				case 'b':
					return 70;
				}
				break;
			case 'Z':
				switch (ch2)
				{
				case 'n':
					return 30;
				case 'r':
					return 40;
				}
				break;
			}
		}
		return 0;
	}

	//parse species formula to calculate nAtom, iAtom, and molecular weight
	//allow ions with +/- characters attached e.g. CO3-2 or CO3--
	//return a non-zero value if failed
	private int parseFormula()
	{
		//currently only allow for formula without parenthes or fraction value of atoms
		int i, j, k;
		int len;
		double dAtom;
		char ch;
		char ch2;
		String ele;
		String cleft;
		nElement = 0;
		cleft = formula;
		while (cleft.length()>0 && nElement<nElementMax)
		{
			ch = cleft.charAt(0);
			if (Character.isLetter(ch) && Character.isUpperCase(ch))		//upper case
			{
				if (cleft.length()>1)
				{
					ch2 = cleft.charAt(1);
					if (Character.isLetter(ch2) && Character.isLowerCase(ch2))
					{
						ele = cleft.substring(0,2);
						cleft = cleft.substring(2);
					}
					else
					{
						ele = cleft.substring(0,1);
						cleft = cleft.substring(1);
					}
				}
				else
				{
					ele = cleft.substring(0,1);
					cleft = cleft.substring(1);
				}
				//get element index
				i = getElementIndex(ele);
				if (i<=-1)
					return 2;
				iAtom[nElement] = i;
				//get number of atoms
				len = cleft.length();
				j = 0;
				while (j<len)
				{
					ch = cleft.charAt(j);
					if (Character.isDigit(ch) || ch=='.')
						j++;
					else
						break;
				}
				if (j>0)
				{
					ele = cleft.substring(0,j);
					cleft = cleft.substring(j);
					dAtom = Double.parseDouble(ele);
				}
				else
					dAtom = 1;
				nAtom[nElement] = dAtom;
				nElement++;
			}
			else	//modified on 4/29/2013 by J Ma to take care of +-( in formula
			{
				if (ch=='+' || ch=='-' || ch=='(')
					break;
				else
					return 1;
			}
		}
		//eliminate duplicated elements
		for (i=0; i<nElement-1; i++)
		{
			for (j=i+1; j<nElement; j++)
			{
				if (iAtom[j]==iAtom[i])
				{
					nAtom[i] += nAtom[j];
					for (k=j; k<nElement-1; k++)
					{
						iAtom[k] = iAtom[k+1];
						nAtom[k] = nAtom[k+1];
					}
					nElement--;
				}
			}
		}
		updateMolecularWeight();
		return 0;
	}

	private void updateMolecularWeight()
	{
		molecularWeight = 0;
		for (int i=0; i<nElement; i++)
			molecularWeight += nAtom[i]*atomicMass[iAtom[i]];
	}

	public boolean containsElement(int iAtomicNumber)
	{
		int i;
		for (i=0; i<nElement; i++)
		{
			if (iAtom[i]==iAtomicNumber)
				return true;
		}
		return false;
	}

	public double getNumberOfAtoms(int iAtomicNumber)
	{
		int i;
		for (i=0; i<nElement; i++)
		{
			if (iAtom[i]==iAtomicNumber)
				return nAtom[i];
		}
		return 0;
	}

	public boolean equals(Object obj)
	{
		if (obj==null) return false;
		if (!(obj instanceof Species)) return false;
		return name.equalsIgnoreCase(((Species)obj).name) && formula.equalsIgnoreCase(((Species)obj).formula);
	}
}
