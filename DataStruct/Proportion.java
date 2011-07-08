/*----------------------------------------------------------------------*/
/*

        Module          : Proportion.java

        Package         : DataStruct

        Classes Included: Proportion

        Purpose         : Handle low-level proportion information

        Programmer      : Ted Dumitrescu

        Date Started    : 99

Updates:
9/19/05: added some arithmetic operations (addition, reduction, comparison)
7/1/10:  added init from double (estimate)

                                                                        */
/*----------------------------------------------------------------------*/

package DataStruct;

/*------------------------------------------------------------------------
Class:   Proportion
Extends: -
Purpose: Information structure for a proportion of two integers
------------------------------------------------------------------------*/

public class Proportion
{
/*----------------------------------------------------------------------*/
/* Class variables */

  public static final Proportion EQUALITY=new Proportion(1,1);

/*----------------------------------------------------------------------*/
/* Instance variables */

  public int i1,i2;

/*----------------------------------------------------------------------*/
/* Class methods */

/*------------------------------------------------------------------------
Method:  Proportion copyProportion(Proportion p)
Purpose: Create new proportion with given value, or null
Parameters:
  Input:  Proportion p - item to copy
  Output: -
  Return: new proportion or null
------------------------------------------------------------------------*/

  public static Proportion copyProportion(Proportion p)
  {
    return p==null ? null : new Proportion(p);
  }

/*------------------------------------------------------------------------
Method:  Proportion difference(Proportion p1,Proportion p2)
Purpose: Calculate difference of two proportions without modifying either
Parameters:
  Input:  Proportion p1,p2 - proportions to subtract (p2 from p1)
  Output: -
  Return: new proportion representing difference
------------------------------------------------------------------------*/

  public static Proportion difference(Proportion p1,Proportion p2)
  {
    if (p1==null || p2==null)
      return null;

    Proportion diffVal=new Proportion(p1);
    diffVal.subtract(p2);
    return diffVal;
  }

/*------------------------------------------------------------------------
Method:  Proportion product(Proportion p1,Proportion p2)
Purpose: Calculate product of two proportions without modifying either
Parameters:
  Input:  Proportion p1,p2 - proportions to multiply
  Output: -
  Return: new proportion representing product
------------------------------------------------------------------------*/

  public static Proportion product(Proportion p1,Proportion p2)
  {
    if (p1==null || p2==null)
      return null;

    Proportion productVal=new Proportion(p1);
    productVal.multiply(p2);
    return productVal;
  }

  public static Proportion quotient(Proportion p1,Proportion p2)
  {
    if (p1==null || p2==null)
      return null;

    Proportion quotientVal=new Proportion(p1);
    quotientVal.divide(p2);
    return quotientVal;
  }

/*------------------------------------------------------------------------
Method:  Proportion sum(Proportion p1,Proportion p2)
Purpose: Calculate sum of two proportions without modifying either
Parameters:
  Input:  Proportion p1,p2 - proportions to add
  Output: -
  Return: new proportion representing sum
------------------------------------------------------------------------*/

  public static Proportion sum(Proportion p1,Proportion p2)
  {
    if (p1==null && p2==null)
      return null;
    if (p1==null)
      return new Proportion(p2);
    if (p2==null)
      return new Proportion(p1);

    Proportion sumVal=new Proportion(p1);
    sumVal.add(p2);
    return sumVal;
  }

  /* max/min */
  public static Proportion max(Proportion p1,Proportion p2)
  {
    return p1.greaterThanOrEqualTo(p2) ? p1 : p2;
  }

  public static Proportion min(Proportion p1,Proportion p2)
  {
    return p1.lessThanOrEqualTo(p2) ? p1 : p2;
  }

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: public Proportion(int n1,int n2)
Purpose:     Initialize proportion information
Parameters:
  Input:  int n1 - first number in proportion (numerator)
          int n2 - second number in proportion (denominator)
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public Proportion(int n1,int n2)
  {
    i1=n1;
    i2=n2;
  }

  /* copy another Proportion */
  public Proportion(Proportion p)
  {
    i1=p.i1;
    i2=p.i2;
  }

  /* estimate from double */
  static final double FLOAT_ACCURACY=10000;

