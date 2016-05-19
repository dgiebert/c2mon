/******************************************************************************
 * Copyright (C) 2010-2016 CERN. All rights not expressly granted are reserved.
 *
 * This file is part of the CERN Control and Monitoring Platform 'C2MON'.
 * C2MON is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the license.
 *
 * C2MON is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for
 * more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with C2MON. If not, see <http://www.gnu.org/licenses/>.
 *****************************************************************************/

package cern.c2mon.client.core.configuration;

import cern.c2mon.shared.client.configuration.ConfigurationReport;
import cern.c2mon.shared.client.configuration.api.alarm.Alarm;
import cern.c2mon.shared.client.configuration.api.alarm.AlarmCondition;

import java.util.List;
import java.util.Map;

/**
 * @author Franz Ritter
 */
public interface AlarmConfiguration {

  /**
   * Creates a new 'Alarm' on the server with the given id, alarm condition and fault parameters.
   * <br/>
   * The Alarm is created with default parameters.
   *
   * @param tagId          The id of the overlying Tag.
   * @param alarmCondition The alarm condition which triggers the alarm.
   * @param faultFamily    LASER fault family of the alarm.
   * @param faultMember    LASER fault member of the alarm.
   * @param faultCode      LASER fault code of the alarm.
   * @return A {@link ConfigurationReport} containing all details of the Alarm configuration,
   * including if it was successful or not.
   * @see AlarmConfiguration#createAlarm(Long, Alarm)
   * @see AlarmConfiguration#createAlarm(String, AlarmCondition, String, String, Integer)
   * @see AlarmConfiguration#createAlarm(String, Alarm)
   */
  ConfigurationReport createAlarm(Long tagId, AlarmCondition alarmCondition, String faultFamily, String faultMember, Integer faultCode);

  /**
   * Creates a new 'Alarm' on the server with the given id, alarm condition and fault parameters set in the {@link Alarm} object.
   * <br/>
   * Next to the specified parameters the Alarm is created with default parameters.
   * <p>
   * Note: You have to use {@link Alarm#create(String, String, Integer, AlarmCondition)} to instantiate the 'alarm' parameter of this method.
   * </p>
   *
   * @param tagId The id of the overlying Tag.
   * @param alarm The {@link Alarm} configuration for the 'create'.
   * @return A {@link ConfigurationReport} containing all details of the Alarm configuration,
   * including if it was successful or not.
   * @see AlarmConfiguration#createAlarm(Long, AlarmCondition, String, String, Integer)
   * @see AlarmConfiguration#createAlarm(String, AlarmCondition, String, String, Integer)
   * @see AlarmConfiguration#createAlarm(String, Alarm)
   */
  ConfigurationReport createAlarm(Long tagId, Alarm alarm);

  /**
   * Creates multiple new 'Alarms' on the server with the given id, alarm condition and fault parameters set in the {@link Alarm} objects.
   * <br/>
   * Next to the specified parameters the Alarm is created with default parameters.
   * <p>
   * Note: You have to use {@link Alarm#create(String, String, Integer, AlarmCondition)} to instantiate the 'alarms' parameter of this method.
   * </p>
   *
   * @param alarms A map which holds all Alarms for the create. The key values represents the id of the overlying Tag.
   * @return A {@link ConfigurationReport} containing all details of the Alarm configuration,
   * including if it was successful or not.
   * @see AlarmConfiguration#createAlarmsByName(Map)
   */
  ConfigurationReport createAlarmsById(Map<Long, Alarm> alarms);

