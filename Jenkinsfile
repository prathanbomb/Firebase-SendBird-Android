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
    failure {
      def token = 'kFrfPqbAvIg4KeXBWKMRRppHyJCsb3tPGTGqeg6XNKN'
      def url = 'https://notify-api.line.me/api/notify'
      def message = "${env.JOB_NAME} #${env.BUILD_NUMBER} \nresult is ${result}. \n${env.BUILD_URL}"
      def stickerId = '173' : '525'
      def stickerPackageId = '2'
      sh "curl ${url} -H 'Authorization: Bearer ${token}' -F 'message=${message}' -F 'stickerId=${stickerId}' -F 'stickerPackageId=${stickerPackageId}'"
    }
    success {
      archiveArtifacts(artifacts: '**/*.apk', fingerprint: true)
      junit '**/*.xml'
      def token = 'kFrfPqbAvIg4KeXBWKMRRppHyJCsb3tPGTGqeg6XNKN'
      def url = 'https://notify-api.line.me/api/notify'
      def message = "${env.JOB_NAME} #${env.BUILD_NUMBER} \nresult is ${result}. \n${env.BUILD_URL}"
      def stickerId = '525'
      def stickerPackageId = '2'
      sh "curl ${url} -H 'Authorization: Bearer ${token}' -F 'message=${message}' -F 'stickerId=${stickerId}' -F 'stickerPackageId=${stickerPackageId}'"
    }
  }
}
