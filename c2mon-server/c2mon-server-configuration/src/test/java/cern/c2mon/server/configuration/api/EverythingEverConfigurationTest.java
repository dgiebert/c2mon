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
package cern.c2mon.server.configuration.api;

import cern.c2mon.cache.actions.datatag.DataTagService;
import cern.c2mon.cache.actions.equipment.EquipmentService;
import cern.c2mon.cache.actions.process.ProcessService;
import cern.c2mon.cache.actions.subequipment.SubEquipmentService;
import cern.c2mon.cache.api.C2monCache;
import cern.c2mon.server.cache.dbaccess.*;
import cern.c2mon.server.common.alive.AliveTag;
import cern.c2mon.server.common.datatag.DataTagCacheObject;
import cern.c2mon.server.common.equipment.EquipmentCacheObject;
import cern.c2mon.server.common.metadata.Metadata;
import cern.c2mon.server.configuration.ConfigurationCacheTest;
import cern.c2mon.server.configuration.ConfigurationLoader;
import cern.c2mon.server.configuration.helper.ObjectEqualityComparison;
import cern.c2mon.server.configuration.junit.ConfigLoaderRuleChain;
import cern.c2mon.server.configuration.parser.util.ConfigurationDataTagUtil;
import cern.c2mon.server.configuration.parser.util.ConfigurationEquipmentUtil;
import cern.c2mon.server.configuration.util.CacheObjectFactory;
import cern.c2mon.server.configuration.util.TestConfigurationProvider;
import cern.c2mon.server.daq.out.ProcessCommunicationManager;
import cern.c2mon.shared.client.configuration.ConfigConstants;
import cern.c2mon.shared.client.configuration.ConfigurationReport;
import cern.c2mon.shared.client.configuration.api.Configuration;
import cern.c2mon.shared.client.configuration.api.equipment.Equipment;
import cern.c2mon.shared.client.configuration.api.tag.CommFaultTag;
import cern.c2mon.shared.client.configuration.api.tag.DataTag;
import cern.c2mon.shared.client.configuration.api.tag.StatusTag;
import cern.c2mon.shared.client.tag.TagMode;
import cern.c2mon.shared.common.NoSimpleValueParseException;
import cern.c2mon.shared.common.datatag.DataTagAddress;
import cern.c2mon.shared.daq.config.Change;
import cern.c2mon.shared.daq.config.ChangeReport;
import cern.c2mon.shared.daq.config.ConfigurationChangeEventReport;
import lombok.extern.slf4j.Slf4j;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.inject.Inject;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Properties;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

/**
 * @author Franz Ritter
 */
@SuppressWarnings("ALL")
@Slf4j
public class EverythingEverConfigurationTest extends ConfigurationCacheTest {

  @Rule
  @Inject
  public ConfigLoaderRuleChain configRuleChain; // This one also starts the process

  @Autowired
  private CacheObjectFactory cacheObjectFactory;

  @Autowired
  private ProcessCommunicationManager communicationManager;

  @Autowired
  private ConfigurationLoader configurationLoader;

  @Autowired
  private C2monCache<cern.c2mon.server.common.datatag.DataTag> dataTagCache;

  @Autowired
  private DataTagMapper dataTagMapper;

  @Autowired
  private CommandTagMapper commandTagMapper;

  @Autowired
  private C2monCache<cern.c2mon.shared.common.command.CommandTag> commandTagCache;

  @Autowired
  private C2monCache<cern.c2mon.server.common.rule.RuleTag> ruleTagCache;

  @Autowired
  private RuleTagMapper ruleTagMapper;

  @Autowired
  private C2monCache<cern.c2mon.server.common.equipment.Equipment> equipmentCache;

  @Autowired
  private EquipmentMapper equipmentMapper;

  @Autowired
  private EquipmentService equipmentService;

  @Autowired
  private C2monCache<cern.c2mon.server.common.subequipment.SubEquipment> subEquipmentCache;

  @Autowired
  private SubEquipmentMapper subEquipmentMapper;

  @Autowired
  private SubEquipmentService subEquipmentService;

  @Autowired
  private C2monCache<cern.c2mon.server.common.process.Process> processCache;

  @Autowired
  private ProcessMapper processMapper;

  @Autowired
  private C2monCache<AliveTag> aliveTimerCache;

  @Autowired
  private C2monCache<cern.c2mon.server.common.commfault.CommFaultTag> commFaultTagCache;

  @Autowired
  private C2monCache<cern.c2mon.server.common.alarm.Alarm> alarmCache;

  @Autowired
  private AlarmMapper alarmMapper;

  @Autowired
  private ProcessService processService;

  @Autowired
  private DataTagService dataTagService;

  @Before
  public void beforeTest() throws IOException {
    // reset mock
    reset(communicationManager);
  }

