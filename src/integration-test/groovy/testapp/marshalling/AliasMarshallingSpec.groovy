package testapp.marshalling

import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import spock.lang.Specification
import testapp.ElasticSearchSpec
import testapp.geopoint.Building
import testapp.geopoint.GeoPoint

/**
 * @author Puneet Behl
 * @since 1.0
 */

@Integration
@Rollback
class AliasMarshallingSpec extends Specification implements ElasticSearchSpec {

    void 'should marshal the alias field and un-marshal correctly (ignore alias)'() {
        given:
        def location = save new GeoPoint(lat: 53.00, lon: 10.00)
        def building = save new Building(name: 'WatchTower', location: location)

        index(building)
        refreshIndices()

        when:
        def result = search(Building, building.name)

        then:
        result.total == 1
        List<Building> searchResults = result.searchResults
        searchResults[0].name == building.name
    }

}
