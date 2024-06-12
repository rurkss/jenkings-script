def call() {
    pipeline {
        agent {
            kubernetes {
                label 'maven-agent'
                defaultContainer 'maven'
                yaml """
apiVersion: v1
kind: Pod
metadata:
  labels:
    some-label: maven
spec:
  containers:
  - name: maven
    image: maven:3.8.4-jdk-11
    command:
    - cat
    tty: true
"""
            }
        }

        stages {
            stage('Run Maven Version') {
                steps {
                    container('maven') {
                        sh 'mvn --version'
                    }
                }
            }
        }
    }
}
