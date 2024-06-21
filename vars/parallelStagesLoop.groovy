def call(def testComponentNames, String webImage, int maxParallelTasks) {
    int totalTasks = testComponentNames.size()
    def semaphore = new java.util.concurrent.Semaphore(maxParallelTasks)

    stage("Parallel Tasks") {
        def parallelStages = [:]
        for (int taskIndex = 0; taskIndex < totalTasks; taskIndex++) {
            def componentName = testComponentNames[taskIndex]
            parallelStages["Test ${componentName}"] = {
                semaphore.acquire()
                try {
                    podTemplate(
                        cloud: getCloud(),
                        label: "nested-agent-${componentName}",
                        containers: multyContainerTemplate(webImage)                                              
                    ) {
                        node("nested-agent-${componentName}") {
                            container('web') {
                                stage("Download File for ${componentName}") {
                                    sh '''        
                                        wget -O artifacts.tar.gz  "https://dl.dropboxusercontent.com/scl/fi/7i3c35qq8881ikdm75qzw/jsdeps.tar.gz?rlkey=xfg8jtssr64puoecbi0itdzyh&st=ecnugmcl" --no-check-certificate                                    
                                        echo "Files after downloading:"
                                        ls -l
                                        pwd
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
                } finally {
                    semaphore.release()
                }
            }
        }
        parallel parallelStages
    }
}
