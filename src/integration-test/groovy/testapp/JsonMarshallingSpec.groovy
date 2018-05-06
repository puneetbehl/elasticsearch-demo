package testapp

import grails.plugins.elasticsearch.ElasticSearchResult
import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import org.grails.web.json.JSONObject
import spock.lang.Specification
import testapp.json.Product

/**
 * @author Puneet Behl
 * @since 1.0
 */

@Integration
@Rollback
class JsonMarshallingSpec extends Specification implements ElasticSearchSpec {

    void 'a json object value should be marshaled and de-marshaled correctly'() {
        given:
        Product product = save new Product(
                productName: 'product with json value',
                json: new JSONObject("""{ "test": { "details": "blah" } }"""))

        index(product)
        refreshIndices()

        when:
        ElasticSearchResult result = search(Product, product.productName)

        then:
        result.total == 1
        List<Product> searchResults = result.searchResults
        searchResults[0].productName == product.productName
    }

}
