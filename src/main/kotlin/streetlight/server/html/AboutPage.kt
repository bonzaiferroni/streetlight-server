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
            +"My name is Luke, I'm a musician and freelance software developer. "
            +"You can find out more about me and my story at "
            a { href = "https://eosris.ing"; +"my blog" }
            +". I am traveling as a musician and developing the app at the same time. "
            +"Soon it will be available for other performers. "
            +"Use the app to take requests and interact with an online audience. "
            +"See who is performing in your area. Find open mics and other resources for your journey. "
            +"Get feedback, connect with fans and other performers, grow your art."
        }
        p {
            +"Streetlight is free, open source and community owned. "
            +"If you would like to contribute, please join us on "
            a { href = "https://github.com/bonzaiferroni/streetlight"; +"GitHub" }
            +". "
            +"Support our project through Patreon (coming soon) or "
            a { href = "https://venmo.com/colfaxband"; +"venmo" }
            +". "
        }
    }
}