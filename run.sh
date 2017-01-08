#!/bin/bash
pushd ~/Rogers-Communication-Console > /dev/null
if [ ! -d "target/classes" ]; then
  mvn compile
fi
if [ ! -e "runtime.cp" ]; then
  mvn dependency:resolve
  mvn -DexcludeGroupIds=com.pi4j -Dmdep.outputFile=runtime.cp dependency:build-classpath
fi
export CP=~/Rogers-Communication-Console/target/classes:`cat runtime.cp`
popd > /dev/null

nc -z 127.0.0.1 65432
if ["$?" -ne 0] then
  pushd ~/Rogers-Communication-Console/target/classes
  sudo pi4j com.venaglia.roger.console.server.ConServer
  popd
fi

echo java -cp "$CP" com.venaglia.roger.Communicator
sudo java -cp "$CP" com.venaglia.roger.Communicator

EC=$?
echo Exit code is: $EC
exit $EC
