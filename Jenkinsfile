pipeline {
    agent any

    stages {
        stage('Checkout') {
            steps {
                checkout scm // Checkout the code from the repository
            }
        }

        stage('Get Latest Changed Files') {
            steps {
                script {
                    // Get the names of the files changed in the latest commit
                    def changedFiles = sh(script: 'git diff --name-only HEAD~1 HEAD', returnStdout: true).trim().split('\n')
                    echo "Changed files in the latest commit:"
                    
                    // Loop through and print each file name
                    changedFiles.each { file ->
                        echo "File: ${file}"
                    }
                }
            }
        }

        stage('Build Plugin') {
            steps {
                // Build the plugin using the shared pipeline library
                buildPlugin()
            }
        }
    }
}