  @Test
  public void updateNonExistingEntity() throws IllegalAccessException, TransformerException, InstantiationException, NoSimpleValueParseException, ParserConfigurationException, NoSuchFieldException {
    expect(communicationManager.sendConfiguration(eq(5L), isA(List.class))).andReturn(new ConfigurationChangeEventReport());
    replay(communicationManager);
    Configuration createProcess = TestConfigurationProvider.createProcess();
    configurationLoader.applyConfiguration(createProcess);
    Configuration newEquipmentConfig = TestConfigurationProvider.createEquipment();
    configurationLoader.applyConfiguration(newEquipmentConfig);
    processService.start(5L, "hostname", new Timestamp(System.currentTimeMillis()));

    DataTag dataTag = DataTag.create("DataTag", Integer.class, new DataTagAddress())
            .id(1000L)
            .description("foo")
            .mode(TagMode.OPERATIONAL)
            .isLogged(false)
            .minValue(0)
            .maxValue(10)
            .unit("testUnit")
            .addMetadata("testMetadata1", 11)
            .addMetadata("testMetadata2", 22)
            .addMetadata("testMetadata3", 33)
            .build();
    dataTag.setEquipmentId(15L);

    Configuration configuration = new Configuration();
    configuration.addEntity(dataTag);
    //apply the configuration to the server
    configurationLoader.applyConfiguration(configuration);

    //now add some tags
    DataTag updatedDataTag = DataTag.update(1000L)
        .removeMetadata("testMetadata2")
        .removeMetadata("testMetadata3")
        .build();
    configuration = new Configuration();
    configuration.addEntity(updatedDataTag);

    //1010L does not exist
    DataTag updatedDataTag2 = DataTag.update(1010L).build();
    configuration.addEntity(updatedDataTag2);
    //apply the configuration to the server
    //should not throw an exception for 1010L
    ConfigurationReport report = configurationLoader.applyConfiguration(configuration);
    assertEquals(2, report.getElementReports().size());
    assertEquals(1010L, (long)report.getElementReports().get(0).getId());
    assertEquals(1000L, (long)report.getElementReports().get(1).getId());
    //the overall report status is WARNING
    assertEquals(ConfigConstants.Status.WARNING, report.getStatus());
    //and the element report status for 1010L is WARNING
    assertEquals(ConfigConstants.Status.WARNING, report.getElementReports().get(0).getStatus());
    assertEquals(ConfigConstants.Status.OK, report.getElementReports().get(1).getStatus());
  }

  @Test
  public void updateRemoveMetadataDataTag() throws ParserConfigurationException, IllegalAccessException, NoSimpleValueParseException, TransformerException, NoSuchFieldException, InstantiationException {
// called once when updating the equipment;
    // mock returns a list with the correct number of SUCCESS ChangeReports
    expect(communicationManager.sendConfiguration(eq(5L), isA(List.class))).andReturn(new ConfigurationChangeEventReport());
    replay(communicationManager);

    // SETUP:
    Configuration createProcess = TestConfigurationProvider.createProcess();
    configurationLoader.applyConfiguration(createProcess);
    Configuration newEquipmentConfig = TestConfigurationProvider.createEquipment();
    configurationLoader.applyConfiguration(newEquipmentConfig);
    processService.start(5L, "hostname", new Timestamp(System.currentTimeMillis()));

    DataTag dataTag = DataTag.create("DataTag", Integer.class, new DataTagAddress())
        .id(1000L)
        .description("foo")
        .mode(TagMode.OPERATIONAL)
        .isLogged(false)
        .minValue(0)
        .maxValue(10)
        .unit("testUnit")
        .addMetadata("testMetadata1", 11)
        .addMetadata("testMetadata2", 22)
        .addMetadata("testMetadata3", 33)
        .build();
    dataTag.setEquipmentId(15L);

    Configuration configuration = new Configuration();
    configuration.addEntity(dataTag);

    //apply the configuration to the server
    configurationLoader.applyConfiguration(configuration);

    DataTag updatedDataTag = DataTag.update(1000L)
        .removeMetadata("testMetadata2")
        .removeMetadata("testMetadata3")
        .build();

    configuration = new Configuration();
    configuration.addEntity(updatedDataTag);

    //apply the configuration to the server
    ConfigurationReport report = configurationLoader.applyConfiguration(configuration);

    // check report result
    assertEquals(ConfigConstants.Status.OK, report.getStatus());
    assertTrue(report.getProcessesToReboot().isEmpty());
    assertTrue(report.getElementReports().size() == 1);
    assertTrue(report.getElementReports().get(0).getAction().equals(ConfigConstants.Action.UPDATE));
    assertTrue(report.getElementReports().get(0).getEntity().equals(ConfigConstants.Entity.DATATAG));

    DataTagCacheObject expectedCacheObjectData = new DataTagCacheObject(0L);
    Metadata metadata = new Metadata();
    metadata.addMetadata("testMetadata1", 11);
    expectedCacheObjectData.setMetadata(metadata);

    // get cacheObject from the cache and compare to the an expected cacheObject
    DataTagCacheObject cacheObjectData = (DataTagCacheObject) dataTagCache.get(1000L);
    assertEquals(expectedCacheObjectData.getMetadata(), cacheObjectData.getMetadata());

    verify(communicationManager);
  }

