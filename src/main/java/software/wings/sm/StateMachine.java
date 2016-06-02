package software.wings.sm;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.PostLoad;
import org.mongodb.morphia.annotations.Serialized;
import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.Base;
import software.wings.beans.ErrorConstants;
import software.wings.beans.Graph;
import software.wings.beans.Graph.Link;
import software.wings.beans.Graph.Node;
import software.wings.beans.User;
import software.wings.beans.Workflow;
import software.wings.exception.WingsException;
import software.wings.sm.states.ForkState;
import software.wings.utils.MapperUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Describes a StateMachine.
 *
 * @author Rishi
 */
@Entity(value = "stateMachines", noClassnameStored = true)
public class StateMachine extends Base {
  @SuppressWarnings("unused") private static final long serialVersionUID = 1L;

  @Indexed private String originId;

  @Indexed private String name;

  private Graph graph;

  @Serialized private List<State> states = new ArrayList<>();

  @Serialized private List<Transition> transitions = new ArrayList<>();

  private String initialStateName;

  @Transient private transient Map<String, State> cachedStatesMap = null;

  @Transient private transient Map<String, Map<TransitionType, List<State>>> cachedTransitionFlowMap = null;

  public StateMachine() {}

  public StateMachine(Workflow workflow, Graph graph, Map<String, StateTypeDescriptor> stencilMap) {
    setAppId(workflow.getAppId());
    this.originId = workflow.getUuid();
    this.graph = graph;
    this.name = graph.getGraphName();
    transform(stencilMap);
  }

  private void transform(Map<String, StateTypeDescriptor> stencilMap) {
    String originStateId = null;
    for (Node node : graph.getNodes()) {
      if (node.isOrigin()) {
        originStateId = node.getId();
        continue;
      }
      if (node.getType() == null || stencilMap.get(node.getType()) == null) {
        throw new WingsException(ErrorConstants.INVALID_REQUEST, "message", "Unknown stencil type");
      }

      StateTypeDescriptor stateTypeDesc = stencilMap.get(node.getType());

      State state = stateTypeDesc.newInstance(node.getName());

      Map<String, Object> properties = node.getProperties();

      // populate properties
      MapperUtils.mapObject(properties, state);

      addState(state);
    }

    if (originStateId == null) {
      throw new WingsException(ErrorConstants.INVALID_REQUEST, "message", "Origin state missing");
    }

    try {
      Map<String, Node> nodeIdMap = graph.getNodesMap();
      Map<String, State> statesMap = getStatesMap();

      if (graph.getLinks() != null) {
        for (Link link : graph.getLinks()) {
          Node nodeFrom = nodeIdMap.get(link.getFrom());
          Node nodeTo = nodeIdMap.get(link.getTo());

          if (nodeFrom.isOrigin()) {
            setInitialStateName(nodeTo.getName());
            continue;
          }

          State stateFrom = statesMap.get(nodeFrom.getName());
          State stateTo = statesMap.get(nodeTo.getName());

          TransitionType transitionType = TransitionType.valueOf(link.getType().toUpperCase());

          if (transitionType == TransitionType.FORK) {
            ((ForkState) stateFrom).addForkState(stateTo);
          } else {
            addTransition(Transition.Builder.aTransition()
                              .withFromState(stateFrom)
                              .withTransitionType(transitionType)
                              .withToState(stateTo)
                              .build());
          }
        }
      }
      validate();
    } catch (IllegalArgumentException e) {
      throw new WingsException(e);
    }
  }

  public String getInitialStateName() {
    return initialStateName;
  }

  public void setInitialStateName(String initialStateName) {
    this.initialStateName = initialStateName;
  }

  public List<State> getStates() {
    return states;
  }

  public void setStates(List<State> states) {
    this.states = states;
  }

  /**
   * Adds a state to state machine.
   *
   * @param state state to be added.
   * @return state after saving.
   */
  public State addState(State state) {
    if (states == null) {
      states = new ArrayList<>();
    }
    states.add(state);
    return state;
  }

  public List<Transition> getTransitions() {
    return transitions;
  }

  public void setTransitions(List<Transition> transitions) {
    this.transitions = transitions;
  }

  /**
   * Adds transition to state machine.
   *
   * @param transition transition to be added.
   * @return transition after add.
   */
  public Transition addTransition(Transition transition) {
    if (transitions == null) {
      transitions = new ArrayList<>();
    }
    transitions.add(transition);
    return transition;
  }

  public State getInitialState() {
    Map<String, State> statesMap = getStatesMap();
    return statesMap.get(initialStateName);
  }

