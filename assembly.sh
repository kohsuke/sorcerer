#!/bin/bash -ex
#
# create a distribution package
maven2 -o clean package
pushd cli
maven2 -o assembly:assembly
popd
maven2 -o assembly:assembly