  @Test
  public void createMetadataDatatag() throws ParserConfigurationException, IllegalAccessException, NoSimpleValueParseException, TransformerException, NoSuchFieldException, InstantiationException {
    // called once when updating the equipment;
    // mock returns a list with the correct number of SUCCESS ChangeReports
    expect(communicationManager.sendConfiguration(eq(5L), isA(List.class))).andReturn(new ConfigurationChangeEventReport());
    replay(communicationManager);

    // SETUP:
    Configuration createProcess = TestConfigurationProvider.createProcess();
    configurationLoader.applyConfiguration(createProcess);
    Configuration newEquipmentConfig = TestConfigurationProvider.createEquipment();
    configurationLoader.applyConfiguration(newEquipmentConfig);
    processService.start(5L, "hostname", new Timestamp(System.currentTimeMillis()));

    DataTag dataTag = DataTag.create("DataTag", Integer.class, new DataTagAddress())
        .id(1000L)
        .description("foo")
        .mode(TagMode.OPERATIONAL)
        .isLogged(false)
        .minValue(0)
        .maxValue(10)
        .unit("testUnit")
        .addMetadata("testMetadata", 11)
        .build();
    dataTag.setEquipmentId(15L);

    Configuration configuration = new Configuration();
    configuration.addEntity(dataTag);

    //apply the configuration to the server
    ConfigurationReport report = configurationLoader.applyConfiguration(configuration);

    // check report result
    assertEquals(ConfigConstants.Status.OK, report.getStatus());
    assertTrue(report.getProcessesToReboot().isEmpty());
    assertTrue(report.getElementReports().size() == 1);
    assertTrue(report.getElementReports().get(0).getAction().equals(ConfigConstants.Action.CREATE));
    assertTrue(report.getElementReports().get(0).getEntity().equals(ConfigConstants.Entity.DATATAG));

    // get cacheObject from the cache and compare to the an expected cacheObject
    DataTagCacheObject cacheObjectData = (DataTagCacheObject) dataTagCache.get(1000L);
    DataTagCacheObject expectedCacheObjectData = new DataTagCacheObject(0L);
    Metadata metadata = new Metadata();
    metadata.addMetadata("testMetadata", 11);
    expectedCacheObjectData.setMetadata(metadata);
    assertEquals(expectedCacheObjectData.getMetadata(), cacheObjectData.getMetadata());

    verify(communicationManager);
  }

  @Test
  public void createEquipment() throws IllegalAccessException, TransformerException, InstantiationException, NoSimpleValueParseException, ParserConfigurationException, NoSuchFieldException {
    // called once when updating the equipment;
    // mock returns a list with the correct number of SUCCESS ChangeReports
    expect(communicationManager.sendConfiguration(eq(5L), isA(List.class))).andAnswer(() -> {
      List<Change> changeList = (List<Change>) EasyMock.getCurrentArguments()[1];
      ConfigurationChangeEventReport report = new ConfigurationChangeEventReport();
      for (Change change : changeList) {
        ChangeReport changeReport = new ChangeReport(change);
        changeReport.setState(ChangeReport.CHANGE_STATE.SUCCESS);
        report.appendChangeReport(changeReport);
      }
      return report;
    });

    replay(communicationManager);

    // SETUP:First add a process to the server
    Configuration createProcess = TestConfigurationProvider.createProcess();
    configurationLoader.applyConfiguration(createProcess);
    processService.start(5L, "hostname", new Timestamp(System.currentTimeMillis()));

    // TEST:Build configuration to add the test equipment
    Properties expectedProperties = new Properties();
    Equipment equipment = ConfigurationEquipmentUtil.buildCreateAllFieldsEquipment(10L, expectedProperties);
    equipment.setProcessId(5L);
    expectedProperties.setProperty("stateTagId", "300000");
    expectedProperties.setProperty("commFaultTagId", "300001");
    expectedProperties.setProperty("aliveTagId", "300002");

    Configuration configuration = new Configuration();
    configuration.addEntity(equipment);

    //apply the configuration to the server
    ConfigurationReport report = configurationLoader.applyConfiguration(configuration);

    // check Report result
    assertFalse(report.toXML().contains(ConfigConstants.Status.FAILURE.toString()));
    assertEquals(ConfigConstants.Status.OK, report.getStatus());
    assertTrue(report.getElementReports().size() == 4);

    // check Equipment in the cache
    EquipmentCacheObject cacheObject = (EquipmentCacheObject) equipmentCache.get(10L);
    EquipmentCacheObject expectedObject = cacheObjectFactory.buildEquipmentCacheObject(10L, equipment);

    ObjectEqualityComparison.assertEquipmentEquals(expectedObject, cacheObject);

    // Check if all caches are updated
    cern.c2mon.server.common.process.Process processObj = processCache.get(expectedObject.getProcessId());
    assertTrue(processObj.getEquipmentIds().contains(expectedObject.getId()));
    assertNotNull(commFaultTagCache.get(expectedObject.getCommFaultTagId()));
    assertEquals(expectedObject.getId(), (long) commFaultTagCache.get(cacheObject.getCommFaultTagId()).getSupervisedId());
    assertNotNull(equipmentMapper.getItem(10L));

    verify(communicationManager);

    // remove the process and equipments from the server
    processService.stop(5L, System.currentTimeMillis());
    Configuration remove = TestConfigurationProvider.deleteProcess();
    report = configurationLoader.applyConfiguration(remove);
    assertFalse(report.toXML().contains(ConfigConstants.Status.FAILURE.toString()));
    assertTrue(report.getStatus() == ConfigConstants.Status.OK);

    assertFalse(processCache.containsKey(5L));
    assertNull(processMapper.getItem(5L));
    assertFalse(aliveTimerCache.containsKey(101L));

    // equipment stuff
    assertFalse(equipmentCache.containsKey(10L));
    assertNull(equipmentMapper.getItem(10L));
    assertFalse(commFaultTagCache.containsKey(300_001L));
    assertFalse(aliveTimerCache.containsKey(300_002L));

    verify(communicationManager);
  }

