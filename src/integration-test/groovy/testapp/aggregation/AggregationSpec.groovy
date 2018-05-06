package testapp.aggregation

import grails.plugins.elasticsearch.ElasticSearchResult
import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.action.search.SearchType
import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.aggregations.AggregationBuilders
import org.elasticsearch.search.builder.SearchSourceBuilder
import spock.lang.Specification
import testapp.ElasticSearchSpec
import testapp.json.Product

@Integration
@Rollback
class AggregationSpec extends Specification implements ElasticSearchSpec {

    void 'Use an aggregation'() {
        given:
        Product jim = save new Product(productName: 'jim', price: 1.99)
        Product xlJim = save new Product(productName: 'xl-jim', price: 5.99)

        index(jim, xlJim)
        refreshIndices()

        QueryBuilder query = QueryBuilders.matchQuery('productName', 'jim')
        SearchRequest request = new SearchRequest()
        request.searchType SearchType.DFS_QUERY_THEN_FETCH

        SearchSourceBuilder source = new SearchSourceBuilder()
        source.aggregation(AggregationBuilders.max('max_price').field('price'))
        source.query(query)

        request.source(source)

        when:
        ElasticSearchResult search = search(request, [indices: Product, types: Product])

        then:
        search.total == 2
        search.aggregations.'max_price'.max == 5.99f
    }

}
