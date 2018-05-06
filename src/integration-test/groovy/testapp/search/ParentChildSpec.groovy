package testapp.search

import grails.plugins.elasticsearch.ElasticSearchResult
import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import org.elasticsearch.index.query.QueryBuilders
import spock.lang.Specification
import testapp.ElasticSearchSpec
import testapp.json.Product
import testapp.parentchild.Department
import testapp.parentchild.Store

@Integration
@Rollback
class ParentChildSpec extends Specification implements ElasticSearchSpec {

    void 'searching for features of the parent element from the actual element'() {
        given: 'parent and child elements'
        def parentParentElement = save new Store(name: 'Eltern-Elternelement', owner: 'Horst')
        def parentElement = save new Department(name: 'Elternelement', numberOfProducts: 4, store: parentParentElement)
        def childElement = save new Product(productName: 'Kindelement', price: 5.00)

        index(parentParentElement, parentElement, childElement)
        refreshIndices()

        when:
        ElasticSearchResult result = elasticSearchService.search(
                /**
                 * TODO: Should be replaced by JoinQueryBuilders in ES 5.5.3
                 */
                QueryBuilders.hasParentQuery('store', QueryBuilders.matchQuery('owner', 'Horst'), false),
                QueryBuilders.matchAllQuery(),
                [indices: Department, types: Department])

        then:
        !result.searchResults.empty
    }
}
