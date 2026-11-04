package app.prontosoggiorno.core

import app.prontosoggiorno.core.model.PraticaNumber

object Fixtures {
    /** Número ficticio usado en todas las fixtures (ZZ no es ninguna provincia). */
    const val FIXTURE_NUMBER = "00ZZ000000"

    fun load(name: String): String =
        requireNotNull(javaClass.getResource("/fixtures/$name")) { "Missing fixture $name" }
            .readText(Charsets.UTF_8)

    fun number(raw: String = FIXTURE_NUMBER): PraticaNumber =
        (PraticaNumber.parse(raw) as PraticaNumber.ParseResult.Valid).number

    /** Feed sintético con la misma estructura que el real, para casos aún no observados. */
    fun syntheticFeed(descriptionHtml: String, number: String = FIXTURE_NUMBER): String = """
        <?xml version="1.0" encoding="UTF-8" ?>
        <rss version="2.0"><channel><title>Permesso di soggiorno</title>
        <item>
          <title>Numero pratica o assicurata : $number </title>
          <description><![CDATA[ $descriptionHtml ]]></description>
          <guid>https://questure.poliziadistato.it/servizio/stranieri/?lang=italian&pratica=$number</guid>
        </item>
        </channel></rss>
    """.trimIndent()
}
