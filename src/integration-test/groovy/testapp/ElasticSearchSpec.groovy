package testapp

import grails.core.GrailsApplication
import grails.plugins.elasticsearch.*
import grails.plugins.elasticsearch.mapping.DomainEntity
import grails.util.GrailsNameUtils
import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.index.query.QueryBuilder
import org.grails.datastore.gorm.GormEntity
import org.hibernate.SessionFactory
import org.springframework.beans.factory.annotation.Autowired

/**
 * @author Puneet Behl
 * @since 1.0
 */

trait ElasticSearchSpec implements ElasticsearchAdminSpec, ElasticSearchMappingSpec {

    @Autowired
    GrailsApplication grailsApplication

    @Autowired
    SessionFactory sessionFactory

    @Autowired
    ElasticSearchService elasticSearchService

    @Autowired
    ElasticSearchHelper elasticSearchHelper

    @Autowired
    ElasticSearchBootStrapHelper elasticSearchBootStrapHelper

    void resetElasticsearch() {
        deleteIndices()
        searchableClassMappingConfigurator.configureAndInstallMappings()
    }

    static <T> T save(GormEntity<T> object, boolean flush = true) {
        object.save(flush: flush, failOnError: true)
    }

    ElasticSearchResult search(Class<?> clazz, String query) {
        elasticSearchService.search(query, [indices: clazz, types: clazz])
    }

    ElasticSearchResult search(Class<?> clazz, Closure query) {
        elasticSearchService.search(query, [indices: clazz, types: clazz])
    }

    ElasticSearchResult search(String query, Map params = [:]) {
        elasticSearchService.search(query, params)
    }

    ElasticSearchResult search(SearchRequest request, Map params) {
        elasticSearchService.search(request, params) as ElasticSearchResult
    }

    ElasticSearchResult search(Class<?> clazz, QueryBuilder queryBuilder) {
        elasticSearchService.search([indices: clazz, types: clazz], queryBuilder)
    }

    void flushSession() {
        sessionFactory.currentSession.flush()
    }

    void index(GroovyObject... instances) {
        elasticSearchService.index(instances as Collection<GroovyObject>)
    }

    void index(Class... domainClass) {
        elasticSearchService.index(domainClass)
    }

    void unindex(GroovyObject... instances) {
        elasticSearchService.unindex(instances as Collection<GroovyObject>)
    }

    void unindex(Class... domainClass) {
        elasticSearchService.unindex(domainClass)
    }

    String getIndexName(DomainEntity domainClass) {
        String name = grailsApplication.config.getProperty("elasticSearch.index.name", String) ?: domainClass.packageName
        if (!name) {
            name = domainClass.defaultPropertyName
        }
        name.toLowerCase()
    }

    String getTypeName(DomainEntity domainClass) {
        GrailsNameUtils.getPropertyName(domainClass.type)
    }

    DomainEntity getDomainClass(Class<?> clazz) {
        elasticsearchContextHolder.getMappingContextByType(clazz).domainClass
    }

    ElasticSearchContextHolder getElasticsearchContextHolder() {
        elasticSearchService.elasticSearchContextHolder
    }

}