  @Test
  public void updateEquipment() throws IllegalAccessException, TransformerException, InstantiationException, NoSimpleValueParseException, ParserConfigurationException, NoSuchFieldException {
    // called once when updating the equipment;
    // mock returns a list with the correct number of SUCCESS ChangeReports
    expect(communicationManager.sendConfiguration(eq(5L), isA(List.class))).andAnswer(new IAnswer<ConfigurationChangeEventReport>() {

      @Override
      public ConfigurationChangeEventReport answer() throws Throwable {
        List<Change> changeList = (List<Change>) EasyMock.getCurrentArguments()[1];
        ConfigurationChangeEventReport report = new ConfigurationChangeEventReport();
        for (Change change : changeList) {
          ChangeReport changeReport = new ChangeReport(change);
          changeReport.setState(ChangeReport.CHANGE_STATE.SUCCESS);
          report.appendChangeReport(changeReport);
        }
        return report;
      }
    });

    replay(communicationManager);

    // SETUP:
    Configuration createProcess = TestConfigurationProvider.createProcess();
    configurationLoader.applyConfiguration(createProcess);
    Configuration createEquipment = TestConfigurationProvider.createEquipment();
    configurationLoader.applyConfiguration(createEquipment);
    processService.start(5L, "hostname", new Timestamp(System.currentTimeMillis()));

    // TEST:
    // Build configuration to add the test equipment
    Equipment equipment = ConfigurationEquipmentUtil.buildUpdateEquipmentWithAllFields(15L, null);
    Configuration configuration = new Configuration();
    configuration.addEntity(equipment);

    //apply the configuration to the server
    ConfigurationReport report = configurationLoader.applyConfiguration(configuration);

    // check report result
    assertFalse(report.toXML().contains(ConfigConstants.Status.FAILURE.toString()));
    assertEquals(ConfigConstants.Status.OK, report.getStatus());
    assertTrue(report.getProcessesToReboot().isEmpty());
    assertTrue(report.getElementReports().size() == 1);

    // get cacheObject from the cache and compare to the an expected cacheObject
    EquipmentCacheObject cacheObjectEquipment = (EquipmentCacheObject) equipmentCache.get(15L);
    EquipmentCacheObject expectedCacheObjectEquipment = cacheObjectFactory.buildEquipmentUpdateCacheObject(cacheObjectEquipment, equipment);

    ObjectEqualityComparison.assertEquipmentEquals(expectedCacheObjectEquipment, cacheObjectEquipment);

    verify(communicationManager);

    // remove the process and equipments from the server
    processService.stop(5L, System.currentTimeMillis());
    Configuration remove = TestConfigurationProvider.deleteProcess();
    report = configurationLoader.applyConfiguration(remove);
    assertFalse(report.toXML().contains(ConfigConstants.Status.FAILURE.toString()));
    assertTrue(report.getStatus() == ConfigConstants.Status.OK);

    assertFalse(processCache.containsKey(5L));
    assertNull(processMapper.getItem(5L));
    assertFalse(aliveTimerCache.containsKey(101L));

    // equipment stuff
    assertFalse(equipmentCache.containsKey(15L));
    assertNull(equipmentMapper.getItem(15L));
    assertFalse(commFaultTagCache.containsKey(201L));
    assertFalse(aliveTimerCache.containsKey(202L));

    verify(communicationManager);
  }

