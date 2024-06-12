#!groovy

def call() {
    pipeline {
        agent any

        stages {
            stage('Hello World') {
                steps {
                    echo 'Hello World'
                }
            }
        }
    }
}
