package testapp

import grails.core.GrailsApplication
import grails.plugins.elasticsearch.ElasticSearchContextHolder
import grails.plugins.elasticsearch.mapping.SearchableClassMappingConfigurator
import org.springframework.beans.factory.annotation.Autowired

trait ElasticSearchMappingSpec {

    @Autowired
    GrailsApplication grailsApplication

    @Autowired
    SearchableClassMappingConfigurator searchableClassMappingConfigurator

    @Autowired
    ElasticSearchContextHolder elasticSearchContextHolder

    void resetElasticsearch() {
        searchableClassMappingConfigurator.configureAndInstallMappings()
    }

}
