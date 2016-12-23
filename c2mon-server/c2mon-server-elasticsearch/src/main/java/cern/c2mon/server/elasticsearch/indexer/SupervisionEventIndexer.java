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
package cern.c2mon.server.elasticsearch.indexer;

import java.util.Collections;
import java.util.List;

import javax.annotation.PostConstruct;

import cern.c2mon.pmanager.IDBPersistenceHandler;
import cern.c2mon.server.elasticsearch.config.ElasticsearchProperties;
import cern.c2mon.server.elasticsearch.connector.TransportConnector;
import cern.c2mon.server.elasticsearch.structure.mappings.Mappings;
import cern.c2mon.server.elasticsearch.structure.types.EsAlarm;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.indices.IndexAlreadyExistsException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import cern.c2mon.pmanager.persistence.exception.IDBPersistenceException;
import cern.c2mon.server.elasticsearch.structure.mappings.MappingFactory;
import cern.c2mon.server.elasticsearch.structure.types.EsSupervisionEvent;

/**
 * @author Alban Marguet
 * @author Justin Lewis Salmon
 */
@Slf4j
@Component
public class SupervisionEventIndexer implements IDBPersistenceHandler<EsSupervisionEvent> {

  private TransportConnector connector;

  @Autowired
  public SupervisionEventIndexer(final TransportConnector connector) {
    this.connector = connector;
  }

  @Override
  public void storeData(EsSupervisionEvent supervisionEvent) throws IDBPersistenceException {
    storeData(Collections.singletonList(supervisionEvent));
  }

  @Override
  public void storeData(List<EsSupervisionEvent> supervisionEvents) throws IDBPersistenceException {
    try {
      long failed = supervisionEvents.stream().filter(alarm -> !this.indexSupervisionEvent(alarm)).count();

      if (failed > 0) {
        throw new IDBPersistenceException("Failed to index " + failed + " of " + supervisionEvents.size() + " supervision events");
      }
    } catch (Exception e) {
      throw new IDBPersistenceException(e);
    }
  }

  private boolean indexSupervisionEvent(EsSupervisionEvent supervisionEvent) {
    String indexName = getOrCreateIndex(supervisionEvent);

    log.debug("Adding new supervision event to index {}", indexName);
    return connector.getClient().prepareIndex().setIndex(indexName)
        .setType("supervision")
        .setSource(supervisionEvent.toString())
        .setRouting(supervisionEvent.getId())
        .get().isCreated();
  }

  private String getOrCreateIndex(EsSupervisionEvent supervisionEvent) {
    String index = Indices.indexFor(supervisionEvent);

    if (!Indices.exists(index)) {
      Indices.create(index, "supervision", MappingFactory.createSupervisionMapping());
    }

    return index;
  }

  @Override
  public String getDBInfo() {
    return "elasticsearch/supervision";
  }
}