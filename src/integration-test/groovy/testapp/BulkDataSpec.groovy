package testapp

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import org.elasticsearch.action.get.GetRequest
import spock.lang.Specification
import testapp.component.Spaceship
import testapp.multifield.Person

/**
 * @author Puneet Behl
 * @since 1.0
 */

@Integration
@Rollback
class BulkDataSpec extends Specification implements ElasticSearchSpec {

    @Transactional
    private void createBulkData() {
        1858.times { n ->
            Person person = save(new Person(firstName: 'Person', lastName: "McNumbery$n"), false)
            save(new Spaceship(name: "Ship-$n", captain: person), false)
        }
        flushSession()
    }

    void 'bulk test'() {
        given:
        createBulkData()

        when:
        index(Spaceship)
        refreshIndices()

        then:
        findFailures().size() == 0
        elasticSearchService.countHits('Ship\\-') == 1858
    }

    private def findFailures() {
        def domainClass = getDomainClass(Spaceship)
        def failures = []
        List<Spaceship> allObjects = Spaceship.list()
        allObjects.each {
            elasticSearchHelper.withElasticSearch { client ->
                GetRequest getRequest = new GetRequest(
                        getIndexName(domainClass), getTypeName(domainClass), it.id.toString())
                def result = client.get(getRequest).actionGet()
                if (!result.isExists()) {
                    failures << it
                }
            }
        }
        failures
    }
}