  public Proportion(double val)
  {
    this.i1=(int)(Math.round(val*FLOAT_ACCURACY));
    this.i2=(int)FLOAT_ACCURACY;
    reduce();
  }

/*------------------------------------------------------------------------
Method:  void setVal(Proportion p)
Purpose: Copy another proportion
Parameters:
  Input:  Proportio p - proportion to copy
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void setVal(Proportion p)
  {
    i1=p.i1;
    i2=p.i2;
  }

  public void setVal(int i1,int i2)
  {
    this.i1=i1;
    this.i2=i2;
  }

/*------------------------------------------------------------------------
Method:  void reduce()
Purpose: Reduce proportion to lowest terms
Parameters:
  Input:  -
  Output: -
  Return: this
------------------------------------------------------------------------*/

  Proportion reduce()
  {
    if (i1==0)
      return this;

    /* find greatest common factor */
    int gnum,lnum,remainder;

    if (i1>i2)
      {
        gnum=i1;
        lnum=i2;
      }
    else
      {
        gnum=i2;
        lnum=i1;
      }
    remainder=gnum%lnum;
    while (remainder!=0)
      {
        gnum=lnum;
        lnum=remainder;
        remainder=gnum%lnum;
      }

    /* lnum==GCF */
    i1/=lnum;
    i2/=lnum;

    return this;
  }

/*------------------------------------------------------------------------
Method:  boolean equals(Proportion p)
Purpose: Compare for equality against another proportion
Parameters:
  Input:  Proportion p - proportion to compare
  Output: -
  Return: true if equal
------------------------------------------------------------------------*/

  public boolean equals(Proportion p)
  {
    return toDouble()==p.toDouble();
  }

/*------------------------------------------------------------------------
Method:  boolean [greater|less]Than[OrEqualto](Proportion p)
Purpose: Compare against another proportion
Parameters:
  Input:  Proportion p - proportion to compare
  Output: -
  Return: true if this > p
------------------------------------------------------------------------*/

  public boolean greaterThan(Proportion p)
  {
    return this.toDouble()>p.toDouble();
  }

  public boolean greaterThanOrEqualTo(Proportion p)
  {
    return this.toDouble()>=p.toDouble();
  }

  public boolean lessThan(Proportion p)
  {
    return this.toDouble()<p.toDouble();
  }

  public boolean lessThanOrEqualTo(Proportion p)
  {
    return this.toDouble()<=p.toDouble();
  }

/*------------------------------------------------------------------------
Method:  void add(Proportion p)
Purpose: Add another proportion to this one
Parameters:
  Input:  Proportion p - proportion to add
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void add(Proportion p)
  {
    if (p==null || p.i1==0)
      return;

    if (i2!=p.i2)
      {
        i1=i1*p.i2+p.i1*i2;
        i2*=p.i2;
        reduce();
      }
    else
      i1+=p.i1;
  }

/*------------------------------------------------------------------------
Method:  void multiply(int o1,int o2|Proportion other)
Purpose: Multiply this proportion by another
Parameters:
  Input:  int o1,int o2|Proportion other - proportion by which to multiply
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void multiply(int o1,int o2)
  {
    i1*=o1;
    i2*=o2;
    reduce();
  }

  public void multiply(Proportion other)
  {
    this.i1*=other.i1;
    this.i2*=other.i2;
    reduce();
  }

  public void divide(Proportion other)
  {
    this.i1*=other.i2;
    this.i2*=other.i1;
    reduce();
  }

/*------------------------------------------------------------------------
Method:  void subtract(Proportion other)
Purpose: Subtract another proportion from this one
Parameters:
  Input:  Proportion other - proportion to subtract
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void subtract(Proportion other)
  {
    if (other==null || other.i1==0)
      return;

    if (this.i2!=other.i2)
      {
        this.i1=this.i1*other.i2-other.i1*this.i2;
        this.i2*=other.i2;
        reduce();
      }
    else
      this.i1-=other.i1;
  }

/*------------------------------------------------------------------------
Method:  int toInt()
Purpose: Convert to int
Parameters:
  Input:  -
  Output: -
  Return: integer representing proportion value rounded down
------------------------------------------------------------------------*/

  public int toInt()
  {
    return i1/i2;
  }

/*------------------------------------------------------------------------
Method:  double toDouble()
Purpose: Convert to double
Parameters:
  Input:  -
  Output: -
  Return: double representing proportion value
------------------------------------------------------------------------*/

  public double toDouble()
  {
    return (double)i1/(double)i2;
  }

  public String toString()
  {
    return i1+"/"+i2;
  }
}
