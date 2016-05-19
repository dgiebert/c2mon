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
package cern.c2mon.server.configuration.parser.util;

import cern.c2mon.shared.client.configuration.api.alarm.Alarm;
import cern.c2mon.shared.client.configuration.api.alarm.RangeCondition;
import cern.c2mon.shared.client.configuration.api.alarm.ValueCondition;
import cern.c2mon.shared.common.metadata.Metadata;

import java.util.Properties;

/**
 * Utility class which provides builder methods for different Alarm objects.
 * All methods imply that the Alarm is build as instance od a DataTag
 */
public class ConfigurationAlarmUtil {


  public static Pair<Alarm, Properties> buildAlarmWithId(Long id) {
    return new Pair<>(Alarm.builder().id(id).build(), new Properties());
  }

  public static Pair<Alarm, Properties> buildAlarmWithPrimFields(Long id) {
    Alarm pro = Alarm.builder()
        .id(id)
        .faultFamily("faultFam_Test")
        .faultMember("faultMember_Test")
        .faultCode(10)
        .build();

    Properties props = new Properties();
    props.setProperty("faultFamily", "faultFam_Test");
    props.setProperty("faultMember", "faultMember_Test");
    props.setProperty("faultCode", "10");
    props.setProperty("dataTagId", "1");

    return new Pair<>(pro, props);
  }

  public static Pair<Alarm, Properties> buildAlarmWithAllFields(Long id) {
    Alarm pro = Alarm.builder()
        .id(id)
        .faultFamily("faultFam_Test")
        .faultMember("faultMember_Test")
        .faultCode(10)
        .alarmCondition(RangeCondition.builder().dataType(Integer.class).minValue(0).maxValue(10).build())
        .metadata(Metadata.builder().addMetadata("testMetadata",11).build())
        .build();

    Properties props = new Properties();
    props.setProperty("faultFamily", "faultFam_Test");
    props.setProperty("faultMember", "faultMember_Test");
    props.setProperty("faultCode", "10");
    props.setProperty("dataTagId", "1");
    String condition = "<AlarmCondition class=\"cern.c2mon.server.common.alarm.RangeAlarmCondition\">\n" +
        "<min-value type=\"" + Integer.class.getName() + "\">0</min-value>\n" +
        "<max-value type=\"" + Integer.class.getName() + "\">10</max-value>\n" +
        "</AlarmCondition>";
    props.setProperty("alarmCondition", condition.toString());
    props.setProperty("metadata", Metadata.toJSON(Metadata.builder().addMetadata("testMetadata",11).build()));

    return new Pair<>(pro, props);
  }


  public static Pair<Alarm, Properties> buildAlarmWithoutDefaultFields(Long id) {

    //because there are no default fields for the alarm this method is the same than the buildPrim Method
    return buildAlarmWithPrimFields(id);
  }

  public static Pair<Alarm, Properties> buildUpdateAlarmWithAllFields(Long id) {
    Alarm pro = Alarm.builder()
        .id(id)
        .faultFamily("faultFam_Test_Update")
        .faultMember("faultMember_Test_Update")
        .faultCode(11)
        .alarmCondition(ValueCondition.builder().dataType(Double.class).value(10).build())
        .metadata(Metadata.builder().addMetadata("testMetadata_update",true).build())
        .build();

    Properties props = new Properties();
    props.setProperty("faultFamily", "faultFam_Test_Update");
    props.setProperty("faultMember", "faultMember_Test_Update");
    props.setProperty("faultCode", "11");
    String condition = "<AlarmCondition class=\"cern.c2mon.server.common.alarm.ValueAlarmCondition\">\n" +
        "<alarm-value type=\"" + Double.class.getName() + "\">10</alarm-value>\n" +
        "</AlarmCondition>";
    props.setProperty("alarmCondition", condition.toString());
    props.setProperty("metadata", Metadata.toJSON(Metadata.builder().addMetadata("testMetadata_update",true).build()));

    return new Pair<>(pro, props);
  }

