package testapp.json

import grails.converters.JSON
import grails.plugins.elasticsearch.ElasticSearchResult
import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import org.grails.web.json.JSONElement
import spock.lang.Specification
import testapp.ElasticSearchSpec
import testapp.component.Spaceship
import testapp.multifield.Person

/**
 * @author Puneet Behl
 * @since 1.0
 */

@Rollback
@Integration
class DynamicJsonStringSpec extends Specification implements ElasticSearchSpec {

    void 'dynamicly mapped JSON strings should be searchable'() {
        given: 'A Spaceship with some cool canons'
        Spaceship spaceship = new Spaceship(
                name: 'Spaceball One', captain: new Person(firstName: 'Dark', lastName: 'Helmet').save())
        Map data = [engines   : [[name: "Primary", maxSpeed: 'Ludicrous Speed'],
                                 [name: "Secondary", maxSpeed: 'Ridiculous Speed'],
                                 [name: "Tertiary", maxSpeed: 'Light Speed'],
                                 [name: "Main", maxSpeed: 'Sub-Light Speed'],],
                    facilities: ['Shopping Mall', 'Zoo', 'Three-Ring circus']]
        spaceship.shipData = (data as JSON).toString()
        spaceship.save(flush: true, validate: false)

        index(spaceship)
        refreshIndices()

        when: 'a search is executed'
        ElasticSearchResult results = search(Spaceship) {
            bool { must { term("shipData.engines.name": 'primary') } }
        }

        then: "the json data should be searchable as if it was an actual component of the Spaceship"
        results.total == 1
        Spaceship firstShip = results.searchResults.first() as Spaceship
        JSONElement shipData = JSON.parse(firstShip.shipData)

        firstShip.name == 'Spaceball One'
        shipData.facilities.size() == 3
    }

}
