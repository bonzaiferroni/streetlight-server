package streetlight.server.routes

object ParserText {
    val locationProperties = """
        * name: name of the location, should be normal casing and appropriately capitalized
        * description: description of the location
        * address: house or building number and street name of the location
        * postalCode: Postal code of the location
        * state: State of the location, two letter abbreviation if relevant (e.g. CO)
        * country: Country of the location
        * url: home page for the location
        * eventsUrl: web address for more information about upcoming events at the location, like a calendar
        * aboutUrl: web address for more information about the location
        * menuUrl: web address for a list of food/drink items available at the location
        * imageUrl: Featured image of the location. If the source is the home page for the location, look for a meta tag with og:image or twitter:image properties.
    """.trimIndent()

    val eventProperties = """
        * name: Event name or title, should be normal casing and appropriately capitalized
        * startTime: Time of day of the event as 24-hour value [HH:MM]
        * endTime: Time when the event ends as 24-hour value [HH:MM]. Must be different from startTime. 
        * date: Date of the event as ISO local date [YYYY-MM-DD]
        * location: The name or description of the location of the event
        * address: The address at which the event is located
        * imageUrl: The featured image for the event, must be a full url
        * description: Additional details given about the event. Provide rich formatting with markdown. Preserve paragraphs.
        * ageMin: The minimum age for attendees
        * cost: The lowest entry fee or ticket price for the event in USD. Provide the string value Free if there is no cost.
        * contact: Any name and/or contact information given for the event
        * url: Url for more information about the event, must be a full url
        
        Only provide details for the first 20 events listed.
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

    val singleEventInstructions = """
        Read the following html. We believe it is information about an event.
        
        Determine the following:
        $eventProperties
    """.trimIndent()

    //language="MD"
    val eventFeedSelectorsInstructions = """
Read the following HTML. We believe it is a calendar page or feed with a listing of events at a particular location.
Your role is to extract a set of CSS selectors that can be used to parse the page for event information.
Remember that you are not providing the details themselves, only the selectors that will be used to
query the document. 

The selector values you provide should be the minimal selector necessary to return the desired elements.
Id or tag selectors may be used in combination with descendant selectors to isolate targets. 
Targets should prioritize class selectors, then attribute selectors, then a combination of both. 
Provide a descendent chain of tags only if neither class selectors nor attribute selectors are sufficient.
Prefer selectors that do not appear to be generated with random numbers and letters: (e.g. "#event-48213", ".css-1a2b3c")

The document body will be queried for the event selector, and this is expected to return a list of elements.

Event-specific selectors are queried against each event element and must not repeat the event
selector as a prefix. If the event selector is ".event-item" and the title sits in
"<h3 class='event-title'>", the title selector is ".event-title", not ".event-item .event-title".

If the content of the document does not contain the information that the selector is intended to query,
leave its value null. A null value in that case is correct and expected.

If any of the selectors must target the event element itself, as you might expect with an anchor that wraps the entire 
event content, define it as a single dot: "."
 
Sometimes the target information will be contained within a set of child elements, that is fine but the
textContent should evaluate to the desired information where relevant.

Each value should be a valid CSS query, except in the case of a single dot that indicates the event element.

Determine the following:
* event: This selector will be used to return a list of elements that each contain details about a specific event.
* title: This selector should return an element with text content that reflects the event title.
* link: This selector should return an anchor element with a href attribute with an absolute or relative address to an event page.
    - It may be the same as title.
    - Unlike the other elements, the information in the href attribute will be used and not the text content.
* image: This selector should return an img element with a src attribute referencing an image for the event.
    - Like link, the src attribute will be used and not the text content.
* cost: This selector should return an element with text content about the cost of an event or whether it is free.
* description: This selector should return an element with text content that describes the event.
    - It may contain a variety of elements.
* date: This selector should return an element with text content about the date of the event.
* time: This selector should return an element with text content about the time of the event. 
    - It may be the same as date.
"""
    //language="MD"
    val eventPageSelectorsInstructions = """
Read the following HTML. We believe it is a page with detailed information about a single event at a particular location.
Your role is to extract a set of CSS selectors that can be used to parse the page for event information.
Remember that you are not providing the details themselves, only the selectors that will be used to
query the document. 

The selector values you provide should be the minimal selector necessary to return the desired elements.
Id or tag selectors may be used in combination with descendant selectors to isolate targets. 
Targets should prioritize class selectors, then attribute selectors, then a combination of both. 
Provide a descendent chain of tags only if neither class selectors or attribute selectors are sufficient.
Prefer selectors that do not appear to be generated with random numbers and letters: (e.g. "#event-48213", ".css-1a2b3c")

Each selector will be queried against the document body and is expected to return a single element.

If the content of the document does not contain the information that the selector is intended to query,
leave its value null. A null value in that case is correct and expected.
 
Sometimes the target information will be contained within a set of child elements, that is fine but the
textContent should evaluate to the desired information where relevant.

Each value should be a valid CSS query.

Determine the following:
* title: This selector should return an element with text content that reflects the event title.
* image: This selector should return an img element with a src attribute referencing an image for the event.
    - Unlike the other elements, the information in the src attribute will be used and not the text content.
* cost: This selector should return an element with text content about the cost of an event or whether it is free.
* description: This selector should return an element with text content that describes the event.
    - It may contain a variety of elements.
* date: This selector should return an element with text content about the date of the event.
* startTime: This selector should return an element with text content about the time the event begins.
    - It may be the same as date.
* endTime: This selector should return an element with text content about the time the event ends.
    - Only provide this if it is different from startTime.
* ageMin: This selector should return an element with text content about the minimum age required to attend the event.
* contact: This selector should return an element with text content providing an email address, phone number,
    or social media link offered for the express purpose of contacting someone about the event.
"""
}

// @Serializable
//data class EventSchema(
//    val feed: EventFeedSelectors,
//    val page: EventPageSelectors?,
//)
//
//@Serializable
//data class EventFeedSelectors(
//    val event: String?,
//    val title: String?,
//    val link: String?,
//    val cost: String?,
//    val description: String?,
//    val time: String?,
//)