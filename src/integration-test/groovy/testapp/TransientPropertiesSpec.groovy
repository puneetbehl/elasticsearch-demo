package testapp

import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import spock.lang.Specification
import testapp.transients.*

/**
 * Created by @marcos-carceles on 29/01/15.
 */

@Integration
@Rollback
class TransientPropertiesSpec extends Specification implements ElasticSearchSpec {

    void 'when includeTransients config is false only properties explictly included in only are indexed and searchable'() {
        expect:
        grailsApplication.config.elasticSearch.includeTransients == false

        when: "Indexing some instances"
        def toIndex = []
        toIndex << new Anagram(original: "unbelievable").save(flush: true)
        toIndex << new Calculation(a: 21, b: 3).save(flush: true)
        toIndex << new Palette(author: "Picasso", colors: [Color.red, Color.blue]).save(flush: true)
        assert toIndex[2].description == "Picasso likes to paint with [red, blue]"
        elasticSearchService.index(toIndex)
        elasticSearchAdminService.refresh()

        and: "searching for explicitly indexed transients"
        def results = Palette.search("cyan")

        then: "we find results when searching for transients explicitly mapped with 'only'"
        results.total == 1

        and: "transients use data stored on ElasticSearch"
        results.searchResults.first().complementaries == ['cyan', 'yellow']
        results.searchResults.first().description == null //as author is not stored in ElasticSearch

        and: "we don't find any other transients"
        Anagram.search("elbaveilebnu").total == 0
        Calculation.search("24").total == 0
        Calculation.search("63").total == 0
    }

    void 'when includeTransients config is true all non excluded transient properties are indexed and searchable'() {
        given: "the configuration says to always include transients"
        grailsApplication.config.elasticSearch.includeTransients = true

        elasticSearchAdminService.deleteIndex()
        searchableClassMappingConfigurator.configureAndInstallMappings()

        when: "Indexing some instances"
        def toIndex = []
        toIndex << new Anagram(original: "unbelievable").save(flush: true)
        toIndex << new Calculation(a: 21, b: 3).save(flush: true)
        toIndex << new Palette(author: "Picasso", colors: [Color.red, Color.blue]).save(flush: true)
        assert toIndex[2].description == "Picasso likes to paint with [red, blue]"
        elasticSearchService.index(toIndex)
        elasticSearchAdminService.refresh()

        then: "We can search using any transient"
        Palette.search("cyan").total == 1
        Anagram.search("elbaveilebnu").total == 1
        Calculation.search("24").total == 1
        Calculation.search("63").total == 1
        Calculation.search("7").total == 0 //because division is not indexed

        and: "transients on results use data stored on ElasticSearch"
        Calculation calc = Calculation.search("24").searchResults.first()
        calc.multiplication == 63 //as multiplication is stored in ElasticSearch
        calc.addition == 0 //as properties a and b are not stored in ElasticSearch

        when: "domain objects are fetched"
        calc = Calculation.get(calc.id)

        then: "all propertie are available"
        calc.addition == 24

        cleanup:
        grailsApplication.config.elasticSearch.includeTransients = false
        searchableClassMappingConfigurator.configureAndInstallMappings()
    }

    void 'when transient associations are mapped as component the association is searchable'() {

        when: "save and index an instance which hasMany associations mapped as component"
        new Player(name: "Ronaldo").save(flush: true)
        def toIndex = []
        toIndex << new Team(name: "Barcelona", strip: "White").save(flush: true)
        elasticSearchService.index(toIndex)
        elasticSearchAdminService.refresh()

        then: "We can search using the transient collection component"
        Team.search("Ronaldo").total == 1

        and: "transients on search results using the component association use data stored on ElasticSearch"
        Team team = Team.search("Barcelona").searchResults.first()
        team.players.size() == 1
        team.players[0].name == "Ronaldo"

        when: "domain objects are fetched"
        team = Team.get(team.id)

        then: "all propertie are available"
        team.name == "Barcelona"

        cleanup:
        elasticSearchService.unindex(toIndex)
    }

    void 'when transient associations are mapped as refernece, the association is searchable'() {

        when: "save and index an instance which hasMany associations mapped as reference"
        def toIndex = []
        toIndex << new Fan(name: "Eric").save(flush: true)
        toIndex << new Team(name: "Barcelona", strip: "White").save(flush: true)
        elasticSearchService.index(toIndex)
        elasticSearchAdminService.refresh()

        then: "we can't search using the transient reference"
        Team.search("Eric").total == 0

        and: "but searches using the parent are built using the association reference data stored on ElasticSearch"
        Team team = Team.search("Barcelona").searchResults.first()
        team.fans.size() == 1
        team.fans[0].name == "Eric"

        when: "domain objects are fetched"
        team = Team.get(team.id)

        then: "all propertie are available"
        team.name == "Barcelona"
        team.fans.size() == 1
        team.fans[0].name == "Eric"

        cleanup:
        elasticSearchService.unindex(toIndex)

    }

}
