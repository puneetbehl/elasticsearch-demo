package testapp.marshalling

import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import spock.lang.Specification
import testapp.ElasticSearchSpec
import testapp.json.Product

/**
 * @author Puneet Behl
 * @since 1.0
 */

@Integration
@Rollback
class DateMarshallingSpec extends Specification implements ElasticSearchSpec {

    void 'a date value should be marshaled and de-marshaled correctly'() {
        given:
        def date = new Date()
        def product = save new Product(productName: 'product with date value', date: date)

        index(product)
        refreshIndices()

        when:
        def result = search(Product, product.productName)

        then:
        result.total == 1
        List<Product> searchResults = result.searchResults
        searchResults[0].productName == product.productName
        searchResults[0].date == product.date
    }

}
