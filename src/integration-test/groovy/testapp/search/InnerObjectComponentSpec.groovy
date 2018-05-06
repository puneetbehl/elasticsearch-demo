package testapp.search

import grails.plugins.elasticsearch.ElasticSearchResult
import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import spock.lang.Specification
import testapp.ElasticSearchSpec
import testapp.innercomponent.Spaceship
import testapp.multifield.Person

@Integration
@Rollback
class InnerObjectComponentSpec extends Specification implements ElasticSearchSpec {

    void 'Component as an inner object'() {
        given:
        Person mal = save new Person(firstName: 'Malcolm', lastName: 'Reynolds')
        Spaceship spaceship = save new Spaceship(name: 'Serenity', captain: mal)

        index(spaceship)
        refreshIndices()

        when:
        ElasticSearchResult search = search(Spaceship, 'serenity')

        then:
        search.total == 1

        Spaceship result = search.searchResults.first() as Spaceship
        result.name == 'Serenity'
        result.captain.firstName == 'Malcolm'
        result.captain.lastName == 'Reynolds'
    }

}
