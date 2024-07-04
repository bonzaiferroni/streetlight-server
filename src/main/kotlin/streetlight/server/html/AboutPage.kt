package streetlight.server.html

import kotlinx.html.*

fun HTML.aboutPage() {
    basePage("Event") {
        img {
            src = "static/img/pearl.jpg"
        }
        h1("event-title") {
            +"About Streetlight"
        }
        p {
            +"Humans, we like to perform. "
            +"It is an ancient tradition, a kind of play, a gift for both the performer and audience. "
            +"The streetlight app seeks to be a bridge between the two."
        }
        p {
            +"Are you a performer? Use the app to take requests and interact with an online audience. "
            +"See who is performing in your area. Find open mics and other resources for your journey. "
            +"Get feedback, connect with fans and other performers, grow your art."
        }
        p {
            +"Streetlight is open source and community owned. "
            +"If you would like to contribute, please join us on "
            a { href = "https://github.com/bonzaiferroni/streetlight"; +"GitHub" }
            +". "
            +"Support our project through Patreon (coming soon) or "
            a { href = "https://venmo.com/colfaxband"; +"venmo" }
            +". "
        }
    }
}