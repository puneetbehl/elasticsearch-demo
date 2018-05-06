package testapp

import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import spock.lang.Specification
import testapp.json.Product

/**
 * @author Puneet Behl
 */

@Integration
@Rollback
class IndexingSpec extends Specification implements ElasticSearchSpec {

    void 'Index and un-index a domain object'() {
        given:
        def product = save new Product(productName: 'myTestProduct')

        when:
        search(Product, 'myTestProduct').total == 1

        then:
        unindex(product)

        and:
        search(Product, 'myTestProduct').total == 0
    }
}
