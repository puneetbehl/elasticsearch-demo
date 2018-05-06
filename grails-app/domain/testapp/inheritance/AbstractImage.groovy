package testapp.inheritance

abstract class AbstractImage {
    String name

    static constraints = {
        name nullable: true
    }

    static mapping = {
        autoImport(false)
    }
}
