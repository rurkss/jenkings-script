def call(def testComponentNames, String webImage, int maxParallelTasks) {
    int totalTasks = testComponentNames.size()
    int stagesRequired = (totalTasks + maxParallelTasks - 1) / maxParallelTasks // Ceiling division

    for (int stageIndex = 0; stageIndex < stagesRequired; stageIndex++) {
        stage("Parallel Stage ${stageIndex + 1}") {
            def parallelStages = [:]
            for (int i = 0; i < maxParallelTasks; i++) {
                int taskIndex = stageIndex * maxParallelTasks + i
                if (taskIndex < totalTasks) {
                    def componentName = testComponentNames[taskIndex] // get the component name
                    parallelStages["Test ${componentName}"] = {
                        podTemplate(
                            cloud: getCloud(),
                            label: "nested-agent-${componentName}",
                            containers: multyContainerTemplate(webImage),
                            volumes: [
                                emptyDirVolume(mountPath: '/var/lib/mysql', memory: false, name: 'mysql-storage'),
                                emptyDirVolume(mountPath: '/data', memory: false, name: 'redis-storage'),
                                emptyDirVolume(mountPath: '/data', memory: false, name: 'psql-storage')
                            ]
                        ) {
                            node("nested-agent-${componentName}") {
                                container('web') {
                                    stage("Download File for ${componentName}") {
                                        sh '''
                                            wget -O artifacts.tar.gz  "https://dl.dropboxusercontent.com/scl/fi/7i3c35qq8881ikdm75qzw/jsdeps.tar.gz?rlkey=xfg8jtssr64puoecbi0itdzyh&st=ecnugmcl" --no-check-certificate
                                            echo "Files after downloading:"
                                            ls -l
                                        '''
                                    }
                                    stage("Unstash Artifacts for ${componentName}") {
                                        script {
                                            echo "Files before unstashing:"
                                            sh 'ls -l'
                                            unstash 'sample-txt'
                                            // unstash 'artifacts-tar-gz'
                                            echo "Files after unstashing:"
                                            sh 'ls -l'
                                        }
                                    }
                                    stage("Extract Artifacts for ${componentName}") {
                                        script {
                                            def scriptContent = libraryResource 'scripts/extract_artifacts.sh'
                                            writeFile file: 'extract_artifacts.sh', text: scriptContent
                                            sh '''
                                                ls -all
                                                sh extract_artifacts.sh                                                
                                                echo "Current working directory after running the script:"
                                                pwd
                                                ls -l
                                            '''
                                        }
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
