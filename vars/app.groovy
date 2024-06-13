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

            stage('Create and Stash File in busybox-agent') {
                container('busybox') {
                    sh '''
                        echo "This is a test file from busybox-agent" > /tmp/testfile.txt
                        echo "busybox-agent Pod IP: ${POD_IP}" >> /tmp/testfile.txt
                        tar -cvf /tmp/testfile.tar -C /tmp testfile.txt
                    '''
                    stash includes: '/tmp/testfile.tar', name: 'testfile-tar'
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
                            stage('Unstash and Read File in Nested Pod') {
                                unstash 'testfile-tar'
                                sh '''
                                    tar -xvf /tmp/testfile.tar -C /tmp
                                    cat /tmp/testfile.txt
                                '''
                            }
                        }
                    }
                }
            }
        }
    }
}