  /**
   * Creates a new 'Alarm' on the server with the given id, alarm condition and fault parameters.
   * <br/>
   * The Alarm is created with default parameters.
   *
   * @param tagName        The name of the overlying Tag.
   * @param alarmCondition The alarm condition which triggers the alarm.
   * @param faultFamily    LASER fault family of the alarm.
   * @param faultMember    LASER fault member of the alarm.
   * @param faultCode      LASER fault code of the alarm.
   * @return A {@link ConfigurationReport} containing all details of the Alarm configuration,
   * including if it was successful or not.
   * @see AlarmConfiguration#createAlarm(Long, Alarm)
   * @see AlarmConfiguration#createAlarm(String, AlarmCondition, String, String, Integer)
   * @see AlarmConfiguration#createAlarm(String, Alarm)
   */
  ConfigurationReport createAlarm(String tagName, AlarmCondition alarmCondition, String faultFamily, String faultMember, Integer faultCode);

  /**
   * Creates a new 'Alarm' on the server with the given id, alarm condition and fault parameters set in the {@link Alarm} object.
   * <br/>
   * Next to the specified parameters the Alarm is created with default parameters.
   * <p>
   * Note: You have to use {@link Alarm#create(String, String, Integer, AlarmCondition)} to instantiate the 'alarm' parameter of this method.
   * </p>
   *
   * @param tagName The name of the overlying Tag.
   * @param alarm The {@link Alarm} configuration for the 'create'.
   * @return A {@link ConfigurationReport} containing all details of the Alarm configuration,
   * including if it was successful or not.
   * @see AlarmConfiguration#createAlarm(Long, AlarmCondition, String, String, Integer)
   * @see AlarmConfiguration#createAlarm(String, AlarmCondition, String, String, Integer)
   * @see AlarmConfiguration#createAlarm(String, Alarm)
   */
  ConfigurationReport createAlarm(String tagName, Alarm alarm);

  /**
   * Creates multiple new 'Alarms' on the server with the given id, alarm condition and fault parameters set in the {@link Alarm} objects.
   * <br/>
   * Next to the specified parameters the Alarm is created with default parameters.
   * <p>
   * Note: You have to use {@link Alarm#create(String, String, Integer, AlarmCondition)} to instantiate the 'alarms' parameter of this method.
   * </p>
   *
   * @param alarms A map which holds all Alarms for the create. The key values represents the name of the overlying Tag.
   * @return A {@link ConfigurationReport} containing all details of the Alarm configuration,
   * including if it was successful or not.
   * @see AlarmConfiguration#createAlarmsById(Map)
   */
  ConfigurationReport createAlarmsByName(Map<String, Alarm> alarms);

  /**
   * Updates a existing 'Alarm' with the given parameters in the {@link Alarm} object.
   * <br/>
   * <p>
   * Note: You have to use {@link Alarm#update(Long)} to instantiate the 'updateAlarm' parameter of this method.
   * </p>
   *
   * @param updateAlarm The {@link Alarm} configuration for the 'update'.
   * @return A {@link ConfigurationReport} containing all details of the Alarm configuration,
   * including if it was successful or not.
   */
  ConfigurationReport updateAlarm(Alarm updateAlarm);

  /**
   * Updates multiple existing 'Alarms' with the given parameters in the {@link Alarm} objects.
   * <br/>
   * <p>
   * Note: You have to use {@link Alarm#update(Long)} to instantiate the 'updateAlarms' parameter of this method.
   * </p>
   *
   * @param updateAlarms The list of {@link Alarm} configurations for the 'update'.
   * @return A {@link ConfigurationReport} containing all details of the Alarm configuration,
   * including if it was successful or not.
   */
  ConfigurationReport updateAlarms(List<Alarm> updateAlarms);

  /**
   * Removes a existing 'Alarm' with the given id.
   *
   * @param id The id of the Alarm which needs to be removed.
   * @return A {@link ConfigurationReport} containing all details of the Alarm configuration,
   * including if it was successful or not.
   */
  ConfigurationReport removeAlarm(Long id);

  /**
   * Removes multiple existing 'Alarms' with the given ids.
   *
   * @param ids The list of ids of the Alarms which needs to be removed.
   * @return A {@link ConfigurationReport} containing all details of the Alarm configuration,
   * including if it was successful or not.
   */
  ConfigurationReport removeAlarms(List<Long> ids);
}
