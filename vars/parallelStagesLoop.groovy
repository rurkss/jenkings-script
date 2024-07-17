def call(def testComponentNames, String webImage, int maxParallelTasks) {
    int totalTasks = testComponentNames.size()    
    def semaphore = new java.util.concurrent.Semaphore(maxParallelTasks)

    stage("Parallel Tasks") {
        def parallelStages = [:]
        for (int taskIndex = 0; taskIndex < totalTasks; taskIndex++) {
            def componentName = testComponentNames[taskIndex]
            def label = "standalone-${componentName}"
            parallelStages["Test ${componentName}-${taskIndex}"] = {   
                semaphore.acquire() 
                try {
                    podTemplate(
                        inheritFrom: '',
                        label: label,                        
                        containers: multyContainerTemplate(webImage),
                        cloud: getCloud(),
                        volumes: [
                            configMapVolume(mountPath: '/etc/mysql/my.cnf', subPath: 'my.cnf', configMapName: 'mysql-config'),
                            emptyDirVolume(mountPath: '/var/lib/mysql', memory: true),  
                            emptyDirVolume(mountPath: '/home/app/.local', memory: true),  
                            emptyDirVolume(mountPath: '/home/app/bundle', memory: true),                 
                        ]                          
                    ) {
                        node(label) {
                            container('web') {
                                stage("Unstashing Files") {
                                    parallel(
                                        'Volume': {
                                            sh '''  
                                              ls -all /home/config
                                            '''                                            
                                        },
                                        'Config.yml': {
                                            unstash 'config-yml'
                                            sh '''  
                                              cp /home/config/config.yml /home/app/src/config/                                                
                                            '''                                            
                                        },
                                        'Database.yml': {
                                            unstash 'database-yml'
                                            sh '''                                                        
                                              cp /home/config/database.yml /home/app/src/config/                                                
                                            '''
                                        },
                                        'Ldap.yml': {
                                            unstash 'ldap-yml'
                                            sh '''                                                       
                                              cp /home/config/ldap.yml /home/app/src/config/                                                
                                            '''
                                        },
                                        //  'JsDeps': {
                                        //     // unstash 'jsdeps'
                                        //     sh '''
                                        //         wget -O jsdeps.tar.gz "https://dl.dropboxusercontent.com/scl/fi/4jfv1f1asfvp2qld4ez4c/jsdeps2.tar.gz?rlkey=3r58xp81zjm48jgw8ndiqvsya&st=kvogr9dx" --no-check-certificate 
                                        //         tar -xf jsdeps.tar.gz
                                        //         mv node_modules /home/app/src/ &
                                        //         for dir in components/*; do
                                        //           folder_name=$(basename "$dir")
                                        //           mv "$dir/node_modules" "/home/app/src/components/$folder_name/" &
                                        //         done
                                        //         wait
                                        //         rm -rf jsdeps.tar.gz
                                        //     '''                                          
                                        // },                                         
                                    )
                                }
                                stage("JS Test") {
                                    sh """
                                        cd /home/app/src/components/${componentName} && \
                                        ([ -z $CI ] && yarn check --integrity 2> /dev/null || yarn install) && \
                                        set -e && yarn lint && \
                                        ([ ! -f .flowconfig ] || yarn flow check) && \
                                        yarn test
                                    """
                                }
                                
                                stage("Ruby Bundle and Tests") {
                                  sh """
                                      cd /home/app/src/components/${componentName} && \
                                      
                                      start_time=\$(date +%s) && \
                                      [ -n "\$CI" ] && export BUNDLE_FROZEN=false && \
                                      bundle check || bundle install && \
                                      end_time=\$(date +%s) && \
                                      echo "Step 1 (Bundle check/install) took \$(expr \$end_time - \$start_time) seconds." && \
                                      
                                      start_time=\$(date +%s) && \
                                      bundle lock --add-platform arm64-darwin-22 \
                                                              arm64-darwin-23 \
                                                              ruby \
                                                              x86_64-darwin-22 \
                                                              x86_64-darwin-23 \
                                                              x86_64-linux && \
                                      end_time=\$(date +%s) && \
                                      echo "Step 2 (Bundle lock) took \$(expr \$end_time - \$start_time) seconds." && \
                                      
                                      start_time=\$(date +%s) && \
                                      ls -all && \
                                      if [ -f bin/schema ]; then
                                          echo "Schema exists"
                                          bin/schema
                                      else
                                          echo "Schema does not exist"
                                          [ ! -f spec/dummy/db/name ] || RAILS_ENV=test DISABLE_DATABASE_ENVIRONMENT_CHECK=true bin/rake app:db:prepare
                                      fi && \
                                      end_time=\$(date +%s) && \
                                      echo "Step 3 (Schema/migration) took \$(expr \$end_time - \$start_time) seconds." && \
                                      
                                      start_time=\$(date +%s) && \
                                      OUTPUT="\$(bin/yard)" && \
                                      echo "\${OUTPUT}" && \
                                      if [ "\$(echo \${OUTPUT} | grep warn | wc -l)" -gt 0 ]; then
                                          echo "Documentation warnings occurred"
                                          exit 1
                                      fi && \
                                      end_time=\$(date +%s) && \
                                      echo "Step 4 (Yard) took \$(expr \$end_time - \$start_time) seconds." && \
                                      
                                      start_time=\$(date +%s) && \
                                      bin/rubocop --display-cop-names --extra-details --display-style-guide --config .rubocop.yml && \
                                      end_time=\$(date +%s) && \
                                      echo "Step 5 (Rubocop) took \$(expr \$end_time - \$start_time) seconds." && \
                                      
                                      start_time=\$(date +%s) && \
                                      echo "Time now is \$(date -Iseconds)" && \
                                      bin/rspec spec && \
                                      end_time=\$(date +%s) && \
                                      echo "Step 6 (RSpec) took \$(expr \$end_time - \$start_time) seconds."
                                  """
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
