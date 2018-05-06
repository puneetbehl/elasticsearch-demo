package testapp.search

import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.index.query.QueryBuilders
import spock.lang.Specification
import testapp.ElasticSearchSpec
import testapp.geopoint.Building
import testapp.geopoint.GeoPoint
import testapp.json.Product

/**
 * @author Puneet Behl
 * @since 1.0
 */

@Integration
@Rollback
class QueryBuilderSpec extends Specification implements ElasticSearchSpec {

    private static final List<Map> EXAMPLE_GEO_BUILDINGS = [
            [lat: 48.13, lon: 11.60, name: '81667'],
            [lat: 48.19, lon: 11.65, name: '85774'],
            [lat: 47.98, lon: 10.18, name: '87700']
    ]

    static Boolean isSetup = false

    /**
     * This test class doesn't delete any ElasticSearch indices, because that would also delete the mapping.
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

        /*
        * TODO: Need to identify why test cases are not working after removing this.
        * */
        // elasticSearchService.index()
        // refreshIndices()
    }

    void 'searching with filtered query'() {
        given: 'some products'
        def wurmProduct = save new Product(productName: 'wurm', price: 2.00)
        def hansProduct = save new Product(productName: 'hans', price: 0.5)
        def fooProduct = save new Product(productName: 'foo', price: 5.0)

        index(wurmProduct, hansProduct, fooProduct)
        refreshIndices()

        when: 'searching for a price'
        def result = elasticSearchService.
                search(QueryBuilders.matchAllQuery(), QueryBuilders.rangeQuery("price").gte(1.99).lte(2.3))

        then: "the result should be product 'wurm'"
        result.total == 1
        List<Product> searchResults = result.searchResults
        searchResults[0].productName == wurmProduct.productName
    }

    void 'searching with a FilterBuilder filter and a Closure query'() {
        when: 'searching for a price'
        QueryBuilder filter = QueryBuilders.rangeQuery("price").gte(1.99).lte(2.3)
        def result = elasticSearchService.search(QueryBuilders.matchAllQuery(), filter)

        then: "the result should be product 'wurm'"
        result.total == 1
        List<Product> searchResults = result.searchResults
        searchResults[0].productName == "wurm"
    }

    void 'searching with wildcards in query at first position'() {
        given:
        setupData()
        refreshIndices()

        when: 'search with asterisk at first position'
        def result = search(Product, { wildcard(productName: '*st') })

        then: 'the result should contain 2 products'
        result.total == 2
        List<Product> searchResults = result.searchResults
        searchResults*.productName.containsAll('best', 'horst')
    }

    void 'searching with wildcards in query at last position'() {
        given:
        setupData()
        refreshIndices()

        when: 'search with asterisk at last position'
        Map params2 = [indices: Product, types: Product]
        def result2 = elasticSearchService.search(
                {
                    wildcard(productName: 'ho*')
                }, params2)

        then: 'the result should return 2 products'
        result2.total == 2
        List<Product> searchResults2 = result2.searchResults
        searchResults2*.productName.containsAll('horst', 'hobbit')
    }

    void 'searching with wildcards in query in between position'() {
        given:
        setupData()
        refreshIndices()

        when: 'search with asterisk in between position'
        def result = search(Product) {
            wildcard(productName: 's*eme')
        }

        then: 'the result should return 1 product'
        result.total == 1
        List<Product> searchResults3 = result.searchResults
        searchResults3[0].productName == 'high and supreme'
    }

    void 'searching for special characters in data pool'() {
        given: 'some products'
        def product = save new Product(productName: 'ästhätik', price: 3.95)

        index(product)
        refreshIndices()

        when: "search for 'a umlaut' "
        def result = elasticSearchService.search({ match(productName: 'ästhätik') })

        then: 'the result should contain 1 product'
        result.total == 1
        List<Product> searchResults = result.searchResults
        searchResults[0].productName == product.productName
    }

}
