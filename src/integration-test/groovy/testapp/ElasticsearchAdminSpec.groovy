package testapp

import grails.core.GrailsApplication
import grails.plugins.elasticsearch.ElasticSearchAdminService
import grails.plugins.elasticsearch.ElasticSearchHelper
import org.elasticsearch.action.admin.cluster.state.ClusterStateRequestBuilder
import org.elasticsearch.client.AdminClient
import org.elasticsearch.client.ClusterAdminClient
import org.elasticsearch.cluster.ClusterState
import org.elasticsearch.cluster.metadata.IndexMetaData
import org.elasticsearch.cluster.metadata.MappingMetaData
import org.springframework.beans.factory.annotation.Autowired

/**
 * @author Puneet Behl
 * @since 1.0
 */

trait ElasticsearchAdminSpec {

    @Autowired
    GrailsApplication grailsApplication

    @Autowired
    ElasticSearchAdminService elasticSearchAdminService

    @Autowired
    ElasticSearchHelper elasticSearchHelper


    void refreshIndex(Class... searchableClasses) {
        elasticSearchAdminService.refresh(searchableClasses)
    }

    void refreshIndices() {
        elasticSearchAdminService.refresh()
    }

    void deleteIndices() {
        elasticSearchAdminService.deleteIndex()
        elasticSearchAdminService.refresh()
    }

    MappingMetaData getFieldMappingMetaData(String indexName, String typeName) {
        if (elasticSearchAdminService.aliasExists(indexName)) {
            indexName = elasticSearchAdminService.indexPointedBy(indexName)
        }
        AdminClient admin = elasticSearchHelper.elasticSearchClient.admin()
        ClusterAdminClient cluster = admin.cluster()
        ClusterStateRequestBuilder indices = cluster.prepareState().setIndices(indexName)
        ClusterState clusterState = indices.execute().actionGet().state
        IndexMetaData indexMetaData = clusterState.metaData.index(indexName)
        return indexMetaData.mapping(typeName)
    }

}