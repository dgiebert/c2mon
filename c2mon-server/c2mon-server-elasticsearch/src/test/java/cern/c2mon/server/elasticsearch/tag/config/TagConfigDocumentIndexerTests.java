/*******************************************************************************
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
 ******************************************************************************/

package cern.c2mon.server.elasticsearch.tag.config;

import cern.c2mon.server.common.datatag.DataTagCacheObject;
import cern.c2mon.server.elasticsearch.Indices;
import cern.c2mon.server.elasticsearch.config.BaseElasticsearchIntegrationTest;
import cern.c2mon.server.elasticsearch.junit.CachePopulationRule;
import cern.c2mon.server.elasticsearch.util.EntityUtils;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Response;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Szymon Halastra
 * @author Justin Lewis Salmon
 */
public class TagConfigDocumentIndexerTests extends BaseElasticsearchIntegrationTest {

  @Autowired
  private TagConfigDocumentIndexer indexer;

  @Autowired
  private TagConfigDocumentConverter converter;

  @Rule
  @Autowired
  public CachePopulationRule cachePopulationRule;

  @Test
  public void addDataTag() throws Exception {
    DataTagCacheObject tag = (DataTagCacheObject) EntityUtils.createDataTag();

    TagConfigDocument document = converter.convert(tag)
            .orElseThrow(()->new Exception("Tag conversion failed"));
    String index = Indices.indexFor(document);

    indexer.indexTagConfig(document);
    assertTrue(Indices.exists(index));

    // Refresh the index to make sure the document is searchable
    client.getClient().admin().indices().prepareRefresh(index).get();
    client.getClient().admin().cluster().prepareHealth().setIndices(index).setWaitForYellowStatus().get();

    // Make sure the tag exists in the index
    SearchResponse response = client.getClient().prepareSearch(index).setRouting(tag.getId().toString()).get();
    assertEquals(1, response.getHits().totalHits());

    // Clean up
    DeleteIndexResponse deleteResponse = client.getClient().admin().indices().prepareDelete(index).get();
    assertTrue(deleteResponse.isAcknowledged());
  }

  @Test
  public void updateDataTag() throws Exception {
    DataTagCacheObject tag = (DataTagCacheObject) EntityUtils.createDataTag();

    TagConfigDocument document = converter.convert(tag)
            .orElseThrow(()->new Exception("Tag conversion failed"));
    String index = Indices.indexFor(document);

      // Insert the document
      indexer.indexTagConfig(document);
      assertTrue(Indices.exists(index));

    // Refresh the index to make sure the document is searchable
//    client.getClient().admin().indices().prepareRefresh(index).get();
//    client.getClient().admin().cluster().prepareHealth().setIndices(index).setWaitForYellowStatus().get();




    // Update the document
    document.put("description", "A better description");
    ((Map<String, Object>) document.get("metadata")).put("spam", "eggs");
    indexer.updateTagConfig(document);



    SearchRequest searchRequest =
        new SearchRequest().indices(index).source(
            new SearchSourceBuilder()
                .query(QueryBuilders.termQuery("id", tag.getId())));
    System.out.println(searchRequest);
    //QueryBuilder queryBuilder = QueryBuilders.boolQuery().must(termQuery("_id", tag.getId()));
    //searchRequest.source((new SearchSourceBuilder()).query(queryBuilder));
//    searchRequest.routing(tag.getId().toString());
    //Response response = this.client.getLowLevelRestClient().performRequest("GET", "/" + index + "/_search?routing=" + tag.getId());
//    SearchResponse response = this.client.getRestClient().search(searchRequest);
    // Make sure the tag exists in the index
//    SearchResponse response = client.getClient().prepareSearch(index).setRouting(tag.getId().toString()).get();
    SearchResponse response = this.client.getRestClient().search(searchRequest);
    assertEquals(1, response.getHits().getTotalHits());

    // Refresh again
//    client.getClient().admin().indices().prepareRefresh(index).get();
//    client.getClient().admin().cluster().prepareHealth().setIndices(index).setWaitForYellowStatus().get();

    // Make sure we still only have one document
/*    response = client.getClient().prepareSearch(index).setRouting(tag.getId().toString()).get();
    assertEquals(1, response.getHits().totalHits());

    SearchHit hit = response.getHits().getAt(0);
    assertEquals("A better description", hit.getSource().get("description"));
    assertEquals("eggs", ((Map) hit.getSource().get("metadata")).get("spam"));*/
  }

  @Test
  public void removeDataTag() throws Exception {
    DataTagCacheObject tag = (DataTagCacheObject) EntityUtils.createDataTag();

    TagConfigDocument document = converter.convert(tag)
            .orElseThrow(()->new Exception("Tag conversion failed"));

      String index = Indices.indexFor(document);

      // Insert the document
      indexer.indexTagConfig(document);
      assertTrue(Indices.exists(index));

    // Refresh the index to make sure the document is searchable
    //client.getClient().admin().indices().prepareRefresh(index).get();
    //client.getClient().admin().cluster().prepareHealth().setIndices(index).setWaitForYellowStatus().get();

    // Make sure the tag exists in the index
    SearchResponse response = client.getClient().prepareSearch(index).setRouting(tag.getId().toString()).get();
    assertEquals(1, response.getHits().totalHits());

    // Delete the document
    indexer.removeTagConfig(document);

    // Refresh again
    //client.getClient().admin().indices().prepareRefresh(index).get();
    //client.getClient().admin().cluster().prepareHealth().setIndices(index).setWaitForYellowStatus().get();

    // Make sure we have no documents
    //response = client.getClient().prepareSearch(index).setRouting(tag.getId().toString()).get();
    //assertEquals(0, response.getHits().totalHits());
  }
}
