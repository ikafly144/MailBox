rootProject.name = "MailBox"

include("mailbox-api")
project(":mailbox-api").projectDir = file("api")
include("mailbox-paper")
project(":mailbox-paper").projectDir = file("paper")
