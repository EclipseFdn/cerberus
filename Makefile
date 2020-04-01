#! /usr/bin/env bash
#*******************************************************************************
# Copyright (c) 2019 Eclipse Foundation and others.
# This program and the accompanying materials are made available
# under the terms of the Eclipse Public License 2.0
# which is available at http://www.eclipse.org/legal/epl-v20.html
# SPDX-License-Identifier: EPL-2.0
#*******************************************************************************

ifeq ($(OS),Windows_NT)
    osname = windows
		osbinext = .exe
		ifdef $(PROCESSOR_ARCHITEW6432)
			osarch = $(echo $(PROCESSOR_ARCHITEW6432) | tr A-Z a-z)
		endif
		ifdef $(PROCESSOR_ARCHITECTURE)
			osarch = $(echo $(PROCESSOR_ARCHITECTURE) | tr A-Z a-z)
		endif
else
		osname = $(shell uname -s | tr A-Z a-z)
		osarch = $(shell uname -m | tr A-Z a-z)
endif

GRAALVM_VERSION:=latest
ADOPTOPENJDK_RUNTIME_VERSION:=openjdk11
JVM_IMPL=openj9
# For debug -XshowSettings:vm -showversion 
MAVEN_OPTS:=-XshowSettings:vm -XX:+IgnoreUnrecognizedVMOptions -Xquickstart -Xms1g -Xmx2g -Xshareclasses:name=mvn,cacheDir=$(TMPDIR)/javasharedresources -Xscmx256m -XX:SharedCacheHardLimit=512m
SPOTBUGS_OPTS:=-XshowSettings:vm -XX:+IgnoreUnrecognizedVMOptions -Xquickstart -Xms1g -Xshareclasses:name=spotbugs,cacheDir=$(TMPDIR)/javasharedresources -Xscmx256m -XX:SharedCacheHardLimit=512m
SPOTBUGS_MAXHEAP:=2048

APP_VERSION:=$(shell xml sel -N mvn="http://maven.apache.org/POM/4.0.0" -t -v  "/mvn:project/mvn:version" pom.xml)
APP_ARTIFACTID:=$(shell xml sel -N mvn="http://maven.apache.org/POM/4.0.0" -t -v  "/mvn:project/mvn:artifactId" pom.xml)
APP_SHADED_CLASSIFIER:=$(shell xml sel -N mvn="http://maven.apache.org/POM/4.0.0" -t -v "/mvn:project/mvn:build/mvn:plugins/mvn:plugin[mvn:artifactId/text()='maven-shade-plugin']/mvn:executions/mvn:execution[mvn:phase/text()='package' and mvn:goals/mvn:goal/text()='shade']/mvn:configuration/mvn:shadedClassifierName" pom.xml)

ADOPTOPENJDK_HOME=$(shell ./adoptopenjdk.sh java_home $(ADOPTOPENJDK_RUNTIME_VERSION) $(JVM_IMPL))
UBERJAR:=target/$(APP_ARTIFACTID)-$(APP_VERSION)-$(APP_SHADED_CLASSIFIER).jar
NATIVE_APP_NAME=$(APP_ARTIFACTID)-$(APP_VERSION)-$(osname)-$(osarch)
NATIVE_BIN=$(NATIVE_APP_NAME)$(osbinext)
SRC=$(shell find src -type f)

export JAVA_HOME=$(ADOPTOPENJDK_HOME)

.PHONY: uberjar relocatable-$(APP_ARTIFACTID) native-$(APP_ARTIFACTID) clean deepclean display-updates

$(UBERJAR): $(SRC) pom.xml $(wildcard launchers/*)
	./adoptopenjdk.sh install $(ADOPTOPENJDK_RUNTIME_VERSION) $(JVM_IMPL)
	./mvnw verify -Dspotbugs.maxHeap="$(SPOTBUGS_MAXHEAP)" -Dspotbugs.jvmArgs="$(SPOTBUGS_OPTS)"

uberjar: $(UBERJAR)

target/runtime: $(ADOPTOPENJDK_HOME)
	$</bin/jlink -p $</jmods --compress=2 --strip-debug --no-header-files --no-man-pages --vm=server --exclude-jmod-section=man --exclude-jmod-section=headers --output target/runtime --add-modules=java.base,java.logging,java.xml,java.naming,jdk.crypto.ec,openj9.sharedclasses
	rm -f target/runtime/release target/runtime/lib/java_*.properties

target/$(APP_ARTIFACTID): $(UBERJAR) target/runtime $(wildcard target/launchers/*)
	mkdir -p $@ && touch $@
	cp -rf target/runtime/* $@
	if [[ $(osname) == "windows" ]]; then cp -f target/launchers/launcher.cmd $@/bin/$(APP_ARTIFACTID).cmd; else cp -f target/launchers/launcher $@/bin/$(APP_ARTIFACTID) && chmod u+x $@/bin/$(APP_ARTIFACTID)*; fi
	cp -f $(UBERJAR) $@/lib/$(APP_ARTIFACTID)-$(APP_VERSION).jar
	if [[ $(osname) == "windows" ]]; then pushd target && zip -r $(NATIVE_APP_NAME).zip $@ && popd; else tar zcf target/$(NATIVE_APP_NAME).tar.gz -C target $(APP_ARTIFACTID); fi

relocatable-$(APP_ARTIFACTID): target/$(APP_ARTIFACTID)

target/$(NATIVE_BIN): $(SRC) pom.xml
	./graalvm.sh install $(GRAALVM_VERSION)
	export JAVA_HOME="$$(./graalvm.sh graalvm_home)" && ./mvnw verify -Pnative -Dspotbugs.maxHeap="$(SPOTBUGS_MAXHEAP)" -Dspotbugs.jvmArgs="$(SPOTBUGS_OPTS)" -Dnative.filename=$(NATIVE_BIN)

native-$(APP_ARTIFACTID): target/$(NATIVE_BIN)

display-updates: 
	./mvnw org.codehaus.mojo:versions-maven-plugin:display-plugin-updates
	./mvnw org.codehaus.mojo:versions-maven-plugin:display-dependency-updates

clean:
	find . -name .DS_Store -delete
	./adoptopenjdk.sh install $(ADOPTOPENJDK_RUNTIME_VERSION) $(JVM_IMPL)
	./mvnw clean

deepclean: clean
	./graalvm.sh clean
	./adoptopenjdk.sh clean