  @Test
  public void updateAliveTag() throws IllegalAccessException, TransformerException, InstantiationException, NoSimpleValueParseException, ParserConfigurationException, NoSuchFieldException {
    replay(communicationManager);

    // SETUP:
    Configuration createProcess = TestConfigurationProvider.createProcess();
    configurationLoader.applyConfiguration(createProcess);
    processService.start(5L, "hostname", new Timestamp(System.currentTimeMillis()));

    // TEST:
    cern.c2mon.shared.client.configuration.api.tag.AliveTag aliveTagUpdate = cern.c2mon.shared.client.configuration.api.tag.AliveTag.update(101L).description("new description").mode(TagMode.OPERATIONAL).build();
    Configuration configuration = new Configuration();
    configuration.addEntity(aliveTagUpdate);

    ///apply the configuration to the server
    ConfigurationReport report = configurationLoader.applyConfiguration(configuration);

    // check report result
    assertFalse(report.toXML().contains(ConfigConstants.Status.FAILURE.toString()));
    assertEquals(ConfigConstants.Status.OK, report.getStatus());
    assertTrue(report.getProcessesToReboot().isEmpty());
    assertTrue(report.getElementReports().size() == 1);

    // check aliveTag in the cache
    AliveTag cacheObjectAlive = (AliveTag) aliveTimerCache.get(101L);
    AliveTag expectedObjectAlive = new AliveTag(101L, 5L, "P_INI_TEST", "PROC", null, 100L, 60000);
    ObjectEqualityComparison.assertAliveTimerValuesEquals(expectedObjectAlive, cacheObjectAlive);

//    ControlTagCacheObject cacheObjectAliveControlCache = (ControlTagCacheObject) controlTagCache.get(101L);
//    Assert.assertEquals(cacheObjectAliveControlCache.getDescription(), "new description");

    verify(communicationManager);

    // remove the process and equipments from the server
    processService.stop(5L, System.currentTimeMillis());
    Configuration remove = TestConfigurationProvider.deleteProcess();
    report = configurationLoader.applyConfiguration(remove);
    assertFalse(report.toXML().contains(ConfigConstants.Status.FAILURE.toString()));
    assertTrue(report.getStatus() == ConfigConstants.Status.OK);

    assertFalse(processCache.containsKey(5L));
    assertNull(processMapper.getItem(5L));
    assertFalse(aliveTimerCache.containsKey(101L));

    // equipment stuff
    assertFalse(equipmentCache.containsKey(15L));
    assertNull(equipmentMapper.getItem(15L));
    assertFalse(commFaultTagCache.containsKey(201L));
    assertFalse(aliveTimerCache.containsKey(202L));

    verify(communicationManager);
  }

  @Test
  public void updateStatusTag() throws IllegalAccessException, TransformerException, InstantiationException, NoSimpleValueParseException, ParserConfigurationException, NoSuchFieldException {
    replay(communicationManager);

    // SETUP:
    Configuration createProcess = TestConfigurationProvider.createProcess();
    configurationLoader.applyConfiguration(createProcess);
    processService.start(5L, "hostname", new Timestamp(System.currentTimeMillis()));

    // TEST:
    StatusTag statusTagUpdate = StatusTag.update(100L).description("new description").mode(TagMode.OPERATIONAL).build();
    Configuration configuration = new Configuration();
    configuration.addEntity(statusTagUpdate);

    ///apply the configuration to the server
    ConfigurationReport report = configurationLoader.applyConfiguration(configuration);

    // check report result
    assertFalse(report.toXML().contains(ConfigConstants.Status.FAILURE.toString()));
    assertEquals(ConfigConstants.Status.OK, report.getStatus());
    assertTrue(report.getProcessesToReboot().isEmpty());
    assertTrue(report.getElementReports().size() == 1);

//    ControlTagCacheObject cacheObjectStatusControlCache = (ControlTagCacheObject) controlTagCache.get(100L);
//    assertEquals(cacheObjectStatusControlCache.getDescription(), "new description");
//    assertEquals(cacheObjectStatusControlCache.getMode(), TagMode.OPERATIONAL.ordinal());

    verify(communicationManager);

    // remove the process and equipments from the server
    processService.stop(5L, System.currentTimeMillis());
    Configuration remove = TestConfigurationProvider.deleteProcess();
    report = configurationLoader.applyConfiguration(remove);
    assertFalse(report.toXML().contains(ConfigConstants.Status.FAILURE.toString()));
    assertTrue(report.getStatus() == ConfigConstants.Status.OK);

    assertFalse(processCache.containsKey(5L));
    assertNull(processMapper.getItem(5L));
    assertFalse(aliveTimerCache.containsKey(101L));

    verify(communicationManager);
  }

