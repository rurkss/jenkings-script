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
                    script {
                        env.POD_IP = sh(
                            script: 'hostname -i',
                            returnStdout: true
                        ).trim()
                        echo "busybox-agent Pod IP: ${env.POD_IP}"
                    }
                }
            }

            stage('Nested Pod Stage') {
                podTemplate(
                    label: 'nested-agent',
                    containers: [
                        containerTemplate(
                            name: 'busybox',
                            image: 'busybox',
                            command: 'cat',
                            ttyEnabled: true
                        )
                    ]
                ) {
                    node('nested-agent') {
                        container('busybox') {
                            stage('Run Hello World in Nested Pod') {
                                script {
                                    echo "Using busybox-agent Pod IP: ${env.POD_IP}"
                                    sh "echo busybox-agent Pod IP: ${env.POD_IP}"
                                }
                            }
                            stage('Create Hello World File in Nested Pod') {
                                sh 'echo "Hello, World!" > /tmp/hello.txt'
                            }
                        }
                    }
                }
            }
        }
    }
}
