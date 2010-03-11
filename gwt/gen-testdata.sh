#!/bin/bash
pushd ../stapler-testapp
mvn sorcerer:sorcerer
rm -rf ../gwt/war/sorcerer.Application/data
mv target/site/sorcerer ../gwt/war/sorcerer.Application/data
