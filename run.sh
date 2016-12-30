#!/bin/bash
pushd ~/Rogers-Communication-Console > /dev/null
if [ ! -d "target/classes" ]; then
  mvn compile
fi
if [ ! -e "runtime.cp" ]; then
  mvn dependency:resolve
  mvn -DexcludeGroupIds=com.pi4j -Dmdep.outputFile=runtime.cp dependency:build-classpath
fi
export CP=~/Rogers-Communication-Console/target/classes:`cat runtime.cp`:/opt/pi4j/lib/'*'
popd > /dev/null
#gpio load spi || echo "Failed to load kernel SPI drivers" && exit 1
#echo java -cp "$CP" com.venaglia.roger.Communicator
#sudo java -cp "$CP" com.venaglia.roger.Communicator
echo java -cp "$CP" com.venaglia.roger.ui.pi.DisplayBus
sudo java -cp "$CP" com.venaglia.roger.ui.pi.DisplayBus

EC=$?
echo Exit code is: $EC
exit $EC
