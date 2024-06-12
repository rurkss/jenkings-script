def call() {
    podTemplate(
        label: 'maven-agent',
        containers: [
            containerTemplate(
                name: 'maven',
                image: 'maven:3.8.4-jdk-11',
                command: 'cat',
                ttyEnabled: true
            )
        ]
    ) {
        node('maven-agent') {
            stage('Run Maven Version') {
                container('maven') {
                    sh 'mvn --version'
                }
            }
        }
    }
}
