def call() {
    podTemplate(
        label: 'busybox-agent',
        containers: [
            containerTemplate(
                name: 'busybox',
                image: 'busybox',
                command: 'cat',
                ttyEnabled: true
            )
        ]
    ) {
        node('busybox-agent') {
            stage('Initial Stage') {
                container('busybox') {
                    sh 'echo "Running in the busybox-agent node"'
                }
            }
            stage('Nested Pod Stage') {
                podTemplate(
                    label: 'nested-agent',
                    containers: [
                        containerTemplate(
                            name: 'maven',
                            image: 'maven:3.8.4-jdk-11',
                            command: 'cat',
                            ttyEnabled: true
                        )
                    ]
                ) {
                    node('nested-agent') {
                        stage('Run Maven Version in Nested Pod') {
                            container('maven') {
                                sh 'mvn --version'
                            }
                        }
                        stage('Create Hello World File in Nested Pod') {
                            container('maven') {
                                sh 'echo "Hello, World!" > /tmp/hello.txt'
                            }
                        }
                    }
                }
            }
        }
    }
}
