package testapp.unmarshaller

import grails.plugins.elasticsearch.conversion.unmarshall.DomainClassUnmarshaller
import grails.plugins.elasticsearch.exception.MappingException
import grails.testing.mixin.integration.Integration
import org.elasticsearch.common.bytes.BytesArray
import org.elasticsearch.common.text.Text
import org.elasticsearch.search.SearchHit
import org.elasticsearch.search.SearchHits
import org.slf4j.Logger
import spock.lang.Specification
import testapp.ElasticSearchSpec
import testapp.geopoint.GeoPoint
import testapp.innercomponent.Circle
import testapp.transients.Color

import java.lang.reflect.Field
import java.lang.reflect.Modifier

@Integration
class DomainClassUnmarshallerSpec extends Specification implements ElasticSearchSpec {

    void cleanupSpec() {
        def dataFolder = new File('data')
        if (dataFolder.isDirectory()) {
            dataFolder.deleteDir()
        }
    }

    void 'An un-marshaled geo_point is marshaled into a GeoPoint domain object'() {
        def unmarshaller = new DomainClassUnmarshaller(elasticSearchContextHolder: elasticsearchContextHolder, grailsApplication: grailsApplication)

        given: 'a search hit with a geo_point'
        SearchHit hit = new SearchHit(1, '1', new Text('building'), [:])
                .sourceRef(new BytesArray('{"location":{"class":"testapp.geopoint.GeoPoint","id":"2", "lat":53.0,"lon":10.0},"name":"WatchTower"}'))
        SearchHit[] hits = [hit]
        def maxScore = 0.1534264087677002f
        def totalHits = 1
        def searchHits = new SearchHits(hits, totalHits, maxScore)

        when: 'an geo_point is unmarshalled'
        List results = unmarshaller.buildResults(searchHits)
        results.size() == 1

        then: 'this results in a GeoPoint domain object'
        results[0].name == 'WatchTower'
        def location = results[0].location
        location.class == GeoPoint
        location.lat == 53.0
        location.lon == 10.0
    }

    void 'An unhandled property in the indexed domain root is ignored'() {
        def unmarshaller = new DomainClassUnmarshaller(elasticSearchContextHolder: elasticsearchContextHolder, grailsApplication: grailsApplication)

        given: 'a search hit with a color with unhandled properties r-g-b'
        SearchHit hit = new SearchHit(1, '1', new Text('color'), [:])
                .sourceRef(new BytesArray('{"name":"Orange", "red":255, "green":153, "blue":0}'))
        SearchHit[] hits = [hit]
        def maxScore = 0.1534264087677002f
        def totalHits = 1
        def searchHits = new SearchHits(hits, totalHits, maxScore)
        GroovySpy([global: true], MappingException)

        when: 'the color is unmarshalled'
        def results = []
        withMockLogger {
            results = unmarshaller.buildResults(searchHits)
        }
        results.size() == 1

        then: 'this results in a color domain object'
        1 * new MappingException('Property Color.red found in index, but is not defined as searchable.')
        1 * new MappingException('Property Color.green found in index, but is not defined as searchable.')
        1 * new MappingException('Property Color.blue found in index, but is not defined as searchable.')
        0 * new MappingException(_)
        def firstResult = results[0]
        firstResult.name == 'Orange'
        firstResult.red == null
        firstResult.green == null
        firstResult.blue == null
    }

    void 'An unhandled property in an embedded indexed domain is ignored'() {
        def unmarshaller = new DomainClassUnmarshaller(elasticSearchContextHolder: elasticsearchContextHolder, grailsApplication: grailsApplication)

        given: 'a search hit with a circle, within it a color with an unhandled properties "red"'
        SearchHit hit = new SearchHit(1, '1', new Text('circle'), [:])
                .sourceRef(new BytesArray('{"radius":7, "color":{"class":"testapp.innercomponent.Color", "id":"2", "name":"Orange", "red":255}}'))
        SearchHit[] hits = [hit]
        def maxScore = 0.1534264087677002f
        def totalHits = 1
        def searchHits = new SearchHits(hits, totalHits, maxScore)
        GroovySpy(MappingException, global: true)

        when: 'the circle is un-marshaled'
        List results = []
        withMockLogger {
            results = unmarshaller.buildResults(searchHits)
        }
        results.size() == 1

        then: 'this results in a circle domain object with color'
        1 * new MappingException('Property Color.red found in index, but is not defined as searchable.')
        0 * new MappingException(_)
        def firstResult = results[0]
        firstResult.radius == 7
        def color = firstResult.color
        color.name == 'Orange'
        color.red == null
        color.green == null
        color.blue == null
    }

    private void withMockLogger(Closure closure) {
        def logField = DomainClassUnmarshaller.class.getDeclaredField('LOG')
        logField.setAccessible(true)
        Field modifiersField = Field.class.getDeclaredField("modifiers")
        modifiersField.setAccessible(true)
        modifiersField.setInt(logField, logField.getModifiers() & ~Modifier.FINAL)

        Logger origLog = (Logger) logField.get(null)
        Logger mockLog = Mock(Logger) {
            debug(_ as String) >> { String s -> if (origLog.debugEnabled) println("DEBUG: $s") }
            debug(_ as String, _ as Throwable) >> { String s, Throwable t ->
                if (origLog.debugEnabled) {
                    println("DEBUG: $s"); t.printStackTrace(System.out)
                }
            }
            error(_ as String) >> { String s -> System.err.println("ERROR: $s") }
            error(_ as String, _ as Throwable) >> { String s, Throwable t -> System.err.println("ERROR: $s"); t.printStackTrace() }
        }
        try {
            logField.set(null, mockLog)
            closure.call()
        } finally {
            logField.set(null, origLog)
        }
    }
}
