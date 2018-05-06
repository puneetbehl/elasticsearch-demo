package testapp

import grails.core.GrailsApplication
import grails.plugins.elasticsearch.mapping.SearchableClassMappingConfigurator
import org.springframework.beans.factory.annotation.Autowired

trait ElasticSearchMappingSpec {

    @Autowired
    GrailsApplication grailsApplication

    @Autowired
    SearchableClassMappingConfigurator searchableClassMappingConfigurator

}
