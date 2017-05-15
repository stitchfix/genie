#!/bin/bash

# Prepare a credentials file so we can publish to artifactory.  
# For now making a call and updating the gradle properties with 
# the right value.  

CURRENT_RESULT=$(curl http://vault.vertigo.stitchfix.com/secure/flotilla-auto)
sed "s/dummy/$CURRENT_RESULT/" gradle.properties > gradle.properties2
mv gradle.properties2 gradle.properties
echo "Credentials file saved"
