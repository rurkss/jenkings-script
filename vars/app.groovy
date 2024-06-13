def getCloud() {
    return 'app-beta-hq'
}

def getResources() {
    return [
        limits: [
            memory: '500Mi'
        ],
        requests: [
            cpu: '100m',
            memory: '500Mi'
        ]
    ]
}

def call() {
    podTemplate(
        cloud: getCloud(),
        label: 'busybox-agent',
        containers: [
            containerTemplate(
                name: 'busybox',
                image: 'busybox',
                command: 'cat',
                ttyEnabled: true,
                resourceRequestMemory: getResources().requests.memory,
                resourceRequestCpu: getResources().requests.cpu,
                resourceLimitMemory: getResources().limits.memory
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
                        env.LOOP_COUNT = 5
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

    int maxParallelTasks = 3
    int totalTasks = env.LOOP_COUNT.toInteger()
    int stagesRequired = (totalTasks + maxParallelTasks - 1) / maxParallelTasks // Ceiling division

    for (int stageIndex = 0; stageIndex < stagesRequired; stageIndex++) {
        stage("Parallel Stage ${stageIndex + 1}") {
            def parallelStages = [:]
            for (int i = 0; i < maxParallelTasks; i++) {
                int taskIndex = stageIndex * maxParallelTasks + i
                if (taskIndex < totalTasks) {
                    def index = taskIndex // need to capture the loop variable
                    parallelStages["Nested Pod Task ${index + 1}"] = {
                        podTemplate(
                            cloud: getCloud(),
                            label: "nested-agent-${index}",
                            containers: [
                                containerTemplate(
                                    name: 'busybox',
                                    image: 'busybox',
                                    command: 'cat',
                                    ttyEnabled: true,
                                    resourceRequestMemory: getResources().requests.memory,
                                    resourceRequestCpu: getResources().requests.cpu,
                                    resourceLimitMemory: getResources().limits.memory
                                )
                            ]
                        ) {
                            node("nested-agent-${index}") {
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
            parallel parallelStages
        }
    }
}
