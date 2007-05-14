#!/bin/bash -ex
#
# create a distribution package
cd target/checkout
mvn -o clean install
pushd uberjar
mvn -o assembly:assembly
popd
mvn -o assembly:assembly
