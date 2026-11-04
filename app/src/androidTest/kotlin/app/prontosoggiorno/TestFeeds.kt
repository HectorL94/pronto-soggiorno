package app.prontosoggiorno

/** Feeds con la misma forma que las respuestas reales del portal (textos verificados). */
object TestFeeds {
    const val IN_PROGRESS = "Stato del suo permesso di soggiorno: documento di soggiorno in trattazione."
    const val READY = "Il documento di soggiorno &egrave; pronto per la consegna.<br />" +
        "Lo pu&ograve; ritirare presso: QUESTURA DI PROVA <br />"

    fun feed(number: String, descriptionHtml: String): String = """
        <?xml version="1.0" encoding="UTF-8" ?>
        <rss version="2.0"><channel><title>Permesso di soggiorno</title>
        <item>
          <title>Numero pratica o assicurata : $number </title>
          <description><![CDATA[ $descriptionHtml ]]></description>
          <guid>https://questure.poliziadistato.it/servizio/stranieri/?lang=italian&pratica=$number</guid>
        </item></channel></rss>
    """.trimIndent()
}
