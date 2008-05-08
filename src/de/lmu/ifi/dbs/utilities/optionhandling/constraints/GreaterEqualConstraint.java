package de.lmu.ifi.dbs.utilities.optionhandling.constraints;

import de.lmu.ifi.dbs.utilities.optionhandling.NumberParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;

/**
 * Represents a Greater-Equal-Than-Number parameter constraint. The
 * value of the number parameter ({@link NumberParameter})
 * tested has to be greater equal than the specified
 * constraint value.
 *
 * @author Steffi Wanka
 */
public class GreaterEqualConstraint extends AbstractNumberConstraint<Number> {
  /**
   * Creates a Greater-Equal parameter constraint.
   * <p/>
   * That is, the value of the number
   * parameter given has to be greater equal than the constraint value given.
   *
   * @param constraintValue the constraint value
   */
  public GreaterEqualConstraint(Number constraintValue) {
    super(constraintValue);
  }

  /**
   * Checks if the number value given by the number parameter is
   * greater equal than the constraint
   * value. If not, a parameter exception is thrown.
   *
   * @see de.lmu.ifi.dbs.utilities.optionhandling.constraints.ParameterConstraint#test(java.lang.Object)
   */
  public void test(Number t) throws ParameterException {
    if (t.doubleValue() < constraintValue.doubleValue()) {
      throw new WrongParameterValueException("Parameter Constraint Error: \n"
                                             + "The parameter value specified has to be greater equal than "
                                             + constraintValue.toString() +
                                             ". (current value: " + t.doubleValue() + ")\n");
    }
  }

}
