package io.harness.telemetry;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@OwnedBy(HarnessTeam.GTM)
public interface TelemetryReporter {
  /**
   * Sending track message
   * AccountId will be detected from PrincipalThreadLocal, and set in property as "groupId"
   * The identity is determined in the following ways:
   * 1.read identity from PrincipalThreadLocal
   * 2.if not present, will send it as a system user
   *
   * @param eventName  event name
   * @param properties event properties
   * @param destinations event destinations, null if sending to all destinations
   */
  void sendTrackEvent(String eventName, HashMap<String, Object> properties, Map<Destination, Boolean> destinations);

  /**
   * Sending track message
   * Use provided accountId in properties, if null retrieve from PrincipalThreadLocal
   * Using provided identity as userid irrespective what the caller user id is, if null retrieve from
   * PrincipalThreadLocal
   *
   * @param eventName event name
   * @param identity identity that used in the track event as userId
   * @param accountId used for value of "groupId" in properties
   * @param properties event properties
   * @param destinations event destinations, null if sending to all destinations
   */
  void sendTrackEvent(String eventName, String identity, String accountId, HashMap<String, Object> properties,
      Map<Destination, Boolean> destinations);

  /**
   * Sending identify message
   * @param identity event user id, use email as userid
   * @param properties event properties
   * @param destinations event destinations, null if sending to all destinations
   */
  void sendIdentifyEvent(String identity, HashMap<String, Object> properties, Map<Destination, Boolean> destinations);

  /**
   * Sending group message
   * The identity is determined in the following ways:
   * 1.read identity from PrincipalThreadLocal
   * 2.if not present, we will send it as a system user
   *
   * @param accountId account id
   * @param properties event properties
   * @param destinations event destinations, null if sending to all destinations
   */
  void sendGroupEvent(String accountId, HashMap<String, Object> properties, Map<Destination, Boolean> destinations);

  /**
   * Sending group message to Segment
   * @param accountId account id
   * @param identity identity message irrespective what the caller user id is
   * @param properties event properties
   * @param destinations event destinations, null if sending to all destinations
   */
  void sendGroupEvent(
      String accountId, String identity, HashMap<String, Object> properties, Map<Destination, Boolean> destinations);

  /**
   * Sending group message to Segment
   * for passing historical date only, otherwise use {@link #sendGroupEvent(String,String,HashMap,Map)} method
   *
   * @param accountId account id
   * @param identity identity message irrespective what the caller user id is
   * @param properties event properties
   * @param destinations event destinations, null if sending to all destinations
   * @param timestamp historical date
   */
  void sendGroupEvent(String accountId, String identity, HashMap<String, Object> properties,
      Map<Destination, Boolean> destinations, Date timestamp);
}