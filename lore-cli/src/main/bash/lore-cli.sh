#!/bin/bash
java -Xmx1G -cp 'bin/*:lib/*:modules/*' ca.on.mshri.lore.cli.Main $@
exit $?
