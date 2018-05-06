package testapp.inheritance

class Photo extends AbstractImage {

    String url

    static constraints = {
        url(nullable: false)
    }

    static searchable = {
        url index: "not_analyzed"
    }


    String toString() {
        return "Photo{" +
                "id=" + id +
                ",url='" + url + '\'' +
                '}'
    }

    static mapping = {
        autoImport(false)
    }
}
