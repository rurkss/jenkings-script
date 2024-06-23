#!/bin/bash

# Set global variables
export CI=true
export RAILS_ENV=test
export DATABASE_NAME=accounting

id
pwd
ls -all
# Run the command and output to stdout
bash -lc 'bin/cobra cmd accounting ci --no-interactive'
