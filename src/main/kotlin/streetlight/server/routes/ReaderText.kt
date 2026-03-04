package streetlight.server.routes

object ReaderText {
    val locationProperties = """
        * name: name of the location, should be normal casing and appropriately capitalized
        * description: description of the location
        * address: house or building number and street name of the location
        * postalCode: Postal code of the location
        * state: State of the location, two letter abbreviation if relevant (e.g. CO)
        * country: Country of the location
        * url: web address for more information about the location
        * eventsUrl: web address for more information about upcoming events at the location, like a calendar
        * imageUrl: featured image of the location
    """.trimIndent()

    val eventProperties = """
        * name: Event name or title, should be normal casing and appropriately capitalized
        * time: Time of day of the event as 24-hour value [HH:MM]
        * date: Date of the event as ISO local date [YYYY-MM-DD]
        * location: The name or description of the location of the event
        * address: The address at which the event is located
        * imageUrl: The featured image for the event, must be a full url
        * description: Additional details given about the event
        * ageMin: The minimum age for attendees
        * contact: Any name and/or contact information given for the event
        * url: Url for more information about the event, must be a full url
    """.trimIndent()

    val locationInstructions = """
        Read the following html. We believe it is information about a venue or location that hosts events.
        
        Try to determine the following:
        $locationProperties
    """.trimIndent()

    val coldInstructions = """
        Read the following html. We believe it is information about an event or a list of events. It might also
        have information about an event location. You will parse information about the events and their location.
              
        For the location, determine the following:
        $locationProperties
        
        For each event, determine the following:
        $eventProperties
""".trimIndent()

    val multiEventInstructions = """
        Read the following html. We believe it is information about an event or a list of events. 
        
        For each event, determine the following:
        $eventProperties
    """.trimIndent()
}