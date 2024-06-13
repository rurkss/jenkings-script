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
                        echo "This is a test file from busybox-agent" > testfile.txt
                        echo "busybox-agent Pod IP: ${POD_IP}" >> testfile.txt
                        tar -cvf testfile.tar testfile.txt
                    '''
                    stash includes: 'testfile.tar', name: 'testfile-tar'
                }
            }
        }
    }

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
            stage('Unstash and Read File in Nested Pod') {
                container('busybox') {
                    unstash 'testfile-tar'
                    sh '''
                        tar -xvf testfile.tar
                        cat testfile.txt
                        ls -all
                        echo pwd
                    '''
                }
            }
            stage('Sleep for 4 Minutes in Nested Pod') {
                sh 'sleep 240'
            }
        }
    }
}
