package testapp.marshalling

import grails.plugins.elasticsearch.ElasticSearchResult
import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import spock.lang.Specification
import testapp.ElasticSearchSpec
import testapp.geopoint.Building
import testapp.geopoint.GeoPoint

@Integration
@Rollback
class GeoPointMarshallingSpec extends Specification implements ElasticSearchSpec {

    void 'a geo point location is marshaled and de-marshaled correctly'() {
        given:
        GeoPoint location = save new GeoPoint(lat: 53.00, lon: 10.00)
        Building building = save new Building(name: 'EvileagueHQ', location: location)

        index(building)
        refreshIndices()

        when:
        ElasticSearchResult result = search(Building, 'EvileagueHQ')

        then:
        elasticSearchHelper.elasticSearchClient.admin().indices()

        result.total == 1
        List<Building> searchResults = result.searchResults
        GeoPoint resultLocation = searchResults[0].location
        resultLocation.lat == location.lat
        resultLocation.lon == location.lon
    }

}
