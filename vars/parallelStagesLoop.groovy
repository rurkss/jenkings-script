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
                        containers: multyContainerTemplate(webImage),                                          
                    ) {
                        node("nested-agent-${componentName}") {
                            container('web') {
                                stage("Unstashing Files") {
                                    parallel(
                                        'Config.yml': {
                                            unstash 'config-yml'
                                            sh '''        
                                                mv config.yml /home/app/src/config/
                                                ls -l /home/app/src/config/
                                            '''                                            
                                        },
                                        'Database.yml': {
                                            unstash 'database-yml'
                                            sh '''        
                                                mv database.yml /home/app/src/config/
                                                ls -l /home/app/src/config/
                                            '''
                                        },
                                        'Ldap.yml': {
                                            unstash 'ldap-yml'
                                            sh '''        
                                                mv ldap.yml /home/app/src/config/
                                                ls -l /home/app/src/config/
                                            '''
                                        },
                                        'JsDeps': {
                                            // unstash 'jsdeps'
                                            sh '''        
                                                wget -O jsdeps.tar.gz "https://dl.dropboxusercontent.com/scl/fi/4jfv1f1asfvp2qld4ez4c/jsdeps2.tar.gz?rlkey=3r58xp81zjm48jgw8ndiqvsya&st=kvogr9dx" --no-check-certificate                                                  
                                                echo "Files after downloading:"
                                                ls -l
                                                pwd
                                            '''
                                            sh 'ls -l'
                                        }
                                    )
                                }  
                                stage("Extracting/Copying Artifacts") {
                                    script {
                                        def scriptContent = libraryResource 'scripts/extract_artifacts.sh'
                                        writeFile file: 'extract_artifacts.sh', text: scriptContent
                                        sh '''
                                            ls -all
                                            ls -l /home/app/src/components/accounting
                                            sh extract_artifacts.sh                                                
                                            echo "Current working directory after running the script:"
                                            pwd
                                            ls -l /home/app/src/components/accounting
                                        '''
                                    }
                                }
                                
                                stage("JS Yarn Check") {
                                    sh '''
                                        cd /home/app/src/components/accounting && \
                                        ([ -z $CI ] && yarn check --integrity 2> /dev/null || yarn install) && \
                                        set -e && yarn lint && \
                                        ([ ! -f .flowconfig ] || yarn flow check) && \
                                        yarn test
                                    '''
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
