#!/bin/bash

# Set global variables
export CI=true
export RAILS_ENV=test
export DATABASE_NAME=accounting

# Run the command and output to stdout
bin/cobra cmd accounting ci --no-interactive
