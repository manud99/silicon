#!/bin/bash

# Run all tests that include a magic wand.
# Recommended Usage:
# bash utils/scripts/run-magic-wand-tests.sh silver/src/test/resources/

if [ "$#" -ne 1 ]; then
  echo "Usage: $0 <tests-folder>"
  exit 1
fi

FOLDER=$1

if [ ! -d "$FOLDER" ]; then
  echo "Folder $FOLDER does not exist"
  exit 1
fi

# Get all tests that include a magic wand
tests=$(grep -rnwl "$FOLDER" -e '--\*' --include "*.vpr" --exclude-dir={graphs,capture_avoidance,transformations,adt})

# Split the tests into an array
tests=($tests)

# Remove the value from $FOLDER from the tests
for i in "${!tests[@]}"; do
  tests[$i]=$(echo ${tests[$i]} | sed "s|$FOLDER||")
done

# Construct the command to run the tests
tests=$(printf " -n %s" "${tests[@]}")

# Run the tests
echo "sbt \"testOnly -- $tests\""
sbt "testOnly -- $tests"
