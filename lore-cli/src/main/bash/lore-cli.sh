#!/bin/bash
java -Xmx1G -cp '.:./modules/*' -jar bin/lore-cli-0.1-SNAPSHOT.jar $@
exit $?
