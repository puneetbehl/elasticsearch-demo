package testapp.search

import grails.plugins.elasticsearch.ElasticSearchResult
import spock.lang.Specification
import testapp.ElasticSearchSpec
import testapp.component.Spaceship
import testapp.multifield.Person

class MultiFieldSpec extends Specification implements ElasticSearchSpec {

    void 'Multi_filed creates untouched field'() {
        given:
        Person mal = save new Person(firstName: 'J. T.', lastName: 'Esteban')
        Spaceship spaceship = save new Spaceship(name: 'USS Grissom', captain: mal)

        index(spaceship)
        refreshIndices()

        when:
        ElasticSearchResult results = search(Spaceship) {
            bool { must { term("name.untouched": 'USS Grissom') } }
        }

        then:
        results.total == 1

        Spaceship ship = results.searchResults.first() as Spaceship
        ship.name == 'USS Grissom'
        ship.captain.firstName == 'J. T.'
        ship.captain.lastName == 'Esteban'
    }

}