  @Test
  public void updateCommFaultTag() throws IllegalAccessException, TransformerException, InstantiationException, NoSimpleValueParseException, ParserConfigurationException, NoSuchFieldException {
    replay(communicationManager);

    // SETUP:
    Configuration createProcess = TestConfigurationProvider.createProcess();
    configurationLoader.applyConfiguration(createProcess);
    Configuration createEquipment = TestConfigurationProvider.createEquipment();
    configurationLoader.applyConfiguration(createEquipment);
    processService.start(5L, "hostname", new Timestamp(System.currentTimeMillis()));

    // TEST:
    // Build configuration to update the test equipment
    CommFaultTag commFaultTagUpdate = CommFaultTag.update(201L).description("new description").mode(TagMode.OPERATIONAL).build();
    Configuration configuration = new Configuration();
    configuration.addEntity(commFaultTagUpdate);

    ///apply the configuration to the server
    ConfigurationReport report = configurationLoader.applyConfiguration(configuration);

    // check report result
    assertFalse(report.toXML().contains(ConfigConstants.Status.FAILURE.toString()));
    assertEquals(ConfigConstants.Status.OK, report.getStatus());
    assertTrue(report.getProcessesToReboot().isEmpty());
    assertTrue(report.getElementReports().size() == 1);

    // check aliveTag in the cache
//    ControlTagCacheObject cacheObjectStatusControlCache = (ControlTagCacheObject) controlTagCache.get(201L);
//    assertEquals(cacheObjectStatusControlCache.getDescription(), "new description");
//    assertEquals(cacheObjectStatusControlCache.getMode(), TagMode.OPERATIONAL.ordinal());

    verify(communicationManager);

    // remove the process and equipments from the server
    processService.stop(5L, System.currentTimeMillis());
    Configuration remove = TestConfigurationProvider.deleteProcess();
    report = configurationLoader.applyConfiguration(remove);
    assertFalse(report.toXML().contains(ConfigConstants.Status.FAILURE.toString()));
    assertTrue(report.getStatus() == ConfigConstants.Status.OK);

    assertFalse(processCache.containsKey(5L));
    assertNull(processMapper.getItem(5L));
    assertFalse(aliveTimerCache.containsKey(101L));

    verify(communicationManager);
  }

  @Test
  public void createEquipmentDataTag() throws IllegalAccessException, TransformerException, InstantiationException, NoSimpleValueParseException, ParserConfigurationException, NoSuchFieldException {
    // called once when updating the equipment;
    // mock returns a list with the correct number of SUCCESS ChangeReports
    expect(communicationManager.sendConfiguration(eq(5L), isA(List.class))).andReturn(new ConfigurationChangeEventReport());
    replay(communicationManager);

    // SETUP:
    Configuration createProcess = TestConfigurationProvider.createProcess();
    configurationLoader.applyConfiguration(createProcess);
    Configuration createEquipment = TestConfigurationProvider.createEquipment();
    configurationLoader.applyConfiguration(createEquipment);
    processService.start(5L, "hostname", new Timestamp(System.currentTimeMillis()));

    // TEST:
    // Build configuration to add the test DataTag
    DataTag dataTag = ConfigurationDataTagUtil.buildCreateAllFieldsDataTag(1000L, null);
    dataTag.setEquipmentId(15L);

    Configuration configuration = new Configuration();
    configuration.addEntity(dataTag);

    //apply the configuration to the server
    ConfigurationReport report = configurationLoader.applyConfiguration(configuration);

    // check report result
    assertFalse(report.toXML().contains(ConfigConstants.Status.FAILURE.toString()));
    assertEquals(ConfigConstants.Status.OK, report.getStatus());
    assertTrue(report.getProcessesToReboot().isEmpty());
    assertTrue(report.getElementReports().size() == 1);

    // get cacheObject from the cache and compare to the an expected cacheObject
    DataTagCacheObject cacheObjectData = (DataTagCacheObject) dataTagCache.get(1000L);
    DataTagCacheObject expectedCacheObjectData = cacheObjectFactory.buildDataTagCacheObject(1000L, dataTag);

    ObjectEqualityComparison.assertDataTagConfigEquals(expectedCacheObjectData, cacheObjectData);
    // Check if all caches are updated
    assertTrue(dataTagService.getDataTagIdsByEquipmentId(cacheObjectData.getEquipmentId()).contains(1000L));

    verify(communicationManager);

    // remove the process and equipments from the server
    processService.stop(5L, System.currentTimeMillis());
    Configuration remove = TestConfigurationProvider.deleteProcess();
    report = configurationLoader.applyConfiguration(remove);
    assertFalse(report.toXML().contains(ConfigConstants.Status.FAILURE.toString()));
    assertTrue(report.getStatus() == ConfigConstants.Status.OK);

    assertFalse(processCache.containsKey(5L));
    assertNull(processMapper.getItem(5L));
    assertFalse(aliveTimerCache.containsKey(101L));

    // equipment stuff
    assertFalse(equipmentCache.containsKey(15L));
    assertNull(equipmentMapper.getItem(15L));
    assertFalse(commFaultTagCache.containsKey(201L));
    assertFalse(aliveTimerCache.containsKey(202L));
    assertFalse(dataTagCache.containsKey(1000L));
    assertNull(dataTagMapper.getItem(1000L));

    verify(communicationManager);
  }

