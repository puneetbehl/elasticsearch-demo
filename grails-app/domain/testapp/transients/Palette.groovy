package testapp.transients

class Palette {

    List<Color> colors
    String author

    String getDescription() {
        (author && colors) ? "${author} likes to paint with ${colors}" : null
    }

    List<String> getComplementaries() {
        colors.collect {
            it.complementary() as String
        }
    }

    static transients = ['complementaries']

    static searchable = {
        only = ['colors', 'complementaries']
    }

    static hasMany = [colors: Color, tags: String, complementaries: String]

    static constraints = {
    }
}

enum Color {
    cyan, magenta, yellow, red, green, blue

    Color complementary() {
        Color complementary = null
        switch (this) {
            case cyan: complementary = red
                break
            case magenta: complementary = green
                break
            case yellow: complementary = blue
                break
            case red: complementary = cyan
                break
            case green: complementary = magenta
                break
            case blue: complementary = yellow
                break
            default: complementary = null
        }
        return complementary
    }
}
