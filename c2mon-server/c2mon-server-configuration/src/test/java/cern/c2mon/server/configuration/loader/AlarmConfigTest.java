package cern.c2mon.server.configuration.loader;

import cern.c2mon.cache.api.C2monCache;
import cern.c2mon.cache.config.collections.TagCacheCollection;
import cern.c2mon.server.cache.dbaccess.AlarmMapper;
import cern.c2mon.server.cache.dbaccess.DataTagMapper;
import cern.c2mon.server.cache.dbaccess.RuleTagMapper;
import cern.c2mon.server.common.alarm.Alarm;
import cern.c2mon.server.common.alarm.AlarmCacheObject;
import cern.c2mon.server.common.datatag.DataTag;
import cern.c2mon.server.common.datatag.DataTagCacheObject;
import cern.c2mon.server.common.rule.RuleTag;
import cern.c2mon.server.common.tag.Tag;
import cern.c2mon.server.configuration.parser.util.ConfigurationAlarmUtil;
import cern.c2mon.server.configuration.util.TestConfigurationProvider;
import cern.c2mon.shared.client.alarm.condition.AlarmCondition;
import cern.c2mon.shared.client.alarm.condition.ValueAlarmCondition;
import cern.c2mon.shared.client.configuration.ConfigConstants;
import cern.c2mon.shared.client.configuration.ConfigurationReport;
import cern.c2mon.shared.client.configuration.api.Configuration;
import org.junit.Test;

import javax.inject.Inject;

import static org.junit.Assert.*;

public class AlarmConfigTest extends ConfigurationCacheLoaderTest<Alarm> {

  @Inject private C2monCache<Alarm> alarmCache;

  @Inject private AlarmMapper alarmMapper;

  @Inject private C2monCache<RuleTag> ruleTagCache;

  @Inject private RuleTagMapper ruleTagMapper;

  @Inject private C2monCache<DataTag> dataTagCache;

  @Inject private DataTagMapper dataTagMapper;

  @Inject private TagCacheCollection tagCacheCollection;

  /**
   * Test the creation, update and removal of alarm.
   */

  @Test
  public void testCreateAlarmWithExistingDatatag() {
    dataTagCache.computeQuiet(200003L, dataTag -> {
      ((DataTagCacheObject) dataTag).setValue(Boolean.TRUE);
      dataTag.getDataTagQuality().validate();
    });

    configurationLoader.applyConfiguration(22);
  }


  /**
   * Test the creation, update and removal of alarm.
   */
  @Test
  public void testCreateUpdateAlarm() {
    ConfigurationReport report = configurationLoader.applyConfiguration(22);

    assertFalse(report.toXML().contains(ConfigConstants.Status.FAILURE.toString()));

    Alarm expectedObject = expectedObject();

    assertEquals(expectedObject, alarmCache.get(300000L));

    // also check that the Tag was updated
    DataTag tag = dataTagCache.get(expectedObject.getDataTagId());
    assertTrue(tag.getAlarmIds().contains(expectedObject.getId()));
  }

  @Test
  public void update() {
    configurationLoader.applyConfiguration(22);

    ConfigurationReport report = configurationLoader.applyConfiguration(23);

    assertFalse(report.toXML().contains(ConfigConstants.Status.FAILURE.toString()));
    AlarmCacheObject expectedObject = expectedObject();
    expectedObject.setFaultFamily("updated fault family");
    assertEquals(expectedObject, alarmCache.get(300000L));
  }

  private static AlarmCacheObject expectedObject() {
    AlarmCacheObject expectedObject = new AlarmCacheObject(300000L);
    expectedObject.setDataTagId(200003L);
    expectedObject.setFaultFamily("fault family");
    expectedObject.setFaultMember("fault member");
    expectedObject.setFaultCode(223);
    expectedObject
      .setCondition(AlarmCondition
        .fromConfigXML("<AlarmCondition class=\"cern.c2mon.server.common.alarm.ValueAlarmCondition\"><alarm-value type=\"Boolean\">true</alarm-value></AlarmCondition>"));

    return expectedObject;
  }

  @Test
  public void testRemoveAlarm() {
    Alarm alarm = alarmCache.get(350000L);
    assertNotNull(alarm);
    assertTrue(alarmCache.containsKey(350000L));
    assertNotNull(alarmMapper.getItem(350000L));

    ConfigurationReport report = configurationLoader.applyConfiguration(24);

    assertFalse(report.toXML().contains(ConfigConstants.Status.FAILURE.toString()));
    assertFalse(alarmCache.containsKey(350000L));
    assertNull(alarmMapper.getItem(350000L));

    Tag tag = tagCacheCollection.get(alarm.getDataTagId());
    assertFalse(tag.getAlarmIds().contains(alarm.getId()));
  }