  @Test
  public void updateEquipmentDataTag() throws IllegalAccessException, TransformerException, InstantiationException, NoSimpleValueParseException, ParserConfigurationException, NoSuchFieldException {
    // called once when updating the equipment;
    // mock returns a list with the correct number of SUCCESS ChangeReports
    expect(communicationManager.sendConfiguration(eq(5L), isA(List.class))).andReturn(new ConfigurationChangeEventReport());
    replay(communicationManager);

    // SETUP:
    Configuration createProcess = TestConfigurationProvider.createProcess();
    configurationLoader.applyConfiguration(createProcess);
    Configuration createEquipment = TestConfigurationProvider.createEquipment();
    configurationLoader.applyConfiguration(createEquipment);
    Configuration createDataTag = TestConfigurationProvider.createEquipmentDataTag(15L);
    configurationLoader.applyConfiguration(createDataTag);
    processService.start(5L, "hostname", new Timestamp(System.currentTimeMillis()));

    // TEST:
    // Build configuration to update the test DataTag
    DataTag dataTagUpdate = DataTag.update(1000L)
        .description("new description")
        .mode(TagMode.OPERATIONAL)
        .minValue(99)
        .unit("updateUnit").build();
    Configuration configuration = new Configuration();
    configuration.addEntity(dataTagUpdate);

    ///apply the configuration to the server
    ConfigurationReport report = configurationLoader.applyConfiguration(configuration);

    // check report result
    assertFalse(report.toXML().contains(ConfigConstants.Status.FAILURE.toString()));
    assertEquals(ConfigConstants.Status.OK, report.getStatus());
    assertTrue(report.getProcessesToReboot().isEmpty());
    assertTrue(report.getElementReports().size() == 1);

    // get cacheObject from the cache and compare to the an expected cacheObject
    DataTagCacheObject cacheObjectData = (DataTagCacheObject) dataTagCache.get(1000L);
    DataTagCacheObject expectedCacheObjectData = cacheObjectFactory.buildDataTagUpdateCacheObject(cacheObjectData, dataTagUpdate);

    ObjectEqualityComparison.assertDataTagConfigEquals(expectedCacheObjectData, cacheObjectData);

    verify(communicationManager);

    // remove the process and equipments from the server
    processService.stop(5L, System.currentTimeMillis());
    Configuration remove = TestConfigurationProvider.deleteProcess();
    report = configurationLoader.applyConfiguration(remove);
    assertFalse(report.toXML().contains(ConfigConstants.Status.FAILURE.toString()));
    assertTrue(report.getStatus() == ConfigConstants.Status.OK);

    assertFalse(processCache.containsKey(5L));
    assertNull(processMapper.getItem(5L));
    assertFalse(aliveTimerCache.containsKey(101L));

    // equipment stuff
    assertFalse(equipmentCache.containsKey(15L));
    assertNull(equipmentMapper.getItem(15L));
    assertFalse(commFaultTagCache.containsKey(201L));
    assertFalse(aliveTimerCache.containsKey(202L));
    assertFalse(dataTagCache.containsKey(1000L));
    assertNull(dataTagMapper.getItem(1000L));

    verify(communicationManager);
  }

  @Test
  public void createSubEquipmentDataTag() throws IllegalAccessException, TransformerException, InstantiationException, NoSimpleValueParseException, ParserConfigurationException, NoSuchFieldException {
    // called once when updating the equipment;
    // mock returns a list with the correct number of SUCCESS ChangeReports
    expect(communicationManager.sendConfiguration(eq(5L), isA(List.class))).andReturn(new ConfigurationChangeEventReport());
    replay(communicationManager);

    /// SETUP:
    Configuration createProcess = TestConfigurationProvider.createProcess();
    configurationLoader.applyConfiguration(createProcess);
    Configuration createEquipment = TestConfigurationProvider.createEquipment();
    configurationLoader.applyConfiguration(createEquipment);
    Configuration createSubEquipment = TestConfigurationProvider.createSubEquipment();
    configurationLoader.applyConfiguration(createSubEquipment);
    processService.start(5L, "hostname", new Timestamp(System.currentTimeMillis()));

    // TEST:
    // Build configuration to add the test DataTag
    DataTag dataTag = ConfigurationDataTagUtil.buildCreateAllFieldsDataTag(1000L, null);
    dataTag.setSubEquipmentId(25L);
    dataTag.setEquipmentId(null);

    Configuration configuration = new Configuration();
    configuration.addEntity(dataTag);

    //apply the configuration to the server
    ConfigurationReport report = configurationLoader.applyConfiguration(configuration);

    // check report result
    assertFalse(report.toXML().contains(ConfigConstants.Status.FAILURE.toString()));
    assertEquals(ConfigConstants.Status.OK, report.getStatus());
    assertTrue(report.getProcessesToReboot().isEmpty());
    assertTrue(report.getElementReports().size() == 1);

    // get cacheObject from the cache and compare to the an expected cacheObject
    DataTagCacheObject cacheObjectData = (DataTagCacheObject) dataTagCache.get(1000L);
    DataTagCacheObject expectedCacheObjectData = cacheObjectFactory.buildDataTagCacheObject(1000L, dataTag);
    expectedCacheObjectData.setSubEquipmentId(dataTag.getSubEquipmentId());
    expectedCacheObjectData.setEquipmentId(null);

    ObjectEqualityComparison.assertDataTagConfigEquals(expectedCacheObjectData, cacheObjectData);
    // Check if all caches are updated
    assertTrue(dataTagService.getDataTagIdsBySubEquipmentId(cacheObjectData.getSubEquipmentId()).contains(1000L));

    verify(communicationManager);

    // remove the process and equipments from the server
    processService.stop(5L, System.currentTimeMillis());
    Configuration remove = TestConfigurationProvider.deleteProcess();
    report = configurationLoader.applyConfiguration(remove);
    assertFalse(report.toXML().contains(ConfigConstants.Status.FAILURE.toString()));
    assertTrue(report.getStatus() == ConfigConstants.Status.OK);

    assertFalse(processCache.containsKey(5L));
    assertNull(processMapper.getItem(5L));
    assertFalse(aliveTimerCache.containsKey(101L));

    // equipment stuff
    assertFalse(equipmentCache.containsKey(15L));
    assertNull(equipmentMapper.getItem(15L));
    assertFalse(commFaultTagCache.containsKey(201L));
    assertFalse(aliveTimerCache.containsKey(201L));

    assertFalse(equipmentCache.containsKey(25L));
    assertNull(subEquipmentMapper.getItem(25L));
    assertFalse(commFaultTagCache.containsKey(301L));
    assertFalse(aliveTimerCache.containsKey(302L));
    assertFalse(dataTagCache.containsKey(1000L));
    assertNull(dataTagMapper.getItem(1000L));

    verify(communicationManager);
  }

