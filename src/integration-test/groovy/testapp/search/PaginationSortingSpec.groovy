package testapp.search

import grails.plugins.elasticsearch.ElasticSearchResult
import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import org.elasticsearch.search.sort.FieldSortBuilder
import org.elasticsearch.search.sort.SortOrder
import spock.lang.Specification
import testapp.ElasticSearchSpec
import testapp.json.Product

/**
 * @author Puneet Behl
 * @since 1.0
 */

@Integration
@Rollback
class PaginationSortingSpec extends Specification implements ElasticSearchSpec {

    void 'Paging and sorting through search results'() {
        given: 'a bunch of products'
        10.times {
            def product = save new Product(productName: "Product${it}", price: it)
            index(product)
        }
        refreshIndices()

        when: 'a search is performed'
        Map params = [from: 3, size: 2, indices: Product, types: Product, sort: 'productName']
        Closure query = {
            wildcard(productName: 'product*')
        }
        ElasticSearchResult result = elasticSearchService.search(query, params)

        then: 'the correct result-part is returned'
        result.total == 10
        result.searchResults.size() == 2
        result.searchResults*.productName == ['Product3', 'Product4']
    }

    void 'Multiple sorting through search results'() {
        given: 'a bunch of products'
        Product product
        2.times { int i ->
            2.times { int k ->
                product = new Product(productName: "Yogurt$i", price: k).save(failOnError: true, flush: true)
                elasticSearchService.index(product)
            }
        }
        refreshIndices()

        when: 'a search is performed'
        FieldSortBuilder sort1 = new FieldSortBuilder('productName').order(SortOrder.ASC)
        FieldSortBuilder sort2 = new FieldSortBuilder('price').order(SortOrder.DESC)
        Map params = [indices: Product, types: Product, sort: [sort1, sort2]]
        Closure query = {
            wildcard(productName: 'yogurt*')
        }
        ElasticSearchResult result = elasticSearchService.search(query, params)

        then: 'the correct result-part is returned'
        result.searchResults.size() == 4
        result.searchResults*.productName == ['Yogurt0', 'Yogurt0', 'Yogurt1', 'Yogurt1']
        result.searchResults*.price == [1, 0, 1, 0]

        when: 'another search is performed'
        sort1 = new FieldSortBuilder('productName').order(SortOrder.DESC)
        sort2 = new FieldSortBuilder('price').order(SortOrder.ASC)
        params = [indices: Product, types: Product, sort: [sort1, sort2]]
        query = {
            wildcard(productName: 'yogurt*')
        }
        result = elasticSearchService.search(query, params)

        then: 'the correct result-part is returned'
        result.total == 4
        result.searchResults.size() == 4
        result.searchResults*.productName == ['Yogurt1', 'Yogurt1', 'Yogurt0', 'Yogurt0']
        result.searchResults*.price == [0, 1, 0, 1]
    }

}
