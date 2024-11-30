pipeline {
    agent any

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Get Latest Changed Files') {
            steps {
                script {
                    def changedFiles = sh(script: 'git diff --name-only HEAD~1 HEAD', returnStdout: true).trim().split('\n')
                    echo "Changed files in the latest commit:"
                    changedFiles.each { file ->
                        echo "File: ${file}"
                    }
                }
            }
        }

        stage('Build Plugin') {
            steps {
                script {
                    try {
                        echo "Attempting to use Last Changes plugin for building."
                        def publisher = new com.github.jenkins.lastchanges.LastChangesPublisher()
                        def publisherScript = new com.github.jenkins.lastchanges.pipeline.LastChangesPublisherScript(publisher)
                        publisherScript.publishLastChanges()
                    } catch (Exception e) {
                        echo "Last Changes plugin not found. Skipping Last Changes step."
                    }
                }
            }
        }

        stage('Build Plugin') {
            steps {
                buildPlugin()
            }
        }
    }
}