  /**
   * @return map to state to stateNames.
   */
  public Map<String, State> getStatesMap() {
    if (cachedStatesMap != null && cachedStatesMap.size() > 0) {
      return cachedStatesMap;
    }
    Map<String, State> statesMap = new HashMap<>();
    HashSet<String> dupNames = new HashSet<>();

    if (states != null) {
      for (State state : states) {
        if (statesMap.get(state.getName()) != null) {
          dupNames.add(state.getName());
        } else {
          statesMap.put(state.getName(), state);
        }
      }
    }
    if (dupNames.size() > 0) {
      throw new WingsException(ErrorConstants.DUPLICATE_STATE_NAMES, "dupStateNames", dupNames.toString());
    }

    cachedStatesMap = statesMap;
    return statesMap;
  }

  public State getSuccessTransition(String fromStateName) {
    return getNextState(fromStateName, TransitionType.SUCCESS);
  }

  /**
   * Returns next state given start state and transition type.
   *
   * @param fromStateName  start state.
   * @param transitionType transition type to look state from.
   * @return first next state if any or null.
   */
  public State getNextState(String fromStateName, TransitionType transitionType) {
    List<State> nextStates = getNextStates(fromStateName, transitionType);
    if (nextStates == null || nextStates.size() == 0) {
      return null;
    }
    return nextStates.get(0);
  }

  /**
   * Returns list of next states given start state and transition type.
   *
   * @param fromStateName  start state.
   * @param transitionType transition type to look state from.
   * @return list of next states or null.
   */
  public List<State> getNextStates(String fromStateName, TransitionType transitionType) {
    Map<String, Map<TransitionType, List<State>>> transitionFlowMap = getTransitionFlowMap();
    if (transitionFlowMap == null || transitionFlowMap.get(fromStateName) == null) {
      return null;
    }
    return transitionFlowMap.get(fromStateName).get(transitionType);
  }

  /**
   * @return a transition flow map describing transition types to list of states.
   */
  public Map<String, Map<TransitionType, List<State>>> getTransitionFlowMap() {
    if (cachedTransitionFlowMap != null && cachedTransitionFlowMap.size() > 0) {
      return cachedTransitionFlowMap;
    }

    Map<String, State> statesMap = getStatesMap();
    Set<String> invalidStateNames = new HashSet<>();
    Set<String> statesWithDupTransitions = new HashSet<>();
    Set<String> nonForkStates = new HashSet<>();
    Set<String> nonRepeatStates = new HashSet<>();
    Map<String, Map<TransitionType, List<State>>> flowMap = new HashMap<>();

    Map<String, List<String>> forkStateNamesMap = new HashMap<>();
    if (transitions != null) {
      for (Transition transition : transitions) {
        if (transition.getTransitionType() == null) {
          throw new WingsException(ErrorConstants.TRANSITION_TYPE_NULL);
        }
        State fromState = transition.getFromState();
        State toState = transition.getToState();
        if (fromState == null || toState == null) {
          throw new WingsException(ErrorConstants.TRANSITION_NOT_LINKED);
        }
        boolean invalidState = false;
        if (statesMap.get(fromState.getName()) == null) {
          invalidStateNames.add(fromState.getName());
          invalidState = true;
        }
        if (statesMap.get(toState.getName()) == null) {
          invalidStateNames.add(toState.getName());
          invalidState = true;
        }
        if (!invalidState) {
          if (transition.getTransitionType() == TransitionType.FORK
              && !StateType.FORK.name().equals(fromState.getStateType())) {
            nonForkStates.add(fromState.getName());
            continue;
          }

          if (transition.getTransitionType() == TransitionType.REPEAT
              && !StateType.REPEAT.name().equals(fromState.getStateType())) {
            nonRepeatStates.add(fromState.getName());
            continue;
          }

          Map<TransitionType, List<State>> transitionMap = flowMap.get(fromState.getName());
          if (transitionMap == null) {
            transitionMap = new HashMap<>();
            flowMap.put(fromState.getName(), transitionMap);
            List<State> toStates = new ArrayList<>();
            toStates.add(toState);
            transitionMap.put(transition.getTransitionType(), toStates);
          } else {
            List<State> toStates = transitionMap.get(transition.getTransitionType());
            if (toStates == null) {
              toStates = new ArrayList<>();
              toStates.add(toState);
              transitionMap.put(transition.getTransitionType(), toStates);
            } else {
              if (transition.getTransitionType() != TransitionType.FORK
                  && transition.getTransitionType() != TransitionType.CONDITIONAL) {
                statesWithDupTransitions.add(fromState.getName());
              } else {
                toStates.add(toState);
              }
            }
          }

          if (transition.getTransitionType() == TransitionType.FORK) {
            List<String> forkStateNames = forkStateNamesMap.get(fromState.getName());
            if (forkStateNames == null) {
              forkStateNames = new ArrayList<>();
              forkStateNamesMap.put(fromState.getName(), forkStateNames);
            }
            forkStateNames.add(toState.getName());
          }
        }
      }
    }
    if (invalidStateNames.size() > 0) {
      throw new WingsException(
          ErrorConstants.TRANSITION_TO_INCORRECT_STATE, "invalidStateNames", invalidStateNames.toString());
    }
    if (nonForkStates.size() > 0) {
      throw new WingsException(ErrorConstants.NON_FORK_STATES, "nonForkStates", nonForkStates.toString());
    }
    if (nonRepeatStates.size() > 0) {
      throw new WingsException(ErrorConstants.NON_REPEAT_STATES, "nonRepeatStates", nonRepeatStates.toString());
    }
    if (statesWithDupTransitions.size() > 0) {
      throw new WingsException(
          ErrorConstants.STATES_WITH_DUP_TRANSITIONS, "statesWithDupTransitions", statesWithDupTransitions.toString());
    }
    for (String forkStateName : forkStateNamesMap.keySet()) {
      ForkState forkFromState = (ForkState) statesMap.get(forkStateName);
      forkFromState.setForkStateNames(forkStateNamesMap.get(forkStateName));
    }

    cachedTransitionFlowMap = flowMap;
    return flowMap;
  }

