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

            stage('Create and Archive File in busybox-agent') {
                container('busybox') {
                    sh '''
                        echo "This is a test file from busybox-agent" > /tmp/testfile.txt
                        echo "busybox-agent Pod IP: ${POD_IP}" >> /tmp/testfile.txt
                        tar -cvf /tmp/testfile.tar -C /tmp testfile.txt
                    '''
                    archiveArtifacts artifacts: '/tmp/testfile.tar', allowEmptyArchive: true
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
                            stage('Decompress and Read File in Nested Pod') {
                                script {
                                    // Copy the archived file from Jenkins workspace to the nested-agent pod
                                    sh 'cp ${WORKSPACE}/testfile.tar /tmp/testfile.tar'
                                    // Decompress and read the file
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
}