  public static Pair<Alarm, Properties> buildUpdateAlarmWithSomeFields(Long id) {
    Alarm pro = Alarm.builder()
        .id(id)
        .faultFamily("faultFam_Test_Update")
        .faultCode(11)
        .alarmCondition(ValueCondition.builder().dataType(Double.class).value(10).build())
        .metadata(Metadata.builder().addMetadata("testMetadata_update",true).build())
        .build();

    Properties props = new Properties();
    props.setProperty("faultFamily", "faultFam_Test_Update");
    props.setProperty("faultCode", "11");
    String condition = "<AlarmCondition class=\"cern.c2mon.server.common.alarm.ValueAlarmCondition\">\n" +
        "<alarm-value type=\"" + Double.class.getName() + "\">10</alarm-value>\n" +
        "</AlarmCondition>";
    props.setProperty("alarmCondition", condition.toString());
    props.setProperty("metadata", Metadata.toJSON(Metadata.builder().addMetadata("testMetadata_update",true).build()));

    return new Pair<>(pro, props);
  }

  public static Alarm buildDeleteAlarm(Long id) {
    Alarm pro = Alarm.builder()
        .id(id)
        .deleted(true)
        .build();

    return pro;
  }
  // ##################### Builder #####################

  public static Pair<Alarm.AlarmBuilder, Properties> builderAlarmWithPrimFields(Long id, String parent, Long parentId) {
    Alarm.AlarmBuilder pro = Alarm.builder()
        .id(id)
        .faultFamily("faultFam_Test")
        .faultMember("faultMember_Test")
        .faultCode(10);

    Properties props = new Properties();
    props.setProperty("faultFamily", "faultFam_Test");
    props.setProperty("faultMember", "faultMember_Test");
    props.setProperty("faultCode", "10");
    switch(parent){
      case "commFaultTag" : props.setProperty("dataTagId", Long.toString(parentId)); break;
      case "aliveTag" : props.setProperty("dataTagId", Long.toString(parentId)); break;
      case "dataTag" : props.setProperty("dataTagId", Long.toString(parentId)); break;
      case "ruleTag" : props.setProperty("dataTagId", Long.toString(parentId)); break;
      case "statusTag" : props.setProperty("dataTagId", Long.toString(parentId)); break;
      default: throw new RuntimeException("not such super class given");
    }

    return new Pair<>(pro, props);
  }

  public static Pair<Alarm.AlarmBuilder, Properties> builderAlarmWithAllFields(Long id, Long parentId) {
    Alarm.AlarmBuilder pro = Alarm.builder()
        .id(id)
        .faultFamily("faultFam_Test")
        .faultMember("faultMember_Test")
        .faultCode(10)
        .alarmCondition(RangeCondition.builder().dataType(Integer.class).minValue(0).maxValue(10).build())
        .metadata(Metadata.builder().addMetadata("testMetadata",11).build());

    Properties props = new Properties();
    props.setProperty("faultFamily", "faultFam_Test");
    props.setProperty("faultMember", "faultMember_Test");
    props.setProperty("faultCode", "10");
    String condition = "<AlarmCondition class=\"cern.c2mon.server.common.alarm.RangeAlarmCondition\">\n" +
        "<min-value type=\"" + Integer.class.getName() + "\">0</min-value>\n" +
        "<max-value type=\"" + Integer.class.getName() + "\">10</max-value>\n" +
        "</AlarmCondition>";
    props.setProperty("alarmCondition", condition);
    props.setProperty("dataTagId", Long.toString(parentId));
    props.setProperty("metadata", Metadata.toJSON(Metadata.builder().addMetadata("testMetadata",11).build()));

    return new Pair<>(pro, props);
  }

  public static Pair<Alarm.AlarmBuilder, Properties> builderAlarmUpdate(Long id, Long parentId) {
    Alarm.AlarmBuilder pro = Alarm.builder()
        .id(id)
        .faultFamily("faultFam_Test_Update")
        .metadata(Metadata.builder().addMetadata("testMetadata_update",true).build());

    Properties props = new Properties();
    props.setProperty("faultFamily", "faultFam_Test_Update");
    props.setProperty("metadata", Metadata.toJSON(Metadata.builder().addMetadata("testMetadata_update",true).build()));

    return new Pair<>(pro, props);
  }
}