  public State getFailureTransition(String fromStateName) {
    return getNextState(fromStateName, TransitionType.FAILURE);
  }

  /**
   * Get state based on name.
   *
   * @param stateName name of state to lookup for.
   * @return state object if found or null.
   */
  public State getState(String stateName) {
    Map<String, State> statesMap = getStatesMap();
    if (statesMap == null) {
      return null;
    }
    return statesMap.get(stateName);
  }

  @PostLoad
  public void afterLoad() {
    validate();
  }

  /**
   * Validates a state machine.
   *
   * @return true if valid.
   */
  public boolean validate() {
    Map<String, State> statesMap = getStatesMap();
    if (initialStateName == null || statesMap.get(initialStateName) == null) {
      throw new WingsException(ErrorConstants.INITIAL_STATE_NOT_DEFINED);
    }
    getTransitionFlowMap();
    return true;
  }

  public String getOriginId() {
    return originId;
  }

  public void setOriginId(String originId) {
    this.originId = originId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Graph getGraph() {
    return graph;
  }

  public void setGraph(Graph graph) {
    this.graph = graph;
  }

  @Override
  public String toString() {
    return "StateMachine [initialStateName=" + initialStateName + ", states=" + states + ", transitions=" + transitions
        + "]";
  }

  public static final class Builder {
    private String originId;
    private String name;
    private Graph graph;
    private List<State> states;
    private List<Transition> transitions;
    private String initialStateName;
    private String uuid;
    private String appId;
    private User createdBy;
    private long createdAt;
    private User lastUpdatedBy;
    private long lastUpdatedAt;
    private boolean active = true;

    private Builder() {}

    public static Builder aStateMachine() {
      return new Builder();
    }

    public Builder withOriginId(String originId) {
      this.originId = originId;
      return this;
    }

    public Builder withName(String name) {
      this.name = name;
      return this;
    }

    public Builder withGraph(Graph graph) {
      this.graph = graph;
      return this;
    }

    public Builder withStates(List<State> states) {
      this.states = states;
      return this;
    }

    public Builder withTransitions(List<Transition> transitions) {
      this.transitions = transitions;
      return this;
    }

    public Builder withInitialStateName(String initialStateName) {
      this.initialStateName = initialStateName;
      return this;
    }

    public Builder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public Builder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    public Builder withCreatedBy(User createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    public Builder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public Builder withLastUpdatedBy(User lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    public Builder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    public Builder withActive(boolean active) {
      this.active = active;
      return this;
    }

    public StateMachine build() {
      StateMachine stateMachine = new StateMachine();
      stateMachine.setOriginId(originId);
      stateMachine.setName(name);
      stateMachine.setGraph(graph);
      stateMachine.setStates(states);
      stateMachine.setTransitions(transitions);
      stateMachine.setInitialStateName(initialStateName);
      stateMachine.setUuid(uuid);
      stateMachine.setAppId(appId);
      stateMachine.setCreatedBy(createdBy);
      stateMachine.setCreatedAt(createdAt);
      stateMachine.setLastUpdatedBy(lastUpdatedBy);
      stateMachine.setLastUpdatedAt(lastUpdatedAt);
      stateMachine.setActive(active);
      return stateMachine;
    }
  }
}
