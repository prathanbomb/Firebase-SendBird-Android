pipeline {
  agent any
  stages {
    stage('build') {
      steps {
        sh './gradlew clean build'
      }
    }
    stage('unit test') {
      steps {
        sh './gradlew check'
      }
    }
    stage('assemble') {
      steps {
        sh './gradlew assemble'
      }
    }
    stage('archive') {
      steps {
        archiveArtifacts(artifacts: '**/*.apk', fingerprint: true)
        junit(testResults: 'app/build/reports/*.xml', allowEmptyResults: true)
      }
    }
  }
  post {
    failure {
      sh "curl https://notify-api.line.me/api/notify -H 'Authorization: Bearer kFrfPqbAvIg4KeXBWKMRRppHyJCsb3tPGTGqeg6XNKN' -F 'message=${env.JOB_NAME} #${env.BUILD_NUMBER} \nresult is failed. \n${env.BUILD_URL}' -F 'stickerId=173' -F 'stickerPackageId=2'"

    }

    success {
      sh "curl https://notify-api.line.me/api/notify -H 'Authorization: Bearer kFrfPqbAvIg4KeXBWKMRRppHyJCsb3tPGTGqeg6XNKN' -F 'message=${env.JOB_NAME} #${env.BUILD_NUMBER} \nresult is succeed. \n${env.BUILD_URL}' -F 'stickerId=525' -F 'stickerPackageId=2'"

    }

  }
}