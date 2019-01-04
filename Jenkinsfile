pipeline {
  agent any
  stages {
    stage('build') {
      steps {
        sh './gradlew clean build'
      }
    }
    stage('test') {
      parallel {
        stage('unit test') {
          steps {
            sh './gradlew test'
          }
        }
        stage('coverage test') {
          steps {
            sh './gradlew check'
          }
        }
        stage('lint check') {
          steps {
            sh './gradlew lint'
          }
        }
      }
    }
    stage('assemble') {
      steps {
        sh './gradlew assemble'
      }
    }
  }
  post {
    always {
      archiveArtifacts(artifacts: '**/*.apk', fingerprint: true)
      junit 'build/reports/**/*.xml'
    }
    failure {
        mail to: 'supitsara@digio.co.th',
             subject: "Failed Pipeline: ${currentBuild.fullDisplayName}",
             body: "Something is wrong with ${env.BUILD_URL}"
    }
    success {
        mail to: 'supitsara@digio.co.th',
             subject: "Succeed Pipeline: ${currentBuild.fullDisplayName}",
             body: "Congrats and check it out at ${env.BUILD_URL}"
    }
  }
}
