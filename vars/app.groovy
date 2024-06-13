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

    def parallelStages = [:]
    for (int i = 0; i < env.LOOP_COUNT.toInteger(); i++) {
        def index = i // need to capture the loop variable
        parallelStages["Nested Pod Stage ${index+1}"] = {
            podTemplate(
                label: "nested-agent-${index}",
                containers: [
                    containerTemplate(
                        name: 'busybox',
                        image: 'busybox',
                        command: 'cat',
                        ttyEnabled: true
                    )
                ]
            ) {
                node("nested-agent-${index}") {
                    stage('Sleep for 2 Minutes in Nested Pod') {
                        container('busybox') {
                            sh 'sleep 120'
                        }
                    }
                    stage('Unstash and Read File in Nested Pod') {
                        container('busybox') {
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

    stage('Run Nested Agents in Parallel') {
        parallel parallelStages
    }
}
