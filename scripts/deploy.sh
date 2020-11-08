#!/bin/bash

./gradlew assemble sourcesJar javadocJar generatePomFileForMavenPublication
./gradlew bintrayUpload
