package testapp

import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import spock.lang.Specification
import testapp.customid.Toy

@Integration
@Rollback
class CustomIdSpec extends Specification implements ElasticSearchSpec {

    void 'Index a domain object with UUID-based id'() {
        given:
        Toy car = save new Toy(name: 'Car', color: "Red")
        Toy plane = save new Toy(name: 'Plane', color: "Yellow")

        index(car, plane)
        refreshIndices()

        when:
        def search = search(Toy, 'Yellow')

        then:
        search.total == 1
        search.searchResults[0].id == plane.id
    }

}
