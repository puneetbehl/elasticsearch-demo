package testapp

import grails.plugins.elasticsearch.ElasticSearchResult
import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import org.elasticsearch.common.unit.DistanceUnit
import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.sort.GeoDistanceSortBuilder
import org.elasticsearch.search.sort.SortBuilders
import org.elasticsearch.search.sort.SortOrder
import spock.lang.Specification
import spock.lang.Unroll
import testapp.geopoint.Building
import testapp.geopoint.GeoPoint
import testapp.json.Product

import java.math.RoundingMode

/**
 * @author Puneet Behl
 * @since 1.0
 */

@Integration
@Rollback
class ElasticsearchServiceSpec extends Specification implements ElasticSearchSpec {

    static Boolean isSetup = false

    private static final List<Map> EXAMPLE_GEO_BUILDINGS = [
            [lat: 48.13, lon: 11.60, name: '81667'],
            [lat: 48.19, lon: 11.65, name: '85774'],
            [lat: 47.98, lon: 10.18, name: '87700']
    ]

    /**
     * This test class doesn't delete any ElasticSearch indices,
     * because that would also delete the mapping.
     *
     * Be aware of this when indexing new objects.
     */
    def setup() {
        // This is workaround due to issue with Grails3 and springbboot, otherwise we could have added in setupSpec
        if (!isSetup) {
            isSetup = true
            setupData()
        }
    }

    private static void setupData() {
        save new Product(productName: 'horst', price: 3.95)
        save new Product(productName: 'hobbit', price: 5.99)
        save new Product(productName: 'best', price: 10.99)
        save new Product(productName: 'high and supreme', price: 45.50)

        EXAMPLE_GEO_BUILDINGS.each {
            GeoPoint geoPoint = save new GeoPoint(lat: it.lat, lon: it.lon)
            save new Building(name: "${it.name}", location: geoPoint)
        }
    }

    void 'A search with Uppercase Characters should return appropriate results'() {
        given: 'a product with an uppercase name'
        Product product = save new Product(productName: 'Großer Kasten', price: 0.85)

        index(product)
        refreshIndices()

        when: 'a search is performed'
        ElasticSearchResult result = search(Product) {
            match('productName': 'Großer')
        }

        then: 'the correct result-part is returned'
        result.total == 1
        result.searchResults.size() == 1
        result.searchResults*.productName == ['Großer Kasten']
    }

    void 'A search with lowercase Characters should return appropriate results'() {
        given: 'a product with a lowercase name'
        Product product = save new Product(productName: 'KLeiner kasten', price: 0.45)

        index(product)
        refreshIndices()

        when: 'a search is performed'
        ElasticSearchResult result = search(Product) {
            wildcard('productName': 'klein*')
        }

        then: 'the correct result-part is returned'
        result.total == 1
        result.searchResults.size() == 1
        result.searchResults*.productName == ['KLeiner kasten']
    }

    @Unroll
    void 'a geo distance search finds geo points at varying distances'(String distance, List<String> postalCodesFound) {
        given:
        setupData()
        refreshIndices()

        when: 'a geo distance search is performed'
        Map params = [indices: Building, types: Building]
        QueryBuilder query = QueryBuilders.matchAllQuery()
        def location = [lat: 48.141, lon: 11.57]

        Closure filter = {
            geo_distance(
                    'distance': distance,
                    'location': location)
        }
        ElasticSearchResult result = elasticSearchService.search(params, query, filter)

        then: 'all geo points in the search radius are found'
        List<Building> searchResults = result.searchResults

        (postalCodesFound.empty && searchResults.empty) ||
                searchResults.each { it.name in postalCodesFound }

        where:
        distance  | postalCodesFound
        '1km'     | []
        '5km'     | ['81667']
        '20km'    | ['81667', '85774']
        '1000km'  | ['81667', '85774', '87700']
    }

    void 'the distances are returned'() {
        given:
        setupData()
        refreshIndices()

        // Building.list().each { it.delete() }

        when: 'a geo distance search is sorted by distance'

        GeoDistanceSortBuilder sortBuilder = SortBuilders.geoDistanceSort('location', 48.141d, 11.57d).
                unit(DistanceUnit.KILOMETERS).
                order(SortOrder.ASC)

        Map params = [indices: Building, types: Building, sort: sortBuilder]
        QueryBuilder query = QueryBuilders.matchAllQuery()
        Map<String, Double> location = [lat: 48.141, lon: 11.57]

        Closure filter = {
            geo_distance(
                    'distance': '5km',
                    'location': location)
        }
        ElasticSearchResult result = elasticSearchService.search(params, query, filter)

        and:
        List<Building> searchResults = result.searchResults
        //Avoid double precission issues
        List<BigDecimal> sortResults = result.sort.(searchResults[0].id).
                collect { (it as BigDecimal).setScale(4, RoundingMode.HALF_UP) }

        then: 'all geo points in the search radius are found'
        sortResults == [2.5401]
    }

    void 'a geo point is mapped correctly'() {
        when:
        GeoPoint location = new GeoPoint(lat: 53.00, lon: 10.00).save()
        Building building = new Building(location: location).save(flush: true)

        index(building)
        refreshIndices()

        then:
        def mapping = getFieldMappingMetaData(elasticsearchContextHolder.getMappingContextByType(Building).indexName, 'building').sourceAsMap
        mapping.(properties).location.type == 'geo_point'
    }
}
