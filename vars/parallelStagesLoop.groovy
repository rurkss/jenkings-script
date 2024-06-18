def call(totalTasks, maxParallelTasks) {
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
                                multyContainerTemplate(index)
                            ]
                        ) {
                            node("nested-agent-${index}") {
                                container('busybox') {
                                    stage('Download File in Nested Pod') {
                                        sh '''
                                            wget -O artifacts.tar.gz  "https://dl.dropboxusercontent.com/scl/fi/7i3c35qq8881ikdm75qzw/jsdeps.tar.gz?rlkey=xfg8jtssr64puoecbi0itdzyh&st=ecnugmcl" --no-check-certificate
                                            echo "Files after downloading:"
                                            ls -l
                                            pwd
                                        '''
                                    }
                                    stage('Unstash Artifacts in Nested Pod') {
                                        script {
                                            echo "Files before unstashing:"
                                            sh 'ls -l'
                                            unstash 'sample-txt'
                                            // unstash 'artifacts-tar-gz'
                                            echo "Files after unstashing:"
                                            sh 'ls -l'
                                        }
                                    }
                                    stage('Extract Artifacts in Nested Pod') {
                                        sh '''
                                            pwd
                                            ls -l
                                            sh extract_artifacts.sh                                            
                                            echo "Current working directory after running the script:"
                                            ls -l
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
