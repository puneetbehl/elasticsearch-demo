package testapp

import grails.plugins.elasticsearch.mapping.ElasticSearchMappingFactory
import grails.testing.mixin.integration.Integration
import spock.lang.Specification
import spock.lang.Unroll
import testapp.geopoint.Building
import testapp.geopoint.GeoPoint
import testapp.json.Product
import testapp.mapping.migration.Catalog
import testapp.multifield.Person
import testapp.transients.Anagram
import testapp.transients.Palette

/**
 * Created by @marcos-carceles on 28/01/15.
 * ['string', 'integer', 'long', 'float', 'double', 'boolean', 'null', 'date']
 */
@Integration
class ElasticsearchMappingFactorySpec extends Specification implements ElasticSearchSpec {

    void setup() {
        grailsApplication.config.elasticSearch.includeTransients = true
        resetElasticsearch()
    }

    void cleanup() {
        grailsApplication.config.elasticSearch.includeTransients = false
        resetElasticsearch()
    }

    @Unroll('#clazz / #property is mapped as #expectedType')
    void "calculates the correct ElasticSearch types"() {
        given:
        def scm = elasticsearchContextHolder.getMappingContextByType(clazz)

        when:
        Map mapping = ElasticSearchMappingFactory.getElasticMapping(scm)

        then:
        mapping[clazz.simpleName.toLowerCase()]['properties'][property].type == expectedType

        where:
        clazz    | property          || expectedType

        Building | 'name'            || 'text'
        Building | 'date'            || 'date'
        Building | 'location'        || 'geo_point'

        Product  | 'price'           || 'float'
        Product  | 'json'            || 'object'

        Catalog  | 'pages'           || 'object'

        Person   | 'fullName'        || 'text'
        Person   | 'nickNames'       || 'text'

        Palette  | 'colors'          || 'text'
        Palette  | 'complementaries' || 'text'

        Anagram  | 'length'          || 'integer'
        Anagram  | 'palindrome'      || 'boolean'
    }

    void 'a geo point is mapped correctly'() {
        when:
        GeoPoint location = new GeoPoint(lat: 53.00, lon: 10.00).save()
        Building building = new Building(location: location).save(flush: true)

        index(building)
        refreshIndices()

        then:
        def mapping = getFieldMappingMetaData('test', 'building').sourceAsMap
        mapping.(properties).location.type == 'geo_point'
    }
}
