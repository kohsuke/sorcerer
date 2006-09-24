#!/bin/bash -ex
#
# create a distribution package
cd target/checkout
mvn -o clean package
pushd cli
mvn -o assembly:assembly
popd
mvn -o assembly:assembly
