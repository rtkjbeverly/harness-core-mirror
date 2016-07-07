/**
 *
 */

package software.wings.sm;

import software.wings.beans.Application;
import software.wings.beans.Environment;

import java.util.List;

// TODO: Auto-generated Javadoc

/**
 * The Interface ExecutionContext.
 *
 * @author Rishi
 */
public interface ExecutionContext {
  /**
   * Evaluate expression.
   *
   * @param expression the expression
   * @return the object
   */
  public Object evaluateExpression(String expression);

  /**
   * Evaluate expression.
   *
   * @param expression         the expression
   * @param stateExecutionData the state execution data
   * @return the object
   */
  public Object evaluateExpression(String expression, StateExecutionData stateExecutionData);

  /**
   * Gets state execution data.
   *
   * @return the state execution data
   */
  public StateExecutionData getStateExecutionData();

  /**
   * Render expression.
   *
   * @param expression the expression
   * @return the string
   */
  public String renderExpression(String expression);

  /**
   * Render expression.
   *
   * @param expression         the expression
   * @param stateExecutionData the state execution data
   * @return the string
   */
  public String renderExpression(String expression, StateExecutionData stateExecutionData);

  /**
   * Gets context element.
   *
   * @param <T>                the type parameter
   * @param contextElementType the context element type
   * @return the context element
   */
  <T extends ContextElement> T getContextElement(ContextElementType contextElementType);

  /**
   * Gets context element list.
   *
   * @param <T>                the type parameter
   * @param contextElementType the context element type
   * @return the context element list
   */
  <T extends ContextElement> List<T> getContextElementList(ContextElementType contextElementType);

  /**
   * Gets app.
   *
   * @return app app
   */
  Application getApp();

  /**
   * Gets env.
   *
   * @return env env
   */
  Environment getEnv();

  String getWorkflowExecutionId();
}