  @Test
  public void createAlarm() {
    setUp();

    // TEST:Build configuration to add the test Alarm
    cern.c2mon.shared.client.configuration.api.alarm.Alarm alarm = ConfigurationAlarmUtil.buildCreateAllFieldsAlarm(2000L, null);
    Configuration configuration = new Configuration();
    configuration.addEntity(alarm);

    ///apply the configuration to the server
    ConfigurationReport report = configurationLoader.applyConfiguration(configuration);

    // check report result
    assertFalse(report.toXML().contains(ConfigConstants.Status.FAILURE.toString()));
    assertEquals(ConfigConstants.Status.OK, report.getStatus());
    assertEquals(1, report.getElementReports().size());

    // get cacheObject from the cache and compare to the an expected cacheObject
    AlarmCacheObject cacheObjectAlarm = (AlarmCacheObject) alarmCache.get(2000L);
    AlarmCacheObject expectedCacheObjectAlarm = cacheObjectFactory.buildAlarmCacheObject(2000L, alarm);

    assertEquals(expectedCacheObjectAlarm, cacheObjectAlarm);
    // Check if all caches are updated
    assertNotNull(alarmMapper.getItem(2000L));
  }

  @Test
  public void updateAlarm() {
    setUp();
    configurationLoader.applyConfiguration(TestConfigurationProvider.createAlarm());

    // TEST:Build configuration to update the test Alarm
    cern.c2mon.shared.client.configuration.api.alarm.Alarm alarmUpdate = cern.c2mon.shared.client.configuration.api.alarm.Alarm.update(2000L).alarmCondition(new ValueAlarmCondition(5)).build();
    Configuration configuration = new Configuration();
    configuration.addEntity(alarmUpdate);

    ///apply the configuration to the server
    ConfigurationReport report = configurationLoader.applyConfiguration(configuration);

    // check report result
    assertFalse(report.toXML().contains(ConfigConstants.Status.FAILURE.toString()));
    assertEquals(ConfigConstants.Status.OK, report.getStatus());
    assertEquals(1, report.getElementReports().size());

    // get cacheObject from the cache and compare to the an expected cacheObject
    AlarmCacheObject cacheObjectAlarm = (AlarmCacheObject) alarmCache.get(2000L);
    AlarmCacheObject expectedCacheObjectAlarm = cacheObjectFactory.buildAlarmUpdateCacheObject(cacheObjectAlarm, alarmUpdate);

    assertEquals(expectedCacheObjectAlarm, cacheObjectAlarm);
  }

  @Test
  public void deleteAlarmWithDeleteDataTag() {
    setUp();
    configurationLoader.applyConfiguration(TestConfigurationProvider.createAlarm());

    // TEST:
    // check if the DataTag and rules are in the cache
    assertTrue(alarmCache.containsKey(2000L));
    assertNotNull(alarmMapper.getItem(2000L));
    assertTrue(dataTagCache.containsKey(1000L));
    assertNotNull(dataTagMapper.getItem(1000L));

    // Build configuration to remove the DataTag
    Configuration removeTag = TestConfigurationProvider.deleteDataTag();
    ConfigurationReport report = configurationLoader.applyConfiguration(removeTag);

    //check report result
    assertFalse(report.toXML().contains(ConfigConstants.Status.FAILURE.toString()));
    assertSame(report.getStatus(), ConfigConstants.Status.OK);
    assertEquals(1, report.getElementReports().size());

    // Check if all caches are updated
    assertFalse(alarmCache.containsKey(2000L));
    assertNull(alarmMapper.getItem(2000L));
    assertFalse(dataTagCache.containsKey(100L));
    assertNull(dataTagMapper.getItem(100L));
  }

  /**
   * Tests that a tag removal does indeed remove an associated alarm.
   */
  @Test
  public void testAlarmRemovedOnTagRemoval() {
    // test removal of (rule)tag 60000 removes the alarm also
    configurationLoader.applyConfiguration(27);
    assertFalse(alarmCache.containsKey(350000L));
    assertNull(alarmMapper.getItem(350000L));
    assertFalse(ruleTagCache.containsKey(60000L));
    assertNull(ruleTagMapper.getItem(60000L));
  }
}