  @Test
  public void updateSubEquipmentDataTag() throws IllegalAccessException, TransformerException, InstantiationException, NoSimpleValueParseException, ParserConfigurationException, NoSuchFieldException {
    // called once when updating the equipment;
    // mock returns a list with the correct number of SUCCESS ChangeReports
    expect(communicationManager.sendConfiguration(eq(5L), isA(List.class))).andReturn(new ConfigurationChangeEventReport());
    replay(communicationManager);

    /// SETUP:
    Configuration createProcess = TestConfigurationProvider.createProcess();
    configurationLoader.applyConfiguration(createProcess);
    Configuration createEquipment = TestConfigurationProvider.createEquipment();
    configurationLoader.applyConfiguration(createEquipment);
    Configuration createSubEquipment = TestConfigurationProvider.createSubEquipment();
    configurationLoader.applyConfiguration(createSubEquipment);
    Configuration createDataTag = TestConfigurationProvider.createSubEquipmentDataTag(25L);
    configurationLoader.applyConfiguration(createDataTag);
    processService.start(5L, "hostname", new Timestamp(System.currentTimeMillis()));

    // TEST:
    // Build configuration to update the test DataTag
    DataTag dataTagUpdate = DataTag.update(1000L)
        .description("new description")
        .mode(TagMode.OPERATIONAL)
        .minValue(99)
        .unit("updateUnit").build();
    Configuration configuration = new Configuration();
    configuration.addEntity(dataTagUpdate);

    ///apply the configuration to the server
    ConfigurationReport report = configurationLoader.applyConfiguration(configuration);

    // check report result
    assertFalse(report.toXML().contains(ConfigConstants.Status.FAILURE.toString()));
    assertEquals(ConfigConstants.Status.OK, report.getStatus());
    assertTrue(report.getProcessesToReboot().isEmpty());
    assertTrue(report.getElementReports().size() == 1);

    // get cacheObject from the cache and compare to the an expected cacheObject
    DataTagCacheObject cacheObjectData = (DataTagCacheObject) dataTagCache.get(1000L);
    DataTagCacheObject expectedCacheObjectData = cacheObjectFactory.buildDataTagUpdateCacheObject(cacheObjectData, dataTagUpdate);

    ObjectEqualityComparison.assertDataTagConfigEquals(expectedCacheObjectData, cacheObjectData);

    verify(communicationManager);

    // remove the process and equipments and dataTag from the server
    processService.stop(5L, System.currentTimeMillis());
    Configuration remove = TestConfigurationProvider.deleteProcess();
    report = configurationLoader.applyConfiguration(remove);
    assertFalse(report.toXML().contains(ConfigConstants.Status.FAILURE.toString()));
    assertTrue(report.getStatus() == ConfigConstants.Status.OK);

    assertFalse(processCache.containsKey(5L));
    assertNull(processMapper.getItem(5L));
    assertFalse(aliveTimerCache.containsKey(101L));

    // equipment stuff
    assertFalse(equipmentCache.containsKey(15L));
    assertNull(equipmentMapper.getItem(15L));
    assertFalse(commFaultTagCache.containsKey(201L));
    assertFalse(aliveTimerCache.containsKey(202L));

    assertFalse(equipmentCache.containsKey(25L));
    assertNull(subEquipmentMapper.getItem(25L));
    assertFalse(commFaultTagCache.containsKey(301L));
    assertFalse(aliveTimerCache.containsKey(302L));
    assertFalse(dataTagCache.containsKey(1000L));
    assertNull(dataTagMapper.getItem(1000L));

    verify(communicationManager);
  }
}
