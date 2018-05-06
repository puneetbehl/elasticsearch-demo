package testapp

import grails.core.GrailsApplication
import grails.plugins.elasticsearch.mapping.SearchableClassMappingConfigurator
import org.springframework.beans.factory.annotation.Autowired

/**
 * @author Puneet Behl
 * @since 1.0
 */

trait ElasticSearchMappingSpec {

    @Autowired
    GrailsApplication grailsApplication

    @Autowired
    SearchableClassMappingConfigurator searchableClassMappingConfigurator

}
