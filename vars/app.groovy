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
                        env.LOOP_COUNT = 3
                        echo "Number of loops: ${env.LOOP_COUNT}"
                    }
                }
            }

            stage('Create and Stash File in busybox-agent') {
                container('busybox') {
                    sh '''
                        echo "This is a test file from busybox-agent" > testfile.txt
                        echo "busybox-agent Pod IP: ${POD_IP}" >> testfile.txt
                        tar -cvf testfile.tar testfile.txt
                        echo "Files before stashing:"
                        ls -l
                    '''
                    stash includes: 'testfile.tar', name: 'testfile-tar'
                    script {
                        echo "Files stashed as 'testfile-tar'"
                    }
                }
            }
        }
    }

    for (int i = 0; i < env.LOOP_COUNT.toInteger(); i++) {
        podTemplate(
            label: "nested-agent-${i}",
            containers: [
                containerTemplate(
                    name: 'busybox',
                    image: 'busybox',
                    command: 'cat',
                    ttyEnabled: true
                )
            ]
        ) {
            node("nested-agent-${i}") {
                stage("Nested Pod Stage ${i+1}") {
                    container('busybox') {
                        stage('Sleep for 2 Seconds in Nested Pod') {
                            sh 'sleep 2'
                        }
                        stage('Unstash and Read File in Nested Pod') {
                            script {
                                echo "Files before unstashing:"
                                sh 'ls -l'
                                unstash 'testfile-tar'
                                echo "Files after unstashing:"
                                sh 'ls -l'
                            }
                            sh '''
                                echo "Nested-agent Pod IP: $(hostname -i)"
                                tar -xvf testfile.tar
                                echo "Current working directory after decompressing the archive:"
                                pwd
                                cat testfile.txt
                            '''
                        }
                    }
                }
            }
        }
    }
